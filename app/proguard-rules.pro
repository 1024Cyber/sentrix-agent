# Keep OkHttp WebSocket classes
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**

# Keep Gson model classes
-keep class com.sentrix.agent.network.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Keep coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
