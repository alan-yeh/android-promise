package cn.yerl.android.promise.http.logger;

import java.io.File;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.yerl.android.promise.http.PromiseHttp;
import cn.yerl.android.promise.http.PromiseRequest;
import cn.yerl.android.promise.http.PromiseResponse;
import cz.msebera.android.httpclient.client.utils.URIBuilder;

/**
 * Http 文件日志
 * Created by Alan Yeh on 2016/12/27.
 */
public class FileLogger extends ILogger {
    private final File logPath;
    private final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
    private final SimpleDateFormat logFolderFormatter = new SimpleDateFormat("yyyy-MM-dd");
    private final SimpleDateFormat logFilenameFormatter = new SimpleDateFormat("HH");

    /**
     * 创建文件日志
     * @param logPath 日志保存地址
     */
    public FileLogger(File logPath){
        if (logPath == null){
            throw new IllegalArgumentException("PromiseHttp: logPath不能为空");
        }
        this.logPath = logPath;
        if (!logPath.exists()){
            logPath.mkdirs();
        }
    }

    private void writeContent(String content){
        Date date = new Date();
        String folderName = logFolderFormatter.format(date);
        File folder = new File(logPath.getPath() + File.separator + folderName);
        if (! folder.exists()){
            folder.mkdirs();
        }

        try{
            RandomAccessFile randomAccessFile = new RandomAccessFile(new File(folder.getPath() + File.separator + logFilenameFormatter.format(date) + ".log"), "rw");
            randomAccessFile.seek(randomAccessFile.length());
            randomAccessFile.write(content.getBytes("UTF-8"));
            randomAccessFile.close();
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

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
    public void log(PromiseHttp client, PromiseResponse response) {
        StringBuilder builder = new StringBuilder();
        String lineSeparator = System.getProperty("line.separator", "\n");

        builder.append(lineSeparator)
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

        writeContent(builder.toString());
    }

    @Override
    public void log(PromiseHttp client, PromiseRequest request, Throwable throwable) {
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
                .append("┣ Request Header: ").append(request.getHeaders().toString()).append(lineSeparator)
                .append("┣ Exception: ").append(lineSeparator).append(" ").append(writer.toString())
                .append("┗━━━━━ [ Promise Http Logger ] ━━━━━━━━━━━━━━━").append(lineSeparator);

        writeContent(builder.toString());
    }
}
