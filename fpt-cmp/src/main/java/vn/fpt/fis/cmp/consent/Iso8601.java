package vn.fpt.fis.cmp.consent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Dinh dang thoi diem theo ISO-8601 co offset (vd {@code 2026-08-12T14:05:09+07:00}) —
 * dung kieu {@code OffsetDateTime} ma backend mong doi o truong {@code dateCreated}.
 *
 * <p>Khong dung {@code java.time} de SDK chay duoc tu API 21 ma khong can desugaring.</p>
 */
public final class Iso8601 {

    private Iso8601() {
    }

    public static String now() {
        return format(new Date());
    }

    public static String format(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US);
        String text = formatter.format(date);
        // "+0700" -> "+07:00" (RFC 3339 / OffsetDateTime).
        return text.substring(0, text.length() - 2) + ":" + text.substring(text.length() - 2);
    }
}
