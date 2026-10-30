# Security dependency validation

## Build baseline

The release build and explicitly pinned integration builds use AGP 9.5.0 and
Gradle 9.6.0. The publishing plugin is 0.37.0 and Robolectric is 4.17 (the old
4.14.1 test dependency also brought Bouncy Castle 1.78.1). The SDK publishes the Kotlin BOM
and explicit stdlib/jdk7/jdk8 dependencies at 2.4.21, so the upgrade is carried in both
Gradle metadata and the Maven POM. Newer compatible versions can still win
dependency resolution; these declarations do not force a downgrade.
The release build explicitly upgrades AGP's Kotlin compiler/plugin to 2.4.21
as well. [CVE-2026-53914](https://github.com/advisories/GHSA-r937-wjx7-w2jp)
affects Kotlin Gradle plugin build-cache deserialization before 2.4.20;
upgrading stdlib alone does not fix the build tool. The SDK release build uses
the fixed stable compiler version; legacy host toolchains require separate review.

Kotlin 2.4 metadata requires a compatible Kotlin compiler in Kotlin host apps.
Hosts also need compatible D8/R8 versions; consult the official
[Kotlin/Android compiler compatibility table](https://developer.android.com/build/kotlin-support).
Validate older React Native/Flutter host toolchains before upgrading the SDK;
the legacy React Native fixture keeps its host-managed build tooling. Changing
the SDK release build does not automatically upgrade an application's AGP.
Flutter's legacy Gradle host opts out of AGP built-in Kotlin and the new Android
DSL explicitly; see `integrations/packages/flutter/README.md`.

Official compatibility references:

- [AGP compatibility](https://developer.android.com/build/releases/about-agp)
- [Built-in Kotlin migration](https://developer.android.com/build/migrate-to-built-in-kotlin)
- [Android KMP plugin](https://developer.android.com/kotlin/multiplatform/plugin)

## Reproduce locally

From the repository root, with JDK 17 and the Android SDK configured:

```sh
./gradlew :fpt-cmp:testDebugUnitTest :fpt-cmp:assembleRelease :fpt-cmp:lintRelease :sample:assembleDebug
./gradlew :fpt-cmp:generatePomFileForMavenPublication :fpt-cmp:generateMetadataFileForMavenPublication
./gradlew -I tools/security-dependencies.init.gradle auditSecurityDependencies
```

The audit writes `build/reports/security-dependencies.json`, separating resolved
buildscript, release runtime and unit-test runtime components. It fails on unresolved dependencies
or the reported outdated Kotlin, Netty, Bouncy Castle and ICU4J versions. This
is a dependency regression check, not a vulnerability scanner.

```sh
cd integrations/packages/flutter
flutter pub get --enforce-lockfile
flutter analyze
flutter test
```

For MAUI, use the SDK pinned in `integrations/packages/maui/global.json` and the
workloads documented in that package's README:

```sh
cd integrations/packages/maui
dotnet restore AndroidBinding/AndroidBinding.csproj --locked-mode
dotnet restore IOSBinding/IOSBinding.csproj --locked-mode
```

Restoring iOS dependencies on Windows does not validate an iOS binary or replace
a Mac/Xcode build. `bin/`, `obj/`, and example Flutter lockfiles stay untracked;
the plugin Flutter lockfile and both binding NuGet lockfiles are committed.

## Local verification — 2026-10-30

Verified on Windows with JDK 17, Flutter 3.35.7 and .NET SDK 9.0.320:

| Check | Result |
|---|---|
| SDK unit tests | 25 passed, no failures/errors/skips |
| SDK release AAR, lint and native sample APK | Passed; lint has 0 errors and 11 warnings |
| Publication POM and Gradle metadata | All three stdlib artifacts and BOM use 2.4.21 |
| Java and Compose sample APKs | Passed |
| Native bridge unit tests and local publication | 4 tests passed |
| KMP Android sample APK | Passed |
| Flutter plugin analyze / Dart tests / Android sample APK | Passed; 4 Dart tests |
| Flutter enforced lockfile and MAUI Android/iOS locked restores | Passed |
| Resolved dependency audit in SDK, Java/Compose, KMP and Flutter builds | Passed |
| Backup XML contents and AAR packaging | Verified; sample manifest references both rules |

Capacitor's AGP declaration and host requirements were updated; its host and iOS
binaries were not built here. Legacy React Native fixtures were not rebuilt.
External vulnerability scans and DefectDojo import are deferred at the user's request.

## External security acceptance

The repository currently contains no Black Duck/Coverity/DefectDojo job or
credentials. Run the organization's security pipeline against the resulting
commit, with recursive discovery of `integrations/packages/flutter/pubspec.lock`
and both MAUI `packages.lock.json` files. Include build dependencies in the Gradle
scan and inspect legacy host fixtures as well as the SDK release build.

Acceptance requires the scanner reports to show the Flutter platform-interface
and MAUI Material dependency graphs, no selected Kotlin 1.8.22, Netty
4.1.93.Final or Bouncy Castle 1.78.1, and successful report import into the correct
DefectDojo engagement. Record the commit SHA, scan/job URL and DefectDojo result
URL. Local build/audit success alone does not establish these results.

For backup findings, the README documents the required host opt-in or merging
of exclusions. The library intentionally does not override `allowBackup` or a
customer's existing backup rules. Verify the customer's merged manifest and
backup/restore behavior after integration.
