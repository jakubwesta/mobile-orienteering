param(
    [Parameter(Mandatory=$true)]
    [string]$version,
    
    [Parameter(Mandatory=$false)]
    [ValidateSet('internal', 'alpha', 'beta', 'production')]
    [string]$track = 'internal'
)

Write-Host "========================================" -ForegroundColor Yellow
Write-Host "  Rolling Back Release" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Yellow
Write-Host ""
Write-Host "Version: $version" -ForegroundColor Cyan
Write-Host "Track: $track" -ForegroundColor Cyan
Write-Host ""

# Determine possible tag names (with or without publish flag)
$tagBase = if ($track -eq "internal") { "v$version" } else { "v${version}-${track}" }
$tagWithPublish = "${tagBase}-publish"

Write-Host "This will delete:" -ForegroundColor Yellow
Write-Host "  - Local git tag(s)" -ForegroundColor White
Write-Host "  - Remote git tag(s)" -ForegroundColor White
Write-Host "  - GitHub Release: $version" -ForegroundColor White
Write-Host ""
Write-Host "Note: Play Store releases cannot be deleted" -ForegroundColor DarkGray
Write-Host ""

$confirm = Read-Host "Continue? (y/N)"
if ($confirm -ne "y") {
    Write-Host "Rollback cancelled" -ForegroundColor Red
    exit 1
}

Write-Host ""

# Try to delete both tag variants (with and without -publish)
foreach ($tagName in @($tagBase, $tagWithPublish)) {
    # Delete local tag
    Write-Host "Checking local tag: $tagName..." -ForegroundColor Cyan
    $localTagExists = git tag -l "$tagName"
    if ($localTagExists) {
        git tag -d "$tagName" 2>&1 | Out-Null
        Write-Host "  Local tag deleted" -ForegroundColor Green
    } else {
        Write-Host "  Local tag not found" -ForegroundColor DarkGray
    }
    
    # Delete remote tag
    Write-Host "Checking remote tag: $tagName..." -ForegroundColor Cyan
    $remoteTagExists = git ls-remote --tags origin "refs/tags/$tagName" 2>&1
    if ($remoteTagExists) {
        git push origin --delete "$tagName" 2>&1 | Out-Null
        Write-Host "  Remote tag deleted" -ForegroundColor Green
    } else {
        Write-Host "  Remote tag not found" -ForegroundColor DarkGray
    }
}

# Delete GitHub Release
Write-Host ""
Write-Host "Deleting GitHub Release: $version..." -ForegroundColor Cyan
$releaseExists = gh release view "$version" 2>&1
if ($LASTEXITCODE -eq 0) {
    gh release delete "$version" --yes 2>&1 | Out-Null
    Write-Host "  GitHub Release deleted" -ForegroundColor Green
} else {
    Write-Host "  GitHub Release not found" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Rollback Complete" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Git tags and GitHub Release have been deleted." -ForegroundColor Green
Write-Host ""
Write-Host "To recreate this release:" -ForegroundColor Cyan
Write-Host "  GitHub only:" -ForegroundColor White
Write-Host "    .\scripts\release.ps1 -version $version -track $track" -ForegroundColor DarkGray
Write-Host ""
Write-Host "  With Play Store:" -ForegroundColor White
Write-Host "    .\scripts\release.ps1 -version $version -track $track -PublishToPlayStore" -ForegroundColor DarkGray
Write-Host ""
