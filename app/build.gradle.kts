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



    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    signingConfigs {
        create("release") {
            storeFile = file("C:/Users/Karpi/my-release-key.jks")
            storePassword = "Obraczka04"
            keyAlias = "my-key-alias"
            keyPassword = "Obraczka04"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
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
    implementation(libs.firebase.analytics)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.room.runtime)
    implementation(libs.play.services.gcm)
    implementation(libs.androidx.media3.common)
    implementation(libs.firebase.auth)
    implementation (libs.google.firebase.analytics)
    implementation(libs.firebase.ai)
    implementation(libs.guava)
    implementation (libs.google.firebase.appcheck.playintegrity)
    implementation(libs.reactive.streams)
    annotationProcessor(libs.androidx.room.compiler)
    implementation (libs.androidx.work.runtime.ktx )
    implementation(libs.androidx.recyclerview)
}