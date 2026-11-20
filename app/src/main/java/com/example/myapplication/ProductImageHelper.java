package com.example.myapplication;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.File;

/**
 * Tiện ích hiển thị ảnh sản phẩm (hỗ trợ ảnh từ thư viện máy, Base64 Firestore hoặc drawable có sẵn)
 * kèm bộ nhớ đệm LruCache giúp cuộn danh sách mượt mà.
 */
public final class ProductImageHelper {

    private static final LruCache<String, Bitmap> BITMAP_CACHE = new LruCache<>(32);

    private ProductImageHelper() {}

    public static void bindImage(ImageView target, Product product) {
        if (target == null || product == null) return;
        bindPreview(target, product.getImagePath(), product.getImageBase64(), product.getImageResId());
    }

    public static void bindPreview(ImageView target, String imagePath, String imageBase64, int fallbackResId) {
        if (target == null) return;

        if (imagePath != null && !imagePath.trim().isEmpty()) {
            Bitmap cached = BITMAP_CACHE.get(imagePath);
            if (cached != null && !cached.isRecycled()) {
                target.setImageBitmap(cached);
                return;
            }
            File file = new File(imagePath);
            if (file.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
                if (bmp != null) {
                    BITMAP_CACHE.put(imagePath, bmp);
                    target.setImageBitmap(bmp);
                    return;
                }
            }
        }

        if (imageBase64 != null && !imageBase64.trim().isEmpty()) {
            String key = "b64_" + imageBase64.hashCode();
            Bitmap cached = BITMAP_CACHE.get(key);
            if (cached != null && !cached.isRecycled()) {
                target.setImageBitmap(cached);
                return;
            }
            try {
                byte[] bytes = Base64.decode(imageBase64, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bmp != null) {
                    BITMAP_CACHE.put(key, bmp);
                    target.setImageBitmap(bmp);
                    return;
                }
            } catch (Exception ignored) {
            }
        }

        target.setImageResource(fallbackResId != 0 ? fallbackResId : R.drawable.ic_store_24);
    }
}
