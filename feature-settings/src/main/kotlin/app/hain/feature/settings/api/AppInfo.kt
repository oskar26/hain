package app.hain.feature.settings.api

/** Build facts shown in "Über Hain" (T-215); the app fills them from its BuildConfig. */
data class AppInfo(
    val versionName: String = "",
    val versionCode: Int = 0,
    /** Full commit SHA the build was made from; "local" for builds on a computer. */
    val commit: String = "",
    /** Build day as yyyy-MM-dd; empty for local builds. */
    val buildDate: String = "",
) {
    /** T-281: a debug build (nightly APK, version "…-debug") can only be updated by the next nightly. */
    val nightlyOnly: Boolean get() = versionName.endsWith(DEBUG_SUFFIX)

    /** First 7 characters of [commit], like GitHub shows it. */
    val shortCommit: String get() = commit.take(SHORT_SHA)

    /** "0.1.3.1 (412) · a1b2c3d" — what the version row says. */
    fun versionLine(): String =
        buildString {
            append(versionName)
            if (versionCode > 0) append(" ($versionCode)")
            if (shortCommit.isNotEmpty()) append(" · ").append(shortCommit)
        }

    private companion object {
        const val SHORT_SHA = 7
        const val DEBUG_SUFFIX = "-debug"
    }
}
