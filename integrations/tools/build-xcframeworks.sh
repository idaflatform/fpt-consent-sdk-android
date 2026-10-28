#!/usr/bin/env bash
set -euo pipefail
: "${CMP_IOS_SDK_PATH:?Set CMP_IOS_SDK_PATH to the iOS SDK checkout}"
root="$(cd "$(dirname "$0")/.." && pwd)"
output="$root/build/ios"
mkdir -p "$output"
xcodegen generate --spec "$root/ios/Frameworks/project.yml" --project "$root/ios/Frameworks"
for scheme in ConsentCmp CMPMobileBridge; do
 for target in device simulator; do
  destination='generic/platform=iOS'
  if [ "$target" = simulator ]; then destination='generic/platform=iOS Simulator'; fi
  xcodebuild archive -project "$root/ios/Frameworks/CMPFrameworks.xcodeproj" -scheme "$scheme" \
   -destination "$destination" -archivePath "$output/$scheme-$target.xcarchive" \
   SKIP_INSTALL=NO BUILD_LIBRARY_FOR_DISTRIBUTION=YES CODE_SIGNING_ALLOWED=NO
 done
 xcodebuild -create-xcframework \
  -framework "$output/$scheme-device.xcarchive/Products/Library/Frameworks/$scheme.framework" \
  -framework "$output/$scheme-simulator.xcarchive/Products/Library/Frameworks/$scheme.framework" \
  -output "$output/$scheme.xcframework"
done
echo "Unsigned XCFrameworks ready for binding validation in $output"
