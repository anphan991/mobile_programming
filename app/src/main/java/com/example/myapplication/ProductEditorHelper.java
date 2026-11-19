package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Tiện ích dùng chung cho Admin thêm, sửa và xóa sản phẩm (đồng bộ SQLite + Cloud Firestore).
 */
public final class ProductEditorHelper {

    private static final String[] CATEGORIES = new String[]{
            "Điện thoại",
            "Máy tính bảng",
            "Laptop",
            "Thiết bị âm thanh",
            "Đồng hồ thông minh",
            "Bàn phím",
            "Chuột",
            "Phụ kiện"
    };

    private ProductEditorHelper() {}

    public static void showProductEditor(Context context, Product existing, Runnable onSaved) {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_admin_product, null);

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
        } else {
            edtSku.setText("TS-" + (System.currentTimeMillis() % 9000 + 1000));
            actvCategory.setText(CATEGORIES[0], false);
            edtWarranty.setText("12 tháng chính hãng");
            switchInStock.setChecked(true);
        }

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

                    Product saved;
                    if (isEdit) {
                        existing.setName(name);
                        existing.setPrice(formattedPrice);
                        existing.setCategory(category);
                        existing.setWarranty(warranty);
                        existing.setDescription(desc);
                        existing.setDetailedSpecs(specs);
                        existing.setInStock(inStock);
                        existing.setImageResId(DatabaseHelper.getImageForProduct(name, category));
                        saved = existing;
                    } else {
                        saved = new Product(
                                name, desc, formattedPrice,
                                DatabaseHelper.getImageForProduct(name, category),
                                category, sku, warranty, specs, inStock
                        );
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
