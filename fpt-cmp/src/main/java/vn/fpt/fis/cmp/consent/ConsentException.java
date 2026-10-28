package vn.fpt.fis.cmp.consent;

/** Loi cua SDK: loi mang, loi HTTP hoac loi nghiep vu do backend tra ve. */
public class ConsentException extends Exception {

    /** HTTP status; 0 khi loi xay ra truoc khi co response (mat mang, timeout, parse). */
    public final int httpStatus;
    /** Ma loi nghiep vu trong {@code BaseResponse.code}; null neu khong co. */
    public final String errorCode;

    public ConsentException(String message) {
        this(message, 0, null, null);
    }

    public ConsentException(String message, Throwable cause) {
        this(message, 0, null, cause);
    }

    public ConsentException(String message, int httpStatus, String errorCode, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    /** True khi loi do nguoi dung / cau hinh (4xx) chu khong phai loi he thong. */
    public boolean isClientError() {
        return httpStatus >= 400 && httpStatus < 500;
    }
}
