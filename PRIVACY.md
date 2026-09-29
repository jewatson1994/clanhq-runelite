# Privacy

ClanHQ does not contain a ClanHQ server address, installation token, Discord
credential, or RuneScape credential. Connection settings are blank by default.

## Local collection

Character data is collected only after the player clicks Character Sync or
Bingo Character Submit. Those actions read the complete available bank,
inventory, and equipped-item contents. Evidence remains in memory until the
request completes or RuneLite closes. Before capture, RuneLite shows the
configured destination and data categories and requires confirmation.

## External requests

Verify Character requests public raid kill counts from the RuneScape hiscores
and synchronized Collection Log information from TempleOSRS for the logged-in
RSN. The plugin does not send bank, inventory, equipment, boat, or POH contents
to those services.

Character Sync sends the RSN, levels, capture timestamp, submission ID, and
complete bank, inventory, and equipment contents to the configured ClanHQ
server. Promotion requirements are not present in or evaluated by RuneLite.

If a player explicitly clicks Bingo Character Submit, the plugin sends the RSN,
event code, capture timestamp, submission ID, levels, and complete bank,
inventory, and equipment contents (item IDs, names, and quantities) to the
configured ClanHQ server. ClanHQ retains the original snapshot for audit and an
additions-only verified-item history for the linked Discord identity.

Players should configure only a destination supplied by a clan they trust.
HTTP destinations are rejected except for localhost development.

Pairing sends a short-lived Discord-generated code, a user-visible device
label, and a locally generated random installation token to the configured
ClanHQ server. The code is consumed once. ClanHQ stores only a one-way hash of
the installation token; RuneLite retains the token in a secret configuration
field. Daily-task requests identify the linked Discord wallet through that
token and do not send bank contents.

The Overview module can disconnect the current device. ClanHQ revokes the
server-side token before RuneLite removes the local pairing. ClanHQ also
automatically revokes all installations after the linked Discord identity no
longer owns an active clan member.

Bingo screenshots are disabled by default. When a player enables them, an
accepted board drop may include a watermarked gameplay screenshot captured in
memory and sent to ClanHQ. The plugin does not save that screenshot to disk.
