Pod::Spec.new do |s|
 s.name='CMPMobileBridge'
 s.version='0.1.0'
 s.summary='Objective C compatible CMP native screen facade.'
 s.homepage='https://github.com/idaflatform/fpt-consent-sdk-ios'
 s.license={:type=>'Apache-2.0'}
 s.author='FPT CMP'
 s.source={:path=>'.'}
 s.source_files='Sources/CMPMobileBridge/**/*.swift'
 s.ios.deployment_target='14.0'
 s.swift_version='5.0'
 s.dependency 'ConsentCmp','~> 1.2'
 s.frameworks='UIKit','SwiftUI','Combine'
end
