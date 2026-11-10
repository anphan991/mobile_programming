package com.example.myapplication;

public class Product {

    private String name;
    private String description;
    private String price;
    private int imageResId;

    // Constructor tạo sản phẩm
    public Product(String name, String description, String price, int imageResId) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageResId = imageResId;
    }

    // lấy dữ liệu từng cột
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
}
