plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.noses.onthenose"
    compileSdk = 37

    defaultConfig {
        applicationId = "org.noses.onthenose"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        create("release") {
            val store = providers.gradleProperty("ONTHENOSE_STORE_FILE").orNull
            if (store != null) {
                storeFile = file(store)
                storePassword = providers.gradleProperty("ONTHENOSE_STORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("ONTHENOSE_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("ONTHENOSE_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)
}
