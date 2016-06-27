package cn.yerl.promise.core;

/**
 * 使用 Resolver 回调
 * Created by PSMac on 16/3/17.
 */
public interface PromiseCallbackWithResolver<T, R> {
    void call(T arg, PromiseResolver resolver);
}
