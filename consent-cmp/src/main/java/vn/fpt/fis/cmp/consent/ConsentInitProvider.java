package vn.fpt.fis.cmp.consent;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Tự khởi tạo SDK từ {@code <meta-data>} {@value #META_CODE_CONFIG} / {@value #META_BASE_URL},
 * chạy trước {@code Application.onCreate()}. Không khai codeConfig thì bỏ qua.
 */
public final class ConsentInitProvider extends ContentProvider {

    public static final String META_CODE_CONFIG = "vn.fpt.cmp.codeConfig";
    public static final String META_BASE_URL = "vn.fpt.cmp.baseUrl";

    private static final String TAG = "CMP";

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context == null) {
            return false;
        }
        // Chạy trên main thread: không gọi mạng, không để lỗi làm crash app.
        try {
            Bundle meta = readMetaData(context);
            String codeConfig = string(meta, META_CODE_CONFIG);
            if (codeConfig == null) {
                return true;
            }
            ConsentOptions options = new ConsentOptions.Builder()
                    .baseUrl(string(meta, META_BASE_URL))
                    .codeConfig(codeConfig)
                    .build();
            ConsentCmp.initIfAbsent(context, options);
        } catch (RuntimeException e) {
            Log.w(TAG, "Khong tu khoi tao duoc tu <meta-data>: " + e.getMessage());
        }
        return true;
    }

    @Nullable
    private static Bundle readMetaData(Context context) {
        try {
            ApplicationInfo info = context.getPackageManager()
                    .getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return info.metaData;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /** Đọc dạng chuỗi (aapt có thể lưu thành số). */
    @Nullable
    @SuppressWarnings("deprecation") // Bundle.get(String): cần đọc được mọi kiểu, không chỉ String.
    private static String string(@Nullable Bundle meta, String key) {
        if (meta == null) {
            return null;
        }
        Object value = meta.get(key);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    // Không phục vụ dữ liệu.

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        return 0;
    }
}
