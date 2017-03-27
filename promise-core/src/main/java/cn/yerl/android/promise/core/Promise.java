package cn.yerl.android.promise.core;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Android Promise
 * @param <R> 返回值类型
 * @author Alan Yeh
 * @since 16/3/17
 */
public class Promise<R> {
    static final ExecutorService barrier = Executors.newSingleThreadExecutor();
    static final ExecutorService threadPool = Executors.newFixedThreadPool(3);
    public enum State{
        /**
         * 等待状态
         */
        Pending,
        /**
         * 成功状态
         */
        Fulfilled,
        /**
         * 失败状态
         */
        Rejected
    }

    public static Object Void;


    private State state;
    /**
     * Promise 当前状态
     * @return State
     */
    public State getState(){
        return state;
    }

    private R result;

    /**
     * Promise最终结果
     * @return 结果
     */
    public R getResult(){
        return result;
    }

    private RuntimeException error;

    /**
     * Promise执行过程中的错误
     * @return 错误
     */
    public RuntimeException getError(){
        return error;
    }

    /**
     * 判断最后是否执行成功
     * @return 是否执行成功
     */
    public boolean isSuccess(){
        return error == null;
    }

    /**
     * 未执行的Handler
     */
    private List<PromiseResolver> handlers = new ArrayList<>();

    /// 用于返回主线程
    private static Handler handler = new Handler(Looper.getMainLooper());

    /**
     * 创建一个空的Promise
     */
    public Promise(){
        this.state = State.Fulfilled;
    }

    /**
     * 拼接Promise
     * 如果当前Promise还没有执行,则拼接在当前Promise的执行栈中
     * 如果当前Promise已经执行了,则直接将当前Promise的值传给下一个执行者
     * @param resolver 回调
     */
    public void pipe(PromiseResolver<R> resolver){
        if (this.state == State.Pending){
            this.handlers.add(resolver);
        }else {
            resolver.resolve(result, error);
        }
    }

    /**
     * 创建一个Promise,并拼接在Promise(self)的执行链中
     * @param self Promise
     * @param piper 水管
     * @param <R> 返回值类型
     * @return Promise
     */
    private <A, R> Promise<R> __pipe(final Promise<A> self, final Piper<A, R> piper){
        return new Promise<>(new PromiseCallbackWithResolver<A, R>() {
            @Override
            public void call(A arg, final PromiseResolver<R> resolver) {
                self.pipe(new PromiseResolver<A>() {
                    @Override
                    public void resolve(A result, RuntimeException error) {
                        piper.pipe(result, error, resolver);
                    }
                });
            }
        });
    }

    private interface Piper<A, R>{
        void pipe(A arg, RuntimeException error, PromiseResolver<R> resolver);
    }

    /**
     * 创建一个未执行的Promise
     * @param callback PromiseCallbackWithResolver
     * @param <A> 参数类型
     */
    public <A> Promise(final PromiseCallbackWithResolver<A, R> callback){
        this.state = State.Pending;

        final PromiseResolver<R> finalResolver = new PromiseResolver<R>() {
            @Override
            public void resolve(final R result, final RuntimeException error) {
                final List<PromiseResolver> nextPromises = new ArrayList<>();

                //保证执行链的顺序执行
                final CountDownLatch signal = new CountDownLatch(1);
                Promise.barrier.execute(new Runnable() {
                    @Override
                    public void run() {
                        //race
                        if (Promise.this.getState() == State.Pending){
                            nextPromises.addAll(Promise.this.handlers);

                            Promise.this.result = result;
                            Promise.this.error = error;
                            Promise.this.state = error != null ? State.Rejected : State.Fulfilled;
                        }
                        signal.countDown();
                    }
                });

                try { signal.await(); } catch (InterruptedException e) {}

                for (PromiseResolver next : nextPromises){
                    next.resolve(result, error);
                }
            }
        };

        final PromiseResolver<R> resolver = new PromiseResolver<R>() {
            @Override
            public void resolve(R result, RuntimeException error) {
                if (result != null && error != null){
                    throw new IllegalArgumentException("不允许同时返回结果和错误, 这将导至计算无法继续");
                }
                //保证Promise的结果不变性, 如果当前状态不是Pending, 则抛弃结果
                if (Promise.this.state == State.Pending){
                    if (result instanceof Promise){
                        ((Promise<R>)result).pipe(finalResolver);
                    }else {
                        finalResolver.resolve(result, error);
                    }
                }
            }
        };

        //创建Promise之后, 直接开始执行任务
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    callback.call(null, resolver);
                }catch (RuntimeException ex){
                    finalResolver.resolve(null, ex);
                }
            }
        });
    }

    /**
     * 创建延迟执行的Promise,使用Resolver回调
     * @param delayMillis 延迟时间，毫秒
     * @param callback PromiseCallbackWithResolver
     * @param <A> 参数类型
     */
    public <A> Promise(final long delayMillis, final PromiseCallbackWithResolver<A, R> callback){
        this(new PromiseCallbackWithResolver<A, R>() {
            @Override
            public void call(final A arg, final PromiseResolver<R> resolver) {
                Promise.handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            callback.call(arg, resolver);
                        }catch (RuntimeException ex){
                            resolver.resolve(null, ex);
                        }
                    }
                }, delayMillis);
            }
        });
    }

    /**
     * 创建延迟执行的Promise
     * @param delayMillis 延迟时间，毫秒
     * @param callback PromiseCallback
     * @param <A> 参数类型
     */
    public <A> Promise(final long delayMillis, final PromiseCallback<A, R> callback){
        this(new PromiseCallbackWithResolver<Object, R>() {
            @Override
            public void call(final Object arg, final PromiseResolver<R> resolver) {
                Promise.handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            resolver.resolve(callback.call((A)arg), null);
                        }catch (RuntimeException ex){
                            resolver.resolve(null, ex);
                        }
                    }
                }, delayMillis);
            }
        });
    }

    /**
     * 创建延迟执行的Promise
     * @param delayMillis 延迟时间，毫秒
     * @param callback PromiseVoidArgCallback
     * @param <A> 参数类型
     */
    public <A> Promise(final long delayMillis, final PromiseVoidArgCallback<R> callback){
        this(new PromiseCallbackWithResolver<A, R>() {
            @Override
            public void call(Object arg, final PromiseResolver resolver) {
                Promise.handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            resolver.resolve(callback.call(), null);
                        }catch (RuntimeException ex){
                            resolver.resolve(null, ex);
                        }
                    }
                }, delayMillis);
            }
        });
    }

    /**
     * 主线程执行的Promise
     * @param callback PromiseCallback
     * @param <A> 参数类型
     */
    public <A> Promise(final PromiseCallback<A, R> callback){
        this(new PromiseCallbackWithResolver<Object, R>() {
            @Override
            public void call(Object arg, PromiseResolver<R> resolver) {
                try {
                    resolver.resolve(callback.call((A)arg), null);
                }catch (RuntimeException ex){
                    resolver.resolve(null, ex);
                }
            }
        });
    }

    /**
     * 获取一个Rejected状态的Promise
     * @param error 错误
     * @param <N> 返回值类型
     */
    public <N> Promise(final RuntimeException error){
        this(new PromiseCallbackWithResolver<Object, R>() {
            @Override
            public void call(Object arg, PromiseResolver<R> resolver) {
                resolver.resolve(null, error);
            }
        });
    }


    /**
     * 直接返回promise
     * @param promise Promise
     * @param <R> 返回值类型
     * @return Promise
     */
    public static <R> Promise<R> resolve(Promise<R> promise){
        return promise;
    }

    /**
     * 返回一个Fulfilled状态的Promise
     * @param result 结果
     * @param <R> 返回值类型
     * @return Promise
     */
    public static <R> Promise<R> resolve(final R result){
        return new Promise<>(new PromiseCallbackWithResolver<Object, R>(){
            @Override
            public void call(Object arg, PromiseResolver<R> resolver) {
                resolve(result);
            }
        });
    }

    /**
     * 返回一个Rejected状态的Promise
     * @param ex 错误
     * @param <A> 参数类型
     * @param <R> 返回值类型
     * @return Promise
     */
    public static <A, R> Promise<R> resolve(final RuntimeException ex){
        return new Promise<>(new PromiseCallbackWithResolver<A, R>(){
            @Override
            public void call(A arg, PromiseResolver<R> resolver) {
                resolver.resolve(null, ex);
            }
        });
    }

    /**
     * 包装一系列的Promise对象,返回一个包装后的Promise对象,称之为A
     * 1. 当所有的Promise对象都变成成功态(Fulfilled)后,这个包装后的A才会把自己变成成功状态.
     *    A会等最慢的那个Promise对象变成成功态(Fulfilled)后才把自己变成成功态.
     * 2. 只要其中一个Promise对象变成失败态(Rejected),包装后的A就会变成Rejected,并且每一个Rejected传递的值,
     *    会传递给A后面的catch
     * @param promises List of promise
     * @param <A> 参数类型
     * @param <R> 返回值类型
     * @return Promise
     */

    public static <A, R> Promise<List<R>> all(final List<Promise<R>> promises){
        return new Promise<>(new PromiseCallbackWithResolver<A, List<R>>() {
            @Override
            public void call(A arg, final PromiseResolver<List<R>> resolver) {
                final AtomicInteger totalCount = new AtomicInteger(promises.size());
                for (Promise<R> promise : promises){
                    promise.pipe(new PromiseResolver<R>() {
                        @Override
                        public void resolve(R result, RuntimeException error) {
                            if (error != null){
                                resolver.resolve(null, new RuntimeException("one of promise in promises was rejected", error));
                            }else if (totalCount.decrementAndGet() == 0){
                                List<R> results = new ArrayList<>(promises.size());
                                for (Promise<R> promise : promises){
                                    results.add(promise.result);
                                }
                                resolver.resolve(results, null);
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 包装一列列的Promise对象,返回一个包装后的Promise对象,称之为R
     * 1. 只要其中的一个Promise对象变成成功态(Fulfilled)后,这个包装后的R就会变成成功态(Fulfilled).
     * 2. 当所有的promise对象都变成失败态(Rejected)后,这个包装后的R才会变成失败态.
     * @param promises List of promise
     * @param <A> 参数类型
     * @param <R> 返回值类型
     * @return Promise
     */
    public static <A, R> Promise<R> race(final List<Promise<R>> promises) {
        return new Promise<>(new PromiseCallbackWithResolver<A, R>() {
            @Override
            public void call(A arg, final PromiseResolver<R> resolver) {
                final AtomicInteger totalCount = new AtomicInteger(promises.size());
                for (Promise<R> promise : promises) {
                    promise.pipe(new PromiseResolver<R>() {
                        @Override
                        public void resolve(R result, RuntimeException ex) {
                            if (ex == null) {
                                resolver.resolve(result, null);
                            } else if (totalCount.decrementAndGet() == 0) {
//                                List<RuntimeException> errors = new ArrayList<>();
//                                for (Promise<R> promise : promises){
//                                    errors.add(promise.getError());
//                                }
                                resolver.resolve(null, new RuntimeException("all promise were rejected."));
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 主线程执行
     * @param then next step
     * @param <N> 新的返回值类型
     * @return Promise
     */
    public <N> Promise<N> then(final PromiseCallback<R, N> then){
        return __pipe(this, new Piper<R, N>() {
            @Override
            public void pipe(final R arg, RuntimeException error, final PromiseResolver<N> resolver) {
                if (error != null){
                    resolver.resolve(null, error);
                }else {
                    Promise.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(then.call(arg), null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 主线程执行
     * @param then next step
     */
    public void then(final PromiseVoidReturnCallback<R> then){
        __pipe(this, new Piper<R, Object>() {
            @Override
            public void pipe(final R arg, RuntimeException error, final PromiseResolver<Object> resolver) {
                if (error != null){
                    resolver.resolve(null, error);
                }else {
                    Promise.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                then.call(arg);
                                resolver.resolve(null, null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 主线程执行
     * @param then next step
     */
    public void then(final PromiseVoidArgVoidReturnCallback then){
        __pipe(this, new Piper<R, Object>() {
            @Override
            public void pipe(final R arg, RuntimeException error, final PromiseResolver<Object> resolver) {
                if (error != null){
                    resolver.resolve(null, error);
                }else {
                    Promise.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                then.call();
                                resolver.resolve(null, null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 主线程执行
     * @param then next step
     * @param <N> 返回值类型
     * @return Promise
     */
    public <N> Promise<N> then(final PromiseVoidArgCallback<N> then){
        return __pipe(this, new Piper<R, N>() {
            @Override
            public void pipe(final R arg, RuntimeException error, final PromiseResolver<N> resolver) {
                if (error != null){
                    resolver.resolve(null, error);
                }else {
                    Promise.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(then.call(), null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    });
                }
            }
        });
    }


    /**
     * 主线程执行,使用 Resolver来回调
     * @param then next step
     * @param <N> 返回值类型
     * @return Promise
     */
    public <N> Promise<N> then(final PromiseCallbackWithResolver<R, N> then){
        return __pipe(this, new Piper<R, N>() {
            @Override
            public void pipe(final R arg, RuntimeException error, final PromiseResolver<N> resolver) {
                if (error != null){
                    resolver.resolve(null, error);
                }else {
                    Promise.this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                then.call(arg, resolver);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 异步执行
     * @param then next step
     * @param <N> 返回值类型
     * @return Promise
     */
    public <N> Promise<N> thenAsync(final PromiseCallback<R, N> then){
        return __pipe(this, new Piper<R, N>() {
            @Override
            public void pipe(final R arg, RuntimeException error, final PromiseResolver<N> resolver) {
                if (error != null){
                    resolver.resolve(null, error);
                }else {
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(then.call(arg), null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 异步执行
     * @param then next step
     * @param <N> 返回值类型
     * @return Promise
     */
    public <N> Promise<N> thenAsync(final PromiseVoidArgCallback<N> then){
        return __pipe(this, new Piper<R, N>() {
            @Override
            public void pipe(final R arg, RuntimeException error, final PromiseResolver<N> resolver) {
                if (error != null){
                    resolver.resolve(null, error);
                }else {
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(then.call(), null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 异步执行
     * @param then next step
     */
    public void thenAsync(final PromiseVoidReturnCallback<R> then){
        __pipe(this, new Piper<R, Object>() {
            @Override
            public void pipe(final R arg, RuntimeException error, final PromiseResolver<Object> resolver) {
                if (error != null){
                    resolver.resolve(null, error);
                }else {
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                then.call(arg);
                                resolver.resolve(null, null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 异步执行
     * @param then next step
     */
    public void thenAsync(final PromiseVoidArgVoidReturnCallback then){
        __pipe(this, new Piper<R, Object>() {
            @Override
            public void pipe(final R arg, RuntimeException error, final PromiseResolver<Object> resolver) {
                if (error != null){
                    resolver.resolve(null, error);
                }else {
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                then.call();
                                resolver.resolve(null, null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 异步执行,使用Resolver来回调
     * @param then next step
     * @param <N> 返回值类型
     * @return Promise
     */
    public <N> Promise<N> thenAsync(final PromiseCallbackWithResolver<R, N> then){
        return __pipe(this, new Piper<R, N>() {
            @Override
            public void pipe(final R arg, RuntimeException error, final PromiseResolver<N> resolver) {
                if (error != null){
                    resolver.resolve(null, error);
                }else {
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                then.call(arg, resolver);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 延迟执行
     * @param delayMillis 延迟时间，毫秒
     * @param then 下一步
     * @param <N> 返回值类型
     * @return Promise
     */
    public <N> Promise<N> thenDelay(final long delayMillis, final PromiseCallback<R, N> then){
        return __pipe(this, new Piper<R, N>() {
            @Override
            public void pipe(final R arg, RuntimeException error, final PromiseResolver<N> resolver) {
                if (error != null){
                    resolver.resolve(null, error);
                }else {
                    Promise.this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(then.call(arg), null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    }, delayMillis);
                }
            }
        });
    }

    /**
     * 延迟执行
     * @param delayMillis 延迟时间，毫秒
     * @param then 下一步
     * @param <N> 返回值类型
     * @return Promise
     */
    public <N> Promise<N> thenDelay(final long delayMillis, final PromiseVoidArgCallback<N> then){
        return __pipe(this, new Piper<R, N>() {
            @Override
            public void pipe(final R arg, RuntimeException error, final PromiseResolver<N> resolver) {
                if (error != null){
                    resolver.resolve(null, error);
                }else {
                    Promise.this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(then.call(), null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    }, delayMillis);
                }
            }
        });
    }

    /**
     * 延迟执行
     * @param delayMillis 延迟时间，毫秒
     * @param then 下一步
     */
    public void thenDelay(final long delayMillis, final PromiseVoidReturnCallback<R> then){
        __pipe(this, new Piper<R, Object>() {
            @Override
            public void pipe(final R arg, RuntimeException error, final PromiseResolver<Object> resolver) {
                if (error != null){
                    resolver.resolve(null, error);
                }else {
                    Promise.this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                then.call(arg);
                                resolver.resolve(null, null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    }, delayMillis);
                }
            }
        });
    }

    /**
     * 延迟执行
     * @param delayMillis 延迟时间，毫秒
     * @param then 下一步
     */
    public void thenDelay(final long delayMillis, final PromiseVoidArgVoidReturnCallback then){
        __pipe(this, new Piper<R, Object>() {
            @Override
            public void pipe(final R arg, RuntimeException error, final PromiseResolver<Object> resolver) {
                if (error != null){
                    resolver.resolve(null, error);
                }else {
                    Promise.this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                then.call();
                                resolver.resolve(null, null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    }, delayMillis);
                }
            }
        });
    }

    /**
     * 延迟执行,使用Resolver回调
     * @param delayMillis delay time
     * @param then next step
     * @param <N> 返回值类型
     * @return Promise
     */
    public <N> Promise<N> thenDelay(final long delayMillis, final PromiseCallbackWithResolver<R, N> then) {
        return __pipe(this, new Piper<R, N>() {
            @Override
            public void pipe(final R arg, RuntimeException error, final PromiseResolver<N> resolver) {
                if (error != null){
                    resolver.resolve(null, error);
                }else {
                    Promise.this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                then.call(arg, resolver);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    }, delayMillis);
                }
            }
        });
	}

    /**
     * 同步处理错误
     * @param callback error handler
     * @return Promise
     */
    public Promise<R> error(final PromiseCallback<RuntimeException, R> callback){
        return __pipe(this, new Piper<R, R>() {
            @Override
            public void pipe(final R arg, final RuntimeException error, final PromiseResolver<R> resolver) {
                if (error != null){
                    Promise.this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(callback.call(error), null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    });
                }else {
                    resolver.resolve(arg, null);
                }
            }
        });
    }

    /**
     * 同步处理错误
     * @param callback error handler
     * @return Promise
     */
    public Promise<R> error(final PromiseVoidArgCallback<R> callback){
        return __pipe(this, new Piper<R, R>() {
            @Override
            public void pipe(final R arg, final RuntimeException error, final PromiseResolver<R> resolver) {
                if (error != null){
                    Promise.this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(callback.call(), null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    });
                }else {
                    resolver.resolve(arg, null);
                }
            }
        });
    }

    /**
     * 同步处理错误
     * @param callback error handler
     */
    public void error(final PromiseVoidReturnCallback<RuntimeException> callback){
        __pipe(this, new Piper<R, Object>() {
            @Override
            public void pipe(final R arg, final RuntimeException error, final PromiseResolver<Object> resolver) {
                if (error != null){
                    Promise.this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                callback.call(error);
                                resolver.resolve(null, null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    });
                }else {
                    resolver.resolve(arg, null);
                }
            }
        });
    }

    /**
     * 同步处理错误
     * @param callback error handler
     */
    public void error(final PromiseVoidArgVoidReturnCallback callback){
        __pipe(this, new Piper<R, Object>() {
            @Override
            public void pipe(final R arg, final RuntimeException error, final PromiseResolver<Object> resolver) {
                if (error != null){
                    Promise.this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                callback.call();
                                resolver.resolve(null, null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    });
                }else {
                    resolver.resolve(arg, null);
                }
            }
        });
    }

    /**
     * 异步处理错误
     * @param callback error handler
     * @return Promise
     */
    public Promise<R> errorAsync(final PromiseCallback<RuntimeException, R> callback){
        return __pipe(this, new Piper<R, R>() {
            @Override
            public void pipe(final R arg, final RuntimeException error, final PromiseResolver<R> resolver) {
                if (error != null){
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(callback.call(error), null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    });
                }else {
                    resolver.resolve(arg, null);
                }
            }
        });
    }

    /**
     * 异步处理错误
     * @param callback error handler
     * @return Promise
     */
    public Promise<R> errorAsync(final PromiseVoidArgCallback<R> callback){
        return __pipe(this, new Piper<R, R>() {
            @Override
            public void pipe(final R arg, final RuntimeException error, final PromiseResolver<R> resolver) {
                if (error != null){
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(callback.call(), null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    });
                }else {
                    resolver.resolve(arg, null);
                }
            }
        });
    }

    /**
     * 异步处理错误
     * @param callback error handler
     */
    public void errorAsync(final PromiseVoidReturnCallback<RuntimeException> callback){
        __pipe(this, new Piper<R, Object>() {
            @Override
            public void pipe(R arg, final RuntimeException error, final PromiseResolver<Object> resolver) {
                if (error != null){
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                callback.call(error);
                                resolver.resolve(null, null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    });
                }else {
                    resolver.resolve(arg, null);
                }
            }
        });
    }

    /**
     * 异步处理错误
     * @param callback error handler
     */
    public void errorAsync(final PromiseVoidArgVoidReturnCallback callback){
        __pipe(this, new Piper<R, Object>() {
            @Override
            public void pipe(R arg, final RuntimeException error, final PromiseResolver<Object> resolver) {
                if (error != null){
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                callback.call();
                                resolver.resolve(null, null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(null, ex);
                            }
                        }
                    });
                }else {
                    resolver.resolve(arg, null);
                }
            }
        });
    }

    /**
     * 主线程执行,正确或失败都会执行
     * @param always handle always
     * @param <N> 返回值类型
     * @return Promise
     */
    public <N> Promise<N> always(final PromiseCallback<Object, N> always){
        return __pipe(this, new Piper<R, N>() {
            @Override
            public void pipe(final R arg, final RuntimeException error, final PromiseResolver<N> resolver) {
                Promise.this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            resolver.resolve(always.call(error != null ? error : arg), null);
                        }catch (RuntimeException ex){
                            resolver.resolve(null, ex);
                        }
                    }
                });
            }
        });
    }

    /**
     * 主线程执行,正确或失败都会执行
     * @param always handle always
     * @param <N> 返回值类型
     * @return Promise
     */
    public <N> Promise<N> always(final PromiseVoidArgCallback<N> always){
        return __pipe(this, new Piper<R, N>() {
            @Override
            public void pipe(final R arg, final RuntimeException error, final PromiseResolver<N> resolver) {
                Promise.this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            resolver.resolve(always.call(), null);
                        }catch (RuntimeException ex){
                            resolver.resolve(null, ex);
                        }
                    }
                });
            }
        });
    }

    /**
     * 主线程执行,正确或失败都会执行
     * @param always handle always
     */
    public void always(final PromiseVoidReturnCallback<Object> always){
        __pipe(this, new Piper<R, Object>() {
            @Override
            public void pipe(final R arg, final RuntimeException error, final PromiseResolver<Object> resolver) {
                Promise.this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            always.call(error != null ? error : arg);
                            resolver.resolve(null, null);
                        }catch (RuntimeException ex){
                            resolver.resolve(null, ex);
                        }
                    }
                });
            }
        });
    }

    /**
     * 主线程执行,正确或失败都会执行
     * @param always handle always
     */
    public void always(final PromiseVoidArgVoidReturnCallback always){
        __pipe(this, new Piper<R, Object>() {
            @Override
            public void pipe(R arg, RuntimeException error, final PromiseResolver<Object> resolver) {
                Promise.this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            always.call();
                            resolver.resolve(null, null);
                        }catch (RuntimeException ex){
                            resolver.resolve(null, ex);
                        }
                    }
                });
            }
        });
    }

    /**
     * 异步执行,正确或失败都会执行
     * @param always handle always
     * @param <N> 返回值类型
     * @return Promise
     */
    public <N> Promise<N> alwaysAsync(final PromiseCallback<Object, N> always){
        return __pipe(this, new Piper<R, N>() {
            @Override
            public void pipe(final R arg, final RuntimeException error, final PromiseResolver<N> resolver) {
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            resolver.resolve(always.call(error != null ? error : arg), null);
                        }catch (RuntimeException ex){
                            resolver.resolve(null, ex);
                        }
                    }
                });
            }
        });
    }

    /**
     * 异步执行,正确或失败都会执行
     * @param always handle always
     * @param <N> 返回值类型
     * @return Promise
     */
    public <N> Promise<N> alwaysAsync(final PromiseVoidArgCallback<N> always){
        return __pipe(this, new Piper<R, N>() {
            @Override
            public void pipe(final R arg, final RuntimeException error, final PromiseResolver<N> resolver) {
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            resolver.resolve(always.call(), null);
                        }catch (RuntimeException ex){
                            resolver.resolve(null, ex);
                        }
                    }
                });
            }
        });
    }

    /**
     * 异步执行,正确或失败都会执行
     * @param always handle always
     */
    public void alwaysAsync(final PromiseVoidReturnCallback<Object> always){
        __pipe(this, new Piper<R, Object>() {
            @Override
            public void pipe(final R arg, final RuntimeException error, final PromiseResolver<Object> resolver) {
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            always.call(error != null ? error : arg);
                            resolver.resolve(null, null);
                        }catch (RuntimeException ex){
                            resolver.resolve(null, ex);
                        }
                    }
                });
            }
        });
    }

    /**
     * 异步执行,正确或失败都会执行
     * @param always handle always
     */
    public void alwaysAsync(final PromiseVoidArgVoidReturnCallback always){
        __pipe(this, new Piper<R, Object>() {
            @Override
            public void pipe(final R arg, final RuntimeException error, final PromiseResolver<Object> resolver) {
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            always.call();
                            resolver.resolve(null, null);
                        }catch (RuntimeException ex){
                            resolver.resolve(null, ex);
                        }
                    }
                });
            }
        });
    }
}
