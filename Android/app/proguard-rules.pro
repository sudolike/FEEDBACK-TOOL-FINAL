-keepattributes *Annotation*, InnerClasses
-dontwarn org.jetbrains.annotations.**
-keep class kotlin.Metadata { *; }

-keep class com.squareup.moshi.** { *; }
-keep @com.squareup.moshi.JsonClass class *
-keep class * extends com.squareup.moshi.JsonAdapter { *; }
-keepclasseswithmembers class * { @com.squareup.moshi.* <methods>; }

-keep class com.cen.feedback.data.model.** { *; }
-keep class com.cen.feedback.data.api.** { *; }

-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
