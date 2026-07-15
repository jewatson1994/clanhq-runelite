# ClanHQ RuneLite Verifier

This is the development foundation for ClanHQ rank verification.

The current build is deliberately preview-only. A 15-second development capture session
accumulates equipment and inventory evidence, captures only rank-relevant
bank items when the bank is opened, and remembers supported prayers activated
during the session. It also reads levels and authoritative hard/elite
achievement-diary completion state. Capture also performs a public RuneScape
hiscore lookup for the selected character's raid completion counts. It does
not send captured evidence to ClanHQ, Discord, or any other service.

The checklist covers every Iron Drop progression rank from Opal through
Zenyte. The player selects the requested rank before capture, and the panel
shows only that rank's evidence. Character, prayers, gear, raid KC, Collection
Log, boat, and owner-POH are independent evidence stages with their own capture status.
Collection Log capture reads the visible Doom or raid page and can accumulate
multiple pages. Captured raid pages are combined with current item evidence
for unique-count requirements without double-counting the same unique. Boat
capture reads only visible text from RuneLite's Sailing
boat-selection or customisation interface and can accumulate Skiff and Sloop
panels; staff still confirms that each vessel is maxed. POH capture requires
the player's house in build mode and scans the loaded scene for all five configured facilities. ClanHQ
will validate the member's existing
rank and ensure the request is the next valid progression step when ticket
submission is connected. Colonel is intentionally excluded because it is a
retired-staff designation rather than a progression rank.

The plugin reports evidence status, not an official qualification decision.
Only ClanHQ and the eventual staff ticket approval can verify or award a rank.

Requirements RuneLite cannot yet prove reliably (for example collection-log
counts, raid KC, POH layout, and some compound gear sets) are shown as
`[CHECK]`. They are never treated as passed or guessed from skill levels.

## Why preview-only first?

Iron Drop ranks include evidence that cannot be proven from equipped items
alone. ClanHQ will eventually own the final rules and combine evidence from
approved sources. The RuneLite plugin collects transparent evidence; it does
not independently award ranks.

Rank progression remains cumulative in ClanHQ. The local plugin evaluates the
selected target rank; it does not attempt to replace ClanHQ's authoritative
record of previously awarded ranks.

RuneLite's Plugin Hub also restricts plugins that expose player information
over HTTP. Keeping submission behind `VerificationTransport` lets us evaluate
a private, user-consented integration separately from a Plugin-Hub-safe
manual flow.

## Development

Requirements:

- JDK 17 or newer

From this directory:

```powershell
.\gradlew.bat run
```

RuneLite starts in developer mode with `ClanHQ Verifier` loaded. Log into a
test account, open the ClanHQ sidebar panel, and capture the current character.

### Jagex Account login

Jagex Accounts cannot sign directly into the standalone development client.
RuneLite provides a development-only credential handoff:

1. Close the development client.
2. Open **RuneLite (configure)** from the Windows Start menu.
3. Add `--insecure-write-credentials` to **Client arguments** and save.
4. Launch RuneLite once through the official Jagex Launcher and select the
   test character.
5. Close that RuneLite window, then run `.\gradlew.bat run` again.

The launcher writes `%USERPROFILE%\.runelite\credentials.properties`, which
the development client uses for login. Treat that file like a password. Never
share it or commit it. When development is finished, remove the client
argument and delete `credentials.properties`. If it may have been exposed,
use **End sessions** in the RuneScape account settings.

## Privacy boundary

- Only the local logged-in player is inspected.
- The captured RSN is sent to RuneScape's public hiscore service for raid KC.
- Capture happens only after an explicit button click.
- Changing the selected rank starts a fresh evidence session.
- Reset Session clears all locally accumulated evidence for the selected rank.
- Evidence from different RSNs cannot be combined in one session.
- The ClanHQ API destination is configured by each member in RuneLite settings.
- HTTPS is required except for localhost development; changing settings never submits evidence automatically.
- Collection Log capture stores only visible acquired item names and quantities.
- Boat capture stores visible Sailing panel text, not cargo item contents.
- POH capture stores only the five configured facility results and requires owner build mode.
- Each development capture session lasts 15 seconds and discards its evidence afterward.
- Bank capture retains and displays only items used by configured rank rules;
  unrelated bank contents are discarded immediately.
- The preview lists the exact data collected.
- The current transport never performs a network request.
