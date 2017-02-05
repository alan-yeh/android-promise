package cn.yerl.android.promise.http;

import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.HttpDelete;
import com.loopj.android.http.HttpGet;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.ResponseHandlerInterface;
import com.loopj.android.http.TextHttpResponseHandler;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.yerl.android.promise.core.Promise;
import cn.yerl.android.promise.core.PromiseCallback;
import cn.yerl.android.promise.core.PromiseCallbackWithResolver;
import cn.yerl.android.promise.core.PromiseResolver;
import cn.yerl.android.promise.http.logger.ILogger;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HeaderElement;
import cz.msebera.android.httpclient.client.methods.HttpEntityEnclosingRequestBase;
import cz.msebera.android.httpclient.client.methods.HttpHead;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.client.methods.HttpPut;
import cz.msebera.android.httpclient.client.methods.HttpUriRequest;
import cz.msebera.android.httpclient.client.utils.URLEncodedUtils;

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

    private HttpClient httpClient;

    private PromiseHttp(){
        httpClient = new HttpClient();
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
            public Object call(Object arg) {
                for (ILogger logger : loggers){
                    if (arg instanceof Throwable){
                        logger.log(PromiseHttp.this, request, (Throwable) arg);
                    }else {
                        logger.log(PromiseHttp.this, (PromiseResponse) arg);
                    }
                }
                return arg;
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
            public Object call(Object arg) {
                for (ILogger logger : loggers){
                    if (arg instanceof Throwable){
                        logger.log(PromiseHttp.this, request, (Throwable) arg);
                    }else {
                        logger.log(PromiseHttp.this, (PromiseResponse) arg);
                    }
                }
                return arg;
            }
        });
    }

    /*
     * 真正执行请求的地方
     */
    private RequestHandle _execute(final PromiseRequest request, final ResponseHandlerInterface handler){
        // 处理URL，将QueryParam拼接在url后
        URI requestURI = ProcessUtils.processURI(baseUrl, request);

        // 处理参数
        RequestParams params = ProcessUtils.processParams(request);

        HttpUriRequest req = null;

        switch (request.getMethod()){
            case GET:{
                req = new HttpGet(requestURI);
                break;
            }
            case DELETE:{
                req = new HttpDelete(requestURI);
                break;
            }
            case HEAD:{
                req = new HttpHead(requestURI);
                break;
            }
            case POST: {
                HttpEntityEnclosingRequestBase requestBase = new HttpPost(requestURI);
                req = requestBase;
                try {
                    requestBase.setEntity(params.getEntity(handler));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                break;
            }
            case PUT:{
                HttpEntityEnclosingRequestBase requestBase = new HttpPut(requestURI);
                req = requestBase;

                try {
                    requestBase.setEntity(params.getEntity(handler));
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                break;
            }
        }

        // 处理Header
        Header[] headers = ProcessUtils.processHeader(sharedHeaders, request.getHeaders());
        req.setHeaders(headers);

        return httpClient.sendRequest(req, handler);
    }

    /*
     * 非下载请求都用TextHttpResponseHandler来处理
     */
    private ResponseHandlerInterface getTextHandler(final PromiseRequest request, final PromiseResolver resolver){
        return new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                resolver.resolve(new PromiseHttpException(new PromiseResponse(request, statusCode, headers, responseString), throwable));
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                resolver.resolve(new PromiseResponse(request, statusCode, headers, responseString));
            }
        };
    }

    /*
     * 下载请求都用FileAsyncHttpResponseHandler来处理
     * 会自动生成推荐的文件名
     */
    private ResponseHandlerInterface getDownloadHandler(final PromiseRequest request, final PromiseResolver resolver){
        if (cachePath == null){
            resolver.resolve(new UnsupportedOperationException("请先设置cachePath后再使用下载功能"));
            return null;
        }

        URI uri = ProcessUtils.processURI(baseUrl, request);

        String suggestFileName = uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1);

        // URL Decode
        try {
            suggestFileName = URLDecoder.decode(suggestFileName, request.getEncoding());
        }catch (Exception ex){}


        File cacheFile = new File(cachePath.getAbsolutePath() + File.separator + suggestFileName);
        if (cacheFile.exists()){
            cacheFile.delete();
        }

        return new FileAsyncHttpResponseHandler(cacheFile) {
            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable throwable, File file) {
                resolver.resolve(new PromiseHttpException(new PromiseResponse(request, statusCode, headers, file), throwable));
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, File file) {
                String fileName = "";
                for (Header header : headers) {
                    if ("Content-Disposition".equalsIgnoreCase(header.getName())) {
                        HeaderElement[] elements = header.getElements();
                        for (HeaderElement element : elements) {
                            if ("attachment".equalsIgnoreCase(element.getName())) {
                                fileName = element.getParameterByName("filename").getValue();
                            }
                        }
                    }
                }

                // URL Decode
                if (!fileName.isEmpty()){
                    try {
                        fileName = URLDecoder.decode(fileName, request.getEncoding());
                    }catch (Exception ex){}
                }

                File newFile = file;
                try {
                    if (!fileName.isEmpty()) {
                        newFile = new File(file.getParent() + File.separator + fileName);
                        file.renameTo(newFile);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                resolver.resolve(new PromiseResponse(request, statusCode, headers, newFile));
            }

            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                request.onProgress(bytesWritten, totalSize);
            }
        };
    }
}
