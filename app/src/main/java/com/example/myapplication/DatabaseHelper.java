package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "TechStore.db";
    private static final int DATABASE_VERSION = 3;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //bảng tài khoản người dùng
        db.execSQL("CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT UNIQUE, "
                + "password TEXT)");

        //bảng sản phẩm
        db.execSQL("CREATE TABLE IF NOT EXISTS products ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT, description TEXT, price TEXT, image_res_id INTEGER, "
                + "category TEXT, sku TEXT UNIQUE, warranty TEXT, detailed_specs TEXT, in_stock INTEGER)");

        //bảng đơn đặt hàng
        db.execSQL("CREATE TABLE IF NOT EXISTS orders ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "order_code TEXT UNIQUE, username TEXT, receiver_name TEXT, "
                + "receiver_phone TEXT, receiver_address TEXT, receiver_note TEXT, "
                + "payment_method TEXT, total_price TEXT, item_count INTEGER, created_at TEXT)");

        //bảng giỏ hàng riêng cho từng tài khoản
        db.execSQL("CREATE TABLE IF NOT EXISTS cart_items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT, sku TEXT, product_name TEXT, price TEXT, quantity INTEGER, "
                + "UNIQUE(username, sku))");

        //admin / 123456
        ContentValues admin = new ContentValues();
        admin.put("username", "admin");
        admin.put("password", "123456");
        db.insertWithOnConflict("users", null, admin, SQLiteDatabase.CONFLICT_IGNORE);

        // Nạp sẵn 12 sản phẩm công nghệ vào SQLite
        seedDefaultProducts(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        onCreate(db);
    }

    // Nạp danh sách sản phẩm vào bảng products
    private void seedDefaultProducts(SQLiteDatabase db) {
        for (Product p : ProductListActivity.createDefaultProducts()) {
            ContentValues cv = new ContentValues();
            cv.put("name", p.getName());
            cv.put("description", p.getDescription());
            cv.put("price", p.getPrice());
            cv.put("image_res_id", p.getImageResId());
            cv.put("category", p.getCategory());
            cv.put("sku", p.getSku());
            cv.put("warranty", p.getWarranty());
            cv.put("detailed_specs", p.getDetailedSpecs());
            cv.put("in_stock", p.isInStock() ? 1 : 0);
            db.insertWithOnConflict("products", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    // Đọc danh sách tất cả sản phẩm từ SQLite
    public ArrayList<Product> getAllProducts() {
        ArrayList<Product> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT name, description, price, image_res_id, category, sku, warranty, detailed_specs, in_stock "
                        + "FROM products ORDER BY id ASC", null);
        if (c != null) {
            while (c.moveToNext()) {
                list.add(new Product(
                        c.getString(0), c.getString(1), c.getString(2), c.getInt(3),
                        c.getString(4), c.getString(5), c.getString(6), c.getString(7),
                        c.getInt(8) == 1
                ));
            }
            c.close();
        }
        return list;
    }

    // Đăng ký tài khoản mới
    public boolean registerUser(String username, String password) {
        ContentValues cv = new ContentValues();
        cv.put("username", username);
        cv.put("password", password);
        return getWritableDatabase().insert("users", null, cv) != -1;
    }

    // Kiểm tra username đã tồn tại chưa
    public boolean checkUserExists(String username) {
        Cursor c = getReadableDatabase().rawQuery("SELECT id FROM users WHERE username = ?", new String[]{username});
        boolean exists = (c != null && c.moveToFirst());
        if (c != null) c.close();
        return exists;
    }

    // Kiểm tra đăng nhập
    public boolean checkLogin(String username, String password) {
        Cursor c = getReadableDatabase().rawQuery("SELECT id FROM users WHERE username = ? AND password = ?", new String[]{username, password});
        boolean ok = (c != null && c.moveToFirst());
        if (c != null) c.close();
        return ok;
    }

    // Lưu đơn đặt hàng mới vào SQLite
    public long insertOrder(String code, String user, String name, String phone, String address, String note, String payment, String total, int count) {
        ContentValues cv = new ContentValues();
        cv.put("order_code", code);
        cv.put("username", user);
        cv.put("receiver_name", name);
        cv.put("receiver_phone", phone);
        cv.put("receiver_address", address);
        cv.put("receiver_note", note);
        cv.put("payment_method", payment);
        cv.put("total_price", total);
        cv.put("item_count", count);
        cv.put("created_at", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
        return getWritableDatabase().insert("orders", null, cv);
    }

    // Lưu hoặc cập nhật món hàng trong giỏ của tài khoản
    public void saveCartItem(String username, Product product, int quantity) {
        if (username == null || username.trim().isEmpty() || product == null) return;
        SQLiteDatabase db = getWritableDatabase();
        if (quantity <= 0) {
            db.delete("cart_items", "username = ? AND sku = ?", new String[]{username, product.getSku()});
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put("username", username);
        cv.put("sku", product.getSku());
        cv.put("product_name", product.getName());
        cv.put("price", product.getPrice());
        cv.put("quantity", quantity);
        db.insertWithOnConflict("cart_items", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // Lấy danh sách giỏ hàng riêng của tài khoản từ SQLite
    public ArrayList<CartItem> getCartItemsByUser(String username) {
        ArrayList<CartItem> list = new ArrayList<>();
        if (username == null || username.trim().isEmpty()) return list;

        ArrayList<Product> allProducts = getAllProducts();
        Cursor c = getReadableDatabase().rawQuery("SELECT sku, quantity FROM cart_items WHERE username = ? ORDER BY id ASC", new String[]{username});
        if (c != null) {
            while (c.moveToNext()) {
                String sku = c.getString(0);
                int qty = c.getInt(1);
                for (Product p : allProducts) {
                    if (p.getSku().equals(sku)) {
                        list.add(new CartItem(p, qty));
                        break;
                    }
                }
            }
            c.close();
        }
        return list;
    }

    // Xóa giỏ hàng của tài khoản sau khi đặt hàng
    public void clearUserCart(String username) {
        if (username != null && !username.trim().isEmpty()) {
            getWritableDatabase().delete("cart_items", "username = ?", new String[]{username});
        }
    }
}
