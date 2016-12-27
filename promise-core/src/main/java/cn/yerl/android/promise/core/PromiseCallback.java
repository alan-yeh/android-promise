package cn.yerl.android.promise.core;

/**
 * 回调
 * Created by Alan Yeh on 16/3/18.
 */
public interface PromiseCallback<T, R> {
    Object call(T arg);
}
