package vn.fpt.fis.cmp.consent;

import androidx.annotation.Nullable;

import vn.fpt.fis.cmp.consent.model.SendConsentResult;

/** Ket qua cua man hinh consent. */
public interface ConsentListener {

    /**
     * Nguoi dung da luu quyet dinh (hoac SDK doc lai quyet dinh da luu truoc do).
     *
     * @param state  quyet dinh theo purpose / truong du lieu
     * @param result response cua /sendData; null khi state duoc doc tu bo nho may
     */
    void onCompleted(ConsentState state, @Nullable SendConsentResult result);

    /** Nguoi dung dong man hinh ma khong luu. */
    default void onDismissed() {
    }

    /** Loi khi tai cau hinh hoac khi gui du lieu. */
    default void onError(ConsentException error) {
    }
}
