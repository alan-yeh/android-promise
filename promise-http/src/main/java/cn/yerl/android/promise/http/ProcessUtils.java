package cn.yerl.android.promise.http;

import android.net.Uri;

import com.lzy.okgo.model.HttpParams;
import com.lzy.okgo.request.base.BodyRequest;

import java.io.File;
import java.net.URLEncoder;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;


/**
 * 请求处理工具
 * Created by Alan Yeh on 2016/12/27.
 */
class ProcessUtils {
    /**
     * 处理baseUrl和request的UrlString关系
     */
    static String processURI(String baseUrl, PromiseRequest request){
        try {
            HttpUrl.Builder builder = new HttpUrl.Builder();

            HttpUrl httpUrl = HttpUrl.parse(request.getUrlString());

            // 非标准URL，可能是/xxxx/xxx或 xxxx/xxx形式，转成标准形式的
            if (httpUrl == null){
                if (request.getUrlString().startsWith("/")){
                    httpUrl = HttpUrl.parse("http://xxx.xx" + request.getUrlString());
                }else {
                    httpUrl = HttpUrl.parse("http://xxx.xx/" + request.getUrlString());
                }
            }

            if (!"xxx.xx".equals(httpUrl.host())){
                // request url里面有host之类的信息，说明是完整的URL
                builder.host(httpUrl.host());
                builder.scheme(httpUrl.scheme());
                builder.port(httpUrl.port());
                builder.encodedPath(httpUrl.encodedPath());
                builder.query(httpUrl.query());
            }else {
                // 设置成baseUrl地址
                if (baseUrl == null || baseUrl.isEmpty()){
                    throw new IllegalArgumentException("无法访问指定的URL，请检查URL格式是否正确");
                }
                HttpUrl baseHttpUrl = HttpUrl.parse(baseUrl);

                builder.host(baseHttpUrl.host());
                builder.scheme(baseHttpUrl.scheme());
                builder.port(baseHttpUrl.port());

                // 处理相对路径
                if (!request.getUrlString().startsWith("/")){
                    builder.encodedPath(baseHttpUrl.encodedPath());
                }

                for (String path : httpUrl.encodedPathSegments()){
                    builder.addEncodedPathSegment(path);
                }

                builder.query(httpUrl.query());
            }


            for (Map.Entry<String, String> query: request.getQueryParams().entrySet()){
                builder.addEncodedQueryParameter(URLEncoder.encode(query.getKey(), request.getEncoding()), query.getValue() == null ? "" : URLEncoder.encode(query.getValue(), request.getEncoding()));
            }

            return builder.build().toString();
        }
        catch (RuntimeException e){
            throw e;
        }
        catch (Exception e) {
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
     * 处理Body参数
     */
    static void processBody(PromiseRequest request, BodyRequest httpRequest){
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
                if (isMultipartRequest(request)){
                    // 上传文件用multipart
                    MultipartBody.Builder bodyBuilder = new MultipartBody.Builder();
                    for (Map.Entry<String, Object> param : request.getBodyParams().entrySet()){
                        if (param.getValue() == null){
                            bodyBuilder.addFormDataPart(param.getKey(), "");
                        }else if (param.getValue() instanceof File){
                            File file = (File)param.getValue();
                            bodyBuilder.addFormDataPart(param.getKey(), file.getName(), RequestBody.create(MediaType.parse("application/octet-stream"), file));
                        }else if (param.getValue() instanceof String){
                            bodyBuilder.addFormDataPart(param.getKey(), (String) param.getValue());
                        }else if (param.getValue() instanceof Integer || param.getValue() instanceof Double || param.getValue() instanceof Boolean || param.getValue() instanceof Character || param.getValue() instanceof Float || param.getValue() instanceof Long){
                            bodyBuilder.addFormDataPart(param.getKey(), param.getValue().toString());
                        }else {
                            throw new IllegalArgumentException("不支持的参数类型" + param.getValue().getClass().getName() + ", 请联系开发者");
                        }
                    }

                    httpRequest.upRequestBody(bodyBuilder.build());
                }else {
                    // 参数用Form
                    FormBody.Builder bodyBuilder = new FormBody.Builder();
                    for (Map.Entry<String, Object> param : request.getBodyParams().entrySet()){
                        if (param.getValue() == null){
                            bodyBuilder.addEncoded(URLEncoder.encode(param.getKey()), "");
                        }else if (param.getValue() instanceof String){
                            bodyBuilder.addEncoded(URLEncoder.encode(param.getKey(), request.getEncoding()), URLEncoder.encode((String) param.getValue(), request.getEncoding()));
                        }else if (param.getValue() instanceof Integer || param.getValue() instanceof Double || param.getValue() instanceof Boolean || param.getValue() instanceof Character || param.getValue() instanceof Float || param.getValue() instanceof Long){
                            bodyBuilder.addEncoded(URLEncoder.encode(param.getKey(), request.getEncoding()), param.getValue().toString());
                        }else {
                            throw new IllegalArgumentException("不支持的参数类型" + param.getValue().getClass().getName() + ", 请联系开发者");
                        }
                    }

                    httpRequest.upRequestBody(bodyBuilder.build());
                }
            }

            // JSON
            if (request.getRawBody() != null && request.getRawBody().length() > 0){
                httpRequest.upString(request.getRawBody());
            }

            //TODO: Check
            if (request.getBinaryBody() != null){
                httpRequest.upFile(request.getBinaryBody());
            }
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    private static boolean isMultipartRequest(PromiseRequest request){
        for (Object value : request.getBodyParams().values()){
            if (value instanceof File){
                return true;
            }
        }
        return false;
    }

}
