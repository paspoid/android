plugins {
    alias(libs.plugins.paspo.id.sdk.library)
    alias(libs.plugins.jetbrains.kotlinx.serialization)
    alias(libs.plugins.kotlinx.binary.compatibility.validator)
    alias(libs.plugins.paspo.id.sdk.publish)
}

android {
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
        minSdk = 23
    }
    buildFeatures.buildConfig = false

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
        isCoreLibraryDesugaringEnabled = false
    }
}

apiValidation {
    nonPublicMarkers.add("paspo.id.ssoprovider.shared.PaspoInternalApi")
}

dependencies {
    implementation(libs.jetbrains.kotlinx.serialization.json)
}

sdkPublish {
    displayName.set("Paspo ID SDK Shared")
    description.set("Shared models and crypto for the Paspo ID SDK")
}
