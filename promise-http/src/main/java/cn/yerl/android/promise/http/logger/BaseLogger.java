package cn.yerl.android.promise.http.logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.yerl.android.promise.http.PromiseHttp;
import cn.yerl.android.promise.http.PromiseHttpException;
import cn.yerl.android.promise.http.PromiseRequest;
import cn.yerl.android.promise.http.PromiseResponse;
import cz.msebera.android.httpclient.client.utils.URIBuilder;

/**
 * 基础日志类
 * Created by Alan Yeh on 2016/12/27.
 */
abstract class BaseLogger implements ILogger {

    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");

    private String getRequestUrl(String baseUrl, String requestUrl){
        try {
            URIBuilder uriBuilder = new URIBuilder(baseUrl);
            URI requestURI = new URI(requestUrl);

            if (requestURI.getHost() != null){
                uriBuilder = new URIBuilder(requestUrl);
            }

            if (requestURI.getPath().startsWith("/")){
                uriBuilder.setPath(requestURI.getPath());
            }else {
                uriBuilder.setPath(uriBuilder.getPath() + "/" + requestURI.getPath());
            }

            if (requestURI.getQuery() != null && requestURI.getQuery().length() > 0){
                uriBuilder.setCustomQuery(requestURI.getQuery());
            }

            return uriBuilder.build().toString();

        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void info(PromiseHttp client, PromiseResponse response) {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.getProperty("line.separator", "\n");

        builder.append(lineSeparator).append(lineSeparator)
                .append("┏━━━━━ [ Promise Http Logger ] ━━━━━━━━━━━━━━━").append(lineSeparator)
                .append("┣ Time: ").append(formatter.format(new Date())).append(lineSeparator)
                .append("┣ URL: ").append(getRequestUrl(client.getBaseUrl(), response.getRequest().getUrlString())).append(lineSeparator)
                .append("┣ Method: ").append(response.getRequest().getMethod().toString()).append(lineSeparator)
                .append("┣ QueryParams: ").append(response.getRequest().getQueryParams().toString()).append(lineSeparator)
                .append("┣ BodyParams: ").append(response.getRequest().getBodyParams().toString()).append(lineSeparator)
                .append("┣ Request Header: ").append(response.getRequest().getHeaders().toString()).append(lineSeparator)

                .append("┣ Execute Time: ").append(response.getCreateTime().getTime() - response.getRequest().getCreateTime().getTime()).append(lineSeparator)
                .append("┣ Response Status: ").append(response.getStatusCode()).append(lineSeparator)
                .append("┣ Response Header: ").append(response.getHeaders().toString()).append(lineSeparator)
                .append("┣ Response Content: ").append(response.getResponseString() == null ? "File: " + response.getResponseFile().toString() : response.getResponseString()).append(lineSeparator)
                .append("┗━━━━━ [ Promise Http Logger ] ━━━━━━━━━━━━━━━").append(lineSeparator);

        writeInfo(builder.toString());
    }


    @Override
    public void error(PromiseHttp client, PromiseRequest request, Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.getProperty("line.separator", "\n");

        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        throwable.printStackTrace(printWriter);


        builder.append(lineSeparator)
                .append("┏━━━━━ [ Promise Http Logger ] ━━━━━━━━━━━━━━━").append(lineSeparator)
                .append("┣ Time: ").append(formatter.format(new Date())).append(lineSeparator)
                .append("┣ URL: ").append(getRequestUrl(client.getBaseUrl(), request.getUrlString())).append(lineSeparator)
                .append("┣ Method: ").append(request.getMethod().toString()).append(lineSeparator)
                .append("┣ QueryParams: ").append(request.getQueryParams().toString()).append(lineSeparator)
                .append("┣ BodyParams: ").append(request.getBodyParams().toString()).append(lineSeparator)
                .append("┣ Request Header: ").append(request.getHeaders().toString()).append(lineSeparator);

        if (throwable instanceof PromiseHttpException){
            PromiseHttpException httpException = (PromiseHttpException) throwable;
            builder.append("┣ Response Content: ").append(httpException.getResponse().getResponseString()).append(lineSeparator);
        }

        builder.append("┣ Exception: ").append(lineSeparator).append(" ").append(writer.toString())
                .append("┗━━━━━ [ Promise Http Logger ] ━━━━━━━━━━━━━━━").append(lineSeparator);

        writeError(builder.toString());
    }

//    abstract void writeInfo(String content, boolean isThrowable);

    abstract void writeInfo(String info);
    abstract void writeError(String error);
}
