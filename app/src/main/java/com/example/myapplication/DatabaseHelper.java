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
    private static final int DATABASE_VERSION = 8;

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

        //bảng đánh giá và bình luận sản phẩm
        db.execSQL("CREATE TABLE IF NOT EXISTS product_reviews ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "sku TEXT, username TEXT, rating INTEGER, comment TEXT, "
                + "created_at TEXT, verified INTEGER DEFAULT 1, helpful_count INTEGER DEFAULT 0)");

        //bảng danh sách yêu thích (Wishlist) theo tài khoản
        db.execSQL("CREATE TABLE IF NOT EXISTS wishlist ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT, sku TEXT, created_at TEXT, "
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
        // Nạp sẵn đánh giá mẫu cho các sản phẩm nếu bảng còn trống
        seedDefaultReviews(db);
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

    // ==================== HỆ THỐNG ĐÁNH GIÁ & BÌNH LUẬN SẢN PHẨM ====================

    private void seedDefaultReviews(SQLiteDatabase db) {
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM product_reviews", null);
        int count = 0;
        if (c != null) {
            if (c.moveToFirst()) count = c.getInt(0);
            c.close();
        }
        if (count > 0) return;

        insertSeedReview(db, "TZ-IP16PM-256", "minhkhoa.tech", 5, "[Hàng chính hãng • Hiệu năng đỉnh] Máy nguyên seal VN/A, màu Titan Tự Nhiên cực sang. Màn 6.9 inch 120Hz vuốt siêu mượt, camera zoom 5x nét căng!", "18/11/2026 14:20", 1, 14);
        insertSeedReview(db, "TZ-IP16PM-256", "hoanganh_99", 5, "[Giao nhanh 2h • Đáng tiền] Đặt buổi sáng trưa đã nhận được hàng. Pin trâu hơn đời 15 Pro Max rõ rệt, chip A18 Pro mát rượi.", "19/11/2026 09:15", 1, 8);
        insertSeedReview(db, "TZ-IP16PM-256", "tuananh.dev", 4, "[Đóng gói kỹ] Máy đẹp xuất sắc, nhân viên hỗ trợ kích hoạt bảo hành Apple VN đầy đủ.", "19/11/2026 19:40", 1, 5);

        insertSeedReview(db, "TZ-S24U-512", "quocbao.sfan", 5, "[Hiệu năng đỉnh • Màn hình chống chói] Lớp phủ chống lóa Gorilla Armor trên S24 Ultra quá đỉnh, dùng ngoài trời nắng vẫn rõ mồn một. S-Pen ghi chú cực nhạy.", "17/11/2026 11:30", 1, 11);
        insertSeedReview(db, "TZ-S24U-512", "linhchi.design", 5, "[Camera 200MP • Chính hãng] Chụp concert zoom 5x - 10x nét không đối thủ, Galaxy AI dịch trực tiếp rất tiện.", "18/11/2026 20:05", 1, 7);

        insertSeedReview(db, "TZ-MBA-M3-16", "ducmanh.coder", 5, "[Bản 16GB RAM quá ngon • Pin trâu] Mở 30 tab Chrome + Android Studio vẫn mượt mà không ấm máy. Pin dùng làm việc cả ngày ở quán cafe không cần mang sạc.", "16/11/2026 16:45", 1, 19);
        insertSeedReview(db, "TZ-MBA-M3-16", "thao.marketing", 5, "[Mỏng nhẹ • Sang trọng] Máy nhẹ chỉ hơn 1.2kg bỏ túi xách rất gọn, màn hình Liquid Retina màu chuẩn.", "18/11/2026 10:12", 1, 6);

        insertSeedReview(db, "TZ-IPADP-M4", "huyart.studio", 5, "[Màn OLED kép tuyệt đẹp • Siêu mỏng] Cầm trên tay mỏng đến khó tin (5.3mm), màn Tandem OLED màu đen sâu thẳm, vẽ Procreate bằng Pencil Pro không độ trễ.", "17/11/2026 15:20", 1, 12);
        insertSeedReview(db, "TZ-IPADP-M4", "ngocanh.media", 5, "[Hiệu năng M4 khủng] Render video 4K trên LumaFusion nhanh ngang laptop chuyên nghiệp.", "19/11/2026 08:50", 1, 4);

        insertSeedReview(db, "TZ-APP2-USBC", "khanhvy.music", 5, "[Chống ồn đỉnh cao • Cổng USB-C tiện] Bật ANC lên là tách biệt hoàn toàn tiếng ồn ngoài đường. Chế độ xuyên âm nghe tự nhiên như không đeo tai nghe.", "15/11/2026 13:10", 1, 9);
        insertSeedReview(db, "TZ-APP2-USBC", "nam.nguyen", 4, "[Âm bass chắc • Kết nối mượt] Dùng chung hệ sinh thái Apple chuyển đổi giữa iPhone và MacBook cực nhanh.", "18/11/2026 17:30", 1, 3);

        insertSeedReview(db, "TZ-SN-XM5", "audiophile.vn", 5, "[Vua chống ồn • Đeo êm tai] Đệm tai da mềm đeo 4 tiếng liên tục không đau tai, chất âm LDAC chi tiết và ấm áp.", "16/11/2026 21:00", 1, 10);
        insertSeedReview(db, "TZ-SN-XM5", "hoangnam.it", 5, "[Micro đàm thoại rõ] Họp online ở quán đông người mà đồng nghiệp không nghe thấy tạp âm xung quanh.", "19/11/2026 11:05", 1, 6);

        insertSeedReview(db, "TZ-AWS10-42", "phuongthao.fit", 5, "[Màn rộng • Sạc siêu nhanh] Thiết kế mỏng hơn hẳn đời trước, đeo ngủ theo dõi sức khỏe rất thoải mái. Sạc 30 phút đã đầy 80% pin.", "18/11/2026 07:45", 1, 8);
        insertSeedReview(db, "TZ-KC-K8PRO", "vanhung.mech", 5, "[Gõ đầm tay • Khung nhôm CNC] Đã lót sẵn foam tiêu âm từ hãng, tiếng gõ thocky rất đã, kết nối Bluetooth 3 thiết bị chuyển mượt.", "17/11/2026 22:15", 1, 13);
        insertSeedReview(db, "TZ-LG-MX3S", "minhtri.pm", 5, "[Chuột văn phòng số 1 • Click siêu êm] Con lăn vô cực MagSpeed cuộn file Excel hàng ngàn dòng cực sướng, nút Quiet Click bấm đêm không ồn.", "16/11/2026 14:00", 1, 16);
        insertSeedReview(db, "TZ-MS-EMB2", "baolong.chill", 5, "[Thiết kế cổ điển • Pin 30h] Để decor bàn làm việc cực chất, âm thanh 360 độ lan tỏa đều khắp phòng.", "18/11/2026 18:20", 1, 7);
        insertSeedReview(db, "TZ-MS-MJ4", "lanhuong. acoustic", 5, "[Pin 80 giờ vô địch] Dùng cả tháng mới phải sạc 1 lần, nghe nhạc Rock và Acoustic rất hợp.", "17/11/2026 12:40", 1, 6);
        insertSeedReview(db, "TZ-JBL-CHG5", "thanhdat.travel", 5, "[Bass uy lực • Chống nước IP67] Mang đi dã ngoại ngoài trời âm lượng lớn, bass đánh chắc khỏe, có cổng sạc ngược cho điện thoại luôn.", "19/11/2026 10:30", 1, 9);
    }

    private void insertSeedReview(SQLiteDatabase db, String sku, String username, int rating,
                                  String comment, String createdAt, int verified, int helpful) {
        ContentValues cv = new ContentValues();
        cv.put("sku", sku);
        cv.put("username", username);
        cv.put("rating", rating);
        cv.put("comment", comment);
        cv.put("created_at", createdAt);
        cv.put("verified", verified);
        cv.put("helpful_count", helpful);
        db.insert("product_reviews", null, cv);
    }

    public java.util.List<ProductReview> getReviewsBySku(String sku) {
        java.util.List<ProductReview> list = new java.util.ArrayList<>();
        if (sku == null || sku.trim().isEmpty()) return list;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, sku, username, rating, comment, created_at, verified, helpful_count "
                        + "FROM product_reviews WHERE sku = ? ORDER BY id DESC",
                new String[]{sku.trim()});
        if (c != null) {
            while (c.moveToNext()) {
                list.add(new ProductReview(
                        c.getInt(0),
                        c.getString(1),
                        c.getString(2),
                        c.getInt(3),
                        c.getString(4),
                        c.getString(5),
                        c.getInt(6) == 1,
                        c.getInt(7)
                ));
            }
            c.close();
        }
        return list;
    }

    public long addProductReview(String sku, String username, int rating, String comment, boolean verified) {
        if (sku == null || sku.trim().isEmpty() || comment == null || comment.trim().isEmpty()) {
            return -1;
        }
        ContentValues cv = new ContentValues();
        cv.put("sku", sku.trim());
        cv.put("username", (username == null || username.trim().isEmpty()) ? "Khách hàng" : username.trim());
        cv.put("rating", Math.max(1, Math.min(5, rating)));
        cv.put("comment", comment.trim());
        cv.put("created_at", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
        cv.put("verified", verified ? 1 : 0);
        cv.put("helpful_count", 0);
        return getWritableDatabase().insert("product_reviews", null, cv);
    }

    public void incrementReviewHelpful(int reviewId) {
        getWritableDatabase().execSQL(
                "UPDATE product_reviews SET helpful_count = helpful_count + 1 WHERE id = ?",
                new Object[]{reviewId});
    }

    public boolean deleteReview(int reviewId) {
        return getWritableDatabase().delete("product_reviews", "id = ?", new String[]{String.valueOf(reviewId)}) > 0;
    }

    public float getAverageRating(String sku) {
        if (sku == null || sku.trim().isEmpty()) return 4.9f;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT AVG(rating), COUNT(*) FROM product_reviews WHERE sku = ?",
                new String[]{sku.trim()});
        float avg = 4.9f;
        if (c != null) {
            if (c.moveToFirst() && c.getInt(1) > 0) {
                avg = c.getFloat(0);
            }
            c.close();
        }
        return Math.round(avg * 10f) / 10f;
    }

    public int getReviewCount(String sku) {
        if (sku == null || sku.trim().isEmpty()) return 0;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM product_reviews WHERE sku = ?",
                new String[]{sku.trim()});
        int count = 0;
        if (c != null) {
            if (c.moveToFirst()) count = c.getInt(0);
            c.close();
        }
        return count;
    }

    public int[] getRatingBreakdown(String sku) {
        int[] counts = new int[6]; // index 1..5
        if (sku == null || sku.trim().isEmpty()) return counts;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT rating, COUNT(*) FROM product_reviews WHERE sku = ? GROUP BY rating",
                new String[]{sku.trim()});
        if (c != null) {
            while (c.moveToNext()) {
                int r = c.getInt(0);
                if (r >= 1 && r <= 5) {
                    counts[r] = c.getInt(1);
                }
            }
            c.close();
        }
        return counts;
    }

    public boolean hasUserPurchasedProduct(String username, String sku) {
        if (username == null || username.trim().isEmpty() || sku == null || sku.trim().isEmpty()) {
            return false;
        }
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT items_raw FROM orders WHERE LOWER(username) = LOWER(?) AND status_step != -1",
                new String[]{username.trim()});
        boolean purchased = false;
        if (c != null) {
            while (c.moveToNext()) {
                String raw = c.getString(0);
                if (raw != null && raw.contains(sku.trim())) {
                    purchased = true;
                    break;
                }
            }
            c.close();
        }
        return purchased;
    }

    // ==================== DANH SÁCH YÊU THÍCH (WISHLIST) ====================

    public boolean isWishlisted(String username, String sku) {
        if (username == null || username.trim().isEmpty() || sku == null || sku.trim().isEmpty()) {
            return false;
        }
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT id FROM wishlist WHERE LOWER(username) = LOWER(?) AND sku = ? LIMIT 1",
                new String[]{username.trim(), sku.trim()});
        boolean exists = c != null && c.moveToFirst();
        if (c != null) c.close();
        return exists;
    }

    public boolean toggleWishlist(String username, String sku) {
        if (username == null || username.trim().isEmpty() || sku == null || sku.trim().isEmpty()) {
            return false;
        }
        String cleanUser = username.trim();
        String cleanSku = sku.trim();
        if (isWishlisted(cleanUser, cleanSku)) {
            getWritableDatabase().delete("wishlist", "LOWER(username) = LOWER(?) AND sku = ?",
                    new String[]{cleanUser, cleanSku});
            return false;
        } else {
            ContentValues cv = new ContentValues();
            cv.put("username", cleanUser);
            cv.put("sku", cleanSku);
            cv.put("created_at", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
            getWritableDatabase().insertWithOnConflict("wishlist", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            return true;
        }
    }

    public java.util.Set<String> getWishlistedSkus(String username) {
        java.util.Set<String> set = new java.util.HashSet<>();
        if (username == null || username.trim().isEmpty()) return set;
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT sku FROM wishlist WHERE LOWER(username) = LOWER(?)",
                new String[]{username.trim()});
        if (c != null) {
            while (c.moveToNext()) {
                set.add(c.getString(0));
            }
            c.close();
        }
        return set;
    }

    // ==================== CHỈ SỐ THƯƠNG MẠI ĐIỆN TỬ (SALE / ĐÃ BÁN) ====================

    public static int getDiscountPercent(Product product) {
        if (product == null || product.getSku() == null) return 10;
        int hash = Math.abs(product.getSku().hashCode());
        int[] presets = new int[]{10, 12, 15, 8, 14, 18, 11};
        return presets[hash % presets.length];
    }

    public static String getOriginalPriceFormatted(Product product) {
        if (product == null) return "";
        long current = CartManager.parsePrice(product.getPrice());
        if (current <= 0) return "";
        int discount = getDiscountPercent(product);
        long original = Math.round((current * 100.0) / (100 - discount) / 10000.0) * 10000L;
        return CartManager.formatPrice(original);
    }

    public static String getSoldCountText(Product product) {
        if (product == null || product.getSku() == null) return "Đã bán 85";
        int hash = Math.abs(product.getSku().hashCode());
        int sold = 48 + (hash % 240);
        return "Đã bán " + sold;
    }
}
