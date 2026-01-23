# ================================================================================================
# GENERAL RULES
# ================================================================================================

# Keep line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*

# Keep generic signatures for reflection
-keepattributes Signature

# Keep inner classes
-keepattributes InnerClasses, EnclosingMethod

# ================================================================================================
# KOTLIN
# ================================================================================================

# Kotlin metadata
-keep class kotlin.Metadata { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ================================================================================================
# RETROFIT & OKHTTP
# ================================================================================================

# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep annotation default values (e.g., retrofit2.http.Field.encoded).
-keepattributes AnnotationDefault

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# Keep inherited services.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface * extends <1>

# With R8 full mode generic signatures are stripped for classes that are not kept.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ================================================================================================
# MOSHI (JSON Serialization)
# ================================================================================================

# Keep Moshi annotations
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}

# Keep all model classes used with Moshi
-keep class com.mobileorienteering.data.model.** { *; }

# Moshi uses generic type information stored in a class file when working with fields.
-keepattributes Signature

# Moshi uses kotlin.reflect internally
-keep class kotlin.reflect.** { *; }
-keep class kotlin.Metadata { *; }

# Keep JsonAdapter for R8 full mode
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}

# ================================================================================================
# ROOM DATABASE
# ================================================================================================

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Room DAOs
-keep @androidx.room.Dao class * { *; }

# Keep your Room entities
-keep class com.mobileorienteering.data.local.entity.** { *; }

# ================================================================================================
# HILT / DAGGER
# ================================================================================================

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Hilt modules
-keep @dagger.hilt.InstallIn class * { *; }
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.components.SingletonComponent class * { *; }

# ================================================================================================
# JETPACK COMPOSE
# ================================================================================================

# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# Keep CompositionLocal providers
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ================================================================================================
# GOOGLE PLAY SERVICES
# ================================================================================================

# Keep Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Keep Google Auth
-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.location.** { *; }

# ================================================================================================
# MAPLIBRE
# ================================================================================================

# Keep MapLibre classes
-keep class org.maplibre.** { *; }
-dontwarn org.maplibre.**

# Keep MapLibre Compose
-keep class org.maplibre.compose.** { *; }

# ================================================================================================
# MEDIA3 (EXOPLAYER)
# ================================================================================================

# Keep ExoPlayer classes
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ================================================================================================
# DATASTORE
# ================================================================================================

# Keep DataStore preferences
-keep class androidx.datastore.*.** { *; }

# ================================================================================================
# YOUR APP SPECIFIC RULES
# ================================================================================================

# Keep your API service interfaces
-keep interface com.mobileorienteering.data.api.service.** { *; }

# Keep your domain models
-keep class com.mobileorienteering.data.model.domain.** { *; }

# Keep your network request/response models
-keep class com.mobileorienteering.data.model.network.** { *; }

# Keep service classes
-keep class com.mobileorienteering.service.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ================================================================================================
# SERIALIZATION
# ================================================================================================

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ================================================================================================
# DEBUGGING
# ================================================================================================

# Keep custom exceptions for better crash reports
-keep public class * extends java.lang.Exception

# Keep enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
