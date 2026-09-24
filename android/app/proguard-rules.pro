# Keep Retrofit & Gson data models
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.stockpilot.app.data.models.** { *; }

# OkHttp & Retrofit
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
