plugins {
    alias(libs.plugins.com.android.application)
}

android {
    namespace = "io.github.ebraminio.bouncy"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.ebraminio.bouncy"
        minSdk = 1
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // https://stackoverflow.com/a/75544119
    packaging { dex { useLegacyPackaging = true } }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs["debug"]
        }
    }
}

dependencies {
}
