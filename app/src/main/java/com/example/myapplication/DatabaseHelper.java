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
//lớp DatabaseHelper kế thừa từ SQLiteOpenHelper
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
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, " //khóa chính tự tăng
                + "username TEXT UNIQUE, " //ràng buộc username
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

        // Nạp sẵn 12 sản phẩm vào SQLite
        seedDefaultProducts(db);
    }

    //tạo thêm bảng mới chưa tồn tại
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
        //duyệt qua các sp ở productlist
        for (Product p : ProductListActivity.createDefaultProducts()) {
            ContentValues cv = new ContentValues(); //Khởi tạo đối tượng gom dữ liệu cho 1 dòng sản phẩm
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

    // ánh xạ hình ảnh theo tên sản phẩm
    public static int getImageForProduct(String name) {
        if (name == null) return R.drawable.ic_launcher_foreground;
        switch (name.trim()) {
            case "iPhone 16 Pro Max": return R.drawable.img_iphone_16;
            case "Samsung Galaxy S24 Ultra": return R.drawable.img_samsung_s24;
            case "MacBook Air M3 13 inch": return R.drawable.img_macbook_m3;
            case "iPad Pro M4 11 inch": return R.drawable.img_ipad_pro_m4;
            case "Tai nghe AirPods Pro 2": return R.drawable.img_airpods_pro;
            case "Tai nghe Sony WH-1000XM5": return R.drawable.img_sony_wh1000xm5;
            case "Apple Watch Series 10": return R.drawable.img_apple_watch_series10;
            case "Bàn phím cơ Keychron K8 Pro": return R.drawable.img_keychron;
            case "Chuột Logitech MX Master 3S": return R.drawable.img_logitech_mx;
            case "Loa Marshall Emberton II": return R.drawable.img_marshall_emberton;
            case "Tai nghe Marshall Major IV": return R.drawable.img_marshall_major;
            case "Loa JBL Charge 5": return R.drawable.img_jbl_charge5;
            default: return R.drawable.ic_launcher_foreground;
        }
    }

    // Đọc danh sách tất cả sản phẩm từ SQLite
    public ArrayList<Product> getAllProducts() {
        ArrayList<Product> list = new ArrayList<>();  //ds rỗng
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT name, description, price, image_res_id, category, sku, warranty, detailed_specs, in_stock "
                        + "FROM products ORDER BY id ASC", null);
        //Kiểm tra an toàn cursor có được tạo thành công hay không tránh ứng dụng bị văng
        if (c != null) {
            while (c.moveToNext()) { //duyệt hết data
                String name = c.getString(0);
                int imgRes = getImageForProduct(name); // Luôn lấy ảnh chuẩn xác theo tên
                list.add(new Product( //khởi tạo đối tượng Product từ các cột trên dòng hiện tại
                        name, c.getString(1), c.getString(2), imgRes,
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
        //username ràng buộc UNIQUE
        //Chèn dòng mới vào bảng users trả về ID (hàng, cột) của dòng vừa chèn
        //insert sẽ trả về -1 so sánh != -1, true: đăng ký thành công / false: đăng ký thất bại
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
        //c.moveToFirst(): di chuyển con trỏ đến dòng đầu tiên của kết quả nếu có ít nhất một dòng trùng tên trả về true
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
        //số lượng <=0: xóa sp
        if (quantity <= 0) {
            db.delete("cart_items", "username = ? AND sku = ?", new String[]{username, product.getSku()});
            return;
        }
        //số lượng > 0
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
        //lọc ra sản phẩm thuộc về đúng username
        Cursor c = getReadableDatabase().rawQuery("SELECT sku, quantity FROM cart_items WHERE username = ? ORDER BY id ASC", new String[]{username});
        if (c != null) {
            while (c.moveToNext()) {
                //so sku trong bảng để lấy đúng sp
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
