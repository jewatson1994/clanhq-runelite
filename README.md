# ClanHQ RuneLite Verifier

This is the development foundation for ClanHQ rank verification.

The current build is deliberately preview-only. It reads the logged-in
player's RuneScape name, total level, combat level, and currently equipped
items after the player clicks **Capture Current Character**. It does not send
data to ClanHQ, Discord, or any other service.

## Why preview-only first?

Iron Drop ranks include evidence that cannot be proven from equipped items
alone, including banked items, collection-log counts, quests, diaries,
prayers, combat achievements, raid KC, and cosmetics. ClanHQ will eventually
own those rules and combine evidence from approved sources. The RuneLite
plugin should collect evidence, not independently award ranks.

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

## Privacy boundary

- Only the local logged-in player is inspected.
- Capture happens only after an explicit button click.
- The preview lists the exact data collected.
- The current transport never performs a network request.
