package cn.yerl.android.promise.http;

import com.loopj.android.http.RequestParams;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.utils.URIBuilder;
import cz.msebera.android.httpclient.client.utils.URLEncodedUtils;
import cz.msebera.android.httpclient.message.BasicHeader;

/**
 * 请求处理工具
 * Created by Alan Yeh on 2016/12/27.
 */
class ProcessUtils {
    public static URI processURI(String baseUrl, PromiseRequest request){
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

            return uriBuilder.build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static Header[] processHeader(Map<String, String> sharedHeaders, Map<String, String> reuquestHeaders){
        List<Header> result = new ArrayList<>();

        Map<String, String> headers = new HashMap<>();
        headers.putAll(sharedHeaders);
        headers.putAll(reuquestHeaders);

        for (Map.Entry<String, String> header : headers.entrySet()){
            result.add(new BasicHeader(header.getKey(), header.getValue()));
        }

        return result.toArray(new Header[result.size()]);
    }

    public static RequestParams processParams(PromiseRequest request){
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
        return params;
    }
}
