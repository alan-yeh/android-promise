package cn.yerl.android.promise.http;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.yerl.android.promise.core.Promise;
import cn.yerl.android.promise.core.PromiseCallbackWithResolver;
import cn.yerl.android.promise.core.PromiseResolver;
import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HeaderElement;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.client.utils.URLEncodedUtils;

/**
 * 封装网络访问方法
 * Created by yan on 16/6/8.
 */
public class PromiseHttp {
    private String baseUrl;
    private File cachePath;
    private File logPath;
    private boolean enableLog;

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

    public File getLogPath(){
        return logPath;
    }

    public PromiseHttp setLogPath(File logPath){
        this.logPath = logPath;
        return this;
    }

    public boolean isEnableLog(){
        return enableLog;
    }

    public PromiseHttp setEnableLog(boolean enableLog){
        this.enableLog = enableLog;
        return this;
    }

    private AsyncHttpClient httpClient;

    private PromiseHttp(){
        httpClient = new AsyncHttpClient();
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
    private Map<String, String> sharedCookies = new LinkedHashMap<>();

    public Promise<PromiseResponse> execute(final PromiseRequest request){
        return new Promise<PromiseResponse>(new PromiseCallbackWithResolver<Object, PromiseResponse>() {
            @Override
            public void call(Object arg, final PromiseResolver resolver) {
                String urlString = _processUrl(baseUrl, request);

                // 处理Header
                Header[] headers = _processHeader(sharedHeaders, request.getHeaders(), sharedCookies, request.getCookies());

                // 处理参数
                RequestParams params = new RequestParams();
                params.setContentEncoding(request.getEncoding());

                try {
                    for (Map.Entry<String, Object> param : request.getBodyParams().entrySet()){
                        if (param.getValue() instanceof File){
                            params.put(param.getKey(), (File)param.getValue());
                        }else if (param.getValue() instanceof File[]){
                            params.put(param.getKey(), (File[])param.getValue());
                        }else if (param.getValue() instanceof InputStream){
                            params.put(param.getKey(), (InputStream)param.getValue());
                        }else {
                            params.put(param.getKey(), param.getValue());
                        }
                    }
                }catch (Exception ex){
                    throw new RuntimeException(ex);
                }

                AsyncHttpResponseHandler handler = new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        resolver.resolve(new PromiseHttpException(new PromiseResponse(request, statusCode, headers, responseString), throwable));
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String responseString) {
                        resolver.resolve(new PromiseResponse(request, statusCode, headers, responseString));
                    }
                };

                switch (request.getMethod()){
                    case GET:
                        httpClient.get(null, urlString, headers, null, handler);
                        break;
                    case POST:
                        httpClient.post(null, urlString, headers, params, null, handler);
                        break;
                    case PUT:
                        System.out.print("not support header");
                        httpClient.put(null, urlString, params, handler);
                        break;
                    case DELETE:
                        httpClient.delete(null, urlString, headers, handler);
                        break;
                    case HEAD:
                        httpClient.head(null, urlString, headers, null, handler);
                        break;
                }
            }
        });
    }

    private Header[] _processHeader(Map<String, String> sharedHeaders, Map<String, String> reuquestHeaders, Map<String, String> sharedCookies, Map<String, String> requestCookies){
//        List<Header> headers = new ArrayList<>();

        Map<String, String> cookies = new HashMap<>();
        cookies.putAll(sharedCookies);
        cookies.putAll(requestCookies);

        Map<String, String> headers = new HashMap<>();
        headers.putAll(sharedHeaders);
        headers.putAll(reuquestHeaders);
        

        return null;
    }

    private String _processUrl(String baseUrl, PromiseRequest request){
        try {
            URIBuilder uriBuilder = new URIBuilder(baseUrl);

            URI requestURI = new URI(request.getUrlString());

            if (requestURI.getHost() != null){
                uriBuilder = new URIBuilder(request.getUrlString()).clearParameters();
            }

            if (requestURI.getPath().startsWith("/")){
                uriBuilder.setPath(requestURI.getPath());
            }else {
                uriBuilder.setPath(uriBuilder.getPath() + "/" + requestURI.getPath());
            }

            List<NameValuePair> queryParams = URLEncodedUtils.parse(requestURI.getQuery(), Charset.forName(request.getEncoding()));
            for (NameValuePair pair : queryParams){
                uriBuilder.setParameter(pair.getName(), pair.getValue());
            }

            for (Map.Entry<String, String> param : request.getQueryParams().entrySet()){
                uriBuilder.setParameter(param.getKey(), param.getValue());
            }

            return uriBuilder.build().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Promise<PromiseResponse> download(final PromiseRequest request){
        return new Promise<PromiseResponse>(new PromiseCallbackWithResolver<Object, PromiseResponse>() {
            @Override
            public void call(Object arg, final PromiseResolver resolver) {
                if (cachePath == null){
                    resolver.resolve(new RuntimeException("Promise Http: cacheDir为空，请先初始化"));
                    return;
                }

                String urlString = _processUrl(baseUrl, request);

                // 处理Header
                Header[] headers = _processHeader(sharedHeaders, request.getHeaders(), sharedCookies, request.getCookies());

                // 处理参数
                RequestParams params = new RequestParams();
                params.setContentEncoding(request.getEncoding());

                try {
                    for (Map.Entry<String, Object> param : request.getBodyParams().entrySet()){
                        if (param.getValue() instanceof File){
                            params.put(param.getKey(), (File)param.getValue());
                        }else if (param.getValue() instanceof InputStream){
                            params.put(param.getKey(), (InputStream)param.getValue());
                        }else {
                            params.put(param.getKey(), param.getValue());
                        }
                    }
                }catch (Exception ex){
                    throw new RuntimeException(ex);
                }


                URI uri = null;
                try {
                    uri = new URI(urlString);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }

                String suggestFileName = uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1);

                File cacheFile = getSuggestedFile(cachePath, suggestFileName);


                AsyncHttpResponseHandler handler = new FileAsyncHttpResponseHandler(cacheFile) {
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

                switch (request.getMethod()){
                    case GET:
                        httpClient.get(null, urlString, headers, null, handler);
                        break;
                    case POST:
                        httpClient.post(null, urlString, headers, params, null, handler);
                        break;
                    case PUT:
                        System.out.print("not support header");
                        httpClient.put(null, urlString, params, handler);
                        break;
                    case DELETE:
                        httpClient.delete(null, urlString, headers, handler);
                        break;
                    case HEAD:
                        httpClient.head(null, urlString, headers, null, handler);
                        break;
                }
            }
        });
    }

    private File getSuggestedFile(File cacheDir, String fileName){
        File cacheFile = new File(cacheDir.getAbsolutePath() + File.separator + fileName);
        if (!cacheFile.exists()){
            return cacheFile;
        }

        int subfix = 1;
        while (true){
            cacheFile = new File(cacheDir.getAbsoluteFile() + File.separator + fileName + ( ++ subfix));
            if (!cacheFile.exists()){
                return cacheFile;
            }
        }
    }

}
