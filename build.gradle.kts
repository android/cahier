buildscript {
    extra.apply {
        set("room_version", "2.5.0")
    }
}

plugins {
    alias(libs.plugins.androidApplication) version "8.9.1" apply false
    alias(libs.plugins.androidLibrary) version "8.9.1" apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) version "2.1.10" apply false
    alias(libs.plugins.ksp) version "2.1.10-1.0.29" apply false
    kotlin("plugin.serialization") version "2.1.10"
    id("com.google.dagger.hilt.android") version "2.57.1" apply false
}