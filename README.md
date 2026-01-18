# Mobile Orienteering App

### Release workflow

1. Edit app/build.gradle.kts. Change versionName = "x.y.z"
2. Commit and push changes
3. Run the release script ```.\release.ps1 -version "x.y.z"```
4. GitHub Actions automatically:
    - Builds AAB
    - Builds APK
    - Extracts mapping.txt
    - Creates GitHub Release
    - Uploads all files
5. Done. APK can be downloaded, and AAB uploaded to GooglePlay Console