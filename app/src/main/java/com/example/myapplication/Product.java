package com.example.myapplication;

import com.google.firebase.firestore.DocumentSnapshot;
import java.util.HashMap;
import java.util.Map;

public class Product {
    private String name, description, price, category, sku, warranty, detailedSpecs;
    private int imageResId;
    private boolean inStock;
    private String imagePath = "", imageBase64 = "", imageKey = "";

    public Product(String name, String description, String price, int imageResId) {
        this(name, description, price, imageResId, "Thiết bị công nghệ", "TZ-STD", "12 tháng chính hãng", description, true);
    }

    public Product(String name, String description, String price, int imageResId,
                   String category, String sku, String warranty, String detailedSpecs) {
        this(name, description, price, imageResId, category, sku, warranty, detailedSpecs, true);
    }

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

    public String getName() { return name; }
    public String getDescription() {
        return description != null ? description.replace(" · ", " - ").replace(" • ", " - ").replace(" & ", " và ") : "";
    }
    public String getPrice() { return price; }
    public int getImageResId() { return imageResId; }
    public String getCategory() { return category; }
    public String getSku() { return sku; }
    public String getWarranty() { return warranty; }
    public String getDetailedSpecs() { return detailedSpecs != null ? detailedSpecs.replace(" & ", " và ") : ""; }
    public boolean isInStock() { return inStock; }
    public String getImagePath() { return imagePath; }
    public String getImageBase64() { return imageBase64; }
    public String getImageKey() { return imageKey; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPrice(String price) { this.price = price; }
    public void setImageResId(int imageResId) { this.imageResId = imageResId; }
    public void setCategory(String category) { this.category = category; }
    public void setSku(String sku) { this.sku = sku; }
    public void setWarranty(String warranty) { this.warranty = warranty; }
    public void setDetailedSpecs(String detailedSpecs) { this.detailedSpecs = detailedSpecs; }
    public void setInStock(boolean inStock) { this.inStock = inStock; }
    public void setImagePath(String path) { this.imagePath = path != null ? path : ""; }
    public void setImageBase64(String b64) { this.imageBase64 = b64 != null ? b64 : ""; }
    public void setImageKey(String key) { this.imageKey = key != null ? key : ""; }

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

    public static Product fromFirestore(DocumentSnapshot doc) {
        String name = doc.getString("name");
        String desc = doc.getString("description");
        String cat = doc.getString("category") != null ? doc.getString("category") : "Thiết bị";
        String key = doc.getString("imageKey");
        Product p = new Product(
                name != null ? name : "Sản phẩm",
                desc != null ? desc.replace(" · ", " - ").replace(" • ", " - ") : "",
                doc.getString("price") != null ? doc.getString("price") : "0đ",
                DatabaseHelper.getImageByKeyOrProduct(key, name, cat),
                cat,
                doc.getString("sku") != null ? doc.getString("sku") : doc.getId(),
                doc.getString("warranty") != null ? doc.getString("warranty") : "",
                doc.getString("detailedSpecs") != null ? doc.getString("detailedSpecs") : "",
                Boolean.TRUE.equals(doc.getBoolean("inStock"))
        );
        p.setImageKey(key);
        p.setImageBase64(doc.getString("imageBase64"));
        return p;
    }
}
