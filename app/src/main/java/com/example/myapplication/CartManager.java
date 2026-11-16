package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class CartManager {

    // Danh sách sản phẩm trong giỏ của tài khoản đang đăng nhập
    public static ArrayList<CartItem> cartList = new ArrayList<>();
    private static String activeUsername = "";
    private static Context appContext;

    // Đồng bộ giỏ hàng theo đúng tài khoản đang đăng nhập trong SharedPreferences (UserSession)
    public static void syncForCurrentUser(Context context) {
        if (context == null) return;
        appContext = context.getApplicationContext();

        SharedPreferences sessionPref = appContext.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String currentUser = sessionPref.getString("CURRENT_USER", "");
        if (currentUser == null) {
            currentUser = "";
        }
        currentUser = currentUser.trim();

        // Nếu chưa có tài khoản đăng nhập -> xóa giỏ hàng trên bộ nhớ
        if (currentUser.isEmpty()) {
            activeUsername = "";
            cartList.clear();
            return;
        }

        // Nếu đổi sang tài khoản khác hoặc lần đầu mở app -> nạp đúng giỏ hàng của tài khoản đó từ SQLite
        if (!currentUser.equals(activeUsername)) {
            activeUsername = currentUser;
            DatabaseHelper dbHelper = new DatabaseHelper(appContext);
            cartList.clear();
            cartList.addAll(dbHelper.getCartItemsByUser(activeUsername));
        }
    }

    // Xóa bộ nhớ tạm giỏ hàng khi người dùng đăng xuất
    public static void onLogout() {
        activeUsername = "";
        cartList.clear();
    }

    // Thêm sản phẩm vào giỏ nếu đã có thì cộng dồn số lượng
    public static void addToCart(Product product, int quantity) {
        if (product == null) return;
        for (CartItem item : cartList) {
            if (item.getProduct().getSku().equals(product.getSku())) {
                int newQty = item.getQuantity() + quantity;
                item.setQuantity(newQty);
                persistItem(product, newQty);
                return;
            }
        }
        cartList.add(new CartItem(product, quantity));
        persistItem(product, quantity);
    }

    // Tăng hoặc giảm số lượng
    public static void updateQuantity(CartItem item, int delta) {
        if (item == null) return;
        int newQty = item.getQuantity() + delta;
        if (newQty <= 0) {
            cartList.remove(item);
            persistItem(item.getProduct(), 0);
        } else {
            item.setQuantity(newQty);
            persistItem(item.getProduct(), newQty);
        }
    }

    // Xóa một món khỏi giỏ
    public static void removeItem(CartItem item) {
        if (item == null) return;
        cartList.remove(item);
        persistItem(item.getProduct(), 0);
    }

    // Xóa sạch giỏ hàng của tài khoản hiện tại sau khi đặt hàng thành công
    public static void clearCart() {
        cartList.clear();
        if (appContext != null && !activeUsername.isEmpty()) {
            DatabaseHelper dbHelper = new DatabaseHelper(appContext);
            dbHelper.clearUserCart(activeUsername);
        }
    }

    // Lưu thay đổi của món hàng xuống bảng cart_items trong SQLite theo username hiện tại
    private static void persistItem(Product product, int quantity) {
        if (appContext != null && !activeUsername.isEmpty() && product != null) {
            DatabaseHelper dbHelper = new DatabaseHelper(appContext);
            dbHelper.saveCartItem(activeUsername, product, quantity);
        }
    }

    // Chuyển chuỗi tiền thành số nguyên long
    public static long parsePrice(String priceStr) {
        if (priceStr == null) return 0;
        try {
            String digits = priceStr.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) return 0;
            return Long.parseLong(digits);
        } catch (Exception e) {
            return 0;
        }
    }

    // Định dạng số thành chuỗi tiền "34.990.000đ"
    public static String formatPrice(long price) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(price).replace(',', '.') + "đ";
    }

    // Tính tổng tiền thanh toán của toàn bộ giỏ hàng
    public static long getTotalPrice() {
        long total = 0;
        for (CartItem item : cartList) {
            long unitPrice = parsePrice(item.getProduct().getPrice());
            total += unitPrice * item.getQuantity();
        }
        return total;
    }

    // Đếm tổng số lượng món hàng
    public static int getTotalItemCount() {
        int count = 0;
        for (CartItem item : cartList) {
            count += item.getQuantity();
        }
        return count;
    }
}
