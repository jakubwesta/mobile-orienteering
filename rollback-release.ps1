param(
    [Parameter(Mandatory=$true)]
    [string]$version
)

Write-Host "Rolling back release v$version..." -ForegroundColor Yellow
Write-Host ""

# Delete local tag
Write-Host "Deleting local tag..." -ForegroundColor Cyan
git tag -d "v$version"

# Delete remote tag
Write-Host "Deleting remote tag..." -ForegroundColor Cyan
git push origin --delete "v$version"

# Delete GitHub Release
Write-Host "Deleting GitHub Release..." -ForegroundColor Cyan
gh release delete "v$version" --yes

Write-Host ""
Write-Host "Release v$version deleted!" -ForegroundColor Green
Write-Host "You can now recreate it with: .\release.ps1 -version $version" -ForegroundColor White
