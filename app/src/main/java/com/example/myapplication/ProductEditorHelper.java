package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

public final class ProductEditorHelper {

    private static final String[] CATEGORIES = {
            "Điện thoại", "Máy tính bảng", "Máy tính xách tay", "Thiết bị âm thanh", "Đồng hồ thông minh", "Phụ kiện"
    };

    private static final String[] PRESET_LABELS = {
            "iPhone 16 Pro Max", "Samsung Galaxy S24 Ultra", "MacBook Air M3", "iPad Pro M4",
            "Tai nghe AirPods Pro 2", "Tai nghe Sony WH-1000XM5", "Apple Watch Series 10",
            "Bàn phím Keychron K8 Pro", "Chuột Logitech MX Master 3S", "Loa Marshall Emberton II",
            "Tai nghe Marshall Major IV", "Loa JBL Charge 5", "Biểu tượng mặc định"
    };

    private static final String[] PRESET_KEYS = {
            "img_iphone_16", "img_samsung_s24", "img_macbook_m3", "img_ipad_pro_m4",
            "img_airpods_pro", "img_sony_wh1000xm5", "img_apple_watch_series10",
            "img_keychron", "img_logitech_mx", "img_marshall_emberton",
            "img_marshall_major", "img_jbl_charge5", "ic_store_24"
    };

    private ProductEditorHelper() {}

    public static void showProductEditor(Context context, Product existing, Runnable onSaved) {
        showProductEditor(context, null, existing, onSaved);
    }

    public static void showProductEditor(Context context, ProductImagePicker imagePicker, Product existing, Runnable onSaved) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_admin_product, null);
        ImageView imgPreview = view.findViewById(R.id.imgProdPreview);
        View btnPickGallery = view.findViewById(R.id.btnPickGalleryImage);
        View btnPickUrl = view.findViewById(R.id.btnPickUrlImage);
        View btnPickPreset = view.findViewById(R.id.btnPickPresetImage);

        TextInputEditText edtName = view.findViewById(R.id.edtProdName);
        TextInputEditText edtSku = view.findViewById(R.id.edtProdSku);
        TextInputEditText edtPrice = view.findViewById(R.id.edtProdPrice);
        AutoCompleteTextView actvCategory = view.findViewById(R.id.actvProdCategory);
        TextInputEditText edtWarranty = view.findViewById(R.id.edtProdWarranty);
        TextInputEditText edtDesc = view.findViewById(R.id.edtProdDesc);
        TextInputEditText edtSpecs = view.findViewById(R.id.edtProdSpecs);
        MaterialSwitch switchInStock = view.findViewById(R.id.switchProdInStock);

        actvCategory.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, CATEGORIES));

        final String[] selPath = {""}, selB64 = {""}, selKey = {""};
        final int[] selRes = {R.drawable.ic_store_24};
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
            selPath[0] = existing.getImagePath();
            selB64[0] = existing.getImageBase64();
            selKey[0] = existing.getImageKey();
            selRes[0] = existing.getImageResId();
        } else {
            edtSku.setText("TS-" + (System.currentTimeMillis() % 9000 + 1000));
            actvCategory.setText(CATEGORIES[0], false);
            edtWarranty.setText("12 tháng chính hãng");
            switchInStock.setChecked(true);
            selRes[0] = DatabaseHelper.getImageForProduct("", CATEGORIES[0]);
            selKey[0] = DatabaseHelper.getKeyForDrawable(selRes[0]);
        }
        ProductImageHelper.bindPreview(imgPreview, selPath[0], selB64[0], selRes[0]);

        if (imagePicker == null) {
            btnPickGallery.setVisibility(View.GONE);
            btnPickUrl.setVisibility(View.GONE);
        } else {
            btnPickGallery.setOnClickListener(v -> imagePicker.pickImage((path, b64) -> {
                selPath[0] = path;
                selB64[0] = b64;
                ProductImageHelper.bindPreview(imgPreview, path, b64, selRes[0]);
                Toast.makeText(context, "Đã chọn ảnh sản phẩm!", Toast.LENGTH_SHORT).show();
            }));
            btnPickUrl.setOnClickListener(v -> {
                EditText edtUrl = new EditText(context);
                edtUrl.setHint("https://example.com/anh-san-pham.jpg");
                edtUrl.setSingleLine(true);
                int pad = Math.round(16 * context.getResources().getDisplayMetrics().density);
                FrameLayout box = new FrameLayout(context);
                box.setPadding(pad, pad / 2, pad, 0);
                box.addView(edtUrl);
                new MaterialAlertDialogBuilder(context).setTitle("Nhập đường dẫn ảnh (URL)").setView(box)
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Tải ảnh", (d, w) -> {
                            String url = edtUrl.getText() != null ? edtUrl.getText().toString().trim() : "";
                            if (url.isEmpty()) return;
                            imagePicker.loadFromUrl(url, (path, b64) -> {
                                selPath[0] = path;
                                selB64[0] = b64;
                                ProductImageHelper.bindPreview(imgPreview, path, b64, selRes[0]);
                                Toast.makeText(context, "Đã tải ảnh từ URL!", Toast.LENGTH_SHORT).show();
                            });
                        }).show();
            });
        }

        btnPickPreset.setOnClickListener(v -> new MaterialAlertDialogBuilder(context)
                .setTitle("Chọn ảnh mẫu sản phẩm")
                .setItems(PRESET_LABELS, (d, which) -> {
                    selKey[0] = PRESET_KEYS[which];
                    selRes[0] = DatabaseHelper.getImageByKeyOrProduct(selKey[0], "", "");
                    selPath[0] = "";
                    selB64[0] = "";
                    ProductImageHelper.bindPreview(imgPreview, "", "", selRes[0]);
                }).setNegativeButton("Đóng", null).show());

        switchInStock.setOnCheckedChangeListener((b, c) -> switchInStock.setText(c ? "Trạng thái: Còn hàng" : "Trạng thái: Hết hàng"));
        switchInStock.setText(switchInStock.isChecked() ? "Trạng thái: Còn hàng" : "Trạng thái: Hết hàng");

        new MaterialAlertDialogBuilder(context)
                .setTitle(isEdit ? "Chỉnh sửa sản phẩm" : "Thêm sản phẩm mới")
                .setView(view)
                .setNegativeButton("Hủy", null)
                .setPositiveButton(isEdit ? "Lưu thay đổi" : "Thêm sản phẩm", (dialog, which) -> {
                    String name = textOf(edtName), sku = textOf(edtSku), rawPrice = textOf(edtPrice);
                    String cat = actvCategory.getText() != null ? actvCategory.getText().toString().trim() : "";
                    String warranty = textOf(edtWarranty), desc = textOf(edtDesc), specs = textOf(edtSpecs);
                    if (name.isEmpty() || sku.isEmpty() || rawPrice.isEmpty() || desc.isEmpty()) {
                        Toast.makeText(context, "Vui lòng nhập đầy đủ Tên, SKU, Giá và Mô tả!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (cat.isEmpty()) cat = "Phụ kiện";
                    if (warranty.isEmpty()) warranty = "12 tháng chính hãng";
                    if (specs.isEmpty()) specs = desc;
                    String price = formatPriceInput(rawPrice);
                    int finalRes = DatabaseHelper.getImageByKeyOrProduct(selKey[0], name, cat);
                    if (selKey[0] == null || selKey[0].isEmpty()) selKey[0] = DatabaseHelper.getKeyForDrawable(finalRes);

                    Product saved = isEdit ? existing : new Product(name, desc, price, finalRes, cat, sku, warranty, specs, switchInStock.isChecked());
                    if (isEdit) {
                        saved.setName(name);
                        saved.setPrice(price);
                        saved.setCategory(cat);
                        saved.setWarranty(warranty);
                        saved.setDescription(desc);
                        saved.setDetailedSpecs(specs);
                        saved.setInStock(switchInStock.isChecked());
                        saved.setImageResId(finalRes);
                    } else {
                        ProductListActivity.productList.add(saved);
                    }
                    saved.setImageKey(selKey[0]);
                    saved.setImagePath(selPath[0]);
                    saved.setImageBase64(selB64[0]);

                    new DatabaseHelper(context).upsertProduct(saved);
                    FirebaseFirestore.getInstance().collection("products").document(saved.getSku()).set(saved.toMap());
                    Toast.makeText(context, (isEdit ? "Đã cập nhật " : "Đã thêm ") + name, Toast.LENGTH_SHORT).show();
                    if (onSaved != null) onSaved.run();
                }).show();
    }

    public static void confirmDeleteProduct(Context context, Product product, Runnable onDeleted) {
        if (product == null) return;
        new MaterialAlertDialogBuilder(context)
                .setTitle("Xóa sản phẩm")
                .setMessage("Bạn có chắc muốn xóa \"" + product.getName() + "\" (Mã " + product.getSku() + ") khỏi hệ thống?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa sản phẩm", (d, w) -> {
                    new DatabaseHelper(context).deleteProductBySku(product.getSku());
                    ProductListActivity.productList.remove(product);
                    FirebaseFirestore.getInstance().collection("products").document(product.getSku()).delete();
                    Toast.makeText(context, "Đã xóa sản phẩm " + product.getName(), Toast.LENGTH_SHORT).show();
                    if (onDeleted != null) onDeleted.run();
                }).show();
    }

    private static String textOf(TextInputEditText edt) {
        return edt.getText() != null ? edt.getText().toString().trim() : "";
    }

    private static String formatPriceInput(String input) {
        String clean = input.trim();
        if (clean.matches("\\d+")) {
            try { return CartManager.formatPrice(Long.parseLong(clean)); } catch (NumberFormatException ignored) {}
        }
        return clean.endsWith("đ") || clean.endsWith("Đ") ? clean : clean + "đ";
    }
}
