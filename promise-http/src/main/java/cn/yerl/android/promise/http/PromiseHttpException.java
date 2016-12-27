package cn.yerl.android.promise.http;

/**
 * PromiseHttpException
 * Created by Alan Yeh on 2016/12/12.
 */

public class PromiseHttpException extends RuntimeException {
    private final PromiseResponse response;

    public PromiseHttpException(PromiseResponse response, Throwable ex){
        super(ex);
        this.response = response;
    }

    /**
     * HttpResponse
     * @return PromiseResponse
     */
    public PromiseResponse getResponse() {
        return response;
    }
}
