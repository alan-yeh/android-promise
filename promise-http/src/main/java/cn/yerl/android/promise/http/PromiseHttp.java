package cn.yerl.android.promise.http;

import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.ResponseHandlerInterface;
import com.loopj.android.http.TextHttpResponseHandler;

import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import cn.yerl.android.promise.core.Promise;
import cn.yerl.android.promise.core.PromiseCallback;
import cn.yerl.android.promise.core.PromiseCallbackWithResolver;
import cn.yerl.android.promise.core.PromiseResolver;
import cn.yerl.android.promise.http.logger.ILogger;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HeaderElement;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.CookieStore;
import cz.msebera.android.httpclient.client.methods.HttpUriRequest;
import cz.msebera.android.httpclient.client.protocol.ClientContext;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;

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


    public PromiseHttp setTimeout(int timeout){
        this.httpClient.setTimeout(timeout);
        return this;
    }
    public PromiseHttp setConnectTimeout(int timeout){
        this.httpClient.setConnectTimeout(timeout);
        return this;
    }
    public PromiseHttp setResponseTimeout(int timeout){
        this.httpClient.setResponseTimeout(timeout);
        return this;
    }

    public int getConnectTimeout(){
        return this.httpClient.getConnectTimeout();
    }
    public int getResponseTimeout(){
        return this.httpClient.getResponseTimeout();
    }


    /**
     * 获取Cookies
     * @return Cookies
     */
    public CookieStore getCookieStore(){
        return ((DefaultHttpClient)this.httpClient.getHttpClient()).getCookieStore();
    }

    public PromiseHttp setCookieStore(CookieStore cookieStore){
        this.httpClient.setCookieStore(cookieStore);
        return this;
    }

    public void clearCookies(){
        ((CookieStore)this.httpClient.getHttpContext().getAttribute(ClientContext.COOKIE_STORE)).clear();
        ((DefaultHttpClient)this.httpClient.getHttpClient()).getCookieStore().clear();
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

    private HttpClient httpClient;

    private PromiseHttp(){
        httpClient = new HttpClient();
        httpClient.addHeader("Connection", "Keep-Alive");
        httpClient.addHeader("Accept-Language", Locale.getDefault().toString());
    }

    private static PromiseHttp instance;

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
                request.handler = _execute(request, getTextHandler(request, resolver));
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
                request.handler = _execute(request, getDownloadHandler(request, resolver));
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
    private RequestHandle _execute(final PromiseRequest request, final ResponseHandlerInterface handler){

        HttpUriRequest req = request.getRequest(this, handler);

        return httpClient.sendRequest(req, handler);
    }

    /*
     * 非下载请求都用TextHttpResponseHandler来处理
     */
    private ResponseHandlerInterface getTextHandler(final PromiseRequest request, final PromiseResolver resolver){
        TextHttpResponseHandler handler = new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                resolver.resolve(null, new PromiseHttpException(new PromiseResponse(request, statusCode, headers, responseString), throwable));
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                resolver.resolve(new PromiseResponse(request, statusCode, headers, responseString), null);
            }
        };

        handler.setCharset(request.getEncoding());

        return handler;
    }

    /*
     * 下载请求都用FileAsyncHttpResponseHandler来处理
     * 会自动生成推荐的文件名
     */
    private ResponseHandlerInterface getDownloadHandler(final PromiseRequest request, final PromiseResolver resolver){
        if (cachePath == null){
            resolver.resolve(null, new UnsupportedOperationException("请先设置cachePath后再使用下载功能"));
            return null;
        }

        if (!cachePath.exists()){
            if (!cachePath.mkdir()){
                resolver.resolve(null, new IllegalStateException("无法创建下载缓存目录"));
                return null;
            }
        }


        URI uri = ProcessUtils.processURI(baseUrl, request);

        String suggestFileName = uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1);

        // URL Decode
        try {
            suggestFileName = URLDecoder.decode(suggestFileName, request.getEncoding());
        }catch (Exception ex){}

        if (suggestFileName.lastIndexOf(".") < 0){
            suggestFileName = UUID.randomUUID().toString() + ".tmp";
        }


        File cacheFile = new File(cachePath.getAbsolutePath(), suggestFileName);
        if (cacheFile.exists()){
            cacheFile.delete();
        }

        FileAsyncHttpResponseHandler handler = new FileAsyncHttpResponseHandler(cacheFile) {
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                resolver.resolve(null, new PromiseHttpException(new PromiseResponse(request, statusCode, headers, file), throwable));
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, File file) {
                String fileName = "";
                for (Header header : headers) {
                    if ("Content-Disposition".equalsIgnoreCase(header.getName())) {
                        HeaderElement[] elements = header.getElements();
                        for (HeaderElement element : elements) {
                            NameValuePair attachName = element.getParameterByName("filename");
                            if (attachName != null){
                                fileName = attachName.getValue();
                            }
                        }
                    }
                }

                File newFile = file;

                if (!fileName.isEmpty()){
                    try {
                        //先转码
                        fileName = new String(fileName.getBytes("ISO8859-1"), request.getEncoding());
                        //再重命名
                        newFile = new File(file.getParent(), fileName);
                        if (!file.renameTo(newFile)){
                            // 重命名失败了，还是用回原来的名字
                            newFile = file;
                        }
                    }catch (Exception ex){
                        throw new RuntimeException(ex);
                    }
                }

                resolver.resolve(new PromiseResponse(request, statusCode, headers, newFile), null);
            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                request.onProgress(bytesWritten, totalSize);
            }
        };
        handler.setCharset(request.getEncoding());
        return handler;
    }
}
