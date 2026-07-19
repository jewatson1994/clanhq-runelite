# ClanHQ

ClanHQ is a modular RuneLite shell for opt-in clan tools. Its icon navigation
opens Overview, Character Sync, Events, Bingo, and Daily Tasks without placing
rank rules in the plugin. ClanHQ remains the authority for membership,
promotion requirements, rewards, and event configuration.

## Overview and pairing

Overview is the normal onboarding surface. Run `/plugin pair` in Discord,
enter `https://verify.clanhq.dev` and the private one-time code in the plugin
settings, open **Overview**, then select **Pair Installation**. The code expires
after 10 minutes
and cannot link a second device. RuneLite creates a unique revocable device
token locally; ClanHQ stores only its hash.

Once paired, the panel shows the current skilling, PvM, and minigame tasks and
their local reset time. Each category has its own claim button. Claims use the
same server-side WOM verification, linked-RSN policy, cooldown, placement, and
idempotent DripDrops ledger as Discord. Overview can show the linked RSNs and
private wallet balance; the balance can be hidden in settings.

Pairing remains valid while the Discord identity owns at least one active
ClanHQ member. ClanHQ rejects and revokes installations after the last linked
member becomes inactive, archived, or banned.

Overview also provides **Disconnect This Device**. Disconnecting asks for
confirmation, revokes the server-side installation token, and removes the
local pairing. Reconnecting requires a new single-use pairing code.

## Character Sync

Character Sync is player-initiated and reads the complete currently available
bank, inventory, and equipment contents. ClanHQ stores the immutable snapshot
and updates its additions-only verified-item collection. Promotion rules and
adjudication remain entirely server-side. Before every submission, RuneLite
names the configured destination, lists the data categories, and requires an
explicit confirmation.

## Events

Events connects RuneLite to non-Bingo competitions created with ClanHQ. A
player enters the event's code, and the panel displays the
event type, randomized or selected target, inclusive dates, and current status.
When logged in, RuneLite registers the exact active ClanHQ RSN as a participant
and displays the server-assigned team name without receiving Discord channel IDs.

## Bingo

Bingo is disabled by default. When enabled, it downloads the active board from
the configured ClanHQ server and submits only matching events from RuneLite's
built-in Loot Tracker. This includes Loot Tracker-supported raids, reward
interfaces, clues, minigames, pickpockets, and NPC or player kills. The server
owns the item list, validates active membership, and sends accepted drops to the
configured Discord drops channel. Arbitrary inventory changes are never treated
as drops.

Gameplay screenshots are disabled by default. A player may opt in to capture
RuneLite's next rendered frame for accepted Bingo drops. The image is resized
and watermarked with the event name/code, RSN, drop, quantity, and UTC
timestamp, then uploaded from memory without saving a local screenshot. Team
events are routed by ClanHQ to the server-configured team channel.

## Trust boundary

The plugin captures observations only after an explicit member action or an
enabled Bingo event. It does not contain promotion requirements and never
changes a rank, wallet, WOM group, or Discord role directly. Every request uses
a member-controlled, revocable installation credential and a clan-configured
HTTPS destination.

## Development

Requirements:

- JDK 17 or newer

From this directory:

```powershell
.\gradlew.bat run
```

RuneLite starts in developer mode with ClanHQ loaded. Log into a test account,
open the ClanHQ sidebar panel, and test only against the development API.

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
- Capture happens only after an explicit button click.
- The ClanHQ API destination is configured by each member in RuneLite settings.
- No ClanHQ destination or installation token is bundled with the public plugin. The
  connection settings are blank until a member configures them.
- HTTPS is required except for localhost development; changing settings never submits evidence automatically.
- Bank, inventory, and equipped gear are captured once while the bank is open.
- Character Sync and Bingo Character Submit intentionally send the complete
  snapshot only after their respective buttons are clicked and the disclosure
  dialog is confirmed.
- In the Bingo module, Character Submit is a separate explicit action. It sends
  the complete current bank, inventory, and equipment snapshot to ClanHQ for
  permanent additions-only verification and auditing. It also sends supported
  reward counters for Wintertodt, Guardians of the Rift, and Tempoross.
- Character Submit is phase-aware per Bingo and linked Discord identity. The
  first submission is the baseline, active-event submissions are checkpoints,
  and an ended event accepts one final reconciliation snapshot.
