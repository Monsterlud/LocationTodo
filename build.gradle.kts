// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.androidx.safe.args) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.mapsplatform.secrets.gradle) apply false
}