package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.File;

public final class ProductImageHelper {

    private static final LruCache<String, Bitmap> CACHE = new LruCache<>(32);

    private ProductImageHelper() {}

    public static void bindImage(ImageView target, Product product) {
        if (target != null && product != null) {
            bindPreview(target, product.getImagePath(), product.getImageBase64(), product.getImageResId());
        }
    }

    public static void bindPreview(ImageView target, String path, String base64, int fallbackResId) {
        if (target == null) return;

        if (path != null && !path.trim().isEmpty()) {
            Bitmap cached = CACHE.get(path);
            if (cached != null && !cached.isRecycled()) {
                target.setImageBitmap(cached);
                return;
            }
            File f = new File(path);
            if (f.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (bmp != null) {
                    CACHE.put(path, bmp);
                    target.setImageBitmap(bmp);
                    return;
                }
            }
        }

        if (base64 != null && !base64.trim().isEmpty()) {
            String key = "b64_" + base64.hashCode();
            Bitmap cached = CACHE.get(key);
            if (cached != null && !cached.isRecycled()) {
                target.setImageBitmap(cached);
                return;
            }
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp != null) {
                    CACHE.put(key, bmp);
                    target.setImageBitmap(bmp);
                    return;
                }
            } catch (Exception ignored) {}
        }

        target.setImageResource(fallbackResId != 0 ? fallbackResId : R.drawable.ic_store_24);
    }
}
