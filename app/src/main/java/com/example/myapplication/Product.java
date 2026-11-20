package com.example.myapplication;

import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class Product {

    private String name;
    private String description;
    private String price;
    private int imageResId;
    private String category;
    private String sku; //mã sp
    private String warranty; //bảo hành
    private String detailedSpecs;
    private boolean inStock;
    private String imagePath = "";
    private String imageBase64 = "";
    private String imageKey = "";

    // Constructor
    public Product(String name, String description, String price, int imageResId) {
        this(name, description, price, imageResId, "Thiết bị công nghệ", "TZ-STD", "12 tháng chính hãng", description, true);
    }

    // Constructor đầy đủ thông tin mặc định còn hàng
    public Product(String name, String description, String price, int imageResId,
                   String category, String sku, String warranty, String detailedSpecs) {
        this(name, description, price, imageResId, category, sku, warranty, detailedSpecs, true);
    }

    // Constructor đầy đủ thông tin có trạng thái tồn kho
    public Product(String name, String description, String price, int imageResId,
                   String category, String sku, String warranty, String detailedSpecs, boolean inStock) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageResId = imageResId;
        this.category = category;
        this.sku = sku;
        this.warranty = warranty;
        this.detailedSpecs = detailedSpecs;
        this.inStock = inStock;
    }

    //getter
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description != null
                ? description.replace(" · ", " - ").replace(" • ", " - ").replace(" & ", " và ")
                : "";
    }

    public String getPrice() {
        return price;
    }

    public int getImageResId() {
        return imageResId;
    }

    public String getCategory() {
        return category;
    }

    public String getSku() {
        return sku;
    }

    public String getWarranty() {
        return warranty;
    }

    public String getDetailedSpecs() {
        return detailedSpecs != null ? detailedSpecs.replace(" & ", " và ") : "";
    }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath != null ? imagePath : ""; }

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64 != null ? imageBase64 : ""; }

    public String getImageKey() { return imageKey; }
    public void setImageKey(String imageKey) { this.imageKey = imageKey != null ? imageKey : ""; }

    public boolean isInStock() {
        return inStock;
    }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(String price) { this.price = price; }
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }
    public void setCategory(String category) { this.category = category; }
    public void setSku(String sku) { this.sku = sku; }
    public void setWarranty(String warranty) { this.warranty = warranty; }
    public void setDetailedSpecs(String detailedSpecs) { this.detailedSpecs = detailedSpecs; }

    public void setInStock(boolean inStock) {
        this.inStock = inStock;
    }

    /** Đóng gói thuộc tính sản phẩm thành Map để lưu lên Cloud Firestore. */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("price", price);
        map.put("category", category);
        map.put("sku", sku);
        map.put("description", description);
        map.put("warranty", warranty);
        map.put("detailedSpecs", detailedSpecs);
        map.put("inStock", inStock);
        map.put("imageKey", imageKey);
        map.put("imageBase64", imageBase64);
        return map;
    }

    /** Tái tạo đối tượng Product từ DocumentSnapshot tải về từ Cloud Firestore. */
    public static Product fromFirestore(DocumentSnapshot doc) {
        String name = doc.getString("name");
        String desc = doc.getString("description");
        String price = doc.getString("price");
        String category = doc.getString("category");
        String sku = doc.getString("sku");
        String warranty = doc.getString("warranty");
        String specs = doc.getString("detailedSpecs");
        String imageKey = doc.getString("imageKey");
        String imageBase64 = doc.getString("imageBase64");
        String resolvedCat = category != null ? category : "Thiết bị";
        String normalizedDesc = desc != null ? desc.replace(" · ", " - ").replace(" • ", " - ") : "";
        Product p = new Product(
                name != null ? name : "Sản phẩm",
                normalizedDesc,
                price != null ? price : "0đ",
                DatabaseHelper.getImageByKeyOrProduct(imageKey, name, resolvedCat),
                resolvedCat,
                sku != null ? sku : doc.getId(),
                warranty != null ? warranty : "",
                specs != null ? specs : "",
                Boolean.TRUE.equals(doc.getBoolean("inStock"))
        );
        p.setImageKey(imageKey);
        p.setImageBase64(imageBase64);
        return p;
    }
}
