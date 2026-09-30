package com.clanhq.verifier.daily;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Delayed;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReconciliationLoopTest
{
    private final ManualScheduler scheduler = new ManualScheduler();
    private final List<CompletableFuture<Integer>> requests = new ArrayList<>();
    private final List<Integer> applied = new ArrayList<>();
    private final List<Throwable> errors = new ArrayList<>();
    private final ReconciliationLoop<Integer> loop = new ReconciliationLoop<>(scheduler,
        Runnable::run, () -> {
            CompletableFuture<Integer> request = new CompletableFuture<>();
            requests.add(request);
            return request;
        }, (value, error) -> {
            if (error != null) { errors.add(error); }
            else { applied.add(value); }
        });

    @Test
    public void missedNotificationConvergesWhileClientRemainsOpen()
    {
        loop.start();
        requests.get(0).complete(0);
        // Backend commits; no immediate event is delivered.
        scheduler.tick();
        requests.get(1).complete(1);
        assertEquals(List.of(0, 1), applied);
    }

    @Test
    public void rapidClaimsDuringStaleReadQueueOneTrailingAuthoritativeRead()
    {
        loop.start();
        loop.refresh();
        loop.refresh();
        loop.refresh();
        assertEquals(1, requests.size());
        requests.get(0).complete(0);
        assertEquals(2, requests.size());
        requests.get(1).complete(3);
        assertEquals(List.of(0, 3), applied);
    }

    @Test
    public void offlineAndExceptionalReadRecoverOnNextTick()
    {
        loop.start();
        requests.get(0).completeExceptionally(new IllegalStateException("offline"));
        scheduler.tick();
        requests.get(1).complete(2);
        assertEquals(1, errors.size());
        assertEquals(List.of(2), applied);
    }

    @Test
    public void hungReadTimesOutAndLateResponseCannotResurrectStaleState()
    {
        loop.start();
        scheduler.expire();
        scheduler.tick();
        requests.get(1).complete(4);
        requests.get(0).complete(0);
        assertEquals(1, errors.size());
        assertEquals(List.of(4), applied);
    }

    @Test
    public void restartIgnoresPreviousSessionAndCancelsTimers()
    {
        loop.start();
        Tick oldPeriodic = scheduler.periodic;
        loop.stop();
        assertTrue(oldPeriodic.isCancelled());
        loop.start();
        requests.get(1).complete(5);
        requests.get(0).complete(0);
        assertEquals(List.of(5), applied);
        loop.stop();
        scheduler.tick();
        assertEquals(2, requests.size());
    }

    @Test
    public void synchronousFailureDoesNotWedgeRecovery()
    {
        AtomicInteger attempts = new AtomicInteger();
        ReconciliationLoop<Integer> failing = new ReconciliationLoop<>(scheduler,
            Runnable::run, () -> {
                if (attempts.getAndIncrement() == 0) { throw new IllegalStateException(); }
                return CompletableFuture.completedFuture(9);
            }, (value, error) -> { if (error == null) { applied.add(value); } });
        failing.start();
        scheduler.tick();
        assertEquals(List.of(9), applied);
    }

    private static final class ManualScheduler extends ScheduledThreadPoolExecutor
    {
        Tick periodic;
        Tick timeout;
        ManualScheduler() { super(1); }
        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initial,
            long delay, TimeUnit unit)
        {
            assertEquals(30, unit.toSeconds(delay));
            periodic = new Tick(command);
            return periodic;
        }
        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit)
        {
            timeout = new Tick(command);
            return timeout;
        }
        void tick() { if (!periodic.isCancelled()) { periodic.command.run(); } }
        void expire() { timeout.run(); }
    }

    private static final class Tick extends FutureTask<Void> implements ScheduledFuture<Void>
    {
        final Runnable command;
        Tick(Runnable command) { super(command, null); this.command = command; }
        public long getDelay(TimeUnit unit) { return 0; }
        public int compareTo(Delayed other) { return 0; }
    }
}
