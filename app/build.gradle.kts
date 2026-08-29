import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

/** The CARTO basemap key, read from `local.properties` or the environment.
 *
 * Empty is a working state, deliberately — exactly as it is on the website. With
 * no key the app draws OpenStreetMap's own tiles, which need none; with one it
 * draws CARTO's Positron and Dark Matter, which is what the website draws and
 * what the two styles were picked against. Never a build failure either way.
 *
 * It is not the website's key. That one is locked to the site's domain, and a
 * referrer lock means nothing to an app, so this repository ships without a key
 * rather than with one that would half-work. */
val tileKey: String = run {
    val local = rootProject.file("local.properties")
    val fromFile = if (local.exists()) {
        Properties().apply { local.inputStream().use(::load) }.getProperty("ttb.tileKey")
    } else {
        null
    }
    fromFile ?: System.getenv("TTB_TILE_KEY") ?: ""
}

android {
    namespace = "ee.tallinntastebuds"
    compileSdk = 35

    defaultConfig {
        applicationId = "ee.tallinntastebuds"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "TILE_KEY", "\"$tileKey\"")
    }

    buildTypes {
        release {
            // The app has no secrets to hide and one screen's worth of code;
            // shrinking it buys nothing worth the risk of a stripped serializer.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.browser)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.coil.compose)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    implementation(libs.osmdroid.android)

    testImplementation(libs.junit)
}
