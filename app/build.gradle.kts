plugins {
    id("com.android.application")
}

android {
    namespace = "com.rahulpranaya.iggate"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rahulpranaya.iggate"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = false
        }
    }
}
