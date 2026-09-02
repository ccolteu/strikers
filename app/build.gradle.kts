plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.cc.ww2blitz"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.cc.ww2blitz"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.2"
    }
    buildTypes {
        release {
            isDebuggable = false
            isMinifyEnabled = true     // Activates ProGuard code shrinking and obfuscation
            isShrinkResources = true   // Automatically discards unused asset drawable references
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    androidResources {
        noCompress += "wav"
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
  implementation(libs.androidx.core.ktx)
}
