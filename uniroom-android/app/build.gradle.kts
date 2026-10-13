plugins {
    id("com.android.application")
}

android {
    namespace = "com.uniroom.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.uniroom.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "2.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// No extra libraries are needed: every screen is built with the
// standard Android framework (Activity, LinearLayout, TextView, ...).
dependencies {
}
