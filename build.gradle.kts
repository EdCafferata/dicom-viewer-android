plugins {
    // AGP 9+ has built-in Kotlin support, so the separate
    // org.jetbrains.kotlin.android plugin is no longer needed/allowed.
    id("com.android.application") version "9.2.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}
