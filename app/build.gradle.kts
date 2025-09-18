import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.kotlinCompose)
}

android {
    namespace = "com.microbus.announcer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.microbus.announcer"
        minSdk = 26
        targetSdk = 36
        versionCode = 240
        versionName =
            "2.4.0-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd-HHmm"))
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
        jvmTarget = "11"
    }

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
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.easypermissions)
    implementation(libs.viewpager2)
    implementation(libs.recyclerview)
//    implementation(files("libs/AMap3DMap_10.1.200_AMapSearch_9.7.4_AMapLocation_6.4.9_20241226_reIcon.jar"))
    implementation(files("libs/AMap3DMap_10.1.302_AMapSearch_9.7.4_AMapLocation_6.5.0_20250804.jar"))
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.core)
//    testImplementation(libs.junit)
//    implementation(libs.rxjava)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.kotlinx.serialization.json)
//    androidTestImplementation(libs.androidx.junit)
//    androidTestImplementation(libs.androidx.espresso.core)
//    androidTestImplementation(libs.design)
    implementation(libs.gson)
    implementation(libs.material)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.ui.text)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}