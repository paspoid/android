plugins {
    alias(libs.plugins.paspo.id.sdk.library)
    alias(libs.plugins.kotlinx.binary.compatibility.validator)
    alias(libs.plugins.paspo.id.sdk.publish)
}

android {
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
        minSdk = 23
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

    buildFeatures.buildConfig = false
}

kotlin {
    compilerOptions {
        optIn.add("paspo.id.ssoprovider.shared.PaspoInternalApi")
    }
}

apiValidation {
    nonPublicMarkers.add("paspo.id.ssoprovider.shared.PaspoInternalApi")
}

dependencies {
    api(projects.ssoProvider.shared)

    implementation(libs.androidx.activity.ktx)
    implementation(libs.jetbrains.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
}

sdkPublish {
    displayName.set("Paspo ID Android SDK")
    description.set("Client SDK for SSO authentication via the Paspo ID app")
}
