import fs from 'node:fs/promises';
import path from 'node:path';
import assert from 'node:assert/strict';
const root=path.resolve(import.meta.dirname,'..');
const packages=['flutter','react-native','capacitor','cordova','kmp','maui','unity'];
for(const name of packages)assert((await fs.stat(path.join(root,'packages',name,'README.md'))).isFile(),`${name}: README required`);
for(const name of ['react-native','capacitor','cordova','unity']){
 const manifest=JSON.parse(await fs.readFile(path.join(root,'packages',name,'package.json'),'utf8'));
 assert(manifest.version==='0.1.0',`${name}: package version`);
}
const configuration=await fs.readFile(path.join(root,'packages/flutter/pubspec.yaml'),'utf8');
assert(configuration.includes('MobileConsentPlugin'));
for(const filename of ['android/java-example/build.gradle','android/compose-example/build.gradle','ios/Examples/project.yml','docs/integration-contract.md','docs/acceptance.md'])assert((await fs.stat(path.join(root,filename))).isFile(),filename);
console.log('Integration package manifests and required entry points verified. This is not a platform build test.');
