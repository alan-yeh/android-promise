package cn.yerl.android.promise.http.logger;

import cn.yerl.android.promise.http.PromiseHttp;
import cn.yerl.android.promise.http.PromiseRequest;
import cn.yerl.android.promise.http.PromiseResponse;

/**
 * 日志
 * Created by Alan Yeh on 2016/12/27.
 */
public abstract class ILogger {
    public abstract void log(PromiseHttp client, PromiseResponse response);
    public abstract void log(PromiseHttp client, PromiseRequest request, Throwable throwable);
}
