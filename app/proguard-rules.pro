# Keep Firestore / Serialization / Room / AppFunctions models and annotations intact under R8
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers class com.example.data.model.** { *; }
-keep class com.example.data.model.** { *; }

# Keep Android AppFunctions endpoints & serializable schemas
-keep class com.example.ai.** { *; }
-keepclassmembers class com.example.ai.** { *; }
-keep @androidx.appfunctions.AppFunctionSerializable class * { *; }
-keep class androidx.appfunctions.** { *; }

# Room entities and DAOs
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Credential Manager & Google Identity
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# Keep Kotlin Coroutines debug & metadata safely
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**
