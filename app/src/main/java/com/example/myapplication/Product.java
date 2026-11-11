package com.example.myapplication;

public class Product {

    private String name;
    private String description;
    private String price;
    private int imageResId;
    private String category;
    private String sku;
    private String warranty;
    private String detailedSpecs;
    private boolean inStock;

    // Constructor cơ bản
    public Product(String name, String description, String price, int imageResId) {
        this(name, description, price, imageResId, "Thiết bị công nghệ", "TZ-STD", "12 tháng chính hãng", description, true);
    }

    // Constructor đầy đủ thông tin (mặc định còn hàng)
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
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
        return detailedSpecs;
    }

    public boolean isInStock() {
        return inStock;
    }

    public void setInStock(boolean inStock) {
        this.inStock = inStock;
    }
}
