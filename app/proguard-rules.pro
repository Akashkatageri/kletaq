# =================================================================
# Kletaq Enterprise ProGuard & R8 Obfuscation Rules
# =================================================================

# Optimization & Obfuscation Settings
-repackageclasses 'com.kletaq.app.internal'
-allowaccessmodification

# Strip development logs in production release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Preserve Glance App Widgets & Receivers (Required for 2x1 Panda Widget)
-keep class com.kletaq.app.widgets.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class * extends androidx.glance.appwidget.action.ActionCallback { *; }

# Preserve Firebase & Room / Serialization Models
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}

# Preserve Data Models used across Firestore deserialization
-keep class com.kletaq.app.data.model.** { *; }
-keep class com.kletaq.app.core.flashcards.TopicFlashcard { *; }

# Preserve Hilt Generated Components
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.internal.TestSingletonComponent { *; }
-keep @dagger.hilt.android.HiltAndroidApp class * { *; }

# Preserve Kotlin Coroutines & Reflection attributes
-keepattributes EnclosingMethod,InnerClasses
-dontwarn kotlinx.coroutines.**
