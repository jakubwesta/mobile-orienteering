param(
    [Parameter(Mandatory=$true)]
    [string]$version,
    
    [Parameter(Mandatory=$false)]
    [ValidateSet('internal', 'alpha', 'beta', 'production')]
    [string]$track = 'internal',
    
    [Parameter(Mandatory=$false)]
    [switch]$PublishToPlayStore
)

Write-Host "Creating release v$version..." -ForegroundColor Yellow
Write-Host "  Track: $track" -ForegroundColor Cyan
Write-Host "  Play Store: $(if ($PublishToPlayStore) { 'YES' } else { 'NO (GitHub only)' })" -ForegroundColor $(if ($PublishToPlayStore) { 'Green' } else { 'Yellow' })
Write-Host ""

# Check if version exists in CHANGELOG.md
$changelogPath = "CHANGELOG.md"
if (Test-Path $changelogPath) {
    $changelogContent = Get-Content $changelogPath -Raw
    if ($changelogContent -notmatch "\[$version\]") {
        Write-Host "WARNING: Version $version not found in CHANGELOG.md" -ForegroundColor Yellow
        Write-Host "Please update CHANGELOG.md before releasing" -ForegroundColor Yellow
        $continue = Read-Host "Continue anyway? (y/N)"
        if ($continue -ne "y") {
            Write-Host "Release cancelled" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "Version $version found in CHANGELOG.md" -ForegroundColor Green
    }
} else {
    Write-Host "WARNING: CHANGELOG.md not found" -ForegroundColor Yellow
}

Write-Host ""

# Production warning (only if publishing to Play Store)
if ($track -eq "production" -and $PublishToPlayStore) {
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "  WARNING: PRODUCTION RELEASE" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""
    Write-Host "This will publish to the PRODUCTION track on Google Play Store!" -ForegroundColor Yellow
    Write-Host "Type 'PRODUCTION' to confirm: " -NoNewline
    $confirm = Read-Host
    if ($confirm -ne "PRODUCTION") {
        Write-Host "Release cancelled" -ForegroundColor Red
        exit 1
    }
}

# Create tag with track info and publish flag
# Format: v1.0.0-alpha-publish or v1.0.0 (GitHub only, internal)
$tagParts = @("v$version")
if ($track -ne "internal") {
    $tagParts += $track
}
if ($PublishToPlayStore) {
    $tagParts += "publish"
}
$tagName = $tagParts -join "-"

Write-Host "Creating tag $tagName..." -ForegroundColor Cyan
$tagMessage = "Release version $version"
if ($track -ne "internal") {
    $tagMessage += " ($track track)"
}
if ($PublishToPlayStore) {
    $tagMessage += " with Play Store publishing"
} else {
    $tagMessage += " (GitHub only)"
}
git tag -a "$tagName" -m "$tagMessage"

Write-Host "Pushing tag..." -ForegroundColor Cyan
git push origin "$tagName"

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Release Created!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Version: $version" -ForegroundColor Cyan
Write-Host "Tag: $tagName" -ForegroundColor Cyan
Write-Host "Track: $track" -ForegroundColor Cyan
Write-Host "GitHub Release: $version" -ForegroundColor Cyan
Write-Host "Files: app-release-$version.aab, app-release-$version.apk" -ForegroundColor Cyan
Write-Host "Play Store: $(if ($PublishToPlayStore) { 'Will be published to ' + $track + ' track' } else { 'NOT published (GitHub only)' })" -ForegroundColor $(if ($PublishToPlayStore) { 'Green' } else { 'Yellow' })
Write-Host ""
Write-Host "Check GitHub Actions for build progress:" -ForegroundColor White
Write-Host "  https://github.com/yourusername/MobileOrienteering/actions" -ForegroundColor DarkGray
Write-Host ""
