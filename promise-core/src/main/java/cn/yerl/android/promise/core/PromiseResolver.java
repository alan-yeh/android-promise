package cn.yerl.android.promise.core;

/**
 * Resolver
 * Created by Alan Yeh on 16/3/17.
 */
public interface PromiseResolver<R> {
    void resolve(R result, RuntimeException error);
}
