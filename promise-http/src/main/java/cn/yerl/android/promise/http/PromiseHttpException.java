package cn.yerl.android.promise.http;

/**
 * Created by alan on 2016/12/12.
 */

public class PromiseHttpException extends RuntimeException {
    private final PromiseResponse response;

    public PromiseHttpException(PromiseResponse response, Throwable ex){
        super(ex);
        this.response = response;
    }

    public PromiseResponse getResponse() {
        return response;
    }
}
