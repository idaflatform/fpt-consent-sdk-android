package vn.fpt.fis.cmp.consent;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

/**
 * Doc thong tin app dang nhung SDK de gui kem field {@code source} cua ban ghi consent.
 *
 * <p>Ban ghi consent la bang chung phap ly; biet no den tu app nao, ban nao, giup doi soat khi khach
 * hang co nhieu app cung tro toi mot Collection Point.</p>
 */
final class AppInfo {

    /** Cot {@code source} phia backend gioi han 255, entity khai 100 — cat o 100 cho an toan. */
    private static final int MAX_LENGTH = 100;

    private AppInfo() {
    }

    /**
     * Chuoi mo ta app, dang {@code "Ten app (package) version"}.
     *
     * <p>Vd {@code "CMP Consent Sample (vn.fpt.fis.cmp.sample) 1.0"}. Doc that bai o bat ky phan nao
     * thi tra phan lay duoc, khong bao gio nem loi — day chi la metadata.</p>
     */
    static String describe(Context context) {
        if (context == null) {
            return null;
        }
        String packageName = context.getPackageName();
        String label = readLabel(context);
        String version = readVersion(context, packageName);

        StringBuilder source = new StringBuilder();
        if (label != null && !label.isEmpty()) {
            source.append(label).append(' ');
        }
        source.append('(').append(packageName).append(')');
        if (version != null && !version.isEmpty()) {
            source.append(' ').append(version);
        }
        return trim(source.toString());
    }

    private static String readLabel(Context context) {
        try {
            ApplicationInfo info = context.getApplicationInfo();
            CharSequence label = info.loadLabel(context.getPackageManager());
            return label == null ? null : label.toString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String readVersion(Context context, String packageName) {
        try {
            return context.getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException | RuntimeException e) {
            return null;
        }
    }

    private static String trim(String value) {
        return value.length() <= MAX_LENGTH ? value : value.substring(0, MAX_LENGTH);
    }
}
