plugins {
    id(Plugins.ANDROID_APPLICATION)
    id(Plugins.KOTLIN_ANDROID)
    id(Plugins.KOTLIN_COMPOSE)
    id(Plugins.KOTLIN_SERIALIZATION)
    id(Plugins.HILT_ANDROID)
    id(Plugins.KSP)
}

android {
    namespace = AppConfig.NAMESPACE
    compileSdk = AppConfig.COMPILE_SDK

    defaultConfig {
        applicationId = AppConfig.APPLICATION_ID
        minSdk = AppConfig.MIN_SDK
        targetSdk = AppConfig.TARGET_SDK
        versionCode = AppConfig.VERSION_CODE
        versionName = AppConfig.VERSION_NAME
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

    kotlinOptions {
        jvmTarget = AppConfig.JVM_TARGET
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Core
    implementation(Deps.AndroidX.CORE_KTX)
    implementation(Deps.AndroidX.Lifecycle.RUNTIME_KTX)
    implementation(Deps.AndroidX.Lifecycle.RUNTIME_COMPOSE)
    implementation(Deps.AndroidX.Lifecycle.VIEWMODEL_COMPOSE)
    implementation(Deps.AndroidX.ACTIVITY_COMPOSE)

    // Compose
    implementation(platform(Deps.AndroidX.Compose.BOM))
    implementation(Deps.AndroidX.Compose.UI)
    implementation(Deps.AndroidX.Compose.UI_GRAPHICS)
    implementation(Deps.AndroidX.Compose.UI_TOOLING_PREVIEW)
    implementation(Deps.AndroidX.Compose.MATERIAL3)
    implementation(Deps.AndroidX.Compose.MATERIAL_ICONS_EXTENDED)
    debugImplementation(Deps.AndroidX.Compose.UI_TOOLING)

    // Navigation
    implementation(Deps.AndroidX.Navigation.COMPOSE)

    // Hilt
    implementation(Deps.Hilt.ANDROID)
    ksp(Deps.Hilt.COMPILER)
    implementation(Deps.Hilt.NAVIGATION_COMPOSE)

    // Retrofit + OkHttp
    implementation(Deps.Network.RETROFIT)
    implementation(Deps.Network.OKHTTP)
    implementation(Deps.Network.OKHTTP_LOGGING)
    implementation(Deps.Network.RETROFIT_KOTLINX_SERIALIZATION)

    // Kotlin Serialization
    implementation(Deps.KotlinX.SERIALIZATION_JSON)

    // Room
    implementation(Deps.AndroidX.Room.RUNTIME)
    implementation(Deps.AndroidX.Room.KTX)
    ksp(Deps.AndroidX.Room.COMPILER)

    // Coil
    implementation(Deps.Coil.COMPOSE)
    implementation(Deps.Coil.NETWORK_OKHTTP)

    // DataStore
    implementation(Deps.AndroidX.DataStore.PREFERENCES)

    // Coroutines
    implementation(Deps.KotlinX.COROUTINES_ANDROID)
    implementation(Deps.KotlinX.COROUTINES_CORE)

    // Immutable Collections
    implementation(Deps.KotlinX.COLLECTIONS_IMMUTABLE)
}
