import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// 릴리스 서명 정보(비밀) — 루트의 keystore.properties(=git 제외)에서 읽는다.
// 파일이 없으면 서명 미설정(디버그·CI 빌드는 그대로 동작). keystore.properties.example 참고.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting
        val desktopTest by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            // --- Room KMP (진실의 원천 · commonMain 공유, 오프라인 우선) ---
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            // --- Koin DI (멀티플랫폼) ---
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            // --- 화면 계층: ViewModel · Navigation · 마크다운 렌더 (CMP 공유) ---
            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.navigation.compose)
            implementation(libs.markdown.renderer)
            implementation(libs.markdown.renderer.m3)
            // --- Ktor (멀티플랫폼 HTTP) + serialization — Phase 4-B OpenAI 호출 ---
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            // Koin Android (androidContext)
            implementation(libs.koin.androidx.compose)
            // Dispatchers.Main (viewModelScope) 제공
            implementation(libs.kotlinx.coroutines.android)
            // 구글 인가(Authorization API) — 캘린더 액세스 토큰 (Phase 3)
            implementation(libs.play.services.auth)
            // Ktor 엔진(Android) + API 키 암호화 저장
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.security.crypto)
        }

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            // 데스크톱(Swing/AWT)용 Dispatchers.Main — viewModelScope 가 필요로 함
            implementation(libs.kotlinx.coroutines.swing)
            // Ktor 엔진(Desktop JVM)
            implementation(libs.ktor.client.cio)
        }

        desktopTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// Koin 4.2.2 의 Android 아티팩트가 androidx.activity 1.12.4(=compileSdk 36·AGP 8.9.1 요구)를
// 끌어온다. 핀으로 고정한 버전 매트릭스(AGP 8.7.3 / compileSdk 35)를 유지하기 위해
// SDK 35 와 호환되는 최신 activity 1.10.1 로 되돌린다(우리 사용 범위엔 1.12 API 불필요).
configurations.all {
    resolutionStrategy {
        force(
            "androidx.activity:activity:${libs.versions.activityCompose.get()}",
            "androidx.activity:activity-ktx:${libs.versions.activityCompose.get()}",
            "androidx.activity:activity-compose:${libs.versions.activityCompose.get()}",
        )
    }
}

android {
    namespace = "com.kangtaeyoung.daynote"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kangtaeyoung.daynote"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        // keystore.properties 가 있을 때만 릴리스 서명 구성(없으면 미서명 — 로컬/CI 안전).
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // 키스토어가 있으면 릴리스 서명 적용(bundleRelease/assembleRelease → Play 업로드/사이드로드 가능).
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Room 컴파일러는 Kotlin 코드를 생성하게 한다(KMP 권장 · 양 타겟 공유).
ksp {
    arg("room.generateKotlin", "true")
}

// Room KSP를 Android·Desktop 양 타겟에 적용한다 (commonMain 의 @Database 처리).
dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
}

compose.desktop {
    application {
        mainClass = "com.kangtaeyoung.daynote.MainKt"
        // 패키징(jpackage)은 풀 JDK가 필요하다. Android Studio JBR엔 jpackage가 없으므로,
        // 빌드는 JBR로 돌리되 패키징용 JDK 경로만 -Pdaynote.jpackage.jdk=... 로 따로 지정한다.
        (findProperty("daynote.jpackage.jdk") as String?)?.let { javaHome = it }
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Dmg, TargetFormat.Deb)
            packageName = "DayNote"
            packageVersion = "1.0.0"
        }
    }
}
