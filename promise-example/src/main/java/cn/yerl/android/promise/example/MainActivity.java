package cn.yerl.android.promise.example;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;

import cn.yerl.android.promise.core.PromiseCallback;
import cn.yerl.android.promise.http.PromiseHttp;
import cn.yerl.android.promise.http.PromiseRequest;
import cn.yerl.android.promise.http.PromiseResponse;
import cn.yerl.android.promise.http.logger.LogcatLogger;

public class MainActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PromiseHttp.init(this.getApplication());
        PromiseHttp.client().setCachePath(this.getCacheDir());

        PromiseHttp.client().setBaseUrl("http://192.168.0.208");
        PromiseHttp.client().setLogger(new LogcatLogger());
    }

    public void list(View view){
        PromiseRequest req = PromiseRequest.GET("http://124.227.24.2:7005/mobilework/login/login")
                .withQueryParam("j_username", "admin")
                .withQueryParam("j_password", "11");

//        PromiseHttp.client().execute(req).then(new PromiseCallback<PromiseResponse, Object>() {
//            @Override
//            public Object call(PromiseResponse arg) {
//
//            }
//        })

//        PromiseRequest req = PromiseRequest.GET("/MSGCollaboration/appMain?service=com.minstone.msgcollaboration.action.port.helper.PortCmd&func=loadMyCollaboration")
//                .withQueryParam("self", "true")
//                .withQueryParam("pageNo", "1")
//                .withQueryParam("pageSize", "20");
//
//        PromiseHttp.client().execute(req)
//                .then((PromiseResponse arg) -> {
//                    Toast.makeText(MainActivity.this, "正确: " + arg.getResponseString(), Toast.LENGTH_LONG).show();
//                    return null;
//                }).error(arg -> {
//            Toast.makeText(MainActivity.this, "失败: " + arg.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
//        });
    }

    public void send_mail(View view){
        PromiseRequest req = PromiseRequest.POST("/MSGCollaboration/appMain?service=com.minstone.msgcollaboration.action.port.helper.PortCmd&func=saveMsgBaseInfo")
                .withBodyParam("isUpdate", "false")
                .withBodyParam("isSend", "true")
                .withBodyParam("attachIds", "0")
                .withBodyParam("msgSeq", "");

        PromiseHttp.client().execute(req)
                .then((PromiseResponse arg) -> {
                    Toast.makeText(MainActivity.this, "正确: " + arg.getResponseString(), Toast.LENGTH_LONG).show();
                    return null;
                }).error(arg -> {
            Toast.makeText(MainActivity.this, "失败: " + arg.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    public void create_schedule(View view){
        PromiseRequest req = PromiseRequest.POST("/PASystem/appMain")
                .withQueryParam("service", "com.minstone.pasystem.action.port.helper.PortCmd")
                .withQueryParam("func", "addSchedule")

                .withBodyParam("content", "测试新建行程")
                .withBodyParam("endDate", "1500289860000")
                .withBodyParam("gradeType", "0")
                .withBodyParam("isremindUser", "0")
                .withBodyParam("location", "")
                .withBodyParam("open", "1")
                .withBodyParam("organizer", "")
                .withBodyParam("remindDate", "")
                .withBodyParam("startDate", "1500282720000")
                .withBodyParam("title", "测试一下")
                .withBodyParam("type", "1");
        req.setEncoding("GBK");

        PromiseHttp.client().execute(req)
                .then((PromiseResponse arg) ->{
                    Toast.makeText(MainActivity.this, "正确: " + arg.getResponseString(), Toast.LENGTH_LONG).show();
                    return null;
                }).error(arg -> {
            Toast.makeText(MainActivity.this, "失败: " + arg.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        });
    }


    public void login(View view) throws Exception{
        PromiseRequest loginReq = PromiseRequest.GET("/mobilework/login/login?j_username=admin")
                .withQueryParam("j_password", "11");

        PromiseHttp.client().execute(loginReq)
                .then((PromiseResponse arg) -> {
                    Toast.makeText(MainActivity.this, "正确: " + arg.getResponseString(), Toast.LENGTH_LONG).show();
                    return null;
                }).error(arg -> {
            Toast.makeText(MainActivity.this, "失败: " + arg.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    File downloadedFile;
    public void download(View view){
        // Download
        PromiseRequest request = PromiseRequest.GET("http://archives.codesync.cn/archives/api/releases/download/1697db92-ee79-4b1d-8084-be800093a1b0");

        request.addDownloadProgressListener(new PromiseRequest.OnProgressChanged() {
            @Override
            public void onProgress(long bytesWritten, long totalSize) {
//                Toast.makeText(MainActivity.this, bytesWritten + "/" + totalSize, Toast.LENGTH_LONG).show();
                Log.d("【Download】", "bytesWritten: "+ bytesWritten + "  totalSize: " + totalSize);
            }
        });

        PromiseHttp.client().download(request).then(new PromiseCallback<PromiseResponse, Object>() {
            @Override
            public Object call(PromiseResponse arg) {
                Toast.makeText(MainActivity.this, "下载成功：" + arg.getResponseFile().getName(), Toast.LENGTH_SHORT).show();
                MainActivity.this.downloadedFile = arg.getResponseFile();
                return null;
            }
        }).error(new PromiseCallback<RuntimeException, Object>() {
            @Override
            public Object call(RuntimeException arg) {
                Toast.makeText(MainActivity.this, "下载失败：" + arg.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                return null;
            }
        });
    }

    public void upload(View view){
        // Upload
        PromiseRequest request = PromiseRequest.POST("http://192.168.0.208/mobile-oa/api/disk/file").withBodyParam("file", downloadedFile);
        request.addUploadProgressListener(new PromiseRequest.OnProgressChanged() {
            @Override
            public void onProgress(long bytesWritten, long totalSize) {
                Toast.makeText(MainActivity.this, bytesWritten + "/" + totalSize, Toast.LENGTH_LONG).show();
                Log.d("【Upload】", "bytesWritten: "+ bytesWritten + "  totalSize: " + totalSize);
            }
        });
        PromiseHttp.client().execute(request).then(new PromiseCallback<PromiseResponse, Object>() {
            @Override
            public Object call(PromiseResponse arg) {
                Toast.makeText(MainActivity.this, arg.getResponseString(), Toast.LENGTH_LONG).show();
                return null;
            }
        }).error(new PromiseCallback<RuntimeException, Object>() {
            @Override
            public Object call(RuntimeException arg) {
                Toast.makeText(MainActivity.this, arg.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                return null;
            }
        });
    }
}
