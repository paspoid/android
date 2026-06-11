plugins {
    alias(libs.plugins.paspo.id.sdk.library)
    alias(libs.plugins.paspo.id.compose)
    alias(libs.plugins.paspo.id.sdk.publish)
}

android {
    namespace = "paspo.id.ssoprovider.ui.compose"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
        minSdk = 23
    }

    buildFeatures {
        buildConfig = false
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = false
    }
}

dependencies {
    api(projects.ssoProvider.ui)

    implementation(libs.androidx.activity.compose)
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.kotlinx.coroutines.core)
}

sdkPublish {
    displayName.set("Paspo ID SDK UI (Compose)")
    description.set("Drop-in Compose sign-in button for the Paspo ID SDK")
}
