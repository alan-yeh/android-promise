# Promise
## 引用
### Gradle

```
repositories {
  mavenCentral()
}

dependencies {
  compile 'cn.yerl:android-promise:1.0.1'
}
```
### Maven
```xml
  <dependencies>
    <dependency>
      <groupId>cn.yerl</groupId>
      <artifactId>android-promise</artifactId>
      <version>1.0.1</version>
    </dependency>
  </dependencies>
```


## 目录

[**简介**](#简介)

[**使用Promise**](#使用promise)

[**Promise状态**](#promise状态)

[**Promise Api**](#promise-api)

- [PromiseCallback](#promisecallback)

- [PromiseCallbackWithResolver](#promisecallbackwithresolver)

- [构造函数](#构造函数)

- [静态方法](#静态方法)

- [then](#then)

- [error](#error)

- [always](#always)

## <a id="简介"></a>简介

　　Promise是CommonJS中的Promise/A规范的安卓实现。考虑到需要方便在实际中使用，修改了部份接口，导至与Promise/A规范有略微不同，但使用起来会更加简单。

　　Promise，承诺，在开发中的意思是，我承诺我去做一些事情，但不是现在去做，而是在将来满足一些条件之后才执行。Promise刚开始出现在前端开发领域中，主要用来解决JS开发中的异步问题。在使用Promise之前，异步的处理使用最多的就是回调这种形式。比如：

```javascript
doSomethingAsync(function(result, error){
    if (error){
        ...//处理error
    } else {
        ...//处理result
    }
})
```

　　在Android中，这类代码也是非常常见的。例如AsyncHttpClient中，访问网络就是使用回调。

```java
    RequestParams params = new RequestParams();
    params.put("param1", "123");
    params.put("param2", "234");
    new AsyncHttpClient().post("xxx", params, new TextHttpResponseHandler() {
        @Override
        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
            //处理错误逻辑
        }

        @Override
        public void onSuccess(int statusCode, Header[] headers, String responseString) {
            //处理正确逻辑
        }
    });
```

　　这种书写方式，可以很容易解决对异步操作的问题。但是这种写法，很容易引起回调金字塔的情况。Promise则对异步处理和处理方法都做了规范和抽象，还给了开发者在异步代码中使用return和throw的能力。这也是Promise存在的真正意义。
　　
## <a id="使用promise"></a>使用Promise
　　来看一个常见的业务场景，获取联系人需要先访问一次服务器（登录或者一些必要的操作），然后再访问一次服务器才能真正获取到有效数据，然后再进行一系列的错误处理，代码冗余复杂。

```java
    public static class Contact{
        //Contact实体属性
    }

    public interface ServiceHandler<T>{
        void onSuccess(T result);
        void onFailure(String error);
    }

    public void getContact(final ServiceHandler<List<Contact>> handler){
        RequestParams params = new RequestParams();
        params.put("username", "username");
        params.put("password", "password");
        new AsyncHttpClient().post("xxxx/login", params, new TextHttpResponseHandler() {
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                handler.onFailure(responseString);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                JSONObject result = null;
                try {
                    result = new JSONObject(responseString);
                }catch (JSONException e){
                    handler.onFailure("Json解析错误");
                }

                if (result.optInt("status") == 200){
                    //登录成功
                    RequestParams params = new RequestParams();
                    params.put("pageIndex", 1);
                    params.put("pageSize", 20);

                    new AsyncHttpClient().get("xxxx/contacts", params, new TextHttpResponseHandler() {
                        @Override
                        public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                            handler.onFailure(responseString);
                        }

                        @Override
                        public void onSuccess(int statusCode, Header[] headers, String responseString) {
                            JSONObject result = null;
                            try {
                                result = new JSONObject(responseString);
                            }catch (JSONException e){
                                handler.onFailure("Json解析错误");
                            }

                            if (result.optInt("status") == 200){
                                List<Contact> contacts = new ArrayList<Contact>();
                                //处理业务
                                //组装实体
                                //....
                                //....
                                handler.onSuccess(contacts);

                            }else {
                                handler.onFailure("数据获取失败");
                            }
                        }
                    });
                }else {
                    handler.onFailure("登录失败");
                }
            }
        });
    }
```
　　使用Promise来改造一下上面的业务。

```java
    //网络访问封装
    private Promise<String> get(final String url, final RequestParams params){
        return new Promise<String>(new PromiseCallbackWithResolver<Object, String>() {
            @Override
            public void call(Object arg, final PromiseResolver resolver) {
                new AsyncHttpClient().get(url, params, new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        resolver.resolve(new RuntimeException("网络访问错误", throwable));
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String responseString) {
                        resolver.resolve(responseString);
                    }
                });
            }
        });
    }

    //Json解析与业务分离
    private Promise<JSONObject> parseJson(final String responseString){
        return new Promise<JSONObject>(new PromiseCallback<Object, JSONObject>() {
            @Override
            public Object call(Object arg) {
                try {
                    return new JSONObject(responseString);
                }catch (JSONException e){
                    return new RuntimeException("Json解析出错", e);
                }
            }
        });
    }

    public Promise<Contact> getContact(){
        RequestParams params = new RequestParams();
        params.put("username", "username");
        params.put("password", "password");
        //登录
        return get("xxxx/login", params).then(new PromiseCallback<String, JSONObject>() {
            @Override
            public Object call(String arg) {
                //转换Json
                return parseJson(arg);
            }
        }).then(new PromiseCallback<JSONObject, String>() {
            @Override
            public Object call(JSONObject arg) {
                if (arg.optInt("status") == 200){
                    //登录成功
                    RequestParams params = new RequestParams();
                    params.put("pageIndex", 1);
                    params.put("pageSize", 20);
                    return get("xxxx/contacts", params);
                }else {
                    return new RuntimeException("登录失败");
                }
            }
        }).then(new PromiseCallback<String, JSONObject>() {
            @Override
            public Object call(String arg) {
                //转换Json
                return parseJson(arg);
            }
        }).then(new PromiseCallback<JSONObject, Contact>() {
            @Override
            public Object call(JSONObject arg) {
                if (arg.optInt("status") == 200){
                    List<Contact> contacts = new ArrayList<Contact>();
                    //处理业务
                    //组装实体
                    //....
                    //....
                    return contacts;
                }else {
                    return new RuntimeException("数据获取失败");
                }
            }
        });
    }
```
　　可以看到，使用Promise之后，网络访问、Json转换等做了一次封装，代码变得非常简洁，并且不会再发生层层嵌套的情况了，逻辑由原来的不断嵌套、跳转，变成现在的从上往下顺序执行，逻辑清晰了许多。
## <a id="promise状态"></a>Promise状态
　　每个Promise都只会被成功或失败一次，并且这个状态不会被改变。

　　一个Promise必须处理以下几个状态之一：

- Pending: 操作正在执行中（或等待执行），可以转换到Fulfilled或Rejected状态。
- Fulfilled: 操作执行成功，且状态不可改变。
- Rejected: 操作执行失败，且状态不可改变。

　　有些介绍Promise的文章或实现会出现每4种状态Settled，代码操作已结束，可以认为Settled = Fulfilled & Rejected。它本身并不是一种状态，因为非Pending就是Settled，只是为了说的方便而引入Settled这个说法。

　　Promise处于Pending状态时，其value一定为null；处理Fulfiled状态时，其value为处理结果，可能为null；处理Rejected状态时，其value一定为RuntimeException对象，用于描述Promise被拒绝的原因。



## <a id="promise-api"></a>Promise Api
　　Promise支持标准的CommonJS Promise/A语法。由于语言的特殊性，对其中部分Api进行小量改造。

### <a id="promisecallback"></a>`PromiseCallback`
　　`PromiseCallback`是Promise主要核心Api之一，用于保存业务逻辑的代码。`PromiseCallback`接受一个返回值，它的方法签名如下：

```java
public interface PromiseCallback<T, R> {
    Object call(T arg);
}
```
　　使用方法如以下代码所示：

```java
({xxxxx}).then(new PromiseCallback<JSONObject, String>() {
            @Override
            public Object call(JSONObject arg) {
                if (arg.optInt("status") == 200){
                    //登录成功
                    RequestParams params = new RequestParams();
                    params.put("pageIndex", 1);
                    params.put("pageSize", 20);
                    return get("xxxx/contacts", params);
                }else {
                    return new RuntimeException("登录失败");
                }
            }
        })
```
　　`PromiseCallback`接受以下返回值，不同返回值会导致Promise的不同行为：

- 返回`RuntimeException`对象：代表将当前Promise的状态变更为Rejected状态，当前Promise之后的then不执行，直至error
- 抛出`RuntimeException`对象：Promise将捕捉此对象，并将当前Promise的状态变更为Rejected状态，当前Promise之后的then不执行，直至error
- 返回Promise对象，则将当前Promise对象链之后的Promise插管至新Promise对象执行链中。
- 返回其它对象（包括null），当前Promise状态变更成Fulfiled。

### <a id="promisecallbackwithresolver"></a>`PromiseCallbackWithResolver`
　　`PromiseCallbackWithResolver`的作用与`PromiseCallback`的作用一致，用于保存未执行的代码块。但与`PromiseCallback`直接返回结果不同的是，`PromiseCallbackWithResolver`是不能直接返回结果的，而是要通过它的参数`PromiseResolver`返回结果，它的方法签名如下：

```java
public interface PromiseCallbackWithResolver<T, R> {
    void call(T arg, PromiseResolver resolver);
}
```
　　而`PromiseResolver`的方法签名如下：

```java
public interface PromiseResolver {
    void resolve(Object result);
}
```
　　`PromiseCallbackWithResolver`用于包装一些不能立即返回结果的代码块。例：

```java
    //网络访问封装
    private Promise<String> get(final String url, final RequestParams params){
        return new Promise<String>(new PromiseCallbackWithResolver<Object, String>() {
            @Override
            public void call(Object arg, final PromiseResolver resolver) {
                new AsyncHttpClient().get(url, params, new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        resolver.resolve(new RuntimeException("网络访问错误", throwable));
                    }

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, String responseString) {
                        resolver.resolve(responseString);
                    }
                });
            }
        });
    }
```
### <a id="构造函数"></a>构造函数
```java
//创建一个空的Promise
public Promise(){...}
//主线程执行的Promise
public <T> Promise(final PromiseCallback<T, R> callback){...}
//创建一个未执行的Promise,使用Resolver回调
public <T> Promise(final PromiseCallbackWithResolver<T, R> resolver){...}
//创建延迟执行的Promise
public <T> Promise(final long delayMillis, final PromiseCallback<T, R> callback){...}
//创建延迟执行的Promise,使用Resolver回调
public <T> Promise(final long delayMillis, final PromiseCallbackWithResolver<T, R> resolver){...}
//获取一个Rejected状态的Promise
public <T> Promise(final RuntimeException error){...}
```

### <a id="静态方法"></a>静态方法
```java
//直接返回promise
public static <V> Promise<V> resolve(Promise<V> promise){...}
//返回一个Fulfilled状态的Promise
public static <T, V> Promise<V> resolve(final T result){...}
/**
 * 包装一系列的Promise对象,返回一个包装后的Promise对象,称之为A
 * 1. 当所有的Promise对象都变成成功态(Fulfilled)后,这个包装后的A才会把自己变成成功状态.
 *    A会等最慢的那个Promise对象变成成功态(Fulfilled)后才把自己变成成功态.
 * 2. 只要其中一个Promise对象变成失败态(Rejected),包装后的A就会变成Rejected,并且每一个Rejected传递的值,
 *    会传递给A后面的catch
 */
public static <T, V> Promise< List<V>> all(final List<Promise<V>> promises){...}
 /**
 * 包装一列列的Promise对象,返回一个包装后的Promise对象,称之为R
 * 1. 只要其中的一个Promise对象变成成功态(Fulfilled)后,这个包装后的R就会变成成功态(Fulfilled).
 * 2. 当所有的promise对象都变成失败态(Rejected)后,这个包装后的R才会变成失败态.
 */
public static <T, V> Promise<V> race(final List<Promise<V>> promises){...}
```
### <a id="then"></a>then
　　`then`方法用于处理正确的逻辑。只有当then的上一个promise的状态为fulfilled，then方法才会被执行，因此，在then方法里面，我们仅关注上一步的正确结果。

```java
//主线程执行
public <V> Promise<V> then(final PromiseCallback<R, V> then){...}
//主线程执行,使用 Resolver来回调
public <V> Promise<V> then(final PromiseCallbackWithResolver<R, V> then){...}
//异步执行
public <V> Promise<V> thenAsync(final PromiseCallback<R, V> then){...}
//异步执行,使用Resolver来回调
public <V> Promise<V> thenAsync(final PromiseCallbackWithResolver<R, V> then){...}
//延时执行,在主线程执行
public <V> Promise<V> thenDelay(final long delayMillis, final PromiseCallback<R, V> then){...}
//延迟执行,在主线程执行,使用Resolver回调
public <V> Promise<V> thenDelay(final long delayMillis, final PromiseCallbackWithResolver<R, V> then) {...}
```
### <a id="error"></a>error
　　`error`方法用于处理错误的逻辑。当error之前的promise的状态为rejected时，error才会被执行。因此，可以在error方法里面，统一处理之前的promise的错误结果。

```java
//在主线程中处理错误
public <V> Promise<V> error(final PromiseCallback<RuntimeException, V> error){...}
//异步处理错误
public <V> Promise<V> errorAsync(final PromiseCallback<RuntimeException, V> error){...}
```
### <a id="always"></a>always
　　`always`方法无论之前的promise状态是rejected还是fulfilled，都会被执行。因此可以在always方法里面，去执行一些正确与错误都需要执行的逻辑，比如将Loading状态栏移除之类的。

```java
//主线程执行,正确或失败都会执行
public <V> Promise<V> always(final PromiseCallback<R, V> always){...}
//异步执行,正确或失败都会执行
public <V> Promise<V> alwaysAsync(final PromiseCallback<R, V> always){...}
```