# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 关闭R8混淆（只保留代码缩减），彻底防止residualsignature破坏Retrofit/协程方法签名
-dontobfuscate

# 数据模型不能混淆（Gson反序列化依赖字段名）
-keep class com.aijia.video.data.model.** { *; }
-keep class com.aijia.video.data.remote.ApiSecurity { *; }
# ApiService接口完全保留，Retrofit依赖反射读取方法泛型签名
# 必须用 -keep（不带allowshrinking/allowobfuscation），才能阻止R8 residualsignature替换
-keep interface com.aijia.video.data.remote.ApiService {
    <methods>;
}
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Exceptions

# Retrofit官方R8规则
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
# Retrofit suspend函数协程适配 - 禁止R8优化接口方法签名
# 注意：不带allowobfuscation，确保签名不被residualsignature替换
-if interface * { @retrofit2.http.* <methods>; }
-keep interface <1>

# Hilt/Dagger生成类
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Coil/OkHttp/Gson
-keep class coil.** { *; }
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep class com.google.gson.** { *; }

# Gson TypeToken匿名子类 - R8会内联消除导致Missing type parameter
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.reflect.TypeToken { *; }

# Room数据库
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Media3/ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Kotlin协程
-keepclassmembernames class kotlinx.** { volatile <fields>; }
# 保留Continuation类名，防止R8重写ApiService方法的Signature属性时丢失泛型参数
-keep class kotlin.coroutines.Continuation

# Compose运行时 - 修复锁验证失败问题
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.runtime.snapshots.** { *; }
-keepclassmembers class androidx.compose.runtime.snapshots.SnapshotStateList {
    *;
}
-dontwarn androidx.compose.runtime.**

# 版本更新保护 - 禁止缩减/混淆更新相关类
-keep class com.aijia.video.ui.update.** { *; }
-keep class com.aijia.video.util.UpdateGuard { *; }
-keep class com.aijia.video.util.VersionUtils { *; }
-keep class com.aijia.video.util.AppUpdateManager { *; }
-keep class com.aijia.video.data.repository.VersionRepository { *; }

# 关键API接口不参与任何优化（防止R8重写suspend方法签名）
-keep,allowoptimization class com.aijia.video.data.remote.ApiService {
    public *** checkAppVersion(...);
}