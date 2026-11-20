package com.example.myapplication;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Trình chọn ảnh sản phẩm từ bộ nhớ thiết bị (OpenDocument hiển thị trực tiếp thư mục Downloads)
 * hoặc tải từ đường dẫn URL ảnh, tự động nén kích thước và lưu nội bộ + Base64.
 */
public class ProductImagePicker {

    public interface OnImagePickedListener {
        void onImagePicked(String savedFilePath, String base64Data);
    }

    private final AppCompatActivity activity;
    private final ActivityResultLauncher<String[]> launcher;
    private OnImagePickedListener pendingListener;

    public ProductImagePicker(AppCompatActivity activity) {
        this.activity = activity;
        this.launcher = activity.registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                this::handlePickedUri
        );
    }

    public void pickImage(OnImagePickedListener listener) {
        this.pendingListener = listener;
        launcher.launch(new String[]{"image/*"});
    }

    public void loadFromUrl(String urlString, OnImagePickedListener listener) {
        if (urlString == null || urlString.trim().isEmpty() || listener == null) {
            return;
        }
        String cleanUrl = urlString.trim();
        Toast.makeText(activity, "Đang tải ảnh...", Toast.LENGTH_SHORT).show();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(cleanUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setInstanceFollowRedirects(true);
                conn.connect();

                try (InputStream is = conn.getInputStream()) {
                    Bitmap raw = BitmapFactory.decodeStream(is);
                    if (raw == null) {
                        mainHandler.post(() -> Toast.makeText(activity, "Link không phải ảnh hợp lệ!", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    Bitmap scaled = scaleBitmap(raw, 480);
                    String[] saved = saveBitmapInternal(scaled);
                    mainHandler.post(() -> {
                        if (!activity.isFinishing() && !activity.isDestroyed() && saved != null) {
                            listener.onImagePicked(saved[0], saved[1]);
                        }
                    });
                }
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!activity.isFinishing() && !activity.isDestroyed()) {
                        Toast.makeText(activity, "Không tải được ảnh từ link này!", Toast.LENGTH_SHORT).show();
                    }
                });
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void handlePickedUri(Uri uri) {
        if (uri == null || pendingListener == null) return;
        try {
            Bitmap scaled = decodeScaledBitmap(activity.getContentResolver(), uri, 480);
            if (scaled == null) {
                Toast.makeText(activity, "Không thể đọc ảnh đã chọn!", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] saved = saveBitmapInternal(scaled);
            if (saved != null) {
                pendingListener.onImagePicked(saved[0], saved[1]);
            }
        } catch (Exception e) {
            Toast.makeText(activity, "Lỗi khi xử lý ảnh sản phẩm!", Toast.LENGTH_SHORT).show();
        }
    }

    private String[] saveBitmapInternal(Bitmap bitmap) throws Exception {
        File dir = new File(activity.getFilesDir(), "product_images");
        if (!dir.exists() && !dir.mkdirs()) {
            activity.runOnUiThread(() -> Toast.makeText(activity, "Không thể tạo thư mục lưu ảnh!", Toast.LENGTH_SHORT).show());
            return null;
        }

        File outFile = new File(dir, "prod_" + System.currentTimeMillis() + ".jpg");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 78, baos);
        byte[] jpegBytes = baos.toByteArray();

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(jpegBytes);
        }

        String base64 = Base64.encodeToString(jpegBytes, Base64.NO_WRAP);
        return new String[]{outFile.getAbsolutePath(), base64};
    }

    private static Bitmap decodeScaledBitmap(ContentResolver resolver, Uri uri, int maxDim) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream is = resolver.openInputStream(uri)) {
            BitmapFactory.decodeStream(is, null, bounds);
        }

        int sampleSize = 1;
        int w = bounds.outWidth;
        int h = bounds.outHeight;
        while (w / sampleSize > maxDim * 2 || h / sampleSize > maxDim * 2) {
            sampleSize *= 2;
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sampleSize;
        Bitmap decoded;
        try (InputStream is = resolver.openInputStream(uri)) {
            decoded = BitmapFactory.decodeStream(is, null, opts);
        }
        return decoded != null ? scaleBitmap(decoded, maxDim) : null;
    }

    private static Bitmap scaleBitmap(Bitmap decoded, int maxDim) {
        int curW = decoded.getWidth();
        int curH = decoded.getHeight();
        if (curW <= maxDim && curH <= maxDim) {
            return decoded;
        }
        float scale = Math.min((float) maxDim / curW, (float) maxDim / curH);
        int targetW = Math.max(1, Math.round(curW * scale));
        int targetH = Math.max(1, Math.round(curH * scale));
        return Bitmap.createScaledBitmap(decoded, targetW, targetH, true);
    }
}
