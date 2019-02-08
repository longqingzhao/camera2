package com.blackuio.center.camera2api;



public class Log {

    public static void g(String tag, String message) {
        android.util.Log.d(tag, message);
    }

    public static void e(String tag, String message, Throwable e) {
        android.util.Log.e(tag, message, e);
    }

}
