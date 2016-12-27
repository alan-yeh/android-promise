package cn.yerl.android.promise.http.logger;

import android.util.Log;

/**
 * Http 控制台日志
 * Created by Alan Yeh on 2016/12/27.
 */
public class LogcatLogger extends BaseLogger {
    @Override
    void writeContent(String content, boolean isThrowable) {
        if (isThrowable){
            Log.e("PromiseHttp", content);
        }else {
            Log.d("PromiseHttp", content);
        }
    }
}
