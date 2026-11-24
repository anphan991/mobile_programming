package com.example.myapplication;

import android.content.Context;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class CartManager {

    public static final ArrayList<CartItem> cartList = new ArrayList<>();
    private static String activeUsername = "";
    private static Context appContext;

    public static void syncForCurrentUser(Context context) {
        if (context == null) return;
        appContext = context.getApplicationContext();
        String user = appContext.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
                .getString("CURRENT_USER", "");
        user = user != null ? user.trim() : "";

        if (user.isEmpty()) {
            onLogout();
        } else if (!user.equals(activeUsername)) {
            activeUsername = user;
            cartList.clear();
            cartList.addAll(new DatabaseHelper(appContext).getCartItemsByUser(activeUsername));
        }
    }

    public static void onLogout() {
        activeUsername = "";
        cartList.clear();
    }

    public static void addToCart(Product product, int quantity) {
        if (product == null) return;
        for (CartItem item : cartList) {
            if (item.getProduct().getSku().equals(product.getSku())) {
                item.setQuantity(item.getQuantity() + quantity);
                persistItem(product, item.getQuantity());
                return;
            }
        }
        cartList.add(new CartItem(product, quantity));
        persistItem(product, quantity);
    }

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

    public static void clearCart() {
        cartList.clear();
        if (appContext != null && !activeUsername.isEmpty()) {
            new DatabaseHelper(appContext).clearUserCart(activeUsername);
        }
    }

    private static void persistItem(Product product, int quantity) {
        if (appContext != null && !activeUsername.isEmpty() && product != null) {
            new DatabaseHelper(appContext).saveCartItem(activeUsername, product, quantity);
        }
    }

    public static long parsePrice(String priceStr) {
        if (priceStr == null) return 0;
        String digits = priceStr.replaceAll("[^0-9]", "");
        try {
            return digits.isEmpty() ? 0 : Long.parseLong(digits);
        } catch (Exception e) {
            return 0;
        }
    }

    public static String formatPrice(long price) {
        return new DecimalFormat("#,###").format(price).replace(',', '.') + "đ";
    }

    public static long getTotalPrice() {
        long total = 0;
        for (CartItem item : cartList) {
            total += parsePrice(item.getProduct().getPrice()) * item.getQuantity();
        }
        return total;
    }

    public static int getTotalItemCount() {
        int count = 0;
        for (CartItem item : cartList) count += item.getQuantity();
        return count;
    }
}
