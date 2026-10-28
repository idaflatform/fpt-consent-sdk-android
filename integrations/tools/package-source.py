from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import argparse
import os
root=Path(__file__).resolve().parents[1]
default_android=root.parent if (root.parent/'fpt-cmp').is_dir() else root.parent/'fpt-consent-sdk-android'
parser=argparse.ArgumentParser(description='Package integration sources with locally built SDK dependencies.')
parser.add_argument('--android-sdk',type=Path,default=Path(os.environ.get('CMP_ANDROID_SDK_PATH',str(default_android))))
parser.add_argument('--ios-sdk',type=Path,default=Path(os.environ.get('CMP_IOS_SDK_PATH',str(default_android.parent/'fpt-consent-sdk-ios'))))
parser.add_argument('--bridge-repo',type=Path,default=Path(os.environ.get('CMP_BRIDGE_MAVEN_REPO',str(root/'android/build/repo'))))
parser.add_argument('--output',type=Path,default=root/'build/distributions/cmp-mobile-integrations-source.zip')
args=parser.parse_args()
out=args.output.resolve()
android_repo=args.android_sdk/'fpt-cmp/build/repo'
ios_sdk=args.ios_sdk
for required in [android_repo,args.bridge_repo,ios_sdk/'ConsentCmp.podspec',ios_sdk/'Sources',ios_sdk/'Package.swift',ios_sdk/'README.md']:
    if not required.exists():parser.error(f'Missing {required}; build local artifacts or pass the appropriate path option.')
out.parent.mkdir(parents=True,exist_ok=True)
skip={'build','.gradle','.dart_tool','node_modules','Pods','.idea','.symlinks','.git','DerivedData','test-results','__pycache__'}
def files(folder):
    for item in folder.iterdir():
        if item.name in skip or item.is_symlink():continue
        if item.is_dir():yield from files(item)
        elif item.name not in {'local.properties','pubspec.lock','.env','.flutter-plugins-dependencies'} and item.suffix not in {'.log','.iml'} and not item.name.endswith('.inspect.ndjson'):yield item
with ZipFile(out,'w',ZIP_DEFLATED) as archive:
    count=0
    for item in files(root):archive.write(item,Path(root.name)/item.relative_to(root));count+=1
    sdk_podspec=ios_sdk/'ConsentCmp.podspec'
    archive.write(sdk_podspec,Path(root.name)/'sdk-packaging/ConsentCmp.podspec')
    for location,prefix in [(android_repo,'vendor/maven/sdk'),(args.bridge_repo,'vendor/maven/bridge')]:
        for item in location.rglob('*'):
            if item.is_file():archive.write(item,Path(root.name)/prefix/item.relative_to(location))
    for item in (ios_sdk/'Sources').rglob('*'):
        if item.is_file():archive.write(item,Path(root.name)/'vendor/ConsentCmp'/item.relative_to(ios_sdk))
    for filename in ['Package.swift','ConsentCmp.podspec','README.md']:
        archive.write(ios_sdk/filename,Path(root.name)/'vendor/ConsentCmp'/filename)
with ZipFile(out) as archive:assert archive.testzip() is None
print(f'{out}: integration sources and packaged SDK dependencies; caches and local configuration excluded')
