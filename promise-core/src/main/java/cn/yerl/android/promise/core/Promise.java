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
 * Created by PSMac on 16/3/17.
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

    /**
     * Promise 当前状态
     */
    private State state;
    public State getState(){
        return state;
    }

    /**
     * Promise最终结果
     */
    private Object value;
    public Object getValue(){
        return value;
    }

    /**
     * 未执行的Handler
     */
    private List<PromiseResolver> handlers = new ArrayList<PromiseResolver>();

    private static Handler handler = new Handler(Looper.getMainLooper());

    /**
     * 创建一个空的Promise
     */
    public Promise(){
        this.state = State.Fulfilled;
    }

    /**
     * 创建一个未执行的Promise
     */
    public <T> Promise(final PromiseCallbackWithResolver<T, R> resolver){
        this.state = State.Pending;
        final PromiseResolver __presolver = new PromiseResolver() {
            @Override
            public void resolve(final Object result) {
                final List<PromiseResolver> callbacks = new ArrayList<PromiseResolver>();

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
     * @param error
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
     */
    private <T, V> Promise<V> __pipe(final Promise<R> self, final PromiseCallbackWithResolver<R, V> then){
        return new Promise<V>(new PromiseCallbackWithResolver<T, V>() {
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
     */
    public static <V> Promise<V> resolve(Promise<V> promise){
        return promise;
    }

    /**
     * 返回一个Fulfilled状态的Promise
     */
    public static <T, V> Promise<V> resolve(final T result){
        return new Promise<V>(new PromiseCallbackWithResolver<T, V>(){
            @Override
            public void call(T arg, PromiseResolver resolver) {
                resolve(result);
            }
        });
    }

    /**
     * 返回一个Rejected状态的Promise
     */
    public static <T, V> Promise<V> resolve(final RuntimeException ex){
        return new Promise< V>(new PromiseCallbackWithResolver<T, V>(){
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
     */
    public static <T, V> Promise< List<V>> all(final List<Promise<V>> promises){
        return new Promise<List<V>>(new PromiseCallbackWithResolver<T, List<V>>() {
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
                                ArrayList<V> results = new ArrayList<V>(promises.size());
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
     */
    public static <T, V> Promise<V> race(final List<Promise<V>> promises){
        return new Promise<V>(new PromiseCallbackWithResolver<T, V>() {
            @Override
            public void call(T arg, final PromiseResolver resolver) {
                final AtomicInteger totalCount = new AtomicInteger(promises.size());
                for (Promise<V> promise : promises){
                    promise.pipe(new PromiseResolver() {
                        @Override
                        public void resolve(Object result) {
                            if (!(result instanceof RuntimeException)){
                                resolver.resolve(result);
                            }else if (totalCount.decrementAndGet() == 0){
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
     * 附加Promise
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
     * 异步执行,使用Resolver来回调
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
     * 延迟执迁
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
     * 延迟执行,使用Resolver回调
     */
    public <V> Promise<V> thenDelay(final long delayMillis, final PromiseCallbackWithResolver<R, V> then) {
		return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
			@Override
			public void call(final R arg, final PromiseResolver resolver) {
				if (arg instanceof  RuntimeException){
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
     */
    public <V > Promise<V> error(final PromiseCallback<RuntimeException, V> error){
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
     * 异步处理错误
     */
    public <V > Promise<V> errorAsync(final PromiseCallback<RuntimeException, V> error){
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
     * 主线程执行,正确或失败都会执行
     */
    public <V> Promise<V> always(final PromiseCallback<R, V> always){
        return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
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
     * 异步执行,正确或失败都会执行
     */
    public <V> Promise<V> alwaysAsync(final PromiseCallback<R, V> always){
        return __pipe(this, new PromiseCallbackWithResolver<R, V>() {
            @Override
            public void call(final R arg, final PromiseResolver resolver) {
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
}
