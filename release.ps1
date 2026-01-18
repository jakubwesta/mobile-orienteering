param(
    [Parameter(Mandatory=$true)]
    [string]$version
)

Write-Host "Creating release v$version..." -ForegroundColor Yellow
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