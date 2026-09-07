package vn.fpt.fis.cmp.consent.model;

import java.io.Serializable;

/** Response cua POST /api/v1/cmp/consent/sendData. */
public final class SendConsentResult implements Serializable {

    public final boolean ok;
    /** Khoa dinh danh ban ghi consent — dung de doi soat / DSAR sau nay. */
    public final String consentId;
    public final String code;
    public final String dateCreated;

    public SendConsentResult(boolean ok, String consentId, String code, String dateCreated) {
        this.ok = ok;
        this.consentId = consentId;
        this.code = code;
        this.dateCreated = dateCreated;
    }
}
