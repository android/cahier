buildscript {
    extra.apply {
        set("room_version", "2.5.0")
    }
}

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.ksp) apply false
    kotlin("plugin.serialization") version "2.3.10"
    id("com.google.dagger.hilt.android") version "2.59.2" apply false
}