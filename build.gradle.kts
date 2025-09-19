buildscript {
    extra.apply {
        set("room_version", "2.5.0")
    }
}

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.ksp) apply false
    kotlin("plugin.serialization") version "2.1.10"
    id("com.google.dagger.hilt.android") version "2.57.1" apply false
}