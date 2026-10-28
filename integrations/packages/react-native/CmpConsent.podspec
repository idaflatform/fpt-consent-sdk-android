Pod::Spec.new do |s|
 s.name='CmpConsent'
 s.version='0.1.0'
 s.summary='React Native bridge to CMP native consent screen.'
 s.homepage='https://github.com/idaflatform/fpt-consent-sdk-ios'
 s.license={:type=>'Apache-2.0'}
 s.author='FPT CMP'
 s.source={:path=>'.'}
 s.source_files='ios/**/*.{swift,m}'
 s.ios.deployment_target='14.0'
 s.swift_version='5.0'
 s.dependency 'React-Core'
 s.dependency 'CMPMobileBridge','~> 0.1'
end
