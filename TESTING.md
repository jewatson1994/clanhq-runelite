# ClanHQ Verifier live test checklist

Run the automated suite before opening RuneLite:

```powershell
$env:JAVA_HOME = (Get-ChildItem -LiteralPath ..\localonly\temurin17 -Directory | Select-Object -First 1).FullName
.\gradlew.bat clean test --no-daemon
```

Start the development client with `.\gradlew.bat run`, log into the intended
character, and open the ClanHQ Verifier panel. Use a test ClanHQ API URL only;
the current build never submits evidence.

## Session safety

1. Select a rank and capture Character.
2. Confirm the RSN, total level, combat level, diary totals, and selected rank.
3. Start a slow source such as Raid KC and confirm the rank selector and other
   capture buttons remain disabled until it finishes.
4. Change rank after it finishes. Confirm all prior statuses and preview data
   reset.
5. Log out during Gear capture. Confirm capture stops with a cancellation
   message and no evidence is retained for another character.

## Evidence sources

### Character

- Expected: the logged-in RSN, levels, supported prayer unlocks, and diary
  completion totals appear.
- Confirm no other RuneLite account or character can be selected.

### Prayers

- Capture after the account is fully logged in.
- Expected: Piety, Rigour, Deadeye, and Mystic Vigour reflect their unlock
  varbits. Rite of Vile Transference remains staff review because RuneLite does
  not currently expose a stable unlock signal.

### Gear

- Start capture, then open Equipment, Inventory, and Bank during the 15-second
  window.
- Expected: only rank-relevant items appear. Unrelated bank contents such as
  coins, food, and supplies must not appear.
- Confirm complete Ancestral, fortified Masori, Oathplate/Torva, and Avernic
  tread sets are recognized without mixing pieces from different sets.

### Raid KC

- Expected: the public hiscore result shows normal, expert, and hard-mode raid
  counts and the combined total.
- Test an unavailable/private hiscore result. It must become staff review, not
  zero KC or a failed qualification.

### Collection Log

- Open one supported page before each capture: Chambers of Xeric, Theatre of
  Blood, Tombs of Amascut, or Doom of Mokhaiotl.
- Expected: the page name appears and only acquired visible items are retained.
- Capture multiple pages and confirm earlier pages remain in the same session.
- Confirm raid unique counts combine bank ownership and matching log entries
  without counting the same unique twice.
- Verify Doom only passes after cloth, boots, and eye are all present.
- This collector needs special attention: confirm unacquired slots are not
  mistaken for acquired items and the selected page title is detected correctly.

### Boat

- Open Sailing boat selection or customisation for a Skiff and capture it.
- Open the equivalent Sloop panel and capture again.
- Expected: both vessel types and visible panel details accumulate. Cargo item
  contents must not appear.
- The requirement remains staff review even when both are captured; the plugin
  must not claim that an upgrade state is maxed.

### POH

- Enter the logged-in player's own POH in build mode.
- Expected: capture recognizes the spiritual fairy ring/tree, ornate jewellery
  box, occult altar, crystalline portal nexus, and ornate rejuvenation pool.
- Test a guest house or non-build mode. Capture must reject it.

## Completionism

- With the bank captured, verify separate results for Metamorphic dust,
  Sanguine dust, both ornament kits, Saturated heart, Amulet of rancour, three
  Twisted ancestral colour kits, Champion's cape, and the Expert dragon archer
  hat.
- Dragon Collection Log rank and the complete Tombs of Amascut
  cosmetic/transmog set must remain staff review.

## Known boundary

The Submit Review Ticket button is intentionally disabled. No HTTP request,
ClanHQ mutation, Discord ticket, or rank award occurs in this build. Submission
requires a versioned API contract, member validation, authentication, rate
limiting, and an agreed Discord ticket destination before it is safe to enable.
