import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val releaseSigning = Properties().apply {
    rootProject.file("signing/signing.properties").inputStream().use { load(it) }
}
val localConfig = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}
fun configString(name: String, fallback: String = "") = localConfig.getProperty(name, fallback)
    .replace("\\", "\\\\").replace("\"", "\\\"")

fun configStringAny(vararg names: String, fallback: String = "") = names
    .firstNotNullOfOrNull { localConfig.getProperty(it)?.takeIf(String::isNotBlank) }
    .orEmpty().ifBlank { fallback }
    .replace("\\", "\\\\").replace("\"", "\\\"")

android { namespace = "com.example.kaoyanfocus"; compileSdk = 35
    defaultConfig {
        applicationId = "com.example.kaoyanfocus"
        minSdk = 26
        targetSdk = 35
        versionCode = 27
        versionName = "2.8.9"
        buildConfigField("String", "QWEN_API_KEY", "\"${configStringAny("QWEN_API_KEY", "DASHSCOPE_API_KEY")}\"")
        buildConfigField("String", "QWEN_BASE_URL", "\"${configString("QWEN_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1")}\"")
        buildConfigField("String", "QWEN_MODEL", "\"${configString("QWEN_MODEL", "qwen3-vl-flash")}\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ksp { arg("room.schemaLocation", "$projectDir/schemas") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    sourceSets { getByName("androidTest").assets.srcDir("$projectDir/schemas") }
    signingConfigs {
        create("release") {
            storeFile = rootProject.file(releaseSigning.getProperty("storeFile"))
            storePassword = releaseSigning.getProperty("storePassword")
            keyAlias = releaseSigning.getProperty("keyAlias")
            keyPassword = releaseSigning.getProperty("keyPassword")
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
}

// AGP 8.7 does not always add Kotlin-only unit-test output to Test.classpath
// when the project path contains non-ASCII characters. Keep the test runner
// pointed at the actual compiler output so local verification remains stable.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    testClassesDirs = files(layout.buildDirectory.dir("tmp/kotlin-classes/debugUnitTest"))
    classpath = files(testClassesDirs, classpath)
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
}
