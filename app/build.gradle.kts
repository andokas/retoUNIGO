plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.bismart"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.bismart"
        minSdk = 27
        targetSdk = 34
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
    packaging {
        resources {
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // Material Design (Para componentes UI bonitos)
    implementation(libs.material.v1110)

    // Navegación (Para gestionar los Fragments con el BottomNav)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    // OpenStreetMaps
    implementation(libs.osmdroid.android)

    // Retrofit y GSON (Para las llamadas a las APIs de Euskadi y Clima)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.adapter.rxjava3)

    // RxJava
    implementation(libs.rxjava.v318)
    implementation(libs.rxandroid)

    // Glide
    implementation(libs.glide)

    implementation(platform(libs.firebase.bom))

    // Dependencias para el Backend
    implementation(libs.firebase.firestore) // Base de datos NoSQL
    implementation(libs.firebase.auth)      // Autenticación
    implementation(libs.firebase.messaging) // Notificaciones Push
    implementation(libs.firebase.storage)   // Almacenamiento de imágenes

}