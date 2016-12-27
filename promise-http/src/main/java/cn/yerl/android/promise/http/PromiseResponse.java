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

    public PromiseRequest getRequest() {
        return request;
    }

    public Map<String, String> getHeaders() {
        return new HashMap<>(headers);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseString() {
        return responseString;
    }

    public File getResponseFile() {
        return responseFile;
    }

    public Date getCreateTime(){
        return createTime;
    }
}
