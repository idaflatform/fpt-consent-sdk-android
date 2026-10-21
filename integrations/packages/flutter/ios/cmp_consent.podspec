Pod::Spec.new do |s|
  s.name='cmp_consent'
  s.version='0.1.0'
  s.summary='Native CMP forms for Flutter.'
  s.homepage='https://github.com/idaflatform/fpt-consent-sdk-ios'
  s.license={:type=>'Apache-2.0'}
  s.author='FPT CMP'
  s.source={:path=>'.'}
  s.source_files='Classes/**/*'
  s.dependency 'Flutter'
  s.dependency 'ConsentCmp', '~> 1.2'
  s.platform=:ios,'14.0'
  s.swift_version='5.0'
  s.pod_target_xcconfig={'DEFINES_MODULE'=>'YES'}
end
