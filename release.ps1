param(
    [Parameter(Mandatory=$true)]
    [string]$version
)

Write-Host "Creating release for version $version"

# Create tag
git tag -a "v$version" -m "Release version $version"

# Push tag
git push origin "v$version"

Write-Host "Tag v$version created and pushed!"
Write-Host "Check GitHub Actions for build progress"
