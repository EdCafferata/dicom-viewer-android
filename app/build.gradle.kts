plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "info.cafferata.dicomviewer"
    compileSdk = 36

    defaultConfig {
        applicationId = "info.cafferata.dicomviewer"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.1"
    }

    signingConfigs {
        create("release") {
            storeFile = file(project.findProperty("DICOMVIEWER_STORE_FILE") as String)
            storePassword = project.findProperty("DICOMVIEWER_STORE_PASSWORD") as String
            keyAlias = project.findProperty("DICOMVIEWER_KEY_ALIAS") as String
            keyPassword = project.findProperty("DICOMVIEWER_KEY_PASSWORD") as String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.core:core-ktx:1.17.0")

    // Play Billing — eenmalige tip-jar-aankopen, zelfde patroon als Duski's abonnement.
    implementation("com.android.billingclient:billing-ktx:9.1.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
