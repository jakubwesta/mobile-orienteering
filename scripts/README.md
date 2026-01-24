# Release Scripts

## release.ps1

Create a new release and trigger GitHub Actions build.

**Parameters:**
- `-version` (required): Version number (e.g., "1.0.0")
- `-track` (optional): Play Store track - `internal`, `alpha`, `beta`, or `production` (default: `internal`)
- `-PublishToPlayStore` (optional): Flag to publish to Play Store (default: GitHub only)

**Examples:**
```powershell
# GitHub only (default)
.\release.ps1 -version "1.0.0"

# GitHub only with alpha track
.\release.ps1 -version "1.0.0" -track alpha

# GitHub + Play Store (internal track)
.\release.ps1 -version "1.0.0" -PublishToPlayStore

# GitHub + Play Store (alpha track)
.\release.ps1 -version "1.0.0" -track alpha -PublishToPlayStore

# GitHub + Play Store (production track)
.\release.ps1 -version "1.0.0" -track production -PublishToPlayStore
```

**Requirements:**
- Version must exist in CHANGELOG.md
- Production releases require typing "PRODUCTION" to confirm

**What it does:**
1. Validates version exists in CHANGELOG.md
2. Creates and pushes git tag
3. Triggers GitHub Actions workflow
4. Builds signed AAB and APK
5. Creates GitHub Release with files
6. Optionally publishes to Play Store

## rollback-release.ps1

Delete a GitHub release and its git tags.

**Parameters:**
- `-version` (required): Version number to rollback (e.g., "1.0.0")
- `-track` (optional): Track used in release - `internal`, `alpha`, `beta`, or `production` (default: `internal`)

**Examples:**
```powershell
# Rollback internal track release
.\rollback-release.ps1 -version "1.0.0"

# Rollback alpha track release
.\rollback-release.ps1 -version "1.0.0" -track alpha

# Rollback production release
.\rollback-release.ps1 -version "1.0.0" -track production
```

**What it does:**
1. Deletes local git tag
2. Deletes remote git tag
3. Deletes GitHub Release

**Note:** Play Store releases cannot be deleted. Once published to Play Store, releases are permanent.
