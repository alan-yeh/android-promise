package cn.yerl.android.promise.http;

import com.loopj.android.http.RequestHandle;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.yerl.android.promise.core.Promise;
import cn.yerl.android.promise.core.PromiseCallback;

/**
 * 封装请求参数类
 * Created by yan on 16/6/6.
 */
public class PromiseRequest implements Serializable {
    /**
     * HttpMethod
     */
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
    RequestHandle handler;

    private PromiseRequest(String url, Method method){
        this.urlString = url;
        this.method = method;
        this.createTime = new Date();
    }

    /**
     * 生成一个Http Get请求
     * @param url 服务器地址
     * @return PromiseRequest
     */
    public static PromiseRequest GET(String url){
        return new PromiseRequest(url, Method.GET);
    }

    /**
     * 生成一个Http POST请求
     * @param url 服务器地址
     * @return PromiseRequest
     */
    public static PromiseRequest POST(String url){
        return new PromiseRequest(url, Method.POST);
    }

    /**
     * 生成一个Http PUT请求
     * @param url 服务器地址
     * @return PromiseRequest
     */
    public static PromiseRequest PUT(String url){
        return new PromiseRequest(url, Method.PUT);
    }

    /**
     * 生成一个Http DELETE请求
     * @param url 服务器地址
     * @return PromiseRequest
     */
    public static PromiseRequest DELETE(String url){
        return new PromiseRequest(url, Method.DELETE);
    }

    /**
     * 生成一个Http HEAD请求
     * @param url 服务器地址
     * @return PromiseRequest
     */
    public static PromiseRequest HEAD(String url){
        return new PromiseRequest(url, Method.HEAD);
    }


    private Map<String, String> queryParams = new LinkedHashMap<>();

    /**
     * 添加一个URL参数
     * @param key 参数名
     * @param value 参数值
     * @return PromiseRequest
     */
    public PromiseRequest withQueryParam(String key, String value){
        this.queryParams.put(key, value);
        return this;
    }

    /**
     * 添加多个URL参数
     * @param queryParams 参数键值对
     * @return PromiseRequest
     */
    public PromiseRequest withQueryParams(Map<String, String> queryParams){
        this.queryParams.putAll(queryParams);
        return this;
    }

    /**
     * 移除URL参数
     * @param keys 参数key
     * @return PromiseRequest
     */
    public PromiseRequest removeQueryParam(String... keys){
        for (String key : keys){
            this.queryParams.remove(key);
        }
        return this;
    }

    private Map<String, Object> bodyParams = new LinkedHashMap<>();

    /**
     * 添加一个Body参数
     * @param key 参数名
     * @param value 参数值
     * @return PromiseRequest
     */
    public PromiseRequest withBodyParam(String key, Object value){
        this.bodyParams.put(key, value);
        return this;
    }
    /**
     * 添加多个Body参数
     * @param bodyParams 参数键值对
     * @return PromiseRequest
     */
    public PromiseRequest withBodyParams(Map<String, Object> bodyParams){
        this.bodyParams.putAll(bodyParams);
        return this;
    }

    /**
     * 移除Body参数
     * @param keys 参数key
     * @return PromiseRequest
     */
    public PromiseRequest removeBodyParam(String... keys){
        for (String key : keys){
            this.bodyParams.remove(key);
        }
        return this;
    }

    private Map<String, Object> pathParams = new LinkedHashMap<>();

    /**
     * 添加Path参数, 使用{}进行占位
     * @param key 参数名
     * @param value 参数值
     * @return PromiseRequest
     */
    public PromiseRequest withPathParam(String key, Object value){
        this.pathParams.put(key, value);
        return this;
    }

    /**
     * 添加多个Path参数, 使用{}进行占位
     * @param pathParams 参数键值对
     * @return PromiseRequest
     */
    public PromiseRequest withPathParams(Map<String, Object> pathParams){
        this.pathParams.putAll(pathParams);
        return this;
    }

    /**
     * 移除Path参数
     * @param keys 参数key
     * @return PromiseRequest
     */
    public PromiseRequest removePathParam(String... keys){
        for (String key : keys){
            this.pathParams.remove(key);
        }
        return this;
    }

    private Map<String, String> headers = new LinkedHashMap<>();

    /**
     * 添加一个Header
     * @param key Header名
     * @param value Header值
     * @return PromiseRequest
     */
    public PromiseRequest withHeader(String key, String value){
        this.headers.put(key, value);
        return this;
    }

    /**
     * 添加多个Header
     * @param headers Header键值对
     * @return PromiseRequest
     */
    public PromiseRequest withHeaders(Map<String, String> headers){
        this.headers.putAll(headers);
        return this;
    }

    /**
     * 移除Header
     * @param keys 参数key
     * @return PromiseRequest
     */
    public PromiseRequest removeHeaderParam(String... keys){
        for (String key : keys){
            this.headers.remove(key);
        }
        return this;
    }

    /**
     * 请求进度监听接口
     */
    public interface OnProgressChanged{
        void onProgress(long bytesWritten, long totalSize);
    }

    private List<WeakReference<OnProgressChanged>> progressListeners = new ArrayList<>();

    /**
     * 下载进度反馈
     * @param progressListener 下载进度监听器
     */
    public void addDownloadProgressListener(OnProgressChanged progressListener){
        progressListeners.add(new WeakReference<>(progressListener));
    }

    void onProgress(final long bytesWritten, final long totalSize){
        // 切换到主线程去通知
        new Promise<>(new PromiseCallback<Object, Object>() {
            @Override
            public Object call(Object arg) {
                for (WeakReference<OnProgressChanged> listener : progressListeners){
                    if (listener.get() != null){
                        listener.get().onProgress(bytesWritten, totalSize);
                    }
                }
                return null;
            }
        });
    }

    /**
     * 取消请求
     */
    public void cancel(){
        if (!handler.isCancelled() && !handler.isFinished()){
            handler.cancel(true);
        }
    }

    /**
     * 获取Http Method
     * @return Method
     */
    public Method getMethod() {
        return method;
    }

    /**
     * 获取请求地址
     * @return UrlString
     */
    public String getUrlString() {
        return urlString;
    }

    /**
     * 获取请求编码
     * @return String
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * 设置请求、响应编码，默认UTF-8
     * @param encoding 编码
     * @return PromiseRequest
     */
    public PromiseRequest setEncoding(String encoding){
        this.encoding = encoding;
        return this;
    }

    /**
     * 获取请求创建时间
     * @return Date
     */
    public Date getCreateTime(){
        return createTime;
    }

    /**
     * 获取URL参数
     * @return Map
     */
    public Map<String, String> getQueryParams() {
        return new HashMap<>(queryParams);
    }

    /**
     * 获取Body参数
     * @return Map
     */
    public Map<String, Object> getBodyParams() {
        return new HashMap<>(bodyParams);
    }

    public Map<String, Object> getPathParams(){
        return new HashMap<>(pathParams);
    }

    /**
     * 获取Header
     * @return Map
     */
    public Map<String, String> getHeaders() {
        return new HashMap<>(headers);
    }
}
