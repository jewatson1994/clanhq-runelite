# Publishing ClanHQ Rank Review

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
git subtree split --prefix=runelite-plugin -b codex/clanhq-rank-review-release
gh repo create jewatson1994/clanhq-rank-review \
  --public \
  --description "RuneLite evidence capture for ClanHQ staff rank reviews"
git push https://github.com/jewatson1994/clanhq-rank-review.git \
  codex/clanhq-rank-review-release:main
```

The public repository must not contain `.env`, ClanHQ databases, Discord
credentials, API destinations, clan codes, or Jagex Launcher credentials.

## Pre-submission review

Before requesting Plugin Hub review:

1. Verify the public repository's Actions build passes.
2. Check that README, PRIVACY, LICENSE, and TESTING are current.
3. Verify plugin settings start with a blank API URL and clan code.
4. Capture screenshots that contain no private Discord channels, API codes,
   account credentials, or unrelated bank contents.
5. Ask for informal feedback in RuneLite's development Discord if external
   evidence submission needs clarification.

## Plugin Hub manifest

Fork `runelite/plugin-hub`, create a branch from its current master branch, and
add `plugins/clanhq-rank-review` containing:

```text
repository=https://github.com/jewatson1994/clanhq-rank-review.git
commit=FULL_PUBLIC_REPOSITORY_COMMIT_HASH
```

Open one pull request explaining that the plugin:

- captures only the logged-in player's evidence after explicit actions;
- discards unrelated bank contents;
- sends no raw item list to ClanHQ;
- ships with no endpoint or clan code;
- submits only after the player clicks the button; and
- never awards a rank or performs game actions.

When the plugin changes, push a new public commit and update the manifest's full
commit hash in a Plugin Hub pull request.
