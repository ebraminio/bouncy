plugins {
    alias(libs.plugins.com.android.application)
}

android {
    namespace = "io.github.ebraminio.bouncy"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.ebraminio.bouncy"
        minSdk = 33
        targetSdk = 34
        versionCode = 1
        versionName = "0.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

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
    implementation(libs.dynamicanimation) {
        exclude("androidx.core", "core")
        exclude("androidx.legacy", "legacy-support-core-utils")
    }
}
