package cn.yerl.android.promise.core;

/**
 * 回调
 * Created by Alan Yeh on 16/3/18.
 */
public interface PromiseCallback<A, R> {
    R call(A arg);
}
