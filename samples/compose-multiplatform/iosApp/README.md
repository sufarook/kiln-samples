# iosApp

Swift entry point that hosts the shared Compose UI from this sample's `src/iosMain`.

The Xcode project file (`iosApp.xcodeproj`) is **not** checked in — hand-writing a
valid `.pbxproj` is error-prone, and the tooling generates a correct one for you.

## Running on iOS

**Option A — Android Studio (easiest).** Install the
[Kotlin Multiplatform plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform)
and use the generated `iosApp` run configuration.

**Option B — Xcode.** Create a new iOS App project in this directory named `iosApp`,
add `iOSApp.swift` and `ContentView.swift` to it, then add a **Run Script** build
phase *before* "Compile Sources":

```sh
cd "$SRCROOT/../../.."
./gradlew :compose-multiplatform:embedAndSignAppleFrameworkForXcode
```

and add this to **Framework Search Paths**:

```
$(SRCROOT)/../build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)
```

## Verifying without Xcode

The Kotlin half compiles and links on its own:

```sh
./gradlew :compose-multiplatform:linkDebugFrameworkIosSimulatorArm64
```
