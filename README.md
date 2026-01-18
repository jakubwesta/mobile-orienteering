# Mobile Orienteering App

## About

Mobile Orienteering is a comprehensive Android application designed for orienteering training and competition. 
Track your runs, navigate checkpoints, and analyze your performance and view detailed statistics.

### Key Features

- **Interactive Map View** - View orienteering maps with real-time GPS positioning
- **Checkpoint Navigation** - Mark and navigate to checkpoints during your run
- **Run Tracking** - Record your activities with detailed time and distance metrics
- **Performance Analytics** - Analyze your runs with comprehensive statistics and timelines
- **Map Library** - Create custom orienteering maps
- **Cloud Sync** - Sync your maps and activities across devices (requires account, not yet publicly available)

## Download

- **Google Play Store**: _Coming soon_
- **Website download**: _Coming soon_
- **Direct APK**: [Latest Release](https://github.com/jakubwesta/mobile-orienteering/releases/latest)

## Tech Stack

### Android App
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM
- **Dependency Injection**: Hilt
- **Database**: Room
- **Networking**: Retrofit + OkHttp
- **Maps**: LibreMap
- **Min SDK**: 26 (Android 8)
- **Target SDK**: 35 (Android 15)

### Website
- **Framework**: React + TypeScript
- **Build Tool**: Vite
- **UI Library**: shadcn/ui + Tailwind CSS
- **Hosting**: GitHub Pages

## Development

All scripts used for development and publishing are in /scripts

### Setup

1. Clone the repository:
   ```powershell
   git clone https://github.com/jakubwesta/mobile-orienteering.git
   cd mobile-orienteering
   ```
2. Create `local.properites` in root directory. This file can store placeholders
   ```properties
   GOOGLE_WEB_CLIENT_ID=your_google_client_id
   RELEASE_BASE_URL=your_api_base_url
   KEYSTORE_FILE=path/to/keystore.jks
   KEYSTORE_PASSWORD=your_keystore_password
   KEY_ALIAS=your_key_alias
   KEY_PASSWORD=your_key_password
   ```
3. Sync Gradle and run the app

### Website development

1. Clone the repository:
   ```powershell
   git clone https://github.com/jakubwesta/mobile-orienteering.git
   cd mobile-orienteering/docs-src
   ```
2. Initialize pnpm:
   ```powershell
   pnpm install
   pnpm run dev
   ```
3. Visit http://localhost:5173 to see the site.

### Release workflow

1. Edit app/build.gradle.kts. Change ```versionName = "x.y.z"```
2. Commit and push changes
3. Run the release script ```.\scripts\release.ps1 -version "x.y.z"```
4. GitHub Actions automatically:
    - Builds AAB
    - Builds APK
    - Extracts mapping.txt
    - Creates GitHub Release
    - Uploads all files
5. Done. APK can be downloaded, and AAB uploaded to GooglePlay Console

### Rollback release workflow

1. Make sure you have GitHub CLI installed:
   - ```winget install GitHub.cli```
   - ```gh auth login```
2. Run the rollback-release script ```.\scripts\rollback-release.ps1 -version "x.y.z"```
3. Done. Release tags are deleted and release is removed from GitHub

## Authors

- Jakub Westa
- Dawid Pilarski

## License

Elastic License 2.0. Details are in LICENSE file
