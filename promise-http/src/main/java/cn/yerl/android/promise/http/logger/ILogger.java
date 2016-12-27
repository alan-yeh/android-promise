package cn.yerl.android.promise.http.logger;

import cn.yerl.android.promise.http.PromiseHttp;
import cn.yerl.android.promise.http.PromiseRequest;
import cn.yerl.android.promise.http.PromiseResponse;

/**
 * 日志记录
 * Created by Alan Yeh on 2016/12/27.
 */
public interface ILogger {
    /**
     * 记录Http Response
     * @param client HttpClient
     * @param response Http Response
     */
    void log(PromiseHttp client, PromiseResponse response);

    /**
     * 记录异常
     * @param client HttpClient
     * @param request Http Request
     * @param throwable 异常
     */
    void log(PromiseHttp client, PromiseRequest request, Throwable throwable);
}
