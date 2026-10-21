plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.gur0s.chord.core"
    compileSdk = 37

    defaultConfig {
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }
}

kotlin {
    android()
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.coroutines.android)
                api(libs.ktor.client.core)
                api(libs.ktor.client.cio)
                api(libs.ktor.client.content.negotiation)
                api(libs.ktor.serialization.kotlinx.json)
                api(libs.okio)
                api(libs.kotlinx.datetime)
                api(libs.kotlinx.coroutines.core)
            }
        }
        val androidMain by getting {
            dependsOn(commonMain)
            dependencies {
                api(libs.androidx.lifecycle.runtime)
                api(libs.androidx.lifecycle.viewmodel)
                api(libs.androidx.room.runtime)
                api(libs.androidx.room.ktx)
            }
        }
        val desktopMain by getting {
            dependsOn(commonMain)
            dependencies {
                api(libs.androidx.room.runtime)
                api(libs.androidx.room.ktx)
            }
        }
    }
}

android {
    sourceSets {
        main {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs.add("-Xopt-in=kotlin.RequiresOptIn")
    }
}