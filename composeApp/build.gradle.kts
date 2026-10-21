plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlin.serialization)
    id("com.jetbrains.compose")
}

kotlin {
    android()
    jvm("desktop")
}

android {
    namespace = "com.gur0s.chord"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.gur0s.chord"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }
}

compose.desktop {
    application {
        mainClass = "com.gur0s.chord.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.DMG, TargetFormat.MSI, TargetFormat.DEB, TargetFormat.RPM, TargetFormat.TAR_GZ)
        }
    }
}

dependencies {
    val composeBom = platform("org.jetbrains.compose.compose-bom:2025.09.00")
    implementation(composeBom)
    implementation(compose.material3)
    implementation(compose.runtime)
    implementation(compose.ui)
    implementation(compose.foundation)
    implementation(compose.animation)
    implementation(compose.material.icons.extended)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.okio)
    implementation(project(":core"))
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.media3:media3-exoplayer:1.6.1")
    implementation("androidx.media3:media3-ui:1.6.1")
    implementation("androidx.media3:media3-session:1.6.1")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
}

android {
    namespace = "com.gur0s.chord"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.gur0s.chord"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.0"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/*.kotlin_module"
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs.add("-Xopt-in=kotlin.RequiresOptIn")
    }
}

compose.desktop {
    application {
        mainClass = "com.gur0s.chord.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.DMG, TargetFormat.MSI, TargetFormat.DEB, TargetFormat.RPM, TargetFormat.TAR_GZ)
        }
        packageName = "Chord"
        packageVersion = "1.0.0"
        vendor = "gur0s"
    }
}
