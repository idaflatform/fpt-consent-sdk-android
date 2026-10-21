// Add the packaged CMP Maven repositories during Expo prebuild.
// Environment variables are build-time repository locations, never API keys.
module.exports = function withCmpConsent(config) {
  const {withProjectBuildGradle} = require(require.resolve('expo/config-plugins', {
    paths: [config._internal.projectRoot],
  }));
  return withProjectBuildGradle(config, mod => {
    if (mod.modResults.language !== 'groovy') {
      throw new Error('CMP Expo plugin currently requires a Groovy Android root build.gradle.');
    }
    const marker = '// CMP packaged Maven repositories';
    if (!mod.modResults.contents.includes(marker)) {
      mod.modResults.contents += `
${marker}
allprojects {
    repositories {
        if (System.getenv('CMP_MAVEN_REPO')) maven { url = uri(System.getenv('CMP_MAVEN_REPO')) }
        if (System.getenv('CMP_BRIDGE_MAVEN_REPO')) maven { url = uri(System.getenv('CMP_BRIDGE_MAVEN_REPO')) }
    }
}
`;
    }
    return mod;
  });
};
