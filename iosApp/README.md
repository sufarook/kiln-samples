# iosApp

Swift entry point that hosts the shared Compose UI from `composeApp`.

The Xcode project file (`iosApp.xcodeproj`) is **not** checked in — generating a
valid `.pbxproj` by hand is error-prone, and Xcode/KMP tooling produces a correct
one for you. To run on iOS:

**Option A — Android Studio (easiest).** Install the
[Kotlin Multiplatform plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform),
then use the generated `iosApp` run configuration.

**Option B — Xcode.** Create a new iOS App project in this directory named
`iosApp`, add `iOSApp.swift` and `ContentView.swift` to it, then add a
"Run Script" build phase before "Compile Sources":

```sh
cd "$SRCROOT/.."
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

and add `$(SRCROOT)/../composeApp/build/xcode-frameworks/$(CONFIGURATION)/$(SDK_NAME)`
to **Framework Search Paths**.

You can verify the Kotlin side compiles without Xcode at all:

```sh
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```
