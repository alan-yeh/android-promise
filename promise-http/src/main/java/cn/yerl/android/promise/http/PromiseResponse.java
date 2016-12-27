package cn.yerl.android.promise.http;

import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import cz.msebera.android.httpclient.Header;

/**
 * 封装服务器返回的内容
 * Created by yan on 16/6/11.
 */
public class PromiseResponse implements Serializable {
    private PromiseRequest request;
    private Map<String, String> headers;
    private int statusCode;
    private String responseString;
    private File responseFile;
    final private Date createTime;

    PromiseResponse(PromiseRequest request, int statusCode, Header[] headers, String responseString){
        this.request = request;
        this.statusCode = statusCode;
        this.responseString = responseString;
        this.headers = new HashMap<>();
        this.createTime = new Date();
        if (headers != null){
            for (Header header : headers){
                this.headers.put(header.getName(), header.getValue());
            }
        }
    }
    PromiseResponse(PromiseRequest request, int statusCode, Header[] headers, File responseFile){
        this.request = request;
        this.statusCode = statusCode;
        this.responseFile = responseFile;
        this.headers = new HashMap<>();
        this.createTime = new Date();
        if (headers != null){
            for (Header header : headers){
                this.headers.put(header.getName(), header.getValue());
            }
        }
    }

    /**
     * 获取请求
     * @return PromiseRequest
     */
    public PromiseRequest getRequest() {
        return request;
    }

    /**
     * 获取Response Header
     * @return Map
     */
    public Map<String, String> getHeaders() {
        return new HashMap<>(headers);
    }

    /**
     * 获取Response Status
     * @return int
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * 获取Response String
     * 非下载请求的结果
     * @return String
     */
    public String getResponseString() {
        return responseString;
    }

    /**
     * 获取Response File
     * 下载请求的结果
     * @return File
     */
    public File getResponseFile() {
        return responseFile;
    }

    /**
     * 获取Response的创建时间
     * @return Date
     */
    public Date getCreateTime(){
        return createTime;
    }
}
