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
        versionCode = 305
        versionName =
            "3.0.5-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd-HHmm"))
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
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)

    implementation(platform(libs.androidx.compose.bom))

    implementation(files("libs/AMap3DMap_10.1.500_AMapSearch_9.7.4_AMapLocation_6.5.0_20250814.jar"))
    implementation(libs.easypermissions)
    implementation(libs.okhttp)
    implementation(libs.recyclerview.fastscroll)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.gson)
    implementation(libs.material)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

}