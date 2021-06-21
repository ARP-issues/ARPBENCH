# Don't show warnings for the following libraries
-dontwarn io.evercam.**
-dontwarn okio.**
-dontwarn org.joda.time.**
-dontwarn org.simpleframework.xml.**
-dontwarn com.mixpanel.android.**
-dontwarn com.google.android.gms.**

# Keep JNI methods
-keepclassmembers class **.VideoActivity {
    long native_custom_data;
    native <methods>;
    void nativeRequestSample(String);
    void nativeSetUri(String, int);
    void nativeInit();
    void nativeFinalize();
    void nativePlay();
    void nativePause();
    boolean nativeClassInit();
    void nativeSurfaceInit(Object);
    void nativeSurfaceFinalize();
    void nativeExpose();
    void onVideoLoaded();
    void onVideoLoadFailed();
    void onSampleRequestSuccess(byte[], int);
    void onSampleRequestFailed();
}

# Fix the MenuBuilder NoClassDefFoundError
-keep class !android.support.v7.internal.view.menu.*MenuBuilder*, android.support.v7.** { *; }
-keep interface android.support.v7.** { *; }

