# Add project specific ProGuard rules here.

# Keep Jsoup
-keep class org.jsoup.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep our data classes
-keep class com.cybersecdaily.widget.DailyReport { *; }
-dontwarn org.jspecify.annotations.**
-dontwarn org.jsoup.**