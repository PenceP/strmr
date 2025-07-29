import java.util.Properties

val secrets = Properties().apply {
    val secretsFile = rootProject.file("secrets.properties")
    if (secretsFile.exists()) {
        secretsFile.inputStream().use { load(it) }
    }
}

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") version "2.0.0-1.0.21"
    id("com.google.dagger.hilt.android") version "2.48"
    id("io.gitlab.arturbosch.detekt") version "1.23.4"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

android {
    namespace = "com.strmr.ai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.strmr.ai"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = rootProject.file("version.txt").readText().trim()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "TRAKT_API_KEY", "\"${secrets.getProperty("TRAKT_API_KEY", "")}\"")
        buildConfigField("String", "OMDB_API_KEY", "\"${secrets.getProperty("OMDB_API_KEY", "")}\"")
        buildConfigField("String", "TMDB_READ_KEY", "\"${secrets.getProperty("TMDB_READ_KEY", "")}\"")
        
        // Target specific resources for Android TV
        resourceConfigurations += listOf("en", "xhdpi", "xxhdpi", "xxxhdpi")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true  // Enable resource shrinking - saves 10-20MB
            isDebuggable = false
            
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    
    // Target Android TV architectures only - saves 50-80MB from LibVLC
    splits {
        abi {
            enable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")  // Primary Android TV architectures
            isUniversalApk = false
        }
    }
    
    // Exclude unnecessary resources and architectures
    packagingOptions {
        jniLibs {
            excludes += listOf(
                "**/x86/**",
                "**/x86_64/**"  // Exclude x86 architectures if not needed
            )
        }
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        
        // Compose compiler metrics for performance analysis
        freeCompilerArgs += listOf(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${layout.buildDirectory.get().asFile.absolutePath}/compose_compiler",
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${layout.buildDirectory.get().asFile.absolutePath}/compose_compiler"
        )
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.tv.foundation)
    implementation(libs.androidx.tv.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.compose.runtime:runtime-livedata:1.6.7")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)
    
    // Paging 3
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Hilt Dependency Injection
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    
    // ExoPlayer Media3 with BOM for consistent versions - saves ~5-10MB
    implementation(platform("androidx.media3:media3-bom:1.6.1"))
    implementation("androidx.media3:media3-exoplayer")
    implementation("androidx.media3:media3-exoplayer-dash")
    implementation("androidx.media3:media3-exoplayer-hls")
    implementation("androidx.media3:media3-ui")
    implementation("androidx.media3:media3-datasource-okhttp")
    // Removed smoothstreaming - uncomment if needed
    // implementation("androidx.media3:media3-exoplayer-smoothstreaming")
    
    // LibVLC for Android - MAJOR APK SIZE IMPACT (~80-120MB)
    // Consider removing if ExoPlayer handles your streaming needs
    implementation("org.videolan.android:libvlc-all:4.0.0-eap15")
    
    // OkHttp for network requests
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // DataStore for settings
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Gson for JSON parsing
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Security for encrypted preferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Memory leak detection (debug only)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
    
    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.1.1")
    testImplementation("org.mockito:mockito-inline:5.1.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("androidx.room:room-testing:2.6.1")
    testImplementation("com.google.dagger:hilt-android-testing:2.48")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    kspTest("com.google.dagger:hilt-compiler:2.48")
    
    // Android Test dependencies
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
    androidTestImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.48")
}

// Detekt Configuration
detekt {
    config.setFrom(file("../config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = false
    
    reports {
        html.required.set(true)
        xml.required.set(true)
        txt.required.set(true)
        sarif.required.set(true)
    }
}

// KtLint Configuration
ktlint {
    version.set("1.0.1")
    debug.set(false)
    verbose.set(true)
    android.set(true)
    outputToConsole.set(true)
    outputColorName.set("RED")
    ignoreFailures.set(false)
    
    filter {
        exclude("**/generated/**")
        include("**/src/**")
    }
}