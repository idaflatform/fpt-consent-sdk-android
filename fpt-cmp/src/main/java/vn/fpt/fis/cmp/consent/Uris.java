package vn.fpt.fis.cmp.consent;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/** Tien ich nho cho query string. */
final class Uris {

    private Uris() {
    }

    static String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }
}
