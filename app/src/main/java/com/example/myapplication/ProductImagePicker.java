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

public class ProductImagePicker {

    public interface OnImagePickedListener {
        void onImagePicked(String savedFilePath, String base64Data);
    }

    private final AppCompatActivity activity;
    private final ActivityResultLauncher<String[]> launcher;
    private OnImagePickedListener pendingListener;

    public ProductImagePicker(AppCompatActivity activity) {
        this.activity = activity;
        this.launcher = activity.registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handlePickedUri);
    }

    public void pickImage(OnImagePickedListener listener) {
        this.pendingListener = listener;
        launcher.launch(new String[]{"image/*"});
    }

    public void loadFromUrl(String urlString, OnImagePickedListener listener) {
        if (urlString == null || urlString.trim().isEmpty() || listener == null) return;
        Toast.makeText(activity, "Đang tải ảnh...", Toast.LENGTH_SHORT).show();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(urlString.trim()).openConnection();
                conn.setConnectTimeout(8000);
                conn.setReadTimeout(8000);
                conn.setInstanceFollowRedirects(true);
                try (InputStream is = conn.getInputStream()) {
                    Bitmap raw = BitmapFactory.decodeStream(is);
                    if (raw == null) {
                        mainHandler.post(() -> Toast.makeText(activity, "Link không phải ảnh hợp lệ!", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    String[] saved = saveBitmapInternal(scaleBitmap(raw, 480));
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
            if (saved != null) pendingListener.onImagePicked(saved[0], saved[1]);
        } catch (Exception e) {
            Toast.makeText(activity, "Lỗi khi xử lý ảnh sản phẩm!", Toast.LENGTH_SHORT).show();
        }
    }

    private String[] saveBitmapInternal(Bitmap bitmap) throws Exception {
        File dir = new File(activity.getFilesDir(), "product_images");
        if (!dir.exists() && !dir.mkdirs()) return null;
        File outFile = new File(dir, "prod_" + System.currentTimeMillis() + ".jpg");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 78, baos);
        byte[] bytes = baos.toByteArray();
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(bytes);
        }
        return new String[]{outFile.getAbsolutePath(), Base64.encodeToString(bytes, Base64.NO_WRAP)};
    }

    private static Bitmap decodeScaledBitmap(ContentResolver resolver, Uri uri, int maxDim) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream is = resolver.openInputStream(uri)) {
            BitmapFactory.decodeStream(is, null, bounds);
        }
        int sample = 1;
        while (bounds.outWidth / sample > maxDim * 2 || bounds.outHeight / sample > maxDim * 2) sample *= 2;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        try (InputStream is = resolver.openInputStream(uri)) {
            Bitmap decoded = BitmapFactory.decodeStream(is, null, opts);
            return decoded != null ? scaleBitmap(decoded, maxDim) : null;
        }
    }

    private static Bitmap scaleBitmap(Bitmap bmp, int maxDim) {
        int w = bmp.getWidth(), h = bmp.getHeight();
        if (w <= maxDim && h <= maxDim) return bmp;
        float s = Math.min((float) maxDim / w, (float) maxDim / h);
        return Bitmap.createScaledBitmap(bmp, Math.max(1, Math.round(w * s)), Math.max(1, Math.round(h * s)), true);
    }
}
