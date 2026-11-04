# Hain release build (T-190): R8 keep rules.
#
# Most libraries bring their own consumer rules (Compose, Hilt, Room, WorkManager, DataStore, Coil,
# Lottie, kotlinx.serialization, ProfileInstaller). This file only keeps what is reached BY NAME at
# runtime: from C/C++ through JNI, by the database driver, or by a serializer. The CI job
# "release-smoke" (.github/workflows/screens.yml) installs the minified APK and starts it.

# Readable crash stack traces (line numbers stay, file names are folded).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ---------------------------------------------------------------------------------------------
# JNI: the llama.cpp bridge (core-llm/src/main/cpp/hain_llm.cpp)
# ---------------------------------------------------------------------------------------------
# Native methods are bound by class and method name (Java_app_hain_llm_internal_NativeLlm_*).
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
-keep class app.hain.llm.internal.NativeLlm { *; }
# C++ calls these back by name with GetMethodID: onToken([B)Z and onProgress(F)Z.
-keep interface app.hain.llm.internal.TokenCallback { *; }
-keep interface app.hain.llm.internal.LoadProgress { *; }
-keep class * implements app.hain.llm.internal.TokenCallback { *; }
-keep class * implements app.hain.llm.internal.LoadProgress { *; }

# ---------------------------------------------------------------------------------------------
# SQLCipher (net.zetetic:sqlcipher-android): its native library calls back into these classes.
# ---------------------------------------------------------------------------------------------
-keep class net.zetetic.database.** { *; }
-keep interface net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.**

# ---------------------------------------------------------------------------------------------
# Room: the generated *_Impl classes are created by name.
# ---------------------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class app.hain.data.**_Impl { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------------------------
# kotlinx.serialization: generated serializers are looked up through the Companion.
# ---------------------------------------------------------------------------------------------
-keepclassmembers @kotlinx.serialization.Serializable class app.hain.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class app.hain.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.hain.**$$serializer { *; }
-dontnote kotlinx.serialization.**

# ---------------------------------------------------------------------------------------------
# WorkManager: pack downloads are stored with the worker's class name and created again by name
# (also after an update, so the name must not change between builds).
# ---------------------------------------------------------------------------------------------
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class app.hain.models.internal.ModelDownloadWorker { *; }

# ---------------------------------------------------------------------------------------------
# Compose: libraries bring their rules; keep the runtime's debug names out of warnings.
# ---------------------------------------------------------------------------------------------
-dontwarn androidx.compose.**

# Annotations that are only needed at compile time.
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn org.jetbrains.annotations.**
