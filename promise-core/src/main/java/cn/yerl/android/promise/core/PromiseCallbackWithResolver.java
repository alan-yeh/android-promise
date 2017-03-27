package cn.yerl.android.promise.core;

/**
 * 使用 Resolver 回调
 * Created by Alan Yeh on 16/3/17.
 */
public interface PromiseCallbackWithResolver<A, R> {
    void call(A arg, PromiseResolver<R> resolver);
}