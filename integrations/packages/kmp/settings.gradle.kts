pluginManagement { repositories { google();mavenCentral();gradlePluginPortal() } }
dependencyResolutionManagement { repositories { google();mavenCentral();System.getenv("CMP_MAVEN_REPO")?.let {maven {url=uri(it)}};System.getenv("CMP_BRIDGE_MAVEN_REPO")?.let {maven {url=uri(it)}} } }
rootProject.name="CMPKmpExample"
include(":shared",":androidApp")
