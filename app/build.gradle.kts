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

/** Release signing, read from `keystore.properties` or the environment.
 *
 * Absent is a working state, and has to be: F-Droid builds this app from the
 * tag and signs the result with its own key, so it never has a keystore and a
 * missing one must never fail the build. The signed APK on GitHub Releases is
 * the path that needs these, and CI supplies them from repository secrets.
 *
 * Nothing here is ever committed. `keystore.properties` and every `*.jks` are
 * in .gitignore, and the values below are read, never written. */
val keystore = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun signingSetting(property: String, variable: String): String? =
    (keystore.getProperty(property) ?: System.getenv(variable))?.takeIf { it.isNotBlank() }

val ksPath = signingSetting("storeFile", "TTB_STORE_FILE")
val ksPassword = signingSetting("storePassword", "TTB_STORE_PASSWORD")
val ksAlias = signingSetting("keyAlias", "TTB_KEY_ALIAS")
val ksKeyPassword = signingSetting("keyPassword", "TTB_KEY_PASSWORD")

/** All four, and a file actually on disk. A half-configured keystore is a
 *  typo, and signing with it would fail late and confusingly. */
val ksFile = ksPath?.let { rootProject.file(it) }?.takeIf { it.exists() }
val canSign = ksFile != null && ksPassword != null && ksAlias != null && ksKeyPassword != null

android {
    namespace = "ee.tallinntastebuds"
    compileSdk = 35

    defaultConfig {
        applicationId = "ee.tallinntastebuds"
        minSdk = 26
        targetSdk = 35
        // Literal on purpose. F-Droid checks out the tag and reads these two
        // straight out of this file, so they cannot be computed from the
        // environment or from git — bump them by hand for each release, and
        // the release workflow checks the tag agrees with versionName.
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "TILE_KEY", "\"$tileKey\"")
    }

    signingConfigs {
        if (canSign) {
            create("release") {
                storeFile = ksFile
                storePassword = ksPassword
                keyAlias = ksAlias
                keyPassword = ksKeyPassword
            }
        }
    }

    buildTypes {
        release {
            // Null when there is no keystore, which leaves an unsigned APK —
            // the input F-Droid wants, and a clear failure for anyone who meant
            // to sign and mistyped a secret.
            signingConfig = if (canSign) signingConfigs.getByName("release") else null

            // The app has no secrets to hide and one screen's worth of code.
            // Shrinking buys nothing worth the risk of a stripped serializer,
            // and leaving it off is also what keeps an F-Droid build of this
            // tag byte-for-byte comparable with the one built here.
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
