package cn.yerl.android.promise.http;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestHandle;
import com.loopj.android.http.ResponseHandlerInterface;

import cz.msebera.android.httpclient.client.methods.HttpUriRequest;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;

/**
 * Http Client
 * Created by Alan Yeh on 2016/12/20.
 */

class HttpClient extends AsyncHttpClient {
    public HttpClient(){
        super(true, 80, 443);
        setEnableRedirects(true, true, true);
    }

    RequestHandle sendRequest(HttpUriRequest uriRequest, ResponseHandlerInterface responseHandler){
        return sendRequest((DefaultHttpClient) getHttpClient(), getHttpContext(), uriRequest, null, responseHandler, null);
    }
}
