plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {

    namespace = "com.example.taskmenadzer"
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
            storeFile = file("J://klucz/keystore.jks")
            storePassword = "Obraczka04"
            keyAlias = "key0"
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
    // Import the BoM for the Firebase platform
    implementation(platform(libs.firebase.bom)) // Importuj BOM

    // Add the dependencies for Firebase products you want to use
    // When using the BoM, you don't specify versions in Firebase library dependencies

    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(libs.firebase.ai)
    // Inne zależności Firebase, które mogą być objęte BOM:
    // implementation("com.google.firebase:firebase-messaging-ktx") // Jeśli używasz FCM zamiast GCM
    implementation(libs.firebase.appcheck.debug) // <--- TA LINIJKA JEST NOWA
    // Zależności nieobjęte BOM (zostaw prefiks libs. jeśli definiujesz je w libs.versions.toml):
    implementation(libs.androidx.appcompat.v161)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.room.runtime)
    implementation(libs.play.services.gcm) // Rozważ migrację do FCM
    implementation(libs.androidx.media3.common)
    implementation(libs.firebase.ai) // Sprawdź, czy to jest objęte BOM lub czy jest potrzebne
    implementation(libs.guava)
    implementation(libs.reactive.streams)
    annotationProcessor(libs.androidx.room.compiler)
    implementation (libs.androidx.work.runtime.ktx )
    implementation(libs.androidx.recyclerview)
}