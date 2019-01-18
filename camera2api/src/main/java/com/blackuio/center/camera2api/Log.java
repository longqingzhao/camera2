package com.blackuio.center.camera2api;

import com.tencent.bugly.crashreport.BuglyLog;

public class Log {

    public static void g(String tag, String message) {
        if (BuildConfig.DEBUG)
            BuglyLog.d(tag, message);
       /* else{
            BuglyLog.i(tag, message);
        }*/
    }

    public static void e(String tag, String message, Throwable e) {
        BuglyLog.e(tag, message, e);
    }

}
