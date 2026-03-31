object Deps {

    object AndroidX {
        const val CORE_KTX = "androidx.core:core-ktx:${Versions.CORE_KTX}"
        const val ACTIVITY_COMPOSE = "androidx.activity:activity-compose:${Versions.ACTIVITY_COMPOSE}"

        object Lifecycle {
            const val RUNTIME_KTX = "androidx.lifecycle:lifecycle-runtime-ktx:${Versions.LIFECYCLE}"
            const val RUNTIME_COMPOSE = "androidx.lifecycle:lifecycle-runtime-compose:${Versions.LIFECYCLE}"
            const val VIEWMODEL_COMPOSE = "androidx.lifecycle:lifecycle-viewmodel-compose:${Versions.LIFECYCLE}"
        }

        object Compose {
            const val BOM = "androidx.compose:compose-bom:${Versions.COMPOSE_BOM}"
            const val UI = "androidx.compose.ui:ui"
            const val UI_GRAPHICS = "androidx.compose.ui:ui-graphics"
            const val UI_TOOLING = "androidx.compose.ui:ui-tooling"
            const val UI_TOOLING_PREVIEW = "androidx.compose.ui:ui-tooling-preview"
            const val MATERIAL3 = "androidx.compose.material3:material3"
            const val MATERIAL_ICONS_EXTENDED = "androidx.compose.material:material-icons-extended"
        }

        object Navigation {
            const val COMPOSE = "androidx.navigation:navigation-compose:${Versions.NAVIGATION_COMPOSE}"
        }

        object Room {
            const val RUNTIME = "androidx.room:room-runtime:${Versions.ROOM}"
            const val KTX = "androidx.room:room-ktx:${Versions.ROOM}"
            const val COMPILER = "androidx.room:room-compiler:${Versions.ROOM}"
        }

        object DataStore {
            const val PREFERENCES = "androidx.datastore:datastore-preferences:${Versions.DATASTORE}"
        }
    }

    object Hilt {
        const val ANDROID = "com.google.dagger:hilt-android:${Versions.HILT}"
        const val COMPILER = "com.google.dagger:hilt-android-compiler:${Versions.HILT}"
        const val NAVIGATION_COMPOSE = "androidx.hilt:hilt-navigation-compose:${Versions.HILT_NAVIGATION_COMPOSE}"
    }

    object Network {
        const val RETROFIT = "com.squareup.retrofit2:retrofit:${Versions.RETROFIT}"
        const val OKHTTP = "com.squareup.okhttp3:okhttp:${Versions.OKHTTP}"
        const val OKHTTP_LOGGING = "com.squareup.okhttp3:logging-interceptor:${Versions.OKHTTP}"
        const val RETROFIT_KOTLINX_SERIALIZATION = "com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:${Versions.RETROFIT_KOTLINX_SERIALIZATION}"
    }

    object KotlinX {
        const val SERIALIZATION_JSON = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KOTLINX_SERIALIZATION_JSON}"
        const val COROUTINES_ANDROID = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.COROUTINES}"
        const val COROUTINES_CORE = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.COROUTINES}"
        const val COLLECTIONS_IMMUTABLE = "org.jetbrains.kotlinx:kotlinx-collections-immutable:${Versions.KOTLINX_COLLECTIONS_IMMUTABLE}"
    }

    object Coil {
        const val COMPOSE = "io.coil-kt.coil3:coil-compose:${Versions.COIL}"
        const val NETWORK_OKHTTP = "io.coil-kt.coil3:coil-network-okhttp:${Versions.COIL}"
    }
}
