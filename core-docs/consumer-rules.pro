# Häppchen 9 (T-120/T-121): kept when the app is shrunk with R8 (T-190).
# ONNX Runtime calls its Java classes from native code (JNI).
-keep class ai.onnxruntime.** { *; }
# pdfbox-android loads fonts, filters and security handlers by name.
-keep class com.tom_roush.** { *; }
-dontwarn com.gemalto.jp2.**
-dontwarn org.bouncycastle.**
