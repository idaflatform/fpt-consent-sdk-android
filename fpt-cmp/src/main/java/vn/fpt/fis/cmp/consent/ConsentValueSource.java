package vn.fpt.fis.cmp.consent;

import androidx.annotation.Nullable;

import vn.fpt.fis.cmp.consent.model.ConsentField;

/**
 * Nguon lay gia tri that cua truong du lieu tu form cua app.
 *
 * <p>SDK chi hoi nguon nay cho cac truong co {@code sharedWithSystem = true} trong {@code /config};
 * tra {@code null} khi form khong co truong tuong ung — khi do body chi gui {@code isAccept}.</p>
 */
public interface ConsentValueSource {

    @Nullable
    String valueFor(ConsentField field);
}
