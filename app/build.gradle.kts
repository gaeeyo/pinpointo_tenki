import java.text.SimpleDateFormat
import java.util.Date

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "nikeno.Tenki"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "nikeno.Tenki"
        minSdk = 21
        targetSdk = 34
        versionCode = 29
        versionName = "0.2.7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        base.archivesName =
            "pinpoint_tenki-$versionName-$versionCode-" + SimpleDateFormat("yyyyMMdd").format(
                Date()
            )
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.annotation:annotation:1.8.2")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}