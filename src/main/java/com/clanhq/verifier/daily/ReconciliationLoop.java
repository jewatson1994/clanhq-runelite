package com.clanhq.verifier.daily;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Single-flight authoritative reads, with a trailing refresh and independent recovery timer. */
final class ReconciliationLoop<T>
{
    static final int INTERVAL_SECONDS = 30;
    static final int TIMEOUT_SECONDS = 20;
    private final ScheduledExecutorService executor;
    private final Consumer<Runnable> dispatch;
    private final Supplier<CompletableFuture<T>> fetch;
    private final BiConsumer<T, Throwable> receive;
    private ScheduledFuture<?> periodic;
    private ScheduledFuture<?> timeout;
    private boolean running;
    private boolean busy;
    private boolean pending;
    private long generation;

    ReconciliationLoop(ScheduledExecutorService executor, Consumer<Runnable> dispatch,
        Supplier<CompletableFuture<T>> fetch, BiConsumer<T, Throwable> receive)
    {
        this.executor = executor;
        this.dispatch = dispatch;
        this.fetch = fetch;
        this.receive = receive;
    }

    synchronized void start()
    {
        stop();
        running = true;
        if (executor != null)
        {
            periodic = executor.scheduleWithFixedDelay(this::refresh,
                INTERVAL_SECONDS, INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
        refresh();
    }

    synchronized void stop()
    {
        running = false;
        generation++;
        busy = false;
        pending = false;
        if (periodic != null) { periodic.cancel(false); }
        if (timeout != null) { timeout.cancel(false); }
    }

    synchronized void refresh()
    {
        if (!running) { return; }
        if (busy)
        {
            pending = true;
            return;
        }
        busy = true;
        long request = ++generation;
        if (executor != null)
        {
            timeout = executor.schedule(() -> complete(request, null,
                new TimeoutException("Authoritative refresh timed out")),
                TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        try
        {
            fetch.get().whenComplete((value, error) -> complete(request, value, error));
        }
        catch (RuntimeException error)
        {
            complete(request, null, error);
        }
    }

    private void complete(long request, T value, Throwable error)
    {
        dispatch.accept(() -> {
            synchronized (this)
            {
                if (!running || !busy || request != generation) { return; }
                if (timeout != null) { timeout.cancel(false); }
                busy = false;
                // Retire this request even if a late response arrives after timeout.
                generation++;
                try
                {
                    receive.accept(value, error);
                }
                finally
                {
                    if (pending)
                    {
                        pending = false;
                        refresh();
                    }
                }
            }
        });
    }
}
