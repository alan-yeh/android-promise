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


    private State state;
    /**
     * Promise 当前状态
     * @return State
     */
    public State getState(){
        return state;
    }

    private Object value;
    /**
     * Promise最终结果
     * @return Object
     */
    public Object getValue(){
        return value;
    }

    /**
     * 未执行的Handler
     */
    private List<PromiseResolver> handlers = new ArrayList<>();

    private static Handler handler = new Handler(Looper.getMainLooper());

    /**
     * 创建一个空的Promise
     */
    public Promise(){
        this.state = State.Fulfilled;
    }

    /**
     * 创建一个未执行的Promise
     * @param resolver Promise Resolver
     * @param <T> 返回值类型
     */
    public <T> Promise(final PromiseCallbackWithResolver<T, R> resolver){
        this.state = State.Pending;
        final PromiseResolver __presolver = new PromiseResolver() {
            @Override
            public void resolve(final Object result) {
                final List<PromiseResolver> callbacks = new ArrayList<>();

                //保证执行链的顺序执行
                final CountDownLatch signal = new CountDownLatch(1);
                Promise.barrier.execute(new Runnable() {
                    @Override
                    public void run() {
                        //race
                        if (Promise.this.getState() == State.Pending){
                            callbacks.addAll(Promise.this.handlers);

                            Promise.this.value = result;
                            Promise.this.state = (result instanceof RuntimeException) ? State.Rejected : State.Fulfilled;
                        }
                        signal.countDown();
                    }
                });

                try { signal.await(); } catch (InterruptedException e) {}

                for (PromiseResolver callback : callbacks){
                    callback.resolve(result);
                }
            }
        };

        final PromiseResolver __resolver = new PromiseResolver() {
            @Override
            public void resolve(Object result) {
                //保证Promise的结果不变性
                if (Promise.this.state == State.Pending){
                    if (result instanceof Promise){
                        ((Promise<?>)result).pipe(__presolver);
                    }else {
                        __presolver.resolve(result);
                    }
                }
            }
        };

        //创建Promise之后,直接开始执行任务
        this.handler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    resolver.call(null, __resolver);
                }catch (RuntimeException ex){
                    __presolver.resolve(ex);
                }
            }
        });
    }

    /**
     * 创建延迟执行的Promise,使用Resolver回调
     * @param delayMillis 延迟时间，毫秒
     * @param resolver PromiseCallbackWithResolver
     * @param <T> 返回值类型
     */
    public <T> Promise(final long delayMillis, final PromiseCallbackWithResolver<T, R> resolver){
        this(new PromiseCallbackWithResolver<T, R>() {
            @Override
            public void call(final T arg, final PromiseResolver solver) {
                Promise.handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        resolver.call(arg, solver);
                    }
                }, delayMillis);
            }
        });
    }

    /**
     * 创建延迟执行的Promise
     * @param delayMillis 延迟时间，毫秒
     * @param callback PromiseCallback
     * @param <T> 返回值类型
     */
    public <T> Promise(final long delayMillis, final PromiseCallback<T, R> callback){
        this(new PromiseCallbackWithResolver<T, R>() {
            @Override
            public void call(final T arg, final PromiseResolver resolver) {
                Promise.handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        resolver.resolve(callback.call(arg));
                    }
                }, delayMillis);
            }
        });
    }

    /**
     * 主线程执行的Promise
     * @param callback PromiseCallback
     * @param <T> 返回值类型
     */
    public <T> Promise(final PromiseCallback<T, R> callback){
        this(new PromiseCallbackWithResolver<T, R>() {
            @Override
            public void call(T arg, PromiseResolver resolver) {
                resolver.resolve(callback.call(arg));
            }
        });
    }

    /**
     * 获取一个Rejected状态的Promise
     * @param error 错误
     * @param <T> 返回值类型
     */
    public <T> Promise(final RuntimeException error){
        this(new PromiseCallbackWithResolver<T, R>() {
            @Override
            public void call(T arg, PromiseResolver resolver) {
                resolver.resolve(error);
            }
        });
    }

    /**
     * 拼接Promise
     * 如果当前Promise还没有执行,则拼接在当前Promise的执行栈中
     * 如果当前Promise已经执行了,则直接将当前Promise的值传给下一个执行者
     */
    private void pipe(PromiseResolver resolver){
        if (this.state == State.Pending){
            this.handlers.add(resolver);
        }else {
            resolver.resolve(this.value);
        }
    }

    /**
     * 创建一个Promise,并拼接在Promise(self)的执行链中
     * @param self Promise
     * @param then 下一步
     * @param <T> 参数类型
     * @param <V> 返回值类型
     * @return Promise
     */
    private <T, V> Promise<V> __pipe(final Promise<R> self, final PromiseCallbackWithResolver<R, V> then){
        return new Promise<>(new PromiseCallbackWithResolver<T, V>() {
            @Override
            public void call(T arg, final PromiseResolver resolver) {
                self.pipe(new PromiseResolver() {
                    @SuppressWarnings("unchecked")
					@Override
                    public void resolve(Object result) {
                        then.call((R)result, resolver);
                    }
                });
            }
        });
    }

    /**
     * 直接返回promise
     * @param promise Promise
     * @param <V> 返回值类型
     * @return Promise
     */
    public static <V> Promise<V> resolve(Promise<V> promise){
        return promise;
    }

    /**
     * 返回一个Fulfilled状态的Promise
     * @param result 结果
     * @param <T> 参数类型
     * @param <V> 返回值类型
     * @return Promise
     */
    public static <T, V> Promise<V> resolve(final T result){
        return new Promise<>(new PromiseCallbackWithResolver<T, V>(){
            @Override
            public void call(T arg, PromiseResolver resolver) {
                resolve(result);
            }
        });
    }

    /**
     * 返回一个Rejected状态的Promise
     * @param ex 错误
     * @param <T> 参数类型
     * @param <V> 返回值类型
     * @return Promise
     */
    public static <T, V> Promise<V> resolve(final RuntimeException ex){
        return new Promise<>(new PromiseCallbackWithResolver<T, V>(){
            @Override
            public void call(T arg, PromiseResolver resolver) {
                resolve(ex);
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
     * @param <T> 参数类型
     * @param <V> 返回值类型
     * @return Promise
     */

    public static <T, V> Promise<List<V>> all(final List<Promise<V>> promises){
        return new Promise<>(new PromiseCallbackWithResolver<T, List<V>>() {
            @Override
            public void call(T arg, final PromiseResolver resolver) {
                final AtomicInteger totalCount = new AtomicInteger(promises.size());
                for (Promise<V> promise : promises){
                    promise.pipe(new PromiseResolver() {
                    	@SuppressWarnings("unchecked")
                        @Override
                        public void resolve(Object result) {
                            if (result instanceof RuntimeException){
                                resolver.resolve(new RuntimeException("one of promise in promises was rejected" ,(RuntimeException)result));
                            }else if (totalCount.decrementAndGet() == 0){
                                ArrayList<V> results = new ArrayList<>(promises.size());
                                for (Promise<V> promise : promises){
                                    results.add((V)promise.value);
                                }
                                resolver.resolve(results);
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
     * @param <T> 参数类型
     * @param <V> 返回值类型
     * @return Promise
     */
    public static <T, V> Promise<V> race(final List<Promise<V>> promises) {
        return new Promise<>(new PromiseCallbackWithResolver<T, V>() {
            @Override
            public void call(T arg, final PromiseResolver resolver) {
                final AtomicInteger totalCount = new AtomicInteger(promises.size());
                for (Promise<V> promise : promises) {
                    promise.pipe(new PromiseResolver() {
                        @Override
                        public void resolve(Object result) {
                            if (!(result instanceof RuntimeException)) {
                                resolver.resolve(result);
                            } else if (totalCount.decrementAndGet() == 0) {
                                resolver.resolve(new RuntimeException("all promise were rejected."));
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
     * @param <V> 返回值类型
     * @return Promise
     */
    public <V> Promise<V> then(final PromiseCallback<R, V> then){
        return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    resolver.resolve(arg);
                }else {
                    Promise.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(then.call(arg));
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
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
        __pipe(this, new PromiseCallbackWithResolver<R, Object>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    resolver.resolve(arg);
                }else {
                    Promise.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                then.call(arg);
                                resolver.resolve(null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
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
        __pipe(this, new PromiseCallbackWithResolver<R, Object>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    resolver.resolve(arg);
                }else {
                    Promise.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                then.call();
                                resolver.resolve(null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
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
     * @param <V> 返回值类型
     * @return Promise
     */
    public <V> Promise<V> then(final PromiseVoidArgCallback<V> then){
        return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    resolver.resolve(arg);
                }else {
                    Promise.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(then.call());
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 附加Promise
     * @param thenPromise next step
     * @param <V> 返回值类型
     * @return Promise
     */
    public <V> Promise<V> then(final Promise<V> thenPromise){
        return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
            @Override
            public void call(R arg, PromiseResolver resolver) {
                resolver.resolve(thenPromise);
            }
        });
    }

    /**
     * 主线程执行,使用 Resolver来回调
     * @param then next step
     * @param <V> 返回值类型
     * @return Promise
     */
    public <V> Promise<V> then(final PromiseCallbackWithResolver<R, V> then){
        return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    resolver.resolve(arg);
                }else {
                    Promise.this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                then.call(arg, resolver);
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
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
     * @param <V> 返回值类型
     * @return Promise
     */
    public <V> Promise<V> thenAsync(final PromiseCallback<R, V> then){
        return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    resolver.resolve(arg);
                }else {
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(then.call(arg));
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
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
     * @param <V> 返回值类型
     * @return Promise
     */
    public <V> Promise<V> thenAsync(final PromiseVoidArgCallback<V> then){
        return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    resolver.resolve(arg);
                }else {
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(then.call());
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
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
        __pipe(this, new PromiseCallbackWithResolver<R, Object>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    resolver.resolve(arg);
                }else {
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                then.call(arg);
                                resolver.resolve(null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
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
        __pipe(this, new PromiseCallbackWithResolver<R, Object>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    resolver.resolve(arg);
                }else {
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                then.call();
                                resolver.resolve(null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
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
     * @param <V> 返回值类型
     * @return Promise
     */
    public <V> Promise<V> thenAsync(final PromiseCallbackWithResolver<R, V> then){
        return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    resolver.resolve(arg);
                }else {
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                then.call(arg, resolver);
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
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
     * @param <V> 返回值类型
     * @return Promise
     */
    public <V> Promise<V> thenDelay(final long delayMillis, final PromiseCallback<R, V> then){
        return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    resolver.resolve(arg);
                }else {
                    Promise.this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(then.call(arg));
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
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
     * @param <V> 返回值类型
     * @return Promise
     */
    public <V> Promise<V> thenDelay(final long delayMillis, final PromiseVoidArgCallback<V> then){
        return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    resolver.resolve(arg);
                }else {
                    Promise.this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(then.call());
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
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
        __pipe(this, new PromiseCallbackWithResolver<R, Object>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    resolver.resolve(arg);
                }else {
                    Promise.this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                then.call(arg);
                                resolver.resolve(null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
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
        __pipe(this, new PromiseCallbackWithResolver<R, Object>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    resolver.resolve(arg);
                }else {
                    Promise.this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                then.call();
                                resolver.resolve(null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
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
     * @param <V> 返回值类型
     * @return Promise
     */
    public <V> Promise<V> thenDelay(final long delayMillis, final PromiseCallbackWithResolver<R, V> then) {
		return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
			@Override
			public void call(final R arg, final PromiseResolver resolver) {
				if (arg instanceof RuntimeException){
                    resolver.resolve(arg);
                }else {
                    Promise.this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                then.call(arg, resolver);
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
                            }
                        }
                    }, delayMillis);
                }
				
			}
		});
	}

    /**
     * 同步处理错误
     * @param error handle error
     * @param <V> 返回值类型
     * @return Promise
     */
    public <V> Promise<V> error(final PromiseCallback<RuntimeException, V> error){
        return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    Promise.this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(error.call((RuntimeException) arg));
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
                            }
                        }
                    });
                }else {
                    resolver.resolve(arg);
                }
            }
        });
    }

    /**
     * 同步处理错误
     * @param error handle error
     * @param <V> 返回值类型
     * @return Promise
     */
    public <V> Promise<V> error(final PromiseVoidArgCallback<V> error){
        return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    Promise.this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(error.call());
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
                            }
                        }
                    });
                }else {
                    resolver.resolve(arg);
                }
            }
        });
    }

    /**
     * 同步处理错误
     * @param error handle error
     */
    public void error(final PromiseVoidReturnCallback<RuntimeException> error){
        __pipe(this, new PromiseCallbackWithResolver<R, Object>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    Promise.this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                error.call((RuntimeException) arg);
                                resolver.resolve(null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
                            }
                        }
                    });
                }else {
                    resolver.resolve(arg);
                }
            }
        });
    }

    /**
     * 同步处理错误
     * @param error handle error
     */
    public void error(final PromiseVoidArgVoidReturnCallback error){
        __pipe(this, new PromiseCallbackWithResolver<R, Object>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    Promise.this.handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                error.call();
                                resolver.resolve(null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
                            }
                        }
                    });
                }else {
                    resolver.resolve(arg);
                }
            }
        });
    }

    /**
     * 异步处理错误
     * @param error handle error
     * @param <V> 返回值类型
     * @return Promise
     */
    public <V> Promise<V> errorAsync(final PromiseCallback<RuntimeException, V> error){
        return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(error.call((RuntimeException) arg));
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
                            }
                        }
                    });
                }else {
                    resolver.resolve(arg);
                }
            }
        });
    }

    /**
     * 异步处理错误
     * @param error handle error
     * @param <V> 返回值类型
     * @return Promise
     */
    public <V> Promise<V> errorAsync(final PromiseVoidArgCallback<V> error){
        return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                resolver.resolve(error.call());
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
                            }
                        }
                    });
                }else {
                    resolver.resolve(arg);
                }
            }
        });
    }

    /**
     * 异步处理错误
     * @param error handle error
     */
    public void errorAsync(final PromiseVoidReturnCallback<RuntimeException> error){
        __pipe(this, new PromiseCallbackWithResolver<R, Object>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                error.call((RuntimeException) arg);
                                resolver.resolve(null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
                            }
                        }
                    });
                }else {
                    resolver.resolve(arg);
                }
            }
        });
    }

    /**
     * 异步处理错误
     * @param error handle error
     */
    public void errorAsync(final PromiseVoidArgVoidReturnCallback error){
        __pipe(this, new PromiseCallbackWithResolver<R, Object>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                if (arg instanceof RuntimeException){
                    threadPool.execute(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                error.call();
                                resolver.resolve(null);
                            } catch (RuntimeException ex) {
                                resolver.resolve(ex);
                            }
                        }
                    });
                }else {
                    resolver.resolve(arg);
                }
            }
        });
    }

    /**
     * 主线程执行,正确或失败都会执行
     * @param always handle always
     * @param <V> 返回值类型
     * @return Promise
     */
    public <V> Promise<V> always(final PromiseCallback<Object, V> always){
        return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
            @Override
            public void call(final Object arg, final PromiseResolver resolver) {
                Promise.this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            resolver.resolve(always.call(arg));
                        }catch (RuntimeException ex){
                            resolver.resolve(ex);
                        }
                    }
                });
            }
        });
    }

    /**
     * 主线程执行,正确或失败都会执行
     * @param always handle always
     * @param <V> 返回值类型
     * @return Promise
     */
    public <V> Promise<V> always(final PromiseVoidArgCallback<V> always){
        return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
            @Override
            public void call(final Object arg, final PromiseResolver resolver) {
                Promise.this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            resolver.resolve(always.call());
                        }catch (RuntimeException ex){
                            resolver.resolve(ex);
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
        __pipe(this, new PromiseCallbackWithResolver<R, Object>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                Promise.this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            always.call(arg);
                            resolver.resolve(null);
                        }catch (RuntimeException ex){
                            resolver.resolve(ex);
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
        __pipe(this, new PromiseCallbackWithResolver<R, Object>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                Promise.this.handler.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            always.call();
                            resolver.resolve(null);
                        }catch (RuntimeException ex){
                            resolver.resolve(ex);
                        }
                    }
                });
            }
        });
    }

    /**
     * 异步执行,正确或失败都会执行
     * @param always handle always
     * @param <V> 返回值类型
     * @return Promise
     */
    public <V> Promise<V> alwaysAsync(final PromiseCallback<Object, V> always){
        return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
            @Override
            public void call(final Object arg, final PromiseResolver resolver) {
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            resolver.resolve(always.call(arg));
                        }catch (RuntimeException ex){
                            resolver.resolve(ex);
                        }
                    }
                });
            }
        });
    }

    /**
     * 异步执行,正确或失败都会执行
     * @param always handle always
     * @param <V> 返回值类型
     * @return Promise
     */
    public <V> Promise<V> alwaysAsync(final PromiseVoidArgCallback<V> always){
        return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
            @Override
            public void call(final Object arg, final PromiseResolver resolver) {
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            resolver.resolve(always.call());
                        }catch (RuntimeException ex){
                            resolver.resolve(ex);
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
        __pipe(this, new PromiseCallbackWithResolver<R, Object>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            always.call(arg);
                            resolver.resolve(null);
                        }catch (RuntimeException ex){
                            resolver.resolve(ex);
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
        __pipe(this, new PromiseCallbackWithResolver<R, Object>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
                threadPool.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            always.call();
                            resolver.resolve(null);
                        }catch (RuntimeException ex){
                            resolver.resolve(ex);
                        }
                    }
                });
            }
        });
    }
}
