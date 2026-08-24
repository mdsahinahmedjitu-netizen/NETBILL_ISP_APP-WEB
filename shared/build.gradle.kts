plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "com.example.shared"
        compileSdk = 37
        minSdk = 24

        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.jb.compose.runtime)
                implementation(libs.jb.compose.foundation)
                implementation(libs.jb.compose.material3)
                implementation(libs.ktor.client.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.junit)
            }
        }
        androidMain {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
    }
}
