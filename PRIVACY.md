# Privacy

ClanHQ Rank Review does not contain a ClanHQ server address, clan code, Discord
credential, or RuneScape credential. Connection settings are blank by default.

## Local collection

Evidence is collected only after the player clicks a capture button. The
plugin reads the logged-in character, supported account progression, relevant
equipped/inventory/bank items, configured Collection Log pages, owned boat
configuration, and supported POH facilities. Unrelated bank items are discarded
immediately. Evidence remains in memory and is cleared when the session resets
or RuneLite closes.

## External requests

Verify Character requests public raid kill counts from the RuneScape hiscores
and synchronized Collection Log information from TempleOSRS for the logged-in
RSN. The plugin does not send bank, inventory, equipment, boat, or POH contents
to those services.

Nothing is submitted to ClanHQ automatically. If a player explicitly clicks
Submit Promotion Review, the plugin sends the RSN, levels, capture timestamp,
and calculated requirement results to the HTTPS server configured by that
player. Raw bank, inventory, and equipment lists are not included. The panel
shows the destination hostname before submission.

Players should configure only a destination supplied by a clan they trust.
HTTP destinations are rejected except for localhost development.
