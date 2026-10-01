# Proguard / R8 rules for Nutritrack

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt

# Moshi rules
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <fields>;
}
-keep class com.example.macro_tracker.data.remote.** { *; }
-keep class com.example.macro_tracker.data.remote.neon.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.macro_tracker.data.local.** { *; }
-dontwarn androidx.room.paging.**

# Retrofit & OkHttp
-dontnote retrofit2.Platform
-dontwarn retrofit2.Platform$Java8
-keepattributes Signature
-keepattributes Exceptions
-dontwarn okhttp3.**
-dontwarn okio.**

# Firebase & Play Services
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.firebase.firestore.* <fields>;
    @com.google.firebase.firestore.* <methods>;
}

# Coroutines
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
