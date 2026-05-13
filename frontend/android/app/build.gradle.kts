import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.navigation.safeargs)
}

private fun org.gradle.api.Project.pickedAimongApiBaseUrl(): String? {
    val props = Properties()
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) localFile.inputStream().use { props.load(it) }
    val fromGradle = (findProperty("aimong.api.baseUrl") as? String)?.trim().orEmpty()
    val fromLocal = props.getProperty("aimong.api.baseUrl")?.trim().orEmpty()
    val fromEnv = System.getenv("AIMONG_API_BASE_URL")?.trim().orEmpty()
    return listOf(fromGradle, fromLocal, fromEnv).firstOrNull { it.isNotEmpty() }
}

private fun normalizeApiBaseUrl(raw: String): String {
    val trimmed = raw.trim()
    return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
}

/** assembleRelease/bundleRelease 등에서만 URL 미설정 시 실패 (debug 작업은 설정 단계에서 깨지지 않도록). */
private fun org.gradle.api.Project.shouldEnforceReleaseApiUrl(): Boolean {
    val tasks = gradle.startParameter.taskNames.map { it.lowercase() }
    if (tasks.isEmpty()) return false
    return tasks.any { t ->
        (t.contains("release") || t.contains("bundle")) && !t.contains("debug")
    }
}

/** API 베이스 URL: `-Paimong.api.baseUrl=`, `local.properties`의 `aimong.api.baseUrl`, 환경변수 `AIMONG_API_BASE_URL` 순으로 적용. */
fun org.gradle.api.Project.resolveAimongApiBaseUrl(forReleaseVariant: Boolean): String {
    val picked = pickedAimongApiBaseUrl()
    if (!picked.isNullOrBlank()) return normalizeApiBaseUrl(picked)
    if (forReleaseVariant) {
        if (shouldEnforceReleaseApiUrl()) {
            error(
                "Release 빌드는 API 주소가 필요합니다. " +
                    "local.properties에 aimong.api.baseUrl=... 또는 " +
                    "-Paimong.api.baseUrl=... 또는 환경변수 AIMONG_API_BASE_URL 을 설정하세요."
            )
        }
        return normalizeApiBaseUrl("https://RELEASE_API_URL_NOT_SET.invalid/api/")
    }
    return normalizeApiBaseUrl("http://10.0.2.2:8080/api/")
}

android {
    namespace = "com.kduniv.aimong"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kduniv.aimong"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isDebuggable = true
            val url = resolveAimongApiBaseUrl(forReleaseVariant = false)
            buildConfigField("String", "API_BASE_URL", "\"$url\"")
        }
        release {
            isMinifyEnabled = true
            val url = resolveAimongApiBaseUrl(forReleaseVariant = true)
            buildConfigField("String", "API_BASE_URL", "\"$url\"")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

hilt {
    enableAggregatingTask = true
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation(libs.androidx.core.ktx)
    // Plain Maven coordinates: Cursor/Kotlin LSP often fails to attach version-catalog deps to the IDE classpath.
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Hilt 설정
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    implementation(libs.glide)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.messaging.ktx)

    implementation(libs.play.services.auth)

    implementation(libs.mlkit.entity.extraction)
    implementation(libs.mlkit.text.recognition.korean)

    implementation(libs.lottie.android)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
