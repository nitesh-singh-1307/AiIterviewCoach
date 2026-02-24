import java.util.Properties

plugins {
    id("com.android.application")         // ← MUST be first
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")         // ← KSP before Hilt
    id("com.google.dagger.hilt.android")  // ← Hilt always last, no version here
}


val localProps = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}



android {
    namespace = "com.example.aiinterview"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.aiinterview"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // ⚠️  Replace with your Anthropic API key — never commit a real key to VCS.
        // Better: move this to local.properties + buildConfigField
        buildConfigField(
            "String",
            "GROQ_API_KEY",
            "\"${localProps["GROQ_API_KEY"]}\""
        )

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17  // ← was 11, change to 17
        targetCompatibility = JavaVersion.VERSION_17  // ← was 11, change to 17
    }
    kotlinOptions {
        jvmTarget = "17"  // ← must match above
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
//    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-android-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation(libs.androidx.splashscreen)
    implementation(libs.coroutines.android)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)


}
