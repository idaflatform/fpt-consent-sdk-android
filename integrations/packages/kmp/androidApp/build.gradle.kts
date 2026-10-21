plugins {id("com.android.application");kotlin("android")}
android {
 namespace="vn.fpt.cmp.kmp.example";compileSdk=35
 defaultConfig{applicationId="vn.fpt.cmp.kmp.example";minSdk=21;targetSdk=35;versionCode=1;versionName="0.1.0"}
 compileOptions{sourceCompatibility=JavaVersion.VERSION_11;targetCompatibility=JavaVersion.VERSION_11}
 kotlinOptions{jvmTarget="11"}
}
dependencies{implementation(project(":shared"))}
