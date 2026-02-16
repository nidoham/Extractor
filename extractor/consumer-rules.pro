# Consumer ProGuard rules for Extractor library
# These rules will be applied to apps that use this library

# Keep NewPipeExtractor classes
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.mozilla.javascript.** { *; }
-dontwarn org.mozilla.javascript.tools.**

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep generic signatures for Kotlin
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable