plugins { kotlin("multiplatform");id("com.android.library") }
kotlin {
 androidTarget { compilations.all { kotlinOptions.jvmTarget="11" } }
 if(System.getProperty("os.name").contains("Mac")) {
  listOf(iosArm64(),iosSimulatorArm64(),iosX64()).forEach { it.binaries.framework {baseName="SharedCmp"} }
 }
 sourceSets { androidMain.dependencies {implementation("vn.fpt.cmp:mobile-bridge:0.1.0")} }
}
android {namespace="vn.fpt.cmp.kmp";compileSdk=35;defaultConfig{minSdk=21};compileOptions{sourceCompatibility=JavaVersion.VERSION_11;targetCompatibility=JavaVersion.VERSION_11}}
