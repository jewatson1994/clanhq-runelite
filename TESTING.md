# ClanHQ RuneLite live test checklist

Use a development Discord server, development database, and localhost API.
Never test unpublished changes against production member data.

## Build and launch

```powershell
$env:JAVA_HOME = (Get-ChildItem .\localonly\temurin17 -Directory | Select-Object -First 1).FullName
.\runelite-plugin\gradlew.bat -p .\runelite-plugin clean test
.\runelite-plugin\gradlew.bat -p .\runelite-plugin run
```

## Shell and Overview

- Confirm the top navigation is one compact icon row at minimum panel width.
- Confirm tooltips identify Overview, Character, Events, Bingo, and Dailies.
- Overview should show paired status, device name, and all active linked RSNs.
- Toggle **Show currency balance** and confirm the private balance hides and
  reappears without changing the wallet.
- Build the plugin JAR and confirm all five navigation icons load through the
  classpath stream rather than a filesystem URL.

## Pairing and eligibility

- Run `/plugin pair`, configure `http://localhost:8765`, enter the code, and
  open Overview and select Pair Installation.
- Confirm the code is single-use, expires after ten minutes, and a lost response
  can retry the same pending installation without duplicating the link reward.
- Pair a second device for the same Discord identity. It must not repeat the
  once-per-wallet reward.
- Inactivate one of two linked RSNs. Pairing must remain valid while the other
  is active.
- Inactivate, archive, or ban the final active RSN. Existing installation
  requests must return HTTP 401 and all identity installations must be revoked.
- Rejoining requires a new pairing code and does not repeat the link reward.
- Select **Disconnect This Device**, cancel once, then confirm. The server token
  must be revoked, local pairing fields cleared, and subsequent authenticated
  requests rejected until the device pairs again.

## Character Sync

- Open the full bank and click **Sync Character Data**.
- Cancel the disclosure once and confirm that no capture or request occurs.
- Confirm again and verify the dialog identifies the configured destination and
  complete bank, inventory, and equipment data categories.
- Confirm ClanHQ stores bank, inventory, and equipment item IDs, names, and
  quantities, including items unrelated to current promotion or Bingo rules.
- Retry the same request after simulating a lost response. It must not create a
  duplicate snapshot.
- Submit a later lower quantity or omit an old item. The immutable snapshots
  remain unchanged and accumulated ownership retains the highest observed
  quantity.
- Character Sync must not publish a Bingo Discord embed.

## Daily Tasks

- Refresh and compare all three tasks, reset time, completion, reward, and
  placement with Discord.
- Claim Skilling, PvM, and Minigame independently. One category's cooldown must
  not block a different category.
- A completed category button must disable after refresh.
- Confirm the all-three bonus is issued only when the final category completes.
- Retry from RuneLite and Discord; no task, placement, or completion reward may
  be issued twice.

## Events

- Enter a non-Bingo event code under **ClanHQ Settings → Clan event**.
- Confirm the panel shows event details, registers the logged-in RSN, displays
  a custom team name, and contains no internal `Activity: Waiting` label.
- Enter a Bingo code in Events. ClanHQ must direct the member to the Bingo tab.

## Bingo

- Enable server and plugin Bingo, then enter the code under
  **ClanHQ Settings → Bingo**.
- Confirm the panel reports participation, custom team name, board item/task
  count, and active drop tracking.
- Confirm eligible Loot Tracker drops route to the team's configured Discord
  channel and non-board items are ignored.
- With screenshots enabled, verify the image watermark contains event, RSN,
  drop, quantity, and UTC timestamp without writing an image to disk.
- On a fresh profile, confirm Bingo screenshots are disabled by default.
- Click Bingo Character Submit and confirm the panel changes from
  `Not submitted` to `Baseline captured` and exactly one baseline embed is
  posted for the immutable complete snapshot.
- Submit again while the event is active and confirm it is recorded as a
  checkpoint without replacing the baseline. End the event, refresh the board,
  submit the final check, and confirm the panel reports `Finalized`.
- Confirm the embed reports clue/casket/key reserves plus Wintertodt points,
  Guardians of the Rift searches, and Tempoross permits. Checkpoint/final
  embeds should show baseline-to-current reserve changes.
- Verify scheduled, ended, invalid, server-disabled, and missing-code messages
  are distinct and actionable.
