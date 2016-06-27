package cn.yerl.promise.core;

/**
 * 回调
 * Created by PoiSon on 16/3/18.
 */
public interface PromiseCallback<T, R> {
    Object call(T arg);
}
