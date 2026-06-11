plugins {
    alias(libs.plugins.paspo.id.sdk.library)
    alias(libs.plugins.paspo.id.sdk.publish)
}

android {
    namespace = "paspo.id.ssoprovider.ui"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
        minSdk = 23
    }

    buildFeatures {
        buildConfig = false
        androidResources = true
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
    api(projects.ssoProvider.clientSdk)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.kotlinx.coroutines.core)
}

sdkPublish {
    displayName.set("Paspo ID SDK UI (View)")
    description.set("Drop-in View sign-in button for the Paspo ID SDK")
}
