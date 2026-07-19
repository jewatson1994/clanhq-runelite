# Publishing ClanHQ

The `runelite-plugin` directory is already a standalone repository root. Its
public repository must contain this directory's contents at the root; do not
publish the entire ClanHQ bot repository.

## Prerequisites

- Install GitHub CLI and authenticate with `gh auth login`.
- Confirm the full test suite passes.
- Confirm the production API uses a stable HTTPS hostname.
- Complete one end-to-end submission using a development Discord server, then
  one controlled submission using the production endpoint.

## Create the public repository

From the ClanHQ repository root, create a branch whose root is this directory:

```bash
git subtree split --prefix=runelite-plugin -b codex/clanhq-runelite-release
gh repo create jewatson1994/clanhq-runelite \
  --public \
  --description "RuneLite clan tools for ClanHQ"
git push https://github.com/jewatson1994/clanhq-runelite.git \
  codex/clanhq-runelite-release:main
```

The public repository must not contain `.env`, ClanHQ databases, Discord
credentials, API destinations, installation tokens, or Jagex Launcher credentials.

## Pre-submission review

Before requesting Plugin Hub review:

1. Verify the public repository's Actions build passes.
2. Check that README, PRIVACY, LICENSE, and TESTING are current.
3. Verify plugin settings start with a blank API URL and installation token.
4. Capture screenshots that contain no private Discord channels, API codes,
   account credentials, or unrelated bank contents.
5. Ask for informal feedback in RuneLite's development Discord if external
   evidence submission needs clarification.

## Plugin Hub manifest

Fork `runelite/plugin-hub`, create a branch from its current master branch, and
add `plugins/clanhq` containing:

```text
repository=https://github.com/jewatson1994/clanhq-runelite.git
commit=FULL_PUBLIC_REPOSITORY_COMMIT_HASH
```

Open one pull request explaining that the plugin:

- captures only the logged-in player's evidence after explicit actions;
- sends complete character contents only after Character Sync or Bingo
  Character Submit is clicked;
- ships with no endpoint or installation token;
- submits only after the player clicks the button; and
- never awards a rank or performs game actions.

When the plugin changes, push a new public commit and update the manifest's full
commit hash in a Plugin Hub pull request.
