package cn.yerl.android.promise.http;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 封装请求参数类
 * Created by yan on 16/6/6.
 */
public class PromiseRequest implements Serializable {
    public enum Method{
        GET,
        POST,
        PUT,
        DELETE,
        HEAD
    }
    private Method method;
    private String urlString;
    private String encoding = "UTF-8";
    final private Date createTime;

    private PromiseRequest(String url, Method method){
        this.urlString = url;
        this.method = method;
        this.createTime = new Date();
    }

    public static PromiseRequest GET(String url){
        return new PromiseRequest(url, Method.GET);
    }

    public static PromiseRequest POST(String url){
        return new PromiseRequest(url, Method.POST);
    }

    public static PromiseRequest PUT(String url){
        return new PromiseRequest(url, Method.PUT);
    }

    public static PromiseRequest DELETE(String url){
        return new PromiseRequest(url, Method.DELETE);
    }

    public static PromiseRequest HEAD(String url){
        return new PromiseRequest(url, Method.HEAD);
    }

    private Map<String, String> queryParams = new LinkedHashMap<>();
    public PromiseRequest withQueryParam(String key, String value){
        this.queryParams.put(key, value);
        return this;
    }

    public PromiseRequest withQueryParams(Map<String, String> queryParams){
        this.queryParams.putAll(queryParams);
        return this;
    }

    private Map<String, Object> bodyParams = new LinkedHashMap<>();
    public PromiseRequest withBodyParam(String key, Object value){
        this.bodyParams.put(key, value);
        return this;
    }

    public PromiseRequest withBodyParams(Map<String, Object> bodyParams){
        this.bodyParams.putAll(bodyParams);
        return this;
    }

    private Map<String, String> headers = new LinkedHashMap<>();

    public PromiseRequest withHeader(String key, String value){
        this.headers.put(key, value);
        return this;
    }

    public PromiseRequest withHeaders(Map<String, String> headers){
        this.headers.putAll(headers);
        return this;
    }

    public interface OnProgressChanged{
        void onProgress(long bytesWritten, long totalSize);
    }

    private List<WeakReference<OnProgressChanged>> progressListeners = new ArrayList<>();
    public void addDownloadProgressListener(OnProgressChanged progressListener){
        progressListeners.add(new WeakReference<>(progressListener));
    }

    void onProgress(long bytesWritten, long totalSize){
        for (WeakReference<OnProgressChanged> listener : progressListeners){
            if (listener.get() != null){
                listener.get().onProgress(bytesWritten, totalSize);
            }
        }
    }

    public Method getMethod() {
        return method;
    }

    public String getUrlString() {
        return urlString;
    }

    public String getEncoding() {
        return encoding;
    }

    public Date getCreateTime(){
        return createTime;
    }

    public Map<String, String> getQueryParams() {
        return new HashMap<>(queryParams);
    }

    public Map<String, Object> getBodyParams() {
        return new HashMap<>(bodyParams);
    }

    public Map<String, String> getHeaders() {
        return new HashMap<>(headers);
    }


}
