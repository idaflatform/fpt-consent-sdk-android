package vn.fpt.fis.cmp.consent;

/**
 * Callback bat dong bo cua SDK. Ca hai ham deu duoc goi tren main thread.
 *
 * @param <T> kieu du lieu tra ve
 */
public interface ConsentCallback<T> {

    void onSuccess(T result);

    void onError(ConsentException error);
}
