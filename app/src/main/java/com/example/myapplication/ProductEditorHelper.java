package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Tiện ích dùng chung cho Admin thêm, sửa và xóa sản phẩm (hỗ trợ chọn ảnh từ máy hoặc ảnh mẫu,
 * đồng bộ SQLite + Cloud Firestore).
 */
public final class ProductEditorHelper {

    private static final String[] CATEGORIES = new String[]{
            "Điện thoại",
            "Máy tính bảng",
            "Máy tính xách tay",
            "Thiết bị âm thanh",
            "Đồng hồ thông minh",
            "Phụ kiện"
    };

    private static final String[] PRESET_LABELS = new String[]{
            "iPhone 16 Pro Max",
            "Samsung Galaxy S24 Ultra",
            "MacBook Air M3",
            "iPad Pro M4",
            "Tai nghe AirPods Pro 2",
            "Tai nghe Sony WH-1000XM5",
            "Apple Watch Series 10",
            "Bàn phím Keychron K8 Pro",
            "Chuột Logitech MX Master 3S",
            "Loa Marshall Emberton II",
            "Tai nghe Marshall Major IV",
            "Loa JBL Charge 5",
            "Biểu tượng mặc định"
    };

    private static final String[] PRESET_KEYS = new String[]{
            "img_iphone_16",
            "img_samsung_s24",
            "img_macbook_m3",
            "img_ipad_pro_m4",
            "img_airpods_pro",
            "img_sony_wh1000xm5",
            "img_apple_watch_series10",
            "img_keychron",
            "img_logitech_mx",
            "img_marshall_emberton",
            "img_marshall_major",
            "img_jbl_charge5",
            "ic_store_24"
    };

    private ProductEditorHelper() {}

    public static void showProductEditor(Context context, Product existing, Runnable onSaved) {
        showProductEditor(context, null, existing, onSaved);
    }

    public static void showProductEditor(Context context, ProductImagePicker imagePicker,
                                         Product existing, Runnable onSaved) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_admin_product, null);

        ImageView imgPreview = view.findViewById(R.id.imgProdPreview);
        MaterialButton btnPickGallery = view.findViewById(R.id.btnPickGalleryImage);
        MaterialButton btnPickUrl = view.findViewById(R.id.btnPickUrlImage);
        MaterialButton btnPickPreset = view.findViewById(R.id.btnPickPresetImage);

        TextInputEditText edtName = view.findViewById(R.id.edtProdName);
        TextInputEditText edtSku = view.findViewById(R.id.edtProdSku);
        TextInputEditText edtPrice = view.findViewById(R.id.edtProdPrice);
        AutoCompleteTextView actvCategory = view.findViewById(R.id.actvProdCategory);
        TextInputEditText edtWarranty = view.findViewById(R.id.edtProdWarranty);
        TextInputEditText edtDesc = view.findViewById(R.id.edtProdDesc);
        TextInputEditText edtSpecs = view.findViewById(R.id.edtProdSpecs);
        MaterialSwitch switchInStock = view.findViewById(R.id.switchProdInStock);

        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(
                context, android.R.layout.simple_dropdown_item_1line, CATEGORIES);
        actvCategory.setAdapter(catAdapter);

        final String[] selectedPath = {""};
        final String[] selectedBase64 = {""};
        final String[] selectedKey = {""};
        final int[] selectedResId = {R.drawable.ic_store_24};

        boolean isEdit = existing != null;
        if (isEdit) {
            edtName.setText(existing.getName());
            edtSku.setText(existing.getSku());
            edtSku.setEnabled(false);
            edtPrice.setText(existing.getPrice());
            actvCategory.setText(existing.getCategory(), false);
            edtWarranty.setText(existing.getWarranty());
            edtDesc.setText(existing.getDescription());
            edtSpecs.setText(existing.getDetailedSpecs());
            switchInStock.setChecked(existing.isInStock());

            selectedPath[0] = existing.getImagePath();
            selectedBase64[0] = existing.getImageBase64();
            selectedKey[0] = existing.getImageKey();
            selectedResId[0] = existing.getImageResId();
            ProductImageHelper.bindPreview(imgPreview, selectedPath[0], selectedBase64[0], selectedResId[0]);
        } else {
            edtSku.setText("TS-" + (System.currentTimeMillis() % 9000 + 1000));
            actvCategory.setText(CATEGORIES[0], false);
            edtWarranty.setText("12 tháng chính hãng");
            switchInStock.setChecked(true);
            selectedResId[0] = DatabaseHelper.getImageForProduct("", CATEGORIES[0]);
            selectedKey[0] = DatabaseHelper.getKeyForDrawable(selectedResId[0]);
            ProductImageHelper.bindPreview(imgPreview, "", "", selectedResId[0]);
        }

        if (imagePicker == null) {
            btnPickGallery.setVisibility(View.GONE);
            btnPickUrl.setVisibility(View.GONE);
        } else {
            btnPickGallery.setOnClickListener(v -> imagePicker.pickImage((savedPath, base64) -> {
                selectedPath[0] = savedPath;
                selectedBase64[0] = base64;
                ProductImageHelper.bindPreview(imgPreview, savedPath, base64, selectedResId[0]);
                Toast.makeText(context, "Đã chọn ảnh sản phẩm!", Toast.LENGTH_SHORT).show();
            }));

            btnPickUrl.setOnClickListener(v -> {
                android.widget.EditText edtUrl = new android.widget.EditText(context);
                edtUrl.setHint("https://example.com/anh-san-pham.jpg");
                edtUrl.setSingleLine(true);
                int pad = Math.round(16 * context.getResources().getDisplayMetrics().density);
                android.widget.FrameLayout container = new android.widget.FrameLayout(context);
                container.setPadding(pad, pad / 2, pad, 0);
                container.addView(edtUrl);

                new MaterialAlertDialogBuilder(context)
                        .setTitle("Nhập đường dẫn ảnh (URL)")
                        .setView(container)
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Tải ảnh", (d, w) -> {
                            String url = edtUrl.getText() != null ? edtUrl.getText().toString().trim() : "";
                            if (url.isEmpty()) {
                                Toast.makeText(context, "Vui lòng nhập link ảnh!", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            imagePicker.loadFromUrl(url, (savedPath, base64) -> {
                                selectedPath[0] = savedPath;
                                selectedBase64[0] = base64;
                                ProductImageHelper.bindPreview(imgPreview, savedPath, base64, selectedResId[0]);
                                Toast.makeText(context, "Đã tải ảnh từ URL!", Toast.LENGTH_SHORT).show();
                            });
                        })
                        .show();
            });
        }

        btnPickPreset.setOnClickListener(v -> new MaterialAlertDialogBuilder(context)
                .setTitle("Chọn ảnh mẫu sản phẩm")
                .setItems(PRESET_LABELS, (d, which) -> {
                    String key = PRESET_KEYS[which];
                    selectedKey[0] = key;
                    selectedResId[0] = DatabaseHelper.getImageByKeyOrProduct(key, "", "");
                    selectedPath[0] = "";
                    selectedBase64[0] = "";
                    ProductImageHelper.bindPreview(imgPreview, "", "", selectedResId[0]);
                })
                .setNegativeButton("Đóng", null)
                .show());

        switchInStock.setOnCheckedChangeListener((buttonView, isChecked) ->
                switchInStock.setText(isChecked ? "Trạng thái: Còn hàng" : "Trạng thái: Hết hàng"));
        switchInStock.setText(switchInStock.isChecked() ? "Trạng thái: Còn hàng" : "Trạng thái: Hết hàng");

        new MaterialAlertDialogBuilder(context)
                .setTitle(isEdit ? "Chỉnh sửa sản phẩm" : "Thêm sản phẩm mới")
                .setView(view)
                .setNegativeButton("Hủy", null)
                .setPositiveButton(isEdit ? "Lưu thay đổi" : "Thêm sản phẩm", (dialog, which) -> {
                    String name = textOf(edtName);
                    String sku = textOf(edtSku);
                    String rawPrice = textOf(edtPrice);
                    String category = actvCategory.getText() != null ? actvCategory.getText().toString().trim() : "";
                    String warranty = textOf(edtWarranty);
                    String desc = textOf(edtDesc);
                    String specs = textOf(edtSpecs);
                    boolean inStock = switchInStock.isChecked();

                    if (name.isEmpty() || sku.isEmpty() || rawPrice.isEmpty() || desc.isEmpty()) {
                        Toast.makeText(context, "Vui lòng nhập đầy đủ Tên, SKU, Giá và Mô tả!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (category.isEmpty()) category = "Phụ kiện";
                    if (warranty.isEmpty()) warranty = "12 tháng chính hãng";
                    if (specs.isEmpty()) specs = desc;
                    String formattedPrice = formatPriceInput(rawPrice);

                    int finalResId = DatabaseHelper.getImageByKeyOrProduct(selectedKey[0], name, category);
                    if (selectedKey[0] == null || selectedKey[0].isEmpty()) {
                        selectedKey[0] = DatabaseHelper.getKeyForDrawable(finalResId);
                    }

                    Product saved;
                    if (isEdit) {
                        existing.setName(name);
                        existing.setPrice(formattedPrice);
                        existing.setCategory(category);
                        existing.setWarranty(warranty);
                        existing.setDescription(desc);
                        existing.setDetailedSpecs(specs);
                        existing.setInStock(inStock);
                        existing.setImageResId(finalResId);
                        existing.setImageKey(selectedKey[0]);
                        existing.setImagePath(selectedPath[0]);
                        existing.setImageBase64(selectedBase64[0]);
                        saved = existing;
                    } else {
                        saved = new Product(
                                name, desc, formattedPrice,
                                finalResId,
                                category, sku, warranty, specs, inStock
                        );
                        saved.setImageKey(selectedKey[0]);
                        saved.setImagePath(selectedPath[0]);
                        saved.setImageBase64(selectedBase64[0]);
                        ProductListActivity.productList.add(saved);
                    }

                    new DatabaseHelper(context).upsertProduct(saved);
                    FirebaseFirestore.getInstance()
                            .collection("products")
                            .document(saved.getSku())
                            .set(saved.toMap());

                    Toast.makeText(context,
                            isEdit ? "Đã cập nhật sản phẩm " + name : "Đã thêm sản phẩm " + name,
                            Toast.LENGTH_SHORT).show();

                    if (onSaved != null) {
                        onSaved.run();
                    }
                })
                .show();
    }

    public static void confirmDeleteProduct(Context context, Product product, Runnable onDeleted) {
        if (product == null) return;
        new MaterialAlertDialogBuilder(context)
                .setTitle("Xóa sản phẩm")
                .setMessage("Bạn có chắc muốn xóa \"" + product.getName() + "\" (Mã " + product.getSku() + ") khỏi hệ thống?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa sản phẩm", (dialog, which) -> {
                    new DatabaseHelper(context).deleteProductBySku(product.getSku());
                    ProductListActivity.productList.remove(product);
                    FirebaseFirestore.getInstance()
                            .collection("products")
                            .document(product.getSku())
                            .delete();
                    Toast.makeText(context, "Đã xóa sản phẩm " + product.getName(), Toast.LENGTH_SHORT).show();
                    if (onDeleted != null) {
                        onDeleted.run();
                    }
                })
                .show();
    }

    private static String textOf(TextInputEditText edt) {
        return edt.getText() != null ? edt.getText().toString().trim() : "";
    }

    private static String formatPriceInput(String input) {
        String clean = input.trim();
        if (clean.matches("\\d+")) {
            try {
                long value = Long.parseLong(clean);
                return CartManager.formatPrice(value);
            } catch (NumberFormatException ignored) {
            }
        }
        return clean.endsWith("đ") || clean.endsWith("Đ") ? clean : clean + "đ";
    }
}
