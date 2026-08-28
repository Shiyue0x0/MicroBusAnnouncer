import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinCompose)
}

android {
    namespace = "com.microbus.announcer"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.microbus.announcer"
        minSdk = 26
        targetSdk = 37
        versionCode = 313
        versionName =
            "3.1.3-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd-HHmm"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("debug")

        }
    }

//    kotlin {
//        jvmToolchain(11)
//    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }


    layout.buildDirectory.set(file("C:/AndroidBuilds/Announcer"))


}

dependencies {

    implementation(libs.androidx.compose.ui.viewbinding)
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.fragment.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.litert.api)
    implementation(libs.easypermissions)
    implementation(libs.okhttp)
    implementation(libs.recyclerview.fastscroll)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.gson)
    implementation(libs.material)
    implementation(files("libs/AMap3DMap_11.2.100_AMapSearch_9.8.1_AMapLocation_11.2.100_20260805.jar"))
    implementation(libs.appleliquidglassforandroid)


    // miuix
    implementation(libs.miuix.ui.android)

    //exoplayer
    implementation(libs.androidx.media3.exoplayer)

    // Test
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

}