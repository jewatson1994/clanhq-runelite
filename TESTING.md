# ClanHQ Rank Review live test checklist

Run the automated suite before opening RuneLite:

```powershell
$env:JAVA_HOME = (Get-ChildItem -LiteralPath ..\localonly\temurin17 -Directory | Select-Object -First 1).FullName
.\gradlew.bat clean test --no-daemon
```

Start the development client with `.\gradlew.bat run`, log into the intended
character, and open the ClanHQ Rank Review panel. Use a test ClanHQ API URL only;
submission only posts a staff review and never changes a rank.

### Discord promotion review

- Configure a localhost development API URL and matching clan code, or use the
  HTTPS development endpoint. Expected: Submit Promotion Review remains disabled
  until Verify Character and Capture Bank & Gear are complete.
- Leave POH uncaptured for a lower-rank applicant. Expected: submission remains
  available. A Dragon review must still be rejected if Maxed POH evidence is
  missing.
- Submit an active Iron Drop RSN whose next rank has no missing or uncaptured
  requirements. Expected: ClanHQ posts one embed in the configured promotions
  channel and RuneLite reports success.
- Submit with a wrong clan code, non-member RSN, Iron Drip RSN, or missing next-
  rank evidence. Expected: no Discord message is created and RuneLite shows the
  server's rejection reason.
- Click submit again in the same completed session. Expected: the button remains
  disabled. Replaying the same submission ID returns the original review rather
  than creating a duplicate.

## Session safety

1. Capture Character.
2. Confirm the RSN, total level, combat level, diary totals, highest verified
   rank, and next rank appear.
3. Click Verify Character and confirm the other capture buttons remain
   disabled until the local, hiscore, and TempleOSRS reads finish.
4. Click Reset Session. Confirm all statuses, captured evidence, and calculated
   ranks reset.
5. Log out during Gear capture. Confirm capture stops with a cancellation
   message and no evidence is retained for another character.

## Evidence sources

### Character

- Expected: the logged-in RSN, levels, supported prayer unlocks, and diary
  completion totals appear.
- Expected: any saved owned boats are also captured without opening a Sailing
  interface. No separate Prayer or Boat button is shown.
- Expected: Grandmaster Combat Achievements reflects the account's completed
  CA tier state.
- Expected: Raid KC and a fresh TempleOSRS Collection Log are fetched by the
  same action.
- Confirm no other RuneLite account or character can be selected.

### Prayers and boats

- Expected: Piety passes only when King's Ransom and Knight Waves Training
  Grounds are complete; the prayer does not need to be active. Rigour, Deadeye,
  and Mystic Vigour reflect their unlock varbits.
- Expected: owned boat configurations are read from saved Sailing variables. An
  account with no owned boats is a completed read with a missing requirement,
  not an uncaptured source.

### Gear

- Open the bank, then click Capture Bank & Gear.
- Expected: bank, inventory, and equipped gear are captured immediately.
- Close the bank and try again. Capture must reject the attempt rather than use
  stale bank data.
- Expected: only rank-relevant items appear. Unrelated bank contents such as
  coins, food, and supplies must not appear.
- Confirm complete Ancestral, fortified Masori, Oathplate/Torva, and Avernic
  tread sets are recognized without mixing pieces from different sets.

### Raid KC

- Verify Character should show normal, expert, and hard-mode raid counts and
  the combined total.
- Test an unavailable/private hiscore result. It must reveal Fetch Raid KC as
  a retry and become staff review, not zero KC or a failed qualification.

### TempleOSRS Collection Log

- Synchronize the account through the TempleOSRS RuneLite plugin, then click
  Verify Character. Expected: COX, TOB, TOA, Yama, Doom, and the global obtained
  count are captured without opening the in-game Collection Log.
- Confirm the status reports how many hours ago TempleOSRS was synchronized.
- Use old synchronized data. Expected: its items are accepted and the sync age
  is displayed. Use an unsynchronized RSN and confirm the five manual Collection
  Log buttons appear.
- For fallback, open each exact in-game page and use its corresponding button.
  Earlier pages must remain in the same session.
- Confirm raid unique counts combine bank ownership and matching log entries
  without counting the same unique twice.
- Verify Doom only passes after cloth, Avernic treads, and eye are all present.
- Verify Rite of Vile Transference passes only after it appears as acquired on
  the captured Yama page.
- Verify TOA cosmetics report all eight required entries and identify any that
  are missing.
- For Zenyte, COX, TOB, and TOA should show acquired/total slots. Green log
  passes only when all three counts are complete; Sinhaza and Icthlarin KC
  shrouds must not affect those counts.
- For manual fallback, confirm unacquired slots are not mistaken for acquired
  items and the selected page title is detected correctly.

### Boat verification

- Click Verify Character while logged in; no Sailing interface is required.
- Expected: every owned boat slot lists its boat type, hull, mast and sails,
  helm, and keel using the names from RuneLite's Sailing DB tables.
- A Skiff or Sloop passes its maxed-core check only with Rosewood hull and
  mast/sails plus Dragon helm and keel.
- Confirm a wooden Skiff is reported missing, not passed. No screenshot or
  image data should be captured.

### POH

- Enter the logged-in player's own POH in build mode.
- Expected: capture recognizes the spiritual fairy ring/tree, ornate jewellery
  box, occult altar, crystalline portal nexus, and ornate rejuvenation pool.
- Test a guest house or non-build mode. Capture must reject it.

## Completionism

- Confirm Completionism requests COX, TOB, and TOA Collection Log
  captures.
- Metamorphic dust should pass from COX; Sanguine dust and both ornament kits
  should pass from TOB even when their physical items are not in the bank.
- With the bank captured, verify separate results for Metamorphic dust,
  Sanguine dust, both ornament kits, Saturated heart, Amulet of rancour, three
  Twisted ancestral colour kits, Champion's cape, and the Expert dragon archer
  hat.
- Dragon Collection Log rank should pass at 1,200 obtained slots. The 750-slot
  check uses the same window-header count. The TOA set passes
  only when all eight configured cosmetic/transmog entries are acquired.

## Cumulative rank result

- Capture enough evidence to satisfy Opal but leave at least one Jade
  requirement missing. Expected: `Highest verified rank: Opal`, `Next rank:
  Jade`, and only Jade's outstanding requirements are listed.
- Satisfy an isolated later-rank requirement while Opal or Jade remains
  incomplete. The highest verified rank must not skip the incomplete rank.
- Confirm passed next-rank requirements contribute to `Passed` but do not
  appear under `Next Rank requires`.
- Confirm missing automated requirements and staff-review requirements are
  listed separately.
- When every configured rank passes, expected: the next-rank label says all
  configured ranks are verified.

## Submission boundary

Submit Promotion Review sends the versioned evidence payload only after explicit
player action. ClanHQ authenticates the shared clan code, validates exact active
Iron Drop membership and the configured next rank, rate-limits duplicates, and
posts a staff review. It does not update ClanHQ, WOM, or Discord roles.
