plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.carplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.carplayer"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        jvmTarget = "11"
    }

    viewBinding {
        enable = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(project(":shared"))
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    // Media3 ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.2.1")

    // UI components for video playback (optional if audio only)
    implementation("androidx.media3:media3-ui:1.2.1")

    // For session/player management (optional for car/automotive)
    implementation("androidx.media3:media3-session:1.2.1")

    implementation("androidx.media3:media3-ui:1.2.1")

    // HLS support (adaptive streaming)
    implementation("androidx.media3:media3-exoplayer-hls:1.2.1")

    implementation("androidx.core:core-ktx:1.13.0") // ensures coroutine support

    implementation("io.coil-kt:coil:2.4.0")

    implementation("androidx.palette:palette:1.0.0")
}