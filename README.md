# DSU Sideloader

A simple app made to help users easily install GSIs via DSU's Android feature.
<div>
<img src="https://raw.githubusercontent.com/VegaBobo/DSU-Sideloader/master/other/preview_1.png" alt="preview" width="200"/>  
<img src="https://raw.githubusercontent.com/VegaBobo/DSU-Sideloader/master/other/preview_2.png" alt="preview" width="200"/>  
</div>

## Requirements
- Android 10 or higher
- Unlocked Bootloader
- Device with Dynamic Partitions
- A GSI you want to use!

Community GSIs: https://github.com/phhusson/treble_experimentations/wiki/Generic-System-Image-%28GSI%29-list

Google GSIs: https://developer.android.com/topic/generic-system-image/releases

**Remember to use GSIs compatible with your architeture, vndk implementation..*

## Downloads

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/vegabobo.dsusideloader/)

Or download the latest APK from the [Releases Section](https://github.com/Muchyutaka/DSU-SL/releases/latest).
Each release carries **both** a release APK and a debug APK plus a `SHA256SUMS` file:

- `dsu-sideloader-<version>-release.apk` – optimized build, this is the one to install
- `dsu-sideloader-<version>-debug.apk` – debug build, useful when you need logs
- `SHA256SUMS` – verify a download with `sha256sum -c SHA256SUMS`

For builds that were not released yet, check the artifacts of the [Actions](https://github.com/Muchyutaka/DSU-SL/actions) tab.

> This is a fork. Releases published here are built by this repository's own CI and are signed with its key, so an APK from this fork and one from the upstream project or F-Droid cannot be installed over each other: uninstall the previous app first.

## How to use?
1. Install app
2. When opening for the first time, you need to give read/write permission to a folder, create a new folder and allow access  
   **this folder will be used to store temporary files, like extracted GSIs from compressed files)*
3. Select a GSI to install  
   **accepted formats are: gz, xz, img and zip (only DSU packages)
4. You can customize installation as you want  
   **like changing userdata size for dynamic system*  
   **changing gsi file size is not recommended (let app do it automatically)*
5. Tap on "Install"
6. Wait until finishes! (it may take a some time)
7. Once it finishes, next step may vary:
	- If built-in installer is enabled, no extra step is required.
	- When built-in installer is disabled, on rooted/system/shizuku operation mode, DSU screen will appear, prompting you to confirm installation, after that, check your notifications, DSU should start installing GSI.
	- On ADB operation mode, you will be prompted to run a command in adb, once you run, DSU screen will appear asking you to confirm installation, after that, DSU should start installing GSI.
8. Once dynamic system is ready, you can boot it through notifications, or, if operation mode is supported, directly from our app.

For more usage information, you can check [Operation modes](#operation-modes)

## Building and CI

### Building from source

```sh
./gradlew assembleDebug        # debug APK: app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # JVM unit tests (checksum, patch level, diagnostics, history)
```

Requirements: JDK 17 and the Android SDK with platform 33 (`compileSdk = 33`, `minSdk = 29`, `targetSdk = 33`).
Release signing is read from `.sign/dsu_sideloader.prop` (git-ignored), see the comment in `app/build.gradle.kts`.

### GitHub Actions

The pipeline lives in `.github/workflows/build.yml` (it replaces the older `android.yml`) and runs on:

- pushes to `master` / `main`
- pull requests targeting those branches
- tags matching `v*`
- manual runs (`workflow_dispatch`)

Jobs:

| Job | What it does |
| --- | --- |
| `Assemble debug APK` | `./gradlew assembleDebug`, then `./gradlew testDebugUnitTest`, verifies the tests actually executed (a green but empty test run fails the job), then `./gradlew assembleRelease` as a R8/ProGuard compile check, and uploads the `dsu-sideloader-debug-apk` and `unit-test-reports` artifacts |
| `Assemble release APK (tags)` | only on `v*` tags: `./gradlew assembleRelease`, reuses the debug APK built by the previous job and attaches **both** APKs plus a `SHA256SUMS` file to the tag's GitHub Release |

**How to download the CI built APK:** open the Actions tab → pick the workflow run → scroll to *Artifacts* → download `dsu-sideloader-debug-apk` (downloads require being signed in to GitHub). Every artifact is built from the commit shown in the run.

**How to trigger a release build:** push a tag, for instance:

```sh
git tag v2.04
git push origin v2.04
```

The release job creates (or reuses) the GitHub Release for that tag and uploads every asset with `gh release upload --clobber`. A release tagged `v2.04` contains:

| Asset | Description |
| --- | --- |
| `dsu-sideloader-2.04-release.apk` | optimized release build (smaller, R8 minified), signed with the release key when the secrets exist |
| `dsu-sideloader-2.04-debug.apk` | debug build, the one to grab when you want logs or are troubleshooting |
| `SHA256SUMS` | SHA-256 checksums of both APKs, verify with `sha256sum -c SHA256SUMS` |

Both APKs are built from the same commit: the debug APK is the artifact uploaded by the `build` job of the same run, so the release never contains a binary from a different revision. The same three files are also available as the `dsu-sideloader-apks` workflow artifact.

The release notes are generated by the workflow: they list the assets and add a short install guide (Android 10+, install-unknown-apps permission, Shizuku or root), plus a warning when the APK is only signed with the debug key.

#### Release signing secrets (optional)

The release job signs the APK only when these repository secrets are configured (Settings → Secrets and variables → Actions). Nothing is committed: the job decodes the keystore and writes the git-ignored `.sign/dsu_sideloader.prop` at build time.

| Secret | Description |
| --- | --- |
| `KEYSTORE_BASE64` | base64 encoded keystore, e.g. `base64 -w0 keystore.jks` |
| `KEYSTORE_PASSWORD` | keystore password |
| `KEY_ALIAS` | key alias |
| `KEY_PASSWORD` | key password |

When the secrets are absent the release APK is signed with the **debug key** instead, so it stays installable on a device (Android rejects unsigned APKs with `INSTALL_PARSE_FAILED_NO_CERTIFICATES`) and forks can publish working builds without key material. The run logs a warning when this happens. Such a build cannot be upgraded over one signed with the real release key — uninstall the old app first.

The job also verifies both APKs with `apksigner verify --print-certs` before publishing, so an unsigned or corrupt artifact can never reach a release.

The workflow uses a `concurrency` group to cancel superseded runs and requests `contents: read` by default (the release job opts into `contents: write` to publish the release). The unit test totals are written to the run summary, so a run that executed zero tests is obvious at a glance.

The unit tests live in `app/src/test` and only cover plain Kotlin logic (checksum normalisation, patch level parsing and comparison, error classification, log formatting, history model) so they need neither an emulator nor Robolectric.

## Installation safety, diagnostics and history

### SHA-256 verification

After selecting an image the app streams it once (never loading it fully in memory) and computes its SHA-256 in the background, showing progress on the installation card. You can paste the checksum published for your image with *Set expected checksum*; the app compares it locally and warns on mismatch, including an `sha256sum` style line (`<hash>  <file>`).

- Verification can be turned off in **Settings → Verification → Verify SHA-256** (the card then warns that the image was not verified).
- **Strict checksum mode** (opt-in, off by default) blocks the install button while the checksum is being computed and when an expected checksum does not match. Without an expected checksum nothing is blocked.
- Nothing is uploaded: verification is local and non-blocking unless you enable strict mode.

### Security patch level pre-check

The device patch level comes from `Build.VERSION.SECURITY_PATCH`. A GSI does not expose its patch level without mounting the image, so the app **estimates it from the file name** when the name contains a date (`2023-10-12`, `20231012`, ...). When no date can be parsed the card says *unknown* instead of guessing, and when the image looks older than the device you get a warning before installing. The estimation is also disabled with **Settings → Verification → Security patch check**.

### Smart error diagnostics

Failed installations are classified (logcat lines and preparation exceptions) into friendly cards: SELinux denial, partition creation failure, corrupted/incomplete image, insufficient storage, unsupported architecture/image, SD card allocation, f2fs kernel, extents fragmentation and "dynamic system already running". Each card shows a human readable cause, a suggested fix, an optional documentation link and an expandable **Technical details** section with the raw error.

Preparation failures (revoked SAF permission, unreadable archive, ...) are now caught and shown as an error card instead of crashing the app.

### Exporting and sharing logs

- **Share logs** on the installation logs sheet and **Export logs** in Settings build a single text report containing: app version/build type, device model, Android version/SDK, ABIs, security patch level, operation mode, selected image name and configuration, checksum (computed and expected), patch levels, error/step, the captured installation logs and a bounded `logcat` excerpt (last 400 lines).
- The report is bounded (≈32k characters per section), written to the app files directory (last 5 reports are kept) and shared through the existing `FileProvider`. **Nothing is uploaded automatically**, you always pick the target app in the system share sheet. *Save logs* still uses the system file picker.

### Install history and presets

Every attempt records timestamp, image file name, userdata size, image size, result and a short error summary. The history is stored locally in the app DataStore (bounded to 25 entries, no image files, no URIs) and can be cleared from **Settings → History**.

- *Retry* restores that configuration on the home screen; select the same image file again to continue (content URIs are intentionally not stored).
- *Save as preset* keeps a reusable configuration (bounded to 10, duplicates of the same userdata/image size are replaced); presets can be applied or deleted from the same screen.

### Theming

- Material You dynamic color on Android 12+ (can be disabled).
- Theme mode: follow system, light or dark (Settings → Appearance).
- AMOLED black option that forces true black surfaces when dark theme is active, keeping the contrast of the generated color scheme.

## Choosing an access method

On first launch the app asks how it may install a DSU and **verifies the answer** before it lets you continue. The choice is stored in the app preferences, and it is validated again on every start: if the method stopped working (Shizuku killed, root revoked) the onboarding screen comes back instead of silently falling back to another method.

| Method | What is checked (real API) | What you can do when it fails |
| --- | --- | --- |
| **Shizuku** | manager app installed (`PackageManager`), service running (`Shizuku.pingBinder()`), this app authorized (`Shizuku.checkSelfPermission()`) | open Shizuku, request the permission (`Shizuku.requestPermission`), install Shizuku |
| **Root** | `Shell.isAppGrantedRoot()`; when the state is unknown a shell is opened so your root manager shows its prompt (`Shell.getShell { it.isRoot }`) | grant superuser access, then re-check |
| **ADB** | the DSU system app `com.android.dynsystem` is installed, and USB or wireless debugging is enabled (`Settings.Global.adb_enabled` / `adb_wifi_enabled`) | enable developer options and debugging, then re-check |

Every card shows a status badge (Available / Not installed / Not running / Not authorized / Unavailable on this device) with the reason, an inline spinner while the check runs and a check mark when it passes. "Continue" stays disabled until the selected method passes.

**What the app cannot verify:** there is no API for an unprivileged app to detect a paired computer or the wireless debugging pairing state, and ADB mode does not give the app any permission — it only exports an installation script that you run with `adb shell`. The ADB check therefore validates the device-side prerequisites and says so, instead of pretending to know whether a computer is connected.

Installs that already run as a system app (the Magisk module) skip the onboarding screen: the privileged mode is detected (`INSTALL_DYNAMIC_SYSTEM`) and there is nothing to choose.

## App updates

The app checks its own releases (`https://github.com/Muchyutaka/DSU-SL/releases`) and can fetch the newest build without leaving the app.

- **Detection:** one anonymous request to the GitHub API (`/releases/latest`), at most once every 6 hours, plus a manual "Check for updates" in Settings and About. Drafts and pre-releases are ignored.
- **Where it shows:** a banner at the top of the home screen when a newer version exists (dismissible per version), a card in *Settings → About*, and a card on the About screen.
- **Fetching:** "Download" streams the `-release.apk` asset into app private storage with a progress bar. When the release publishes `SHA256SUMS` (this repository's CI always does), the downloaded file is verified against the published SHA-256 and **deleted on mismatch**.
- **Installing:** Android's package installer is opened for the verified file; the app asks for the "install unknown apps" permission when it is missing.
- **Privacy:** nothing is sent anywhere except the version request to GitHub, and nothing is downloaded until you tap Download.

The old updater (a static `updater.json` and an author-signature gate) was removed: it only ever worked for builds signed with the upstream author's key, so forks never saw an update.

## Operation modes

DSU Sideloader support multiple operation modes, they will define how our app will work, also, the operation mode is obtained automatically, and by now, is impossible to change it manually, the picked operation mode will be the best available (the priority is written below, in which, the most feature supported, is the highest number, and the most basic one is the lowest).

1. ADB: Default operation mode when other modes aren't available
- Only prepare selected image to be installed via DSU system-app
- Requires adb command to start installation (which will invoke DSU system-app to install the prepared file)
2. Shizuku: When running app with Shizuku (Obtained when Shizuku permission is granted)
- Same as ADB, however, it does not require to run any adb command
- Support tracking installation progress ¹ ² ³
- Support installation diagnostics (if a common error is detected, it may give you useful information) ¹ ³
3. Root: When running app with root permissions (Obtained when user grant root permission)
- All features avaiable in Shizuku, however, does not require any special permissions
- DynamicSystem API features (check if DSU is installed, reboot to DSU, discard..., everthing directly from app)
- Support built-in DSU installer ⁴ ⁵
4. System mode: When running as system-app (Obtained by installing our Magisk module)
- All features avaiable in Shizuku
- Fixes for some common gsi/dsu-related SELinux denials
- Custom gsid binary (can fix some installation errors in some devices ⁵ ⁶
5. System/Root mode: When running as system-app with granted root permission (Obtained by installing our Magisk module and granting root permission)
- All features available in root and system operation mode

¹ Requires READ_LOGS permission.
<br>
² Partital support on Android 10 and 11.
<br>
³ Android 13 requires "One-time log access".
<br>
⁴ Feature not supported on Android 10.
<br>
⁵ Experimental feature, built-in installer code is [here](https://github.com/VegaBobo/DSU-Sideloader/blob/master/app/src/main/java/vegabobo/dsusideloader/installer/root/DSUInstaller.kt).
<br>
⁶ Module including custom gsid binary is optional, changes made to AOSP gsid binary can be found [here](https://github.com/VegaBobo/DSU-Sideloader/tree/master/magisk-module/src/main/resources/aosp_patches).

#### Recomendations

- For non-rooted devices, Shizuku is a pretty nice operation mode, it support most features with no hassle, however, you need to install and setup [Shizuku](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) app in your device.
- For rooted devices, Root operation mode is more than ok for most people.
- If you're having issues with DSU feature, go with System/Root.
- Rooted devices via Magisk, should be running Magisk v24 or higher, older versions may break DSU feature.
- We highly recommend using this app with Stock ROM, some Custom ROM builds may also work fine.

## Common questions

1. DSU installation finishes with no errors, but device doesn't boot into installed DSU, what should i do?
- It is likely that AVB is preventing device from booting installed images, try flashing disabled vbmeta, check [this](https://developer.android.com/topic/generic-system-image#flash-gsi) for more info.
2. Why isn't possible to set a high userdata value?
- The more storage you have free, the more you can use to be your userdata, some Android versions limit the maximum allowed for allocation (this limitation is 40%, and is not our app limitation, it is a thing from Android itself, you can use our custom gsid binary, which reduces this limitation to 20%, is possible to eliminate it, but no clue if there is some implications, so, i just decided to decrease it).
3. Why "Unmount SD" option exists?
- If available, DSU priorizes allocation in sdcard, but allocating in sdcard is not supported in some cases (it may depends on filesystem present on sd, and if the allocation in SD is allowed by OS itself), since allocating in SD may cause installation errors in some devices, that options is here to enforce allocation in device storage.
4. Why built-in installer requires root?
- Because it uses Android's internal DynamicSystem API, which requires "MANAGE_DYNAMIC_SYSTEM", which is a signature protection level, the convenient way to circumvent it, is by using root. shell (2000) has "INSTALL_DYNAMIC_SYSTEM", which is able to call DSU system-app (this one has "MANAGE_DYNAMIC_SYSTEM") to install images.
5. How about updates?
- Our app comes with a updater, you can check updates in "About" section.
6. Other question? problem?
- Feel free to start a issue, for troubleshooting, don't forget to send logs (logs can be obtained on installation phase, directly on app, when operation mode support installation diagnostics).
- On builds that include the diagnostics features you can use **Share logs** on the installation logs sheet, or **Settings → Export logs**, to get a complete report (device info, installation configuration, error and a bounded logcat excerpt). The report is only shared when you pick a target app.

## About DSU
DSU (Dynamic System Updates), is a feature introduced on Android 10, that let developers boot GSIs without touching current system partition, this is done by creating new partitions to hold a GSI and a separated userdata, to boot on them when desired.

Unfortunelly, DSU depends on Dynamic Partitions (your device need to support, otherwise, won't work), and most GSIs requires unlocked bootloader to get them booting properly (since only OEM-Signed GSIs are allowed to boot on locked bootloader).

GSIs can be installed via DSU without root access, using ADB, running some commands, you can read more about installation process here: https://developer.android.com/topic/dsu

Once installation finishes, Android creates a persistent notification allowing you to boot into "Dynamic System" (GSI installed via DSU), and you can boot into installed GSI, without touching your system partition, or breaking the "real userdata" partition.

After booting Dynamic System, you can try and test whatever you want, when you need to switch back to device's original system image, everything you need to do, is just, a simple reboot!

When doing a long test, that may requires lots of reboots, this can be a pain, however, is possible to enable "sticky mode", that enforces dynamic system, instead of device's original system image, once tests are done, you can disable sticky mode and return to original system image.

That is basically a quickly explanation about DSU, a amazing feature, like a "dual-boot" solution, limited, however, very safe (since no read-only partition will be modified, and if GSI does not boot, just a simple reboot will return you to the original device's system image).

You can read more about DSU here: https://source.android.com/devices/tech/ota/dynamic-system-updates

## How to enable Sticky Mode?

Reboot to Dynamic System, and:
- use this command on adb: `adb shell gsi_tool enable`
- or from local adb shell:  `gsi_tool enable`
- or from local rooted shell (eg. Termux on rooted GSI):  `su -c 'gsi_tool enable'`

When sticky mode is enabled, device will always boot into dynamic system, instead of device's original system image.

To disable, use the same command, instead of `enable` , use `disable`

## Other

For translators, we now have a Crowdin, feel free send your translations:
<br>https://crowdin.com/translate/dsu-sideloader/<br>
App icon made by [WSTxda](https://github.com/WSTxda)
