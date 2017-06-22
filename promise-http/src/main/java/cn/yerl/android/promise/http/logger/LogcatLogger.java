package cn.yerl.android.promise.http.logger;

import android.util.Log;

/**
 * Http 控制台日志
 * Created by Alan Yeh on 2016/12/27.
 */
public class LogcatLogger extends BaseLogger {

    @Override
    void writeInfo(String info) {
        Log.i("PromiseHttp", info);
    }

    @Override
    void writeError(String error) {
        Log.e("PromiseHttp", error);
    }
}
