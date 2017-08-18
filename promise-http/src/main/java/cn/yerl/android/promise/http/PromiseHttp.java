package cn.yerl.android.promise.http;

import android.app.Application;

import com.lzy.okgo.OkGo;
import com.lzy.okgo.cache.CacheMode;
import com.lzy.okgo.callback.Callback;
import com.lzy.okgo.callback.FileCallback;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.cookie.CookieJarImpl;
import com.lzy.okgo.cookie.store.CookieStore;
import com.lzy.okgo.cookie.store.MemoryCookieStore;
import com.lzy.okgo.cookie.store.SPCookieStore;
import com.lzy.okgo.https.HttpsUtils;
import com.lzy.okgo.model.Progress;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.base.Request;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.yerl.android.promise.core.Promise;
import cn.yerl.android.promise.core.PromiseCallback;
import cn.yerl.android.promise.core.PromiseCallbackWithResolver;
import cn.yerl.android.promise.core.PromiseResolver;
import cn.yerl.android.promise.http.logger.ILogger;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

/**
 * Promise Http Client
 * Created by Alan Yeh on 16/6/8.
 */
public class PromiseHttp {
    private String baseUrl;
    private File cachePath;
    private List<ILogger> loggers = new ArrayList<>();

    /**
     * 添加网络请求日志记录功能
     *
     * @see ILogger 日志接口
     * @see cn.yerl.android.promise.http.logger.LogcatLogger 控制台日志输出
     * @see cn.yerl.android.promise.http.logger.FileLogger 文件日志输出
     * @param loggers 日志
     * @return PromiseHttp
     */
    public PromiseHttp setLogger(ILogger... loggers){
        this.loggers = Arrays.asList(loggers);
        return this;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * 设置基础地址
     * eg:
     * baseUrl = http://yerl.cn/api/v3
     * PromiseRequest.GET("group") 最终访问的是http://yerl.cn/api/v3/group
     * PromiseRequest.GET("/group") 最终访问的是http://yerl.cn/group
     * @param baseUrl 基础地址
     * @return PromiseHttp
     */
    public PromiseHttp setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public File getCachePath() {
        return cachePath;
    }

    /**
     * 设置下载的缓存目录
     * 如果没有设置此项，使用download方法时会抛出异常
     * @param cachePath 缓存目录
     * @return PromiseHttp
     */
    public PromiseHttp setCachePath(File cachePath) {
        this.cachePath = cachePath;
        return this;
    }

    private PromiseHttp(){
    }

    private static PromiseHttp instance;

    public static void init(Application application){
        HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory();

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .cookieJar(new CookieJarImpl(new MemoryCookieStore()))
                .sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager);


        OkGo.getInstance().init(application)
                .setCacheMode(CacheMode.NO_CACHE)
                .setOkHttpClient(builder.build());
    }

    public List<Cookie> getCookies(String url){
        return OkGo.getInstance().getOkHttpClient().cookieJar().loadForRequest(HttpUrl.parse(url));
    }

    public void clearCookies(){
        HttpsUtils.SSLParams sslParams = HttpsUtils.getSslSocketFactory();

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .cookieJar(new CookieJarImpl(new MemoryCookieStore()))
                .sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager);

        OkGo.getInstance()
                .setOkHttpClient(builder.build());
    }

    /**
     * PromiseHttp 单例
     * @return PromiseHttp
     */
    public static PromiseHttp client(){
        if (instance == null){
            synchronized (PromiseHttp.class){
                if (instance == null){
                    instance = new PromiseHttp();
                }
            }
        }
        return instance;
    }

    /**
     * 共享Headers
     */
    private Map<String, String> sharedHeaders = new LinkedHashMap<>();

    /**
     * 为所有的请求都添加一个Header
     *
     * @param key Header Key
     * @param value Header Value
     * @return PromiseHttp
     */
    public PromiseHttp addSharedHeader(String key, String value){
        this.sharedHeaders.put(key, value);
        return this;
    }

    /**
     * 为所有请求都添加Headers
     *
     * @param headers Header键值对
     * @return PromiseHttp
     */
    public PromiseHttp addSharedHeaders(Map<String, String> headers){
        this.sharedHeaders.putAll(headers);
        return this;
    }

    public Map<String, String> getSharedHeaders(){
        return new HashMap<>(this.sharedHeaders);
    }

    /**
     * 执行网络请求
     *
     * 非下载请求，全部使用这个方法来执行
     *
     * @param request Http请求
     * @return Promise with PromiseResponse
     */
    public Promise<PromiseResponse> execute(final PromiseRequest request){
        return new Promise<>(new PromiseCallbackWithResolver<Object, PromiseResponse>() {
            @Override
            public void call(Object arg, PromiseResolver resolver) {
                _execute(request, getTextHandler(request, resolver));
            }
        }).alwaysAsync(new PromiseCallback<Object, PromiseResponse>() {
            @Override
            public PromiseResponse call(Object arg) {
                for (ILogger logger : loggers){
                    if (arg instanceof Throwable){
                        logger.error(PromiseHttp.this, request, (Throwable) arg);
                    }else {
                        logger.info(PromiseHttp.this, (PromiseResponse) arg);
                    }
                }
                if (arg instanceof Throwable){
                    throw (RuntimeException)arg;
                }else {
                    return (PromiseResponse)arg;
                }
            }
        });
    }

    /**
     * 执行下载请求
     *
     * 所有的下载请求都使用这个方法来执行，非下载请求使用execute
     *
     * @param request Http请求
     * @return Promise with PromiseResponse
     */
    public Promise<PromiseResponse> download(final PromiseRequest request){
        return new Promise<>(new PromiseCallbackWithResolver<Object, PromiseResponse>() {
            @Override
            public void call(Object arg, PromiseResolver resolver) {
                _execute(request, getDownloadHandler(request, resolver));
            }
        }).alwaysAsync(new PromiseCallback<Object, PromiseResponse>() {
            @Override
            public PromiseResponse call(Object arg) {
                for (ILogger logger : loggers){
                    if (arg instanceof Throwable){
                        logger.error(PromiseHttp.this, request, (Throwable) arg);
                    }else {
                        logger.info(PromiseHttp.this, (PromiseResponse) arg);
                    }
                }
                if (arg instanceof Throwable){
                    throw (RuntimeException)arg;
                }else {
                    return (PromiseResponse)arg;
                }
            }
        });
    }

    /*
     * 真正执行请求的地方
     */
    private void  _execute(final PromiseRequest request, final Callback<?> handler){

        Request req = request.getRequest(this);

        req.execute(handler);
    }

    /*
     * 非下载请求都用TextHttpResponseHandler来处理
     */
    private StringCallback getTextHandler(final PromiseRequest request, final PromiseResolver resolver){
        return new StringCallback() {
            @Override
            public void onSuccess(Response<String> response) {
                if (response.code() >= 400){
                    resolver.resolve(null, new PromiseHttpException(new PromiseResponse(request, response.code(), response.headers(), response.body()), null));
                }else {
                    resolver.resolve(new PromiseResponse(request, response.code(), response.headers(), response.body()), null);
                }
            }

            @Override
            public void onError(Response<String> response) {
                resolver.resolve(null, new PromiseHttpException(new PromiseResponse(request, response.code(), response.headers(), response.body()), response.getException()));
            }

            @Override
            public void downloadProgress(Progress progress) {
                request.onDownloadProgress(progress.currentSize, progress.totalSize);
            }

            @Override
            public void uploadProgress(Progress progress) {
                request.onUploadProgress(progress.currentSize, progress.totalSize);
            }
        };
    }

    /*
     * 下载请求都用FileAsyncHttpResponseHandler来处理
     * 会自动生成推荐的文件名
     */
    private FileCallback getDownloadHandler(final PromiseRequest request, final PromiseResolver resolver){
        return new FileCallback(this.cachePath.getAbsolutePath(), null) {
            @Override
            public void onSuccess(Response<File> response) {
                if (response.code() >= 400){
                    resolver.resolve(null, new PromiseHttpException(new PromiseResponse(request, response.code(), response.headers(), response.body()), null));
                }else {
                    resolver.resolve(new PromiseResponse(request, response.code(), response.headers(), response.body()), null);
                }
            }

            @Override
            public void onError(Response<File> response) {
                resolver.resolve(null, new PromiseHttpException(new PromiseResponse(request, response.code(), response.headers(), response.body()), response.getException()));
            }

            @Override
            public void downloadProgress(Progress progress) {
                request.onDownloadProgress(progress.currentSize, progress.totalSize);
            }

            @Override
            public void uploadProgress(Progress progress) {
                request.onUploadProgress(progress.currentSize, progress.totalSize);
            }
        };
    }
}
