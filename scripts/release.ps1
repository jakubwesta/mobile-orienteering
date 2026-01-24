param(
    [Parameter(Mandatory=$true)]
    [string]$version
)

Write-Host "Creating release v$version..." -ForegroundColor Yellow
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

# Create tag
Write-Host "Creating tag..." -ForegroundColor Cyan
git tag -a "v$version" -m "Release version $version"

# Push tag
Write-Host "Pushing tag..." -ForegroundColor Cyan
git push origin "v$version"

Write-Host ""
Write-Host "Release v$version created!" -ForegroundColor Green
Write-Host "Check GitHub Actions for build progress" -ForegroundColor White
