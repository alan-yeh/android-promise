package cn.yerl.android.promise.example;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import java.io.File;

import cn.yerl.android.promise.core.PromiseCallback;
import cn.yerl.android.promise.http.PromiseHttp;
import cn.yerl.android.promise.http.PromiseRequest;
import cn.yerl.android.promise.http.PromiseResponse;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PromiseHttp.client().setBaseUrl("http://api.fir.im/api");
        PromiseHttp.client().setCachePath(MainActivity.this.getCacheDir());
    }

    public void onClick(View view){
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
//        PromiseHttp.client().download(request).then(new PromiseCallback<PromiseResponse, Object>() {
//            @Override
//            public Object call(PromiseResponse arg) {
//                return null;
//            }
//        });
//        request.setOnDownloadProgress(new PromiseRequest.OnProgress() {
//            @Override
//            public void onProgress(long bytesWritten, long totalSize) {
//                Log.d("【Download】", "bytesWritten: "+ bytesWritten + "  totalSize: " + totalSize);
//            }
//        });

        // Upload
        PromiseRequest request = PromiseRequest.POST("http://ma.minstone.com.cn/MDDisk/file").withBodyParam("file", new File(MainActivity.this.getCacheDir().getAbsolutePath() + File.separator + "build.gradle"));
        PromiseHttp.client().execute(request).then(new PromiseCallback<PromiseResponse, Object>() {
            @Override
            public Object call(PromiseResponse arg) {
                Toast.makeText(MainActivity.this, arg.getResponseString(), Toast.LENGTH_LONG).show();
                return null;
            }
        }).error(new PromiseCallback<RuntimeException, Object>() {
            @Override
            public Object call(RuntimeException arg) {
                return null;
            }
        });
    }
}
