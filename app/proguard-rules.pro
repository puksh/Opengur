# Application classes that will be serialized/deserialized over Gson/Retrofit
-keep class com.kenny.openimgur.api.** { *; }
-keep class com.kenny.openimgur.classes.** { *; }

# Retrofit 2
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes *Annotation*

# ButterKnife
-keep class butterknife.** { *; }
-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}
-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}

# Parcelable
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Universal Image Loader
-keep class com.nostra13.universalimageloader.** { *; }

# Support library
-dontwarn android.support.**
-keep class android.support.** { *; }

# Suppress warnings for missing classes from optional deps
-dontwarn com.google.android.apps.muzei.**
