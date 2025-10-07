buildscript {
    extra.apply {
        set("room_version", "2.5.0")
    }
}

plugins {
    alias(libs.plugins.androidApplication) version "8.13.0" apply false
    alias(libs.plugins.androidLibrary) version "8.13.0" apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) version "2.2.20" apply false
    alias(libs.plugins.ksp) version "2.2.20-2.0.3" apply false
    kotlin("plugin.serialization") version "2.2.20"
    id("com.google.dagger.hilt.android") version "2.57.2" apply false
}