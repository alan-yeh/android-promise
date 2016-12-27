# Promise Http
## 引用

```
repositories {
  mavenCentral()
}

dependencies {
  compile 'cn.yerl.android:promise-http:+'
}
```

## 简介
　　Promise Http在AsyncHttpClient的基础上使用Promise封装了一下，用于简化网络请求。

## GET/POST/PUT/DELETE/HEAD

```java
    PromiseRequest request = PromiseRequest.GET("http://codesync.cn/api/v3/groups");

    // 执行请求
    PromiseHttp.client().execute(request).then(new PromiseCallback<PromiseResponse, Object>() {
        @Override
        public Object call(PromiseResponse arg) {
            Toast.makeText(MainActivity.this, arg.getResponseString(), Toast.LENGTH_LONG).show();
            return null;
        }
    }).error(new PromiseCallback<RuntimeException, Object>() {
        @Override
        public Object call(RuntimeException arg) {
            Toast.makeText(MainActivity.this, arg.getMessage(), Toast.LENGTH_LONG).show();
            return null;
        }
    });
```

## DOWNLOAD

```java
    PromiseRequest request = PromiseRequest.GET("https://raw.githubusercontent.com/alan-yeh/gradle-plugins/master/nexus-plugin/build.gradle");

    // 添加下载监听
    request.addDownloadProgressListener(new PromiseRequest.OnProgressChanged() {
        @Override
        public void onProgress(long bytesWritten, long totalSize) {
            Log.d("【Download】", "bytesWritten: "+ bytesWritten + "  totalSize: " + totalSize);
        }
    });

    // 执行下载请求
    PromiseHttp.client().download(request).then(new PromiseCallback<PromiseResponse, Object>() {
        @Override
        public Object call(PromiseResponse arg) {
            return null;
        }
    });
```

