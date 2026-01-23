plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "top.dwill.phonestatus"
    compileSdk = 34

    defaultConfig {
        applicationId = "top.dwill.phonestatus"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    // 产品变体：cf = Cloudflare Workers, selfhosted = 自托管服务器
    flavorDimensions += "server"
    productFlavors {
        create("cf") {
            dimension = "server"
            buildConfigField("String", "DEFAULT_API_URL", "\"https://phone-status-api-v2.zhangtao3788.workers.dev/update\"")
            buildConfigField("String", "DEFAULT_API_KEY", "\"dwill-status-2026-x7k9m2p4q8r1\"")
        }
        create("selfhosted") {
            dimension = "server"
            applicationIdSuffix = ".selfhosted"
            // 本地测试：改成你电脑的局域网 IP；部署后改成实际域名
            buildConfigField("String", "DEFAULT_API_URL", "\"http://192.168.10.104:9529/api/update\"")
            buildConfigField("String", "DEFAULT_API_KEY", "\"dwill-status-2026-x7k9m2p4q8r1\"")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")
}
