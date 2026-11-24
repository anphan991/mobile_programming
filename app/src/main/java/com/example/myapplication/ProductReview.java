package com.example.myapplication;

public class ProductReview {
    private final int id;
    private final String sku;
    private final String username;
    private final int rating;
    private final String comment;
    private final String createdAt;
    private final boolean verifiedPurchase;
    private int helpfulCount;

    public ProductReview(int id, String sku, String username, int rating,
                         String comment, String createdAt, boolean verifiedPurchase, int helpfulCount) {
        this.id = id;
        this.sku = sku != null ? sku : "";
        this.username = username != null ? username : "Khách hàng";
        this.rating = Math.max(1, Math.min(5, rating));
        this.comment = comment != null ? comment : "";
        this.createdAt = createdAt != null ? createdAt : "";
        this.verifiedPurchase = verifiedPurchase;
        this.helpfulCount = Math.max(0, helpfulCount);
    }

    public int getId() { return id; }
    public String getSku() { return sku; }
    public String getUsername() { return username; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public String getCreatedAt() { return createdAt; }
    public boolean isVerifiedPurchase() { return verifiedPurchase; }
    public int getHelpfulCount() { return helpfulCount; }
    public void setHelpfulCount(int count) { this.helpfulCount = Math.max(0, count); }
}
