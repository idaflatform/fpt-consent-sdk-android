Pod::Spec.new do |s|
 s.name='CmpConsentCapacitor';s.version='0.1.0'
 s.summary='Capacitor adapter for native CMP consent.'
 s.homepage='https://github.com/idaplatform/fpt-consent-sdk-ios';s.license={:type=>'Apache-2.0'};s.author='FPT CMP';s.source={:path=>'.'}
 s.source_files='ios/**/*.swift';s.ios.deployment_target='14.0';s.swift_version='5.0'
 s.dependency 'Capacitor';s.dependency 'CMPMobileBridge','~> 0.1'
end
