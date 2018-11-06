package com.zhaolongqing.zlqpc.camera2api;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.BuildConfig;
import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.LogStrategy;
import com.orhanobut.logger.Logger;
import com.orhanobut.logger.PrettyFormatStrategy;

import java.util.Random;

public class Log {

    private class LogCatStrategy implements LogStrategy {

        @Override
        public void log(int priority, String tag, @NonNull String message) {
            android.util.Log.println(priority, randomKey() + tag, message);
        }

        private int last;

        private String randomKey() {
            int random = 10 * new Random().nextInt();
            if (random == last) {
                random = (random + 1) % 10;
            }
            last = random;
            return String.valueOf(random);
        }
    }

    private class LogAdapter extends AndroidLogAdapter {

        private final boolean setLogger;

        LogAdapter(@NonNull FormatStrategy formatStrategy, boolean setLogger) {
            super(formatStrategy);
            this.setLogger = setLogger;
        }

        @Override
        public boolean isLoggable(int priority, @Nullable String tag) {
            return setLogger ? super.isLoggable(priority, tag) : BuildConfig.DEBUG;
        }
    }



    public void initLogger(){
        FormatStrategy formatStrategy = PrettyFormatStrategy.newBuilder()
                .showThreadInfo(false)      //（可选）是否显示线程信息。 默认值为true
                .methodCount(2)               // （可选）要显示的方法行数。 默认2
                .methodOffset(7)               // （可选）设置调用堆栈的函数偏移值，0的话则从打印该Log的函数开始输出堆栈信息，默认是0
                .logStrategy(new LogCatStrategy())  //（可选）更改要打印的日志策略。 默认LogCat
                .tag("black_uio")                  //（可选）每个日志的全局标记。 默认PRETTY_LOGGER（如上图）
                .build();
        Logger.addLogAdapter(new LogAdapter(formatStrategy,true));
    }

}
