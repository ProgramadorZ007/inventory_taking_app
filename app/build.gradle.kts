plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.procesadoraperu.inventario"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.procesadoraperu.inventario"
        minSdk = 26
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // Core AndroidX
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.activity:activity:1.8.2")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // ROOM (SQLite ORM)
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")

    // RETROFIT + OkHttp (Networking)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // LIFECYCLE (ViewModel + LiveData)
    val lifecycle_version = "2.7.0"
    implementation("androidx.lifecycle:lifecycle-viewmodel:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-livedata:$lifecycle_version")
    implementation("androidx.lifecycle:lifecycle-runtime:$lifecycle_version")

    // GPS / Ubicación
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // ── ESCÁNER DE CÓDIGO DE BARRAS (ZXing Android Embedded) ──
    // Esta es la librería correcta que provee ScanContract, ScanOptions, etc.
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    // La librería core de ZXing (requerida por zxing-android-embedded)
    implementation("com.google.zxing:core:3.5.3")

    // Librería de Lottie para la animación del Splash
    implementation(libs.lottie)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}