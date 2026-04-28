plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.bismart"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.bismart"
        minSdk = 27
        targetSdk = 36
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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // Material Design (Para componentes UI bonitos)
    implementation("com.google.android.material:material:1.11.0")

    // Navegación (Para gestionar los Fragments con el BottomNav)
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Google Maps y Localización
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // Retrofit y GSON (Para las llamadas a las APIs de Euskadi y Clima)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // RxJava (Tema 19)
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("io.reactivex.rxjava3:rxandroid:3.0.2")

    // Glide (Tema 16 - Carga de imágenes)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Importamos el BoM de Firebase para gestionar versiones automáticamente
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

    // Dependencias para el Backend
    implementation("com.google.firebase:firebase-firestore") // Base de datos NoSQL
    implementation("com.google.firebase:firebase-auth")      // Autenticación
    implementation("com.google.firebase:firebase-messaging") // Notificaciones Push
    implementation("com.google.firebase:firebase-storage")   // Almacenamiento de imágenes

}