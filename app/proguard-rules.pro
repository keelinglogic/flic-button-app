# Flic SDK
-keep class io.flic.flic2libandroid.** { *; }
-keep class io.flic.lib.** { *; }

# Keep broadcast intent classes
-keepclassmembers class * extends android.content.BroadcastReceiver {
    public void onReceive(android.content.Context, android.content.Intent);
}
