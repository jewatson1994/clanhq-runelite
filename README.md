# ClanHQ RuneLite Verifier

This is the development foundation for ClanHQ rank verification.

Verify Character reads local
account, prayer, diary, Combat Achievement, and saved-boat state, then performs
public RuneScape hiscore and TempleOSRS Collection Log lookups for that RSN.
With the bank open, Capture Bank & Gear retains equipment, inventory, and only
rank-relevant bank items. Evidence remains local until the player explicitly
uses Submit Promotion Review with a configured ClanHQ API destination and clan
code. Submission posts to ClanHQ's promotions feed and never changes ranks.

The checklist covers every Iron Drop progression rank from Opal through
Zenyte. A single screen exposes every evidence collector and recalculates the
entire cumulative rank ladder after each capture. It shows the highest fully
verified rank, the next rank, that rank's missing requirements, and anything
that still needs staff review. The two core actions are Verify Character and
Capture Bank & Gear. Capture POH Instance is rank-dependent and is needed only
when the requested rank requires Maxed POH evidence. TempleOSRS supplies COX, TOB,
TOA, Yama, Doom, and the global obtained-slot count whenever that account has
been synchronized. The snapshot age is displayed but does not invalidate these
permanent unlocks. Missing or unavailable TempleOSRS data reveals the existing
per-page RuneLite capture buttons as fallbacks. Raid pages retain
acquired and total slot counts and combine with current item evidence without
double-counting the same unique. Verify Character also reads RuneLite's
authoritative owned-boat variables and Sailing DB
tables for all five slots. A maxed Skiff or Sloop requires a Rosewood hull,
Rosewood mast and sails, Dragon helm, and Dragon keel. Visible Sailing panel
text is retained only as a diagnostic fallback. POH capture requires the
player's house in build mode and scans the loaded scene for all five configured
facilities. Colonel is intentionally excluded because it is a
retired-staff designation rather than a progression rank. ClanHQ validates the
exact active Iron Drop RSN and selects only the next rank configured for that
member before publishing a review.

Completionism bank items are reported individually. The global Collection Log
count verifies Dragon rank (1,200 slots) and the 750-slot requirement. TOA capture checks
the four boss remnants, Menaphite ornament kit, Cursed phalanx, Ancient
remnant, and Masori crafting kit. COX capture can prove Metamorphic dust; TOB
capture can prove Sanguine dust and both ornament kits. COX, TOB, and TOA captures can verify raid
green logs from acquired-versus-total visible slots. Sailing core upgrade state
is verified directly from the player's owned-boat records.
Raid KC capes and shrouds are excluded from green-log totals.
The Yama page verifies Rite of Vile Transference. Piety is verified from
completed King's Ransom and Knight Waves Training Grounds progression, so the
prayer does not need to be activated. Grandmaster Combat Achievements are read
from RuneLite's authoritative tier-completion state.

The plugin reports evidence status, not an official qualification decision.
Only ClanHQ and staff approval can verify or award a rank.

Requirements RuneLite cannot yet prove reliably are shown as `[CHECK]`. They are never
treated as passed or guessed from unrelated evidence.

## Trust boundary

Iron Drop ranks include evidence that cannot be proven from equipped items
alone. The RuneLite plugin collects transparent evidence but does not
independently award ranks. ClanHQ validates membership and the requested next
rank, then gives staff the final decision in Discord.

Rank progression is cumulative. The plugin never skips a failed or unverified
rank even if evidence happens to satisfy a later rank. ClanHQ remains the
authority for recorded membership and awarded ranks; the plugin reports the
highest rank supported by the current evidence session.

Submission remains behind `VerificationTransport`, is initiated only by the
player, and requires a clan-configured HTTPS destination. This keeps the
network boundary isolated from evidence capture and preserves a local-only
mode if Plugin Hub review requires it.

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
- The captured RSN is sent to RuneScape's public hiscore service for raid KC
  and TempleOSRS's public API for synchronized Collection Log evidence.
- Capture happens only after an explicit button click.
- Reset Session clears all locally accumulated evidence and rank calculations.
- Evidence from different RSNs cannot be combined in one session.
- The ClanHQ API destination is configured by each member in RuneLite settings.
- No ClanHQ destination or clan code is bundled with the public plugin. The
  connection settings are blank until a member configures them.
- HTTPS is required except for localhost development; changing settings never submits evidence automatically.
- TempleOSRS capture retains only the five configured categories, their item
  names/counts, global obtained-slot count, and last-sync time. Manual fallback
  capture retains only the visible supported page.
- Boat capture stores owned boat configurations and optional visible Sailing
  panel text, not cargo item contents.
- POH capture stores only the five configured facility results and requires owner build mode.
- Bank, inventory, and equipped gear are captured once while the bank is open.
  Evidence remains only in the local panel unless the player explicitly clicks
  Submit Promotion Review; it is otherwise cleared when the session resets or
  RuneLite closes.
- Bank capture retains and displays only items used by configured rank rules;
  unrelated bank contents are discarded immediately.
- The preview lists the exact local data collected. Submission sends only the
  calculated rank requirement results, RSN, levels, and capture time to the
  configured ClanHQ endpoint; it does not send the raw bank or inventory list.
