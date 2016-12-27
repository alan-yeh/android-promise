package cn.yerl.android.promise.http;

import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.HttpDelete;
import com.loopj.android.http.HttpGet;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.ResponseHandlerInterface;
import com.loopj.android.http.TextHttpResponseHandler;

import java.io.File;
import java.io.IOException;
import java.net.URI;
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

/**
 * Promise Http Client
 * Created by Alan Yeh on 16/6/8.
 */
public class PromiseHttp {
    private String baseUrl;
    private File cachePath;
    private List<ILogger> loggers = new ArrayList<>();

    public PromiseHttp setLogger(ILogger... loggers){
        this.loggers = Arrays.asList(loggers);
        return this;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public PromiseHttp setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public File getCachePath() {
        return cachePath;
    }

    public PromiseHttp setCachePath(File cachePath) {
        this.cachePath = cachePath;
        return this;
    }

    private HttpClient httpClient;

    private PromiseHttp(){
        httpClient = new HttpClient();
    }

    private static PromiseHttp instance;
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

    private Map<String, String> sharedHeaders = new LinkedHashMap<>();
    public PromiseHttp addSharedHeader(String key, String value){
        this.sharedHeaders.put(key, value);
        return this;
    }
    public PromiseHttp addSharedHeaders(Map<String, String> headers){
        this.sharedHeaders.putAll(headers);
        return this;
    }

    public Promise<PromiseResponse> execute(final PromiseRequest request){
        return new Promise<>(new PromiseCallbackWithResolver<Object, PromiseResponse>() {
            @Override
            public void call(Object arg, PromiseResolver resolver) {
                _execute(request, getTextHandler(request, resolver));
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

    public Promise<PromiseResponse> download(final PromiseRequest request){
        return new Promise<>(new PromiseCallbackWithResolver<Object, PromiseResponse>() {
            @Override
            public void call(Object arg, PromiseResolver resolver) {
                _execute(request, getDownloadHandler(request, resolver));
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

    private void _execute(final PromiseRequest request, final ResponseHandlerInterface handler){
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

        httpClient.sendRequest(req, handler);
    }

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

    private ResponseHandlerInterface getDownloadHandler(final PromiseRequest request, final PromiseResolver resolver){
        URI uri = ProcessUtils.processURI(baseUrl, request);

        String suggestFileName = uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1);

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
