plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
}

import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    val composeBom = platform("org.jetbrains.compose.compose-bom:2024.10.00")
    implementation(composeBom)
    implementation("org.jetbrains.compose.material3:material3")
    implementation("org.jetbrains.compose.runtime:runtime")
    implementation("org.jetbrains.compose.ui:ui")
    implementation("org.jetbrains.compose.foundation:foundation")
    implementation("org.jetbrains.compose.animation:animation")
    implementation("org.jetbrains.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("io.ktor:ktor-client-core:3.6.0")
    implementation("io.ktor:ktor-client-cio:3.6.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.6.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.6.0")
    implementation("com.squareup.okio:okio:3.10.0")
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
            targetFormats(
                TargetFormat.DMG,
                TargetFormat.MSI,
                TargetFormat.DEB,
                TargetFormat.RPM,
                TargetFormat.TAR_GZ
            )
        }
        packageName = "Chord"
        packageVersion = "1.0.0"
        vendor = "gur0s"
    }
}