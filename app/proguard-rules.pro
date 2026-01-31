-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

-keep class com.lyricauto.model.** { *; }
-keep class com.lyricauto.network.** { *; }
-keep class com.lyricauto.adapter.** { *; }
-keep class com.lyricauto.service.** { *; }

-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

-keep class * extends androidx.viewbinding.ViewBinding {
    public static * inflate(android.view.LayoutInflater);
}
