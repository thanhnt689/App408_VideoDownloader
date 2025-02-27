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

-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel
#Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

-dontwarn com.daimajia.easing.Glider
-dontwarn com.daimajia.easing.Skill

 -dontwarn android.media.LoudnessCodecController$OnLoudnessCodecUpdateListener
 -dontwarn android.media.LoudnessCodecController
 -keep class com.facebook.infer.annotation.** { *; }

 -keepclassmembers class * extends androidx.work.Worker {
     public <init>(android.content.Context,androidx.work.WorkerParameters);
 }

 -keep class com.facebook.** { *; }
 -keep class com.facebook.appevents.** { *; }
