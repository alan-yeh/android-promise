package cn.yerl.android.promise.core;

/**
 * 无返回值回调
 * Created by alan on 2017/3/22.
 */
public interface PromiseVoidReturnCallback<A> {
    void call(A arg);
}
