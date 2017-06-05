package cn.yerl.android.promise.http;

import com.loopj.android.http.RequestParams;
import com.loopj.android.http.ResponseHandlerInterface;

import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.client.utils.URLEncodedUtils;
import cz.msebera.android.httpclient.entity.ContentType;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.message.BasicHeader;
import cz.msebera.android.httpclient.message.BasicNameValuePair;

/**
 * 请求处理工具
 * Created by Alan Yeh on 2016/12/27.
 */
class ProcessUtils {
    /**
     * 处理baseUrl和request的UrlString关系
     */
    public static URI processURI(String baseUrl, PromiseRequest request){
        try {
            URIBuilder uriBuilder = new URIBuilder(baseUrl);

            String URLString = processPathParams(request.getUrlString(), request.getPathParams());

            URI requestURI = new URI(URLString);

            if (requestURI.getHost() != null){
                uriBuilder = new URIBuilder(request.getUrlString()).clearParameters();
            }

            if (requestURI.getPath().startsWith("/")){
                uriBuilder.setPath(requestURI.getPath());
            }else {
                uriBuilder.setPath(uriBuilder.getPath() + "/" + requestURI.getPath());
            }

            URI result = uriBuilder.build();
            String url = result.toString();


            List<NameValuePair> queryParams = new ArrayList<>();

            queryParams.addAll(URLEncodedUtils.parse(requestURI.getRawQuery(), Charset.forName(request.getEncoding())));

            for (Map.Entry<String, String> param : request.getQueryParams().entrySet()){
                queryParams.add(new BasicNameValuePair(param.getKey(), param.getValue()));
            }

            String encodedQuery = URLEncodedUtils.format(queryParams, Charset.forName(request.getEncoding()));
            if (encodedQuery != null && encodedQuery.length() > 0){
                url = url + "?" + encodedQuery;
            }
            return new URI(url);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String processPathParams(String URLString, Map<String, Object> pathParams){
        if (URLString.indexOf('{') == -1){
            return URLString;
        }

        for (Map.Entry<String, Object> entry : pathParams.entrySet()){
            URLString = URLString.replaceAll("{" + entry.getKey() + "}", entry.getValue().toString());
        }
        return URLString;
    }

    /**
     * 将shareHeader和requestHeader处理成Header[]
     */
    public static Header[] processHeader(Map<String, String> sharedHeaders, Map<String, String> requestHeaders){
        List<Header> result = new ArrayList<>();

        Map<String, String> headers = new HashMap<>();
        headers.putAll(sharedHeaders);
        headers.putAll(requestHeaders);

        for (Map.Entry<String, String> header : headers.entrySet()){
            result.add(new BasicHeader(header.getKey(), header.getValue()));
        }

        return result.toArray(new Header[result.size()]);
    }

    /**
     * 处理Body参数
     */
    public static HttpEntity processParams(PromiseRequest request, ResponseHandlerInterface handler){
        int flag = 0;
        if (request.getBodyParams().size() > 0){
            flag ++;
        }
        if (request.getRawBody() != null && request.getRawBody().length() > 0){
            flag ++;
        }
        if (request.getBinaryBody() != null){
            flag ++;
        }
        if (flag > 1){
            throw new IllegalArgumentException("PromiseRequest支持BodyParam、Raw、Binary三种之一，不能同时设置");
        }



        try {
            if (request.getBodyParams().size() > 0){
                RequestParams params = new RequestParams();
                params.setContentEncoding(request.getEncoding());
                for (Map.Entry<String, Object> param : request.getBodyParams().entrySet()){
                    if (param.getValue() instanceof File){
                        params.put(param.getKey(), (File)param.getValue());
                    }else if (param.getValue() instanceof File[]){
                        params.put(param.getKey(), (File[])param.getValue());
                    }else if (param.getValue() instanceof InputStream){
                        params.put(param.getKey(), (InputStream)param.getValue());
                    }else if (param.getValue() instanceof String){
                        params.put(param.getKey(), (String) param.getValue());
                    }else{
                        params.put(param.getKey(), param.getValue());
                    }
                }

                return params.getEntity(handler);
            }

            if (request.getRawBody() != null && request.getRawBody().length() > 0){
                StringEntity entity = new StringEntity(request.getRawBody() , request.getEncoding());
                entity.setContentEncoding(request.getEncoding());
                return entity;
            }

            // 未处理
            if (request.getBinaryBody() != null){
                RequestParams params = new RequestParams();
            }

            return null;
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }
}
