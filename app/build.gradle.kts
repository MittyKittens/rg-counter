plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mittykittens.rgcounter"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mittykittens.rgcounter"
        minSdk = 31
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        aidl = true
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed with the debug keystore for easy in-place sideload upgrades during
            // personal development. For any wider distribution, swap this for a dedicated
            // release keystore and protect it with a strong password outside of source control.
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")

    // Shizuku — gives shell-uid access without root for reading /dev/input/event2
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
}
