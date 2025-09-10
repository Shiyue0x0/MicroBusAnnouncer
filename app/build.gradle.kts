import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "com.microbus.announcer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.microbus.announcer"
        minSdk = 26
        targetSdk = 36
        versionCode = 230
        versionName =
            "2.3.0-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd-HHmm"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isShrinkResources = false

        }
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a")
        }
    }

    android.applicationVariants.all {
        val buildType = this.buildType.name
        val variant = this
        outputs.all {
            val abiName =
                this.filters.find { it.filterType == com.android.build.api.variant.FilterConfiguration.FilterType.ABI.name }?.identifier
            val outputImpl = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            outputImpl.outputFileName = "An-${variant.versionName}-${buildType}-${abiName}.apk"
        }
    }

}


dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.easypermissions)
    implementation(libs.viewpager2)
    implementation(libs.recyclerview)
    implementation(files("libs/AMap3DMap_10.1.200_AMapSearch_9.7.4_AMapLocation_6.4.9_20241226_reIcon.jar"))
    implementation(files("libs/mobile-ffmpeg-full-gpl-4.4.aar"))
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.core)
    testImplementation(libs.junit)
    implementation(libs.rxjava)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.kotlinx.serialization.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.design)
}