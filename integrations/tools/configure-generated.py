"""One-time conversion of Flutter-generated hosts to the integration example."""
from pathlib import Path
import shutil
root=Path(__file__).resolve().parents[1]
p=root/'packages/flutter'
(p/'pubspec.yaml').write_text('''name: cmp_consent
description: Native CMP consent forms and submission for Flutter Android and iOS.
version: 0.1.0
environment:
  sdk: ^3.9.2
  flutter: '>=3.35.0'
dependencies:
  flutter:
    sdk: flutter
  plugin_platform_interface: ^2.0.2
dev_dependencies:
  flutter_test:
    sdk: flutter
  flutter_lints: ^5.0.0
flutter:
  plugin:
    platforms:
      android:
        package: vn.fpt.cmp.cmp_consent
        pluginClass: MobileConsentPlugin
      ios:
        pluginClass: MobileConsentPlugin
''',encoding='utf-8')
(p/'android/build.gradle').write_text('''group = 'vn.fpt.cmp.cmp_consent'
version = '0.1.0'
buildscript {
    ext.kotlin_version='2.1.0'
    repositories { google(); mavenCentral() }
    dependencies { classpath 'com.android.tools.build:gradle:8.7.0'; classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version" }
}
apply plugin:'com.android.library'
apply plugin:'kotlin-android'
repositories {
    google(); mavenCentral()
    def repo=System.getenv('CMP_MAVEN_REPO')
    if(repo)maven { url=uri(repo) }
}
android {
    namespace='vn.fpt.cmp.cmp_consent'; compileSdk=35
    defaultConfig { minSdk=21 }
    compileOptions { sourceCompatibility=JavaVersion.VERSION_11; targetCompatibility=JavaVersion.VERSION_11 }
    kotlinOptions { jvmTarget='11' }
}
dependencies { implementation "io.github.idaflatform:fpt-consent-sdk:${System.getenv('CMP_SDK_VERSION') ?: '1.2.0'}" }
''',encoding='utf-8')
(p/'ios/cmp_consent.podspec').write_text('''Pod::Spec.new do |s|
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
''',encoding='utf-8')
(p/'example/lib/main.dart').write_text("import 'package:flutter/material.dart';\nimport 'consent_example.dart';\nvoid main()=>runApp(const MaterialApp(home:ConsentExample()));\n",encoding='utf-8')
settings=p/'example/android/settings.gradle.kts'
settings.write_text(settings.read_text().replace('8.9.1','8.7.0'),encoding='utf-8')
wrapper=p/'example/android/gradle/wrapper/gradle-wrapper.properties'
wrapper.write_text(wrapper.read_text().replace('gradle-8.12-all','gradle-8.10-bin').replace('gradle-8.10.2-all','gradle-8.10-bin'),encoding='utf-8')
gradle=p/'example/android/build.gradle.kts'
gradle.write_text(gradle.read_text().replace('mavenCentral()','mavenCentral()\n        System.getenv("CMP_MAVEN_REPO")?.let { maven { url = uri(it) } }'),encoding='utf-8')
props=p/'example/android/gradle.properties'
props.write_text('org.gradle.jvmargs=-Xmx2048m\norg.gradle.workers.max=2\nkotlin.compiler.execution.strategy=in-process\nandroid.useAndroidX=true\nandroid.enableJetifier=true\n',encoding='utf-8')
debug=p/'example/android/app/src/debug/AndroidManifest.xml'
debug.write_text('<manifest xmlns:android="http://schemas.android.com/apk/res/android"><uses-permission android:name="android.permission.INTERNET"/><application android:usesCleartextTraffic="true" /></manifest>',encoding='utf-8')
pbx=p/'example/ios/Runner.xcodeproj/project.pbxproj'
pbx.write_text(pbx.read_text().replace('IPHONEOS_DEPLOYMENT_TARGET = 13.0;','IPHONEOS_DEPLOYMENT_TARGET = 14.0;'),encoding='utf-8')
(p/'example/ios/Podfile').write_text('''platform :ios, '14.0'
ENV['COCOAPODS_DISABLE_STATS'] = 'true'
project 'Runner', {'Debug'=>'debug','Profile'=>'release','Release'=>'release'}
def flutter_root
  path=File.expand_path(File.join('..','Flutter','Generated.xcconfig'),__FILE__)
  raise "Run flutter pub get first" unless File.exist?(path)
  File.foreach(path){|line| match=line.match(/FLUTTER_ROOT\\=(.*)/); return match[1].strip if match}
  raise 'FLUTTER_ROOT missing'
end
require File.expand_path(File.join('packages','flutter_tools','bin','podhelper'),flutter_root)
flutter_ios_podfile_setup
target 'Runner' do
  use_frameworks!
  if ENV['CMP_IOS_SDK_PATH']
    pod 'ConsentCmp', :path=>ENV['CMP_IOS_SDK_PATH']
  end
  flutter_install_all_ios_pods File.dirname(File.realpath(__FILE__))
end
post_install do |installer|
  installer.pods_project.targets.each do |target|
    flutter_additional_ios_build_settings(target)
    target.build_configurations.each {|config| config.build_settings['IPHONEOS_DEPLOYMENT_TARGET']='14.0'}
  end
end
''',encoding='utf-8')
# Native examples ship a wrapper so they do not require a global Gradle installation.
sdk=root.parent if (root.parent/'consent-cmp').is_dir() else root.parent/'fpt-consent-sdk-android'
for name in ['gradlew','gradlew.bat','gradle/wrapper/gradle-wrapper.jar','gradle/wrapper/gradle-wrapper.properties']:
    dest=root/'android'/name;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(sdk/name,dest)
print('Configured independent Android and Flutter hosts.')
