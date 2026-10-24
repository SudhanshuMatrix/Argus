# Message text must never reach the log. flutter_notification_listener logs every notification
# it sees with Log.d (including WhatsApp message text), so release builds drop all verbose,
# debug and info logging. Warnings and errors stay.
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Background entry points are looked up by name from Dart callbacks
-keep class im.zoe.labs.flutter_notification_listener.** { *; }
-keep class com.shounakmulay.telephony.** { *; }
-keep class com.dexterous.flutterlocalnotifications.** { *; }
