import org.gradle.api.JavaVersion

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {

    namespace = "com.taskmenadzer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.taskmenadzer"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
    applicationVariants.all {
        if (buildType.name == "release") {
            outputs.all {
                val output = this as com.android.build.gradle.api.ApkVariantOutput
                output.outputFileName = "task-menadzer.apk"
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.appcompat.v161)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.room.runtime)
    implementation(libs.play.services.gcm)
    implementation(libs.androidx.media3.common)
    annotationProcessor(libs.androidx.room.compiler)
    implementation (libs.androidx.work.runtime.ktx )
    implementation(libs.androidx.recyclerview)
}