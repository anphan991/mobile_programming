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
    private static final int DATABASE_VERSION = 7;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //bảng tài khoản người dùng
        db.execSQL("CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, " //khóa chính tự tăng
                + "username TEXT UNIQUE, " //ràng buộc username
                + "password TEXT, "
                + "role TEXT DEFAULT 'user')");
        ensureUsersRoleColumn(db);

        //bảng sản phẩm
        db.execSQL("CREATE TABLE IF NOT EXISTS products ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT, description TEXT, price TEXT, image_res_id INTEGER, "
                + "category TEXT, sku TEXT UNIQUE, warranty TEXT, detailed_specs TEXT, in_stock INTEGER, "
                + "image_path TEXT DEFAULT '', image_base64 TEXT DEFAULT '', image_key TEXT DEFAULT '')");
        ensureProductsImageColumns(db);

        //bảng đơn đặt hàng
        db.execSQL("CREATE TABLE IF NOT EXISTS orders ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "order_code TEXT UNIQUE, username TEXT, receiver_name TEXT, "
                + "receiver_phone TEXT, receiver_address TEXT, receiver_note TEXT, "
                + "payment_method TEXT, total_price TEXT, item_count INTEGER, created_at TEXT, "
                + "status_step INTEGER DEFAULT 0, items_summary TEXT DEFAULT '', "
                + "cancel_reason TEXT DEFAULT '', items_raw TEXT DEFAULT '')");
        ensureOrdersStatusColumn(db);

        //bảng giỏ hàng riêng cho từng tài khoản
        db.execSQL("CREATE TABLE IF NOT EXISTS cart_items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT, sku TEXT, product_name TEXT, price TEXT, quantity INTEGER, "
                + "UNIQUE(username, sku))");

        //admin / 123456
        ContentValues admin = new ContentValues();
        admin.put("username", "admin");
        admin.put("password", "123456");
        admin.put("role", UserAccount.ROLE_ADMIN);
        db.insertWithOnConflict("users", null, admin, SQLiteDatabase.CONFLICT_IGNORE);
        db.execSQL("UPDATE users SET role = 'admin' WHERE LOWER(username) = 'admin'");

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

    private void ensureUsersRoleColumn(SQLiteDatabase db) {
        Cursor c = db.rawQuery("PRAGMA table_info(users)", null);
        boolean hasRole = false;
        try {
            int nameIdx = c.getColumnIndex("name");
            while (c.moveToNext()) {
                if (nameIdx >= 0 && "role".equals(c.getString(nameIdx))) {
                    hasRole = true;
                    break;
                }
            }
        } finally {
            c.close();
        }
        if (!hasRole) {
            db.execSQL("ALTER TABLE users ADD COLUMN role TEXT DEFAULT 'user'");
        }
    }

    private void ensureOrdersStatusColumn(SQLiteDatabase db) {
        Cursor c = db.rawQuery("PRAGMA table_info(orders)", null);
        boolean hasStatusStep = false;
        boolean hasItemsSummary = false;
        boolean hasCancelReason = false;
        boolean hasItemsRaw = false;
        try {
            int nameIdx = c.getColumnIndex("name");
            while (c.moveToNext()) {
                if (nameIdx >= 0) {
                    String colName = c.getString(nameIdx);
                    if ("status_step".equals(colName)) hasStatusStep = true;
                    if ("items_summary".equals(colName)) hasItemsSummary = true;
                    if ("cancel_reason".equals(colName)) hasCancelReason = true;
                    if ("items_raw".equals(colName)) hasItemsRaw = true;
                }
            }
        } finally {
            c.close();
        }
        if (!hasStatusStep) {
            db.execSQL("ALTER TABLE orders ADD COLUMN status_step INTEGER DEFAULT 0");
        }
        if (!hasItemsSummary) {
            db.execSQL("ALTER TABLE orders ADD COLUMN items_summary TEXT DEFAULT ''");
        }
        if (!hasCancelReason) {
            db.execSQL("ALTER TABLE orders ADD COLUMN cancel_reason TEXT DEFAULT ''");
        }
        if (!hasItemsRaw) {
            db.execSQL("ALTER TABLE orders ADD COLUMN items_raw TEXT DEFAULT ''");
        }
    }

    private void ensureProductsImageColumns(SQLiteDatabase db) {
        Cursor c = db.rawQuery("PRAGMA table_info(products)", null);
        boolean hasPath = false;
        boolean hasBase64 = false;
        boolean hasKey = false;
        try {
            int nameIdx = c.getColumnIndex("name");
            while (c.moveToNext()) {
                if (nameIdx >= 0) {
                    String colName = c.getString(nameIdx);
                    if ("image_path".equals(colName)) hasPath = true;
                    if ("image_base64".equals(colName)) hasBase64 = true;
                    if ("image_key".equals(colName)) hasKey = true;
                }
            }
        } finally {
            c.close();
        }
        if (!hasPath) db.execSQL("ALTER TABLE products ADD COLUMN image_path TEXT DEFAULT ''");
        if (!hasBase64) db.execSQL("ALTER TABLE products ADD COLUMN image_base64 TEXT DEFAULT ''");
        if (!hasKey) db.execSQL("ALTER TABLE products ADD COLUMN image_key TEXT DEFAULT ''");
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
        return getImageForProduct(name, null);
    }

    // ánh xạ hình ảnh theo tên hoặc danh mục cho sản phẩm mới
    public static int getImageForProduct(String name, String category) {
        if (name != null) {
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
            }
        }
        if (category != null) {
            String lower = category.toLowerCase(Locale.ROOT);
            if (lower.contains("điện thoại")) return R.drawable.img_iphone_16;
            if (lower.contains("máy tính bảng") || lower.contains("tablet")) return R.drawable.img_ipad_pro_m4;
            if (lower.contains("laptop") || lower.contains("máy tính")) return R.drawable.img_macbook_m3;
            if (lower.contains("âm thanh") || lower.contains("tai nghe") || lower.contains("loa")) return R.drawable.img_airpods_pro;
            if (lower.contains("đồng hồ")) return R.drawable.img_apple_watch_series10;
            if (lower.contains("bàn phím")) return R.drawable.img_keychron;
            if (lower.contains("chuột")) return R.drawable.img_logitech_mx;
        }
        return R.drawable.ic_store_24;
    }

    public static int getImageByKeyOrProduct(String key, String name, String category) {
        if (key != null && !key.trim().isEmpty()) {
            switch (key.trim()) {
                case "img_iphone_16": return R.drawable.img_iphone_16;
                case "img_samsung_s24": return R.drawable.img_samsung_s24;
                case "img_macbook_m3": return R.drawable.img_macbook_m3;
                case "img_ipad_pro_m4": return R.drawable.img_ipad_pro_m4;
                case "img_airpods_pro": return R.drawable.img_airpods_pro;
                case "img_sony_wh1000xm5": return R.drawable.img_sony_wh1000xm5;
                case "img_apple_watch_series10": return R.drawable.img_apple_watch_series10;
                case "img_keychron": return R.drawable.img_keychron;
                case "img_logitech_mx": return R.drawable.img_logitech_mx;
                case "img_marshall_emberton": return R.drawable.img_marshall_emberton;
                case "img_marshall_major": return R.drawable.img_marshall_major;
                case "img_jbl_charge5": return R.drawable.img_jbl_charge5;
                case "ic_store_24": return R.drawable.ic_store_24;
            }
        }
        return getImageForProduct(name, category);
    }

    public static String getKeyForDrawable(int resId) {
        if (resId == R.drawable.img_iphone_16) return "img_iphone_16";
        if (resId == R.drawable.img_samsung_s24) return "img_samsung_s24";
        if (resId == R.drawable.img_macbook_m3) return "img_macbook_m3";
        if (resId == R.drawable.img_ipad_pro_m4) return "img_ipad_pro_m4";
        if (resId == R.drawable.img_airpods_pro) return "img_airpods_pro";
        if (resId == R.drawable.img_sony_wh1000xm5) return "img_sony_wh1000xm5";
        if (resId == R.drawable.img_apple_watch_series10) return "img_apple_watch_series10";
        if (resId == R.drawable.img_keychron) return "img_keychron";
        if (resId == R.drawable.img_logitech_mx) return "img_logitech_mx";
        if (resId == R.drawable.img_marshall_emberton) return "img_marshall_emberton";
        if (resId == R.drawable.img_marshall_major) return "img_marshall_major";
        if (resId == R.drawable.img_jbl_charge5) return "img_jbl_charge5";
        return "ic_store_24";
    }

    // Đọc danh sách tất cả sản phẩm từ SQLite
    public ArrayList<Product> getAllProducts() {
        ArrayList<Product> list = new ArrayList<>();  //ds rỗng
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT name, description, price, image_res_id, category, sku, warranty, detailed_specs, in_stock, "
                        + "COALESCE(image_path, ''), COALESCE(image_base64, ''), COALESCE(image_key, '') "
                        + "FROM products ORDER BY id ASC", null);
        //Kiểm tra an toàn cursor có được tạo thành công hay không tránh ứng dụng bị văng
        if (c != null) {
            while (c.moveToNext()) { //duyệt hết data
                String name = c.getString(0);
                String category = c.getString(4);
                String imagePath = c.getString(9);
                String imageBase64 = c.getString(10);
                String imageKey = c.getString(11);
                int imgRes = getImageByKeyOrProduct(imageKey, name, category);
                Product p = new Product( //khởi tạo đối tượng Product từ các cột trên dòng hiện tại
                        name, c.getString(1), c.getString(2), imgRes,
                        category, c.getString(5), c.getString(6), c.getString(7),
                        c.getInt(8) == 1
                );
                p.setImagePath(imagePath);
                p.setImageBase64(imageBase64);
                p.setImageKey(imageKey);
                list.add(p);
            }
            c.close();
        }
        return list;
    }

    // Thêm mới hoặc cập nhật sản phẩm trong SQLite theo SKU (Admin)
    public boolean upsertProduct(Product p) {
        if (p == null || p.getSku() == null || p.getSku().trim().isEmpty()) {
            return false;
        }
        String key = (p.getImageKey() != null && !p.getImageKey().isEmpty())
                ? p.getImageKey()
                : getKeyForDrawable(p.getImageResId());
        int resolvedRes = getImageByKeyOrProduct(key, p.getName(), p.getCategory());
        ContentValues cv = new ContentValues();
        cv.put("name", p.getName());
        cv.put("description", p.getDescription());
        cv.put("price", p.getPrice());
        cv.put("image_res_id", resolvedRes);
        cv.put("category", p.getCategory());
        cv.put("sku", p.getSku().trim());
        cv.put("warranty", p.getWarranty());
        cv.put("detailed_specs", p.getDetailedSpecs());
        cv.put("in_stock", p.isInStock() ? 1 : 0);
        cv.put("image_path", p.getImagePath());
        cv.put("image_base64", p.getImageBase64());
        cv.put("image_key", key);

        SQLiteDatabase db = getWritableDatabase();
        int updated = db.update("products", cv, "sku = ?", new String[]{p.getSku().trim()});
        if (updated > 0) {
            return true;
        }
        return db.insert("products", null, cv) != -1;
    }

    // Xóa sản phẩm theo SKU (Admin)
    public boolean deleteProductBySku(String sku) {
        if (sku == null || sku.trim().isEmpty()) {
            return false;
        }
        return getWritableDatabase().delete("products", "sku = ?", new String[]{sku.trim()}) > 0;
    }

    // Đăng ký tài khoản mới
    public boolean registerUser(String username, String password) {
        return addUser(username, password, UserAccount.ROLE_USER);
    }

    // Thêm tài khoản mới kèm vai trò
    public boolean addUser(String username, String password, String role) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return false;
        }
        String cleanUser = username.trim();
        ContentValues cv = new ContentValues();
        cv.put("username", cleanUser);
        cv.put("password", password);
        cv.put("role", UserAccount.normalizeRole(role, cleanUser));
        return getWritableDatabase().insert("users", null, cv) != -1;
    }

    // Kiểm tra quyền Admin theo username hoặc cột role
    public boolean isAdminUser(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        String clean = username.trim();
        if ("admin".equalsIgnoreCase(clean)) {
            return true;
        }
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT role FROM users WHERE LOWER(username) = LOWER(?)",
                new String[]{clean});
        boolean admin = false;
        if (c != null) {
            if (c.moveToFirst()) {
                admin = UserAccount.ROLE_ADMIN.equalsIgnoreCase(c.getString(0));
            }
            c.close();
        }
        return admin;
    }

    // Lấy toàn bộ tài khoản người dùng (Admin)
    public java.util.List<UserAccount> getAllUsers() {
        java.util.List<UserAccount> users = new java.util.ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, username, password, COALESCE(role, 'user') FROM users ORDER BY id ASC",
                null);
        if (c != null) {
            while (c.moveToNext()) {
                users.add(new UserAccount(
                        c.getInt(0),
                        c.getString(1),
                        c.getString(2),
                        c.getString(3)
                ));
            }
            c.close();
        }
        return users;
    }

    // Cập nhật thông tin người dùng (Admin)
    public boolean updateUser(int id, String username, String password, String role) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
            return false;
        }
        String cleanUser = username.trim();
        ContentValues cv = new ContentValues();
        cv.put("username", cleanUser);
        cv.put("password", password);
        cv.put("role", UserAccount.normalizeRole(role, cleanUser));
        try {
            return getWritableDatabase().update("users", cv, "id = ?", new String[]{String.valueOf(id)}) > 0;
        } catch (Exception e) {
            return false;
        }
    }

    // Xóa người dùng theo ID (Admin)
    public boolean deleteUser(int id) {
        return getWritableDatabase().delete("users", "id = ?", new String[]{String.valueOf(id)}) > 0;
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
        return insertOrder(code, user, name, phone, address, note, payment, total, count, "", "");
    }

    // Lưu đơn đặt hàng mới kèm tóm tắt sản phẩm
    public long insertOrder(String code, String user, String name, String phone, String address, String note, String payment, String total, int count, String itemsSummary) {
        return insertOrder(code, user, name, phone, address, note, payment, total, count, itemsSummary, "");
    }

    // Lưu đơn đặt hàng mới kèm tóm tắt và mã sản phẩm để hỗ trợ đặt lại
    public long insertOrder(String code, String user, String name, String phone, String address, String note, String payment, String total, int count, String itemsSummary, String itemsRaw) {
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
        cv.put("status_step", 0);
        cv.put("items_summary", itemsSummary != null ? itemsSummary : "");
        cv.put("cancel_reason", "");
        cv.put("items_raw", itemsRaw != null ? itemsRaw : "");
        return getWritableDatabase().insert("orders", null, cv);
    }

    // Lấy danh sách đơn hàng theo user (mới nhất trước)
    public java.util.List<Order> getOrdersByUser(String username) {
        java.util.List<Order> result = new java.util.ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT order_code, username, receiver_name, receiver_phone, receiver_address, "
                        + "receiver_note, payment_method, total_price, item_count, created_at, status_step, "
                        + "COALESCE(items_summary, ''), COALESCE(cancel_reason, ''), COALESCE(items_raw, '') "
                        + "FROM orders WHERE username = ? ORDER BY id DESC",
                new String[]{username});
        try {
            while (c.moveToNext()) {
                result.add(readOrderFromCursor(c));
            }
        } finally {
            c.close();
        }
        return result;
    }

    // Lấy toàn bộ đơn hàng của tất cả người dùng (dành cho Admin)
    public java.util.List<Order> getAllOrders() {
        java.util.List<Order> result = new java.util.ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT order_code, username, receiver_name, receiver_phone, receiver_address, "
                        + "receiver_note, payment_method, total_price, item_count, created_at, status_step, "
                        + "COALESCE(items_summary, ''), COALESCE(cancel_reason, ''), COALESCE(items_raw, '') "
                        + "FROM orders ORDER BY id DESC",
                null);
        try {
            while (c.moveToNext()) {
                result.add(readOrderFromCursor(c));
            }
        } finally {
            c.close();
        }
        return result;
    }

    // Lấy chi tiết một đơn hàng theo mã đơn
    public Order getOrderByCode(String orderCode) {
        if (orderCode == null || orderCode.trim().isEmpty()) {
            return null;
        }
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT order_code, username, receiver_name, receiver_phone, receiver_address, "
                        + "receiver_note, payment_method, total_price, item_count, created_at, status_step, "
                        + "COALESCE(items_summary, ''), COALESCE(cancel_reason, ''), COALESCE(items_raw, '') "
                        + "FROM orders WHERE order_code = ? LIMIT 1",
                new String[]{orderCode.trim()});
        try {
            if (c.moveToFirst()) {
                return readOrderFromCursor(c);
            }
        } finally {
            c.close();
        }
        return null;
    }

    private Order readOrderFromCursor(Cursor c) {
        return new Order(
                c.getString(0),
                c.getString(1),
                c.getString(2),
                c.getString(3),
                c.getString(4),
                c.getString(5),
                c.getString(6),
                c.getString(7),
                c.getInt(8),
                c.getString(9),
                c.getInt(10),
                c.getString(11),
                c.getString(12),
                c.getString(13)
        );
    }

    // Cập nhật trạng thái tiến trình đơn hàng (-1 là Đã hủy, 0..4 là các bước giao hàng)
    public int updateOrderStatus(String orderCode, int statusStep) {
        return updateOrderStatusWithReason(orderCode, statusStep, null);
    }

    // Cập nhật trạng thái tiến trình kèm lý do hủy (nếu có)
    public int updateOrderStatusWithReason(String orderCode, int statusStep, String cancelReason) {
        int clamped = Order.clampStep(statusStep);
        ContentValues cv = new ContentValues();
        cv.put("status_step", clamped);
        if (clamped == Order.STEP_CANCELLED) {
            cv.put("cancel_reason", cancelReason != null ? cancelReason : "");
        } else if (cancelReason != null) {
            cv.put("cancel_reason", cancelReason);
        }
        return getWritableDatabase().update("orders", cv, "order_code = ?", new String[]{orderCode});
    }

    // Đồng bộ đơn hàng từ Firestore về SQLite cục bộ
    public void upsertOrderFromCloud(Order order) {
        if (order == null || order.getOrderCode() == null) {
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put("order_code", order.getOrderCode());
        cv.put("username", order.getUserEmail());
        cv.put("receiver_name", order.getReceiverName());
        cv.put("receiver_phone", order.getReceiverPhone());
        cv.put("receiver_address", order.getReceiverAddress());
        cv.put("receiver_note", order.getReceiverNote());
        cv.put("payment_method", order.getPaymentMethod());
        cv.put("total_price", order.getTotalPrice());
        cv.put("item_count", order.getItemCount());
        cv.put("created_at", order.getCreatedAt());
        cv.put("status_step", order.getStatusStep());
        cv.put("items_summary", order.getItemsSummary());
        cv.put("cancel_reason", order.getCancelReason());
        if (order.getItemsRaw() != null && !order.getItemsRaw().isEmpty()) {
            cv.put("items_raw", order.getItemsRaw());
        }
        SQLiteDatabase db = getWritableDatabase();
        int updated = db.update("orders", cv, "order_code = ?", new String[]{order.getOrderCode()});
        if (updated == 0) {
            if (!cv.containsKey("items_raw")) {
                cv.put("items_raw", "");
            }
            db.insert("orders", null, cv);
        }
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
