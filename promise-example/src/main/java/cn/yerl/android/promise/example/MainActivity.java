package cn.yerl.android.promise.example;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import cn.yerl.android.promise.core.PromiseCallback;
import cn.yerl.android.promise.http.PromiseHttp;
import cn.yerl.android.promise.http.PromiseRequest;
import cn.yerl.android.promise.http.PromiseResponse;
import cn.yerl.android.promise.http.logger.LogcatLogger;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PromiseHttp.client().setBaseUrl("https://moa.liuzhou.gov.cn");
//        PromiseHttp.client().addSharedHeader("PRIVATE-TOKEN", "oK-19ifaqNhVbqAs5xwe");
        PromiseHttp.client().setLogger(new LogcatLogger());
//        PromiseHttp.client().setBaseUrl("http://ma.minstone.com.cn");
        PromiseHttp.client().setCachePath(MainActivity.this.getCacheDir());
    }

    public void onClick(View view){
        PromiseRequest request = PromiseRequest.GET("/mobilework/login/login")
                .withQueryParam("j_username", "18878911678")
                .withQueryParam("j_password", "Lzz!1234");

        PromiseHttp.client().execute(request).then(new PromiseCallback<PromiseResponse, PromiseResponse>() {
            @Override
            public Object call(PromiseResponse arg) {
                PromiseRequest request = PromiseRequest.GET("/mobilework/s")
                        .withQueryParam("service", "flowAttach")
                        .withQueryParam("fileInid", "2014941353")
                        .withQueryParam("fileName", "附件3政府公文处理笺.pdf")
                        .withQueryParam("stepInco", "2019115675")
                        .withQueryParam("flowInid", "2012948532")
                        ;
                request.addDownloadProgressListener(new PromiseRequest.OnProgressChanged() {
                    @Override
                    public void onProgress(long bytesWritten, long totalSize) {

                    }
                });
                return PromiseHttp.client().download(request);
            }
        }).then(new PromiseCallback<PromiseResponse, Object>() {
            @Override
            public Object call(PromiseResponse arg) {
                Log.d("response", arg.getResponseString());
                return null;
            }
        }).error(new PromiseCallback<RuntimeException, Object>() {
            @Override
            public Object call(RuntimeException arg) {
                arg.printStackTrace();
                return null;
            }
        });
//                .setEncoding("GBK");


//        PromiseRequest request = PromiseRequest.GET("http://codesync.cn/api/v3/groups");
//
//        PromiseHttp.client().execute(request).then(new PromiseCallback<PromiseResponse, Object>() {
//            @Override
//            public Object call(PromiseResponse arg) {
//                Toast.makeText(MainActivity.this, arg.getResponseString(), Toast.LENGTH_LONG).show();
//                return null;
//            }
//        }).error(new PromiseCallback<RuntimeException, Object>() {
//            @Override
//            public Object call(RuntimeException arg) {
//                Toast.makeText(MainActivity.this, arg.getMessage(), Toast.LENGTH_LONG).show();
//                return null;
//            }
//        });
//        PromiseRequest request =PromiseRequest.POST("http://ma.minstone.com.cn/mobilework/login/login?aa=bb")
//                .withQueryParam("j_username", "admin")
//                .withQueryParam("j_password", "11");
//        PromiseHttp.client().execute(request).then(new PromiseCallback<PromiseResponse, Object>() {
//            @Override
//            public Object call(PromiseResponse arg) {
//                Toast.makeText(MainActivity.this, arg.getResponseString(), Toast.LENGTH_LONG).show();
//                return null;
//            }
//        }).error(new PromiseCallback<RuntimeException, Object>() {
//            @Override
//            public Object call(RuntimeException arg) {
////                arg.printStackTrace();
//                Toast.makeText(MainActivity.this, arg.getMessage(), Toast.LENGTH_LONG).show();
//                return null;
//            }
//        });

//        PromiseRequest request = PromiseRequest.GET("http://192.168.0.185:9081/mobilework/login/login")
//                .withQueryParam("j_username", "admin")
//                .withQueryParam("j_password", "11");
//
//        PromiseHttp.client().execute(request).then(new PromiseCallback<PromiseResponse, Object>() {
//            @Override
//            public Object call(PromiseResponse arg) {
//                Toast.makeText(MainActivity.this, arg.getResponseString(), Toast.LENGTH_LONG).show();
//
//                PromiseRequest request =PromiseRequest.GET("http://192.168.0.185:9081/OAMessage/api/summary");
//                return PromiseHttp.client().execute(request).then(new PromiseCallback<PromiseResponse, Object>() {
//                    @Override
//                    public Object call(PromiseResponse arg) {
//                        Toast.makeText(MainActivity.this, arg.getResponseString(), Toast.LENGTH_LONG).show();
//                        return null;
//                    }
//                });
//            }
//        }).error(new PromiseCallback<RuntimeException, Object>() {
//            @Override
//            public Object call(RuntimeException arg) {
//                Toast.makeText(MainActivity.this, arg.getMessage(), Toast.LENGTH_LONG).show();
//                return null;
//            }
//        });
        // GET Success
//        PromiseRequest request = PromiseRequest.GET("/OATasks/andes/interface.json?test=abc").withQueryParam("hello", "haha").withQueryParam("bb", "bb");
//        PromiseHttp.client().execute(request).then(new PromiseCallback<PromiseResponse, Object>() {
//            @Override
//            public Object call(PromiseResponse arg) {
//                Toast.makeText(MainActivity.this, arg.getResponseString(), Toast.LENGTH_LONG).show();
//                return null;
//            }
//        });
//
        // GET Error
//        PromiseRequest request = PromiseRequest.GET("/apps/latest/576107d2e75e2d717d000014");
//        PromiseHttp.client().execute(request).then(new PromiseCallback<PromiseResponse, Object>() {
//            @Override
//            public Object call(PromiseResponse arg) {
//                Toast.makeText(MainActivity.this, arg.getResponseString(), Toast.LENGTH_LONG).show();
//                return null;
//            }
//        }).error(new PromiseCallback<RuntimeException, Object>() {
//            @Override
//            public Object call(RuntimeException arg) {
//                if (arg instanceof PromiseHttpException){
//                    PromiseHttpException ex = (PromiseHttpException)arg;
//                    Toast.makeText(MainActivity.this, ex.getResponse().getResponseString(), Toast.LENGTH_LONG).show();
//                }
//                return null;
//            }
//        });

        // POST
//        PromiseRequest request = PromiseRequest.POST("http://api.fir.im/apps")
//                .withBodyParam("type", "ios")
//                .withBodyParam("bundle_id", "cn.yerl.aa")
//                .withBodyParam("api_token", "f156b688dd49f664d85a5c5eac6597d4");
//        PromiseHttp.client().execute(request).then(new PromiseCallback<PromiseResponse, Object>() {
//            @Override
//            public Object call(PromiseResponse arg) {
//                Toast.makeText(MainActivity.this, arg.getResponseString(), Toast.LENGTH_LONG).show();
//                return null;
//            }
//        }).error(new PromiseCallback<RuntimeException, Object>() {
//            @Override
//            public Object call(RuntimeException arg) {
//                if (arg instanceof PromiseHttpException){
//                    PromiseHttpException ex = (PromiseHttpException)arg;
//                    Toast.makeText(MainActivity.this, ex.getResponse().getResponseString(), Toast.LENGTH_LONG).show();
//                }
//                return null;
//            }
//        });

        // POST Error
//        PromiseRequest request = PromiseRequest.POST("http://api.fir.im/apps")
//                .withBodyParam("type", "ios")
//                .withBodyParam("bundle_id", "cn.yerl.aa")
//                .withBodyParam("api_token", "f156b688dd49f664d85a5c5eac6a597d4");
//        PromiseHttp.client().execute(request).then(new PromiseCallback<PromiseResponse, Object>() {
//            @Override
//            public Object call(PromiseResponse arg) {
//                Toast.makeText(MainActivity.this, arg.getResponseString(), Toast.LENGTH_LONG).show();
//                return null;
//            }
//        }).error(new PromiseCallback<RuntimeException, Object>() {
//            @Override
//            public Object call(RuntimeException arg) {
//                if (arg instanceof PromiseHttpException){
//                    PromiseHttpException ex = (PromiseHttpException)arg;
//                    Toast.makeText(MainActivity.this, ex.getResponse().getResponseString(), Toast.LENGTH_LONG).show();
//                }
//                return null;
//            }
//        });

        // Download
//        PromiseRequest request = PromiseRequest.GET("https://raw.githubusercontent.com/alan-yeh/gradle-plugins/master/nexus-plugin/build.gradle");
//
//        request.addDownloadProgressListener(new PromiseRequest.OnProgressChanged() {
//            @Override
//            public void onProgress(long bytesWritten, long totalSize) {
//                Log.d("【Download】", "bytesWritten: "+ bytesWritten + "  totalSize: " + totalSize);
//            }
//        });
//
//        PromiseHttp.client().download(request).then(new PromiseCallback<PromiseResponse, Object>() {
//            @Override
//            public Object call(PromiseResponse arg) {
//                return null;
//            }
//        });

        // Upload
//        PromiseRequest request = PromiseRequest.POST("http://ma.minstone.com.cn/MDDisk/file").withBodyParam("file", new File(MainActivity.this.getCacheDir().getAbsolutePath() + File.separator + "build.gradle"));
//        PromiseHttp.client().execute(request).then(new PromiseCallback<PromiseResponse, Object>() {
//            @Override
//            public Object call(PromiseResponse arg) {
//                Toast.makeText(MainActivity.this, arg.getResponseString(), Toast.LENGTH_LONG).show();
//                return null;
//            }
//        }).error(new PromiseCallback<RuntimeException, Object>() {
//            @Override
//            public Object call(RuntimeException arg) {
//                return null;
//            }
//        });
    }
}
