package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "TechStore.db";
    private static final int DATABASE_VERSION = 8;

    private static final String[] PRODUCT_NAMES = {
            "iPhone 16 Pro Max", "Samsung Galaxy S24 Ultra", "MacBook Air M3 13 inch",
            "iPad Pro M4 11 inch", "Tai nghe AirPods Pro 2", "Tai nghe Sony WH-1000XM5",
            "Apple Watch Series 10", "Bàn phím cơ Keychron K8 Pro", "Chuột Logitech MX Master 3S",
            "Loa Marshall Emberton II", "Tai nghe Marshall Major IV", "Loa JBL Charge 5"
    };

    private static final String[] IMAGE_KEYS = {
            "img_iphone_16", "img_samsung_s24", "img_macbook_m3",
            "img_ipad_pro_m4", "img_airpods_pro", "img_sony_wh1000xm5",
            "img_apple_watch_series10", "img_keychron", "img_logitech_mx",
            "img_marshall_emberton", "img_marshall_major", "img_jbl_charge5"
    };

    private static final int[] IMAGE_RES_IDS = {
            R.drawable.img_iphone_16, R.drawable.img_samsung_s24, R.drawable.img_macbook_m3,
            R.drawable.img_ipad_pro_m4, R.drawable.img_airpods_pro, R.drawable.img_sony_wh1000xm5,
            R.drawable.img_apple_watch_series10, R.drawable.img_keychron, R.drawable.img_logitech_mx,
            R.drawable.img_marshall_emberton, R.drawable.img_marshall_major, R.drawable.img_jbl_charge5
    };

    private static final String ORDER_COLS =
            "SELECT order_code, username, receiver_name, receiver_phone, receiver_address, "
                    + "receiver_note, payment_method, total_price, item_count, created_at, status_step, "
                    + "COALESCE(items_summary, ''), COALESCE(cancel_reason, ''), COALESCE(items_raw, '') FROM orders ";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT, role TEXT DEFAULT 'user')");
        ensureColumn(db, "users", "role", "TEXT DEFAULT 'user'");

        db.execSQL("CREATE TABLE IF NOT EXISTS products ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, description TEXT, price TEXT, image_res_id INTEGER, "
                + "category TEXT, sku TEXT UNIQUE, warranty TEXT, detailed_specs TEXT, in_stock INTEGER, "
                + "image_path TEXT DEFAULT '', image_base64 TEXT DEFAULT '', image_key TEXT DEFAULT '')");
        ensureColumn(db, "products", "image_path", "TEXT DEFAULT ''");
        ensureColumn(db, "products", "image_base64", "TEXT DEFAULT ''");
        ensureColumn(db, "products", "image_key", "TEXT DEFAULT ''");

        db.execSQL("CREATE TABLE IF NOT EXISTS orders ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, order_code TEXT UNIQUE, username TEXT, receiver_name TEXT, "
                + "receiver_phone TEXT, receiver_address TEXT, receiver_note TEXT, payment_method TEXT, "
                + "total_price TEXT, item_count INTEGER, created_at TEXT, status_step INTEGER DEFAULT 0, "
                + "items_summary TEXT DEFAULT '', cancel_reason TEXT DEFAULT '', items_raw TEXT DEFAULT '')");
        ensureColumn(db, "orders", "status_step", "INTEGER DEFAULT 0");
        ensureColumn(db, "orders", "items_summary", "TEXT DEFAULT ''");
        ensureColumn(db, "orders", "cancel_reason", "TEXT DEFAULT ''");
        ensureColumn(db, "orders", "items_raw", "TEXT DEFAULT ''");

        db.execSQL("CREATE TABLE IF NOT EXISTS cart_items ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, sku TEXT, product_name TEXT, price TEXT, "
                + "quantity INTEGER, UNIQUE(username, sku))");

        db.execSQL("CREATE TABLE IF NOT EXISTS product_reviews ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, sku TEXT, username TEXT, rating INTEGER, comment TEXT, "
                + "created_at TEXT, verified INTEGER DEFAULT 1, helpful_count INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE IF NOT EXISTS wishlist ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, sku TEXT, created_at TEXT, UNIQUE(username, sku))");

        ContentValues admin = userValues("admin", "123456", UserAccount.ROLE_ADMIN);
        db.insertWithOnConflict("users", null, admin, SQLiteDatabase.CONFLICT_IGNORE);
        db.execSQL("UPDATE users SET role = 'admin' WHERE LOWER(username) = 'admin'");

        for (Product p : ProductListActivity.createDefaultProducts()) {
            db.insertWithOnConflict("products", null, productValues(p), SQLiteDatabase.CONFLICT_IGNORE);
        }
        seedDefaultReviews(db);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int o, int n) { onCreate(db); }
    @Override public void onOpen(SQLiteDatabase db) { super.onOpen(db); onCreate(db); }

    private void ensureColumn(SQLiteDatabase db, String table, String column, String def) {
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int idx = c.getColumnIndex("name");
            while (c.moveToNext()) {
                if (idx >= 0 && column.equals(c.getString(idx))) return;
            }
        }
        db.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + def);
    }

    public static int getImageForProduct(String name, String category) {
        if (name != null) {
            String clean = name.trim();
            for (int i = 0; i < PRODUCT_NAMES.length; i++) {
                if (PRODUCT_NAMES[i].equals(clean)) return IMAGE_RES_IDS[i];
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
            String cleanKey = key.trim();
            if ("ic_store_24".equals(cleanKey)) return R.drawable.ic_store_24;
            for (int i = 0; i < IMAGE_KEYS.length; i++) {
                if (IMAGE_KEYS[i].equals(cleanKey)) return IMAGE_RES_IDS[i];
            }
        }
        return getImageForProduct(name, category);
    }

    public static String getKeyForDrawable(int resId) {
        for (int i = 0; i < IMAGE_RES_IDS.length; i++) {
            if (IMAGE_RES_IDS[i] == resId) return IMAGE_KEYS[i];
        }
        return "ic_store_24";
    }

    private static String nowStr() {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
    }

    // SẢN PHẨM
    private static ContentValues productValues(Product p) {
        String key = (p.getImageKey() != null && !p.getImageKey().isEmpty())
                ? p.getImageKey() : getKeyForDrawable(p.getImageResId());
        ContentValues cv = new ContentValues();
        cv.put("name", p.getName());
        cv.put("description", p.getDescription());
        cv.put("price", p.getPrice());
        cv.put("image_res_id", getImageByKeyOrProduct(key, p.getName(), p.getCategory()));
        cv.put("category", p.getCategory());
        cv.put("sku", p.getSku().trim());
        cv.put("warranty", p.getWarranty());
        cv.put("detailed_specs", p.getDetailedSpecs());
        cv.put("in_stock", p.isInStock() ? 1 : 0);
        cv.put("image_path", p.getImagePath());
        cv.put("image_base64", p.getImageBase64());
        cv.put("image_key", key);
        return cv;
    }

    public ArrayList<Product> getAllProducts() {
        ArrayList<Product> list = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT name, description, price, image_res_id, category, sku, warranty, detailed_specs, in_stock, "
                        + "COALESCE(image_path, ''), COALESCE(image_base64, ''), COALESCE(image_key, '') "
                        + "FROM products ORDER BY id ASC", null)) {
            while (c.moveToNext()) {
                String name = c.getString(0), cat = c.getString(4), key = c.getString(11);
                Product p = new Product(name, c.getString(1), c.getString(2),
                        getImageByKeyOrProduct(key, name, cat), cat, c.getString(5),
                        c.getString(6), c.getString(7), c.getInt(8) == 1);
                p.setImagePath(c.getString(9));
                p.setImageBase64(c.getString(10));
                p.setImageKey(key);
                list.add(p);
            }
        }
        return list;
    }

    public boolean upsertProduct(Product p) {
        if (p == null || p.getSku() == null || p.getSku().trim().isEmpty()) return false;
        ContentValues cv = productValues(p);
        SQLiteDatabase db = getWritableDatabase();
        return db.update("products", cv, "sku = ?", new String[]{p.getSku().trim()}) > 0
                || db.insert("products", null, cv) != -1;
    }

    public boolean deleteProductBySku(String sku) {
        return sku != null && !sku.trim().isEmpty()
                && getWritableDatabase().delete("products", "sku = ?", new String[]{sku.trim()}) > 0;
    }

    // NGƯỜI DÙNG
    private static ContentValues userValues(String username, String password, String role) {
        String clean = username.trim();
        ContentValues cv = new ContentValues();
        cv.put("username", clean);
        cv.put("password", password);
        cv.put("role", UserAccount.normalizeRole(role, clean));
        return cv;
    }

    public boolean registerUser(String username, String password) {
        return addUser(username, password, UserAccount.ROLE_USER);
    }

    public boolean addUser(String username, String password, String role) {
        return username != null && !username.trim().isEmpty() && password != null && !password.isEmpty()
                && getWritableDatabase().insert("users", null, userValues(username, password, role)) != -1;
    }

    public boolean isAdminUser(String username) {
        if (username == null || username.trim().isEmpty()) return false;
        String clean = username.trim();
        if ("admin".equalsIgnoreCase(clean)) return true;
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT role FROM users WHERE LOWER(username) = LOWER(?)", new String[]{clean})) {
            return c.moveToFirst() && UserAccount.ROLE_ADMIN.equalsIgnoreCase(c.getString(0));
        }
    }

    public List<UserAccount> getAllUsers() {
        List<UserAccount> users = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, username, password, COALESCE(role, 'user') FROM users ORDER BY id ASC", null)) {
            while (c.moveToNext()) users.add(new UserAccount(c.getInt(0), c.getString(1), c.getString(2), c.getString(3)));
        }
        return users;
    }

    public boolean updateUser(int id, String username, String password, String role) {
        if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) return false;
        try {
            return getWritableDatabase().update("users", userValues(username, password, role), "id = ?", new String[]{String.valueOf(id)}) > 0;
        } catch (Exception e) { return false; }
    }

    public boolean deleteUser(int id) {
        return getWritableDatabase().delete("users", "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    public boolean checkUserExists(String username) {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT id FROM users WHERE username = ?", new String[]{username})) {
            return c.moveToFirst();
        }
    }

    public boolean checkLogin(String username, String password) {
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id FROM users WHERE username = ? AND password = ?", new String[]{username, password})) {
            return c.moveToFirst();
        }
    }

    // ĐƠN HÀNG & GIỎ HÀNG
    public void insertOrder(String code, String user, String name, String phone, String address,
                            String note, String payment, String total, int count, String itemsSummary, String itemsRaw) {
        upsertOrderFromCloud(new Order(code, user, name, phone, address, note, payment, total, count,
                nowStr(), 0, itemsSummary, "", itemsRaw));
    }

    public List<Order> getOrdersByUser(String username) {
        return queryOrders("WHERE username = ? ORDER BY id DESC", new String[]{username});
    }

    public List<Order> getAllOrders() {
        return queryOrders("ORDER BY id DESC", null);
    }

    public Order getOrderByCode(String orderCode) {
        if (orderCode == null || orderCode.trim().isEmpty()) return null;
        List<Order> found = queryOrders("WHERE order_code = ? LIMIT 1", new String[]{orderCode.trim()});
        return found.isEmpty() ? null : found.get(0);
    }

    private List<Order> queryOrders(String suffix, String[] args) {
        List<Order> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(ORDER_COLS + suffix, args)) {
            while (c.moveToNext()) {
                result.add(new Order(c.getString(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4),
                        c.getString(5), c.getString(6), c.getString(7), c.getInt(8), c.getString(9),
                        c.getInt(10), c.getString(11), c.getString(12), c.getString(13)));
            }
        }
        return result;
    }

    public int updateOrderStatus(String orderCode, int statusStep) {
        return updateOrderStatusWithReason(orderCode, statusStep, null);
    }

    public int updateOrderStatusWithReason(String orderCode, int statusStep, String cancelReason) {
        int clamped = Order.clampStep(statusStep);
        ContentValues cv = new ContentValues();
        cv.put("status_step", clamped);
        if (clamped == Order.STEP_CANCELLED) cv.put("cancel_reason", cancelReason != null ? cancelReason : "");
        else if (cancelReason != null) cv.put("cancel_reason", cancelReason);
        return getWritableDatabase().update("orders", cv, "order_code = ?", new String[]{orderCode});
    }

    public void upsertOrderFromCloud(Order o) {
        if (o == null || o.getOrderCode() == null) return;
        ContentValues cv = new ContentValues();
        cv.put("order_code", o.getOrderCode());
        cv.put("username", o.getUserEmail());
        cv.put("receiver_name", o.getReceiverName());
        cv.put("receiver_phone", o.getReceiverPhone());
        cv.put("receiver_address", o.getReceiverAddress());
        cv.put("receiver_note", o.getReceiverNote());
        cv.put("payment_method", o.getPaymentMethod());
        cv.put("total_price", o.getTotalPrice());
        cv.put("item_count", o.getItemCount());
        cv.put("created_at", o.getCreatedAt());
        cv.put("status_step", o.getStatusStep());
        cv.put("items_summary", o.getItemsSummary());
        cv.put("cancel_reason", o.getCancelReason());
        if (o.getItemsRaw() != null && !o.getItemsRaw().isEmpty()) cv.put("items_raw", o.getItemsRaw());

        SQLiteDatabase db = getWritableDatabase();
        if (db.update("orders", cv, "order_code = ?", new String[]{o.getOrderCode()}) == 0) {
            if (!cv.containsKey("items_raw")) cv.put("items_raw", "");
            db.insert("orders", null, cv);
        }
    }

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

    public ArrayList<CartItem> getCartItemsByUser(String username) {
        ArrayList<CartItem> list = new ArrayList<>();
        if (username == null || username.trim().isEmpty()) return list;
        ArrayList<Product> allProducts = getAllProducts();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT sku, quantity FROM cart_items WHERE username = ? ORDER BY id ASC", new String[]{username})) {
            while (c.moveToNext()) {
                String sku = c.getString(0);
                int qty = c.getInt(1);
                for (Product p : allProducts) {
                    if (p.getSku().equals(sku)) { list.add(new CartItem(p, qty)); break; }
                }
            }
        }
        return list;
    }

    public void clearUserCart(String username) {
        if (username != null && !username.trim().isEmpty()) {
            getWritableDatabase().delete("cart_items", "username = ?", new String[]{username});
        }
    }

    // ĐÁNH GIÁ & BÌNH LUẬN
    private void seedDefaultReviews(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM product_reviews", null)) {
            if (c.moveToFirst() && c.getInt(0) > 0) return;
        }
        String[][] seeds = {
                {"TZ-IP16PM-256", "minhkhoa.tech", "5", "[Hàng chính hãng • Hiệu năng đỉnh] Máy nguyên seal VN/A, màu Titan Tự Nhiên cực sang. Màn 6.9 inch 120Hz vuốt siêu mượt, camera zoom 5x nét căng!", "18/11/2026 14:20", "14"},
                {"TZ-IP16PM-256", "hoanganh_99", "5", "[Giao nhanh 2h • Đáng tiền] Đặt buổi sáng trưa đã nhận được hàng. Pin trâu hơn đời 15 Pro Max rõ rệt, chip A18 Pro mát rượi.", "19/11/2026 09:15", "8"},
                {"TZ-IP16PM-256", "tuananh.dev", "4", "[Đóng gói kỹ] Máy đẹp xuất sắc, nhân viên hỗ trợ kích hoạt bảo hành Apple VN đầy đủ.", "19/11/2026 19:40", "5"},
                {"TZ-S24U-512", "quocbao.sfan", "5", "[Hiệu năng đỉnh • Màn hình chống chói] Lớp phủ chống lóa Gorilla Armor trên S24 Ultra quá đỉnh, dùng ngoài trời nắng vẫn rõ mồn một.", "17/11/2026 11:30", "11"},
                {"TZ-S24U-512", "linhchi.design", "5", "[Camera 200MP • Chính hãng] Chụp concert zoom 5x - 10x nét không đối thủ, Galaxy AI dịch trực tiếp rất tiện.", "18/11/2026 20:05", "7"},
                {"TZ-MBA-M3-16", "ducmanh.coder", "5", "[Bản 16GB RAM quá ngon • Pin trâu] Mở 30 tab Chrome + Android Studio vẫn mượt mà không ấm máy. Pin dùng làm việc cả ngày.", "16/11/2026 16:45", "19"},
                {"TZ-MBA-M3-16", "thao.marketing", "5", "[Mỏng nhẹ • Sang trọng] Máy nhẹ chỉ hơn 1.2kg bỏ túi xách rất gọn, màn hình Liquid Retina màu chuẩn.", "18/11/2026 10:12", "6"},
                {"TZ-IPADP-M4", "huyart.studio", "5", "[Màn OLED kép tuyệt đẹp • Siêu mỏng] Cầm trên tay mỏng đến khó tin (5.3mm), màn Tandem OLED màu đen sâu thẳm.", "17/11/2026 15:20", "12"},
                {"TZ-IPADP-M4", "ngocanh.media", "5", "[Hiệu năng M4 khủng] Render video 4K trên LumaFusion nhanh ngang laptop chuyên nghiệp.", "19/11/2026 08:50", "4"},
                {"TZ-APP2-USBC", "khanhvy.music", "5", "[Chống ồn đỉnh cao • Cổng USB-C tiện] Bật ANC lên là tách biệt hoàn toàn tiếng ồn ngoài đường.", "15/11/2026 13:10", "9"},
                {"TZ-APP2-USBC", "nam.nguyen", "4", "[Âm bass chắc • Kết nối mượt] Dùng chung hệ sinh thái Apple chuyển đổi giữa iPhone và MacBook cực nhanh.", "18/11/2026 17:30", "3"},
                {"TZ-SN-XM5", "audiophile.vn", "5", "[Vua chống ồn • Đeo êm tai] Đệm tai da mềm đeo 4 tiếng liên tục không đau tai, chất âm LDAC chi tiết.", "16/11/2026 21:00", "10"},
                {"TZ-SN-XM5", "hoangnam.it", "5", "[Micro đàm thoại rõ] Họp online ở quán đông người mà đồng nghiệp không nghe thấy tạp âm xung quanh.", "19/11/2026 11:05", "6"},
                {"TZ-AWS10-42", "phuongthao.fit", "5", "[Màn rộng • Sạc siêu nhanh] Thiết kế mỏng hơn hẳn đời trước, đeo ngủ theo dõi sức khỏe rất thoải mái.", "18/11/2026 07:45", "8"},
                {"TZ-KC-K8PRO", "vanhung.mech", "5", "[Gõ đầm tay • Khung nhôm CNC] Đã lót sẵn foam tiêu âm từ hãng, tiếng gõ rất đã, kết nối Bluetooth 3 thiết bị.", "17/11/2026 22:15", "13"},
                {"TZ-LG-MX3S", "minhtri.pm", "5", "[Chuột văn phòng số 1 • Click siêu êm] Con lăn vô cực MagSpeed cuộn file Excel cực sướng, nút Quiet Click không ồn.", "16/11/2026 14:00", "16"},
                {"TZ-MS-EMB2", "baolong.chill", "5", "[Thiết kế cổ điển • Pin 30h] Để decor bàn làm việc cực chất, âm thanh 360 độ lan tỏa đều khắp phòng.", "18/11/2026 18:20", "7"},
                {"TZ-MS-MJ4", "lanhuong.acoustic", "5", "[Pin 80 giờ vô địch] Dùng cả tháng mới phải sạc 1 lần, nghe nhạc Rock và Acoustic rất hợp.", "17/11/2026 12:40", "6"},
                {"TZ-JBL-CHG5", "thanhdat.travel", "5", "[Bass uy lực • Chống nước IP67] Mang đi dã ngoại ngoài trời âm lượng lớn, bass đánh chắc khỏe.", "19/11/2026 10:30", "9"}
        };
        for (String[] row : seeds) {
            ContentValues cv = new ContentValues();
            cv.put("sku", row[0]);
            cv.put("username", row[1]);
            cv.put("rating", Integer.parseInt(row[2]));
            cv.put("comment", row[3]);
            cv.put("created_at", row[4]);
            cv.put("verified", 1);
            cv.put("helpful_count", Integer.parseInt(row[5]));
            db.insert("product_reviews", null, cv);
        }
    }

    public List<ProductReview> getReviewsBySku(String sku) {
        List<ProductReview> list = new ArrayList<>();
        if (sku == null || sku.trim().isEmpty()) return list;
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, sku, username, rating, comment, created_at, verified, helpful_count "
                        + "FROM product_reviews WHERE sku = ? ORDER BY id DESC", new String[]{sku.trim()})) {
            while (c.moveToNext()) {
                list.add(new ProductReview(c.getInt(0), c.getString(1), c.getString(2), c.getInt(3),
                        c.getString(4), c.getString(5), c.getInt(6) == 1, c.getInt(7)));
            }
        }
        return list;
    }

    public long addProductReview(String sku, String username, int rating, String comment, boolean verified) {
        if (sku == null || sku.trim().isEmpty() || comment == null || comment.trim().isEmpty()) return -1;
        ContentValues cv = new ContentValues();
        cv.put("sku", sku.trim());
        cv.put("username", (username == null || username.trim().isEmpty()) ? "Khách hàng" : username.trim());
        cv.put("rating", Math.max(1, Math.min(5, rating)));
        cv.put("comment", comment.trim());
        cv.put("created_at", nowStr());
        cv.put("verified", verified ? 1 : 0);
        cv.put("helpful_count", 0);
        return getWritableDatabase().insert("product_reviews", null, cv);
    }

    public void incrementReviewHelpful(int reviewId) {
        getWritableDatabase().execSQL("UPDATE product_reviews SET helpful_count = helpful_count + 1 WHERE id = ?", new Object[]{reviewId});
    }

    public boolean deleteReview(int reviewId) {
        return getWritableDatabase().delete("product_reviews", "id = ?", new String[]{String.valueOf(reviewId)}) > 0;
    }

    public float getAverageRating(String sku) {
        if (sku == null || sku.trim().isEmpty()) return 4.9f;
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT AVG(rating), COUNT(*) FROM product_reviews WHERE sku = ?", new String[]{sku.trim()})) {
            if (c.moveToFirst() && c.getInt(1) > 0) return Math.round(c.getFloat(0) * 10f) / 10f;
        }
        return 4.9f;
    }

    public int getReviewCount(String sku) {
        if (sku == null || sku.trim().isEmpty()) return 0;
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM product_reviews WHERE sku = ?", new String[]{sku.trim()})) {
            return c.moveToFirst() ? c.getInt(0) : 0;
        }
    }

    public int[] getRatingBreakdown(String sku) {
        int[] counts = new int[6];
        if (sku == null || sku.trim().isEmpty()) return counts;
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT rating, COUNT(*) FROM product_reviews WHERE sku = ? GROUP BY rating", new String[]{sku.trim()})) {
            while (c.moveToNext()) {
                int r = c.getInt(0);
                if (r >= 1 && r <= 5) counts[r] = c.getInt(1);
            }
        }
        return counts;
    }

    public boolean hasUserPurchasedProduct(String username, String sku) {
        if (username == null || username.trim().isEmpty() || sku == null || sku.trim().isEmpty()) return false;
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT items_raw FROM orders WHERE LOWER(username) = LOWER(?) AND status_step != -1", new String[]{username.trim()})) {
            while (c.moveToNext()) {
                String raw = c.getString(0);
                if (raw != null && raw.contains(sku.trim())) return true;
            }
        }
        return false;
    }

    // DANH SÁCH YÊU THÍCH & CHỈ SỐ SALE
    public boolean isWishlisted(String username, String sku) {
        if (username == null || username.trim().isEmpty() || sku == null || sku.trim().isEmpty()) return false;
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id FROM wishlist WHERE LOWER(username) = LOWER(?) AND sku = ? LIMIT 1",
                new String[]{username.trim(), sku.trim()})) {
            return c.moveToFirst();
        }
    }

    public boolean toggleWishlist(String username, String sku) {
        if (username == null || username.trim().isEmpty() || sku == null || sku.trim().isEmpty()) return false;
        String u = username.trim(), s = sku.trim();
        if (isWishlisted(u, s)) {
            getWritableDatabase().delete("wishlist", "LOWER(username) = LOWER(?) AND sku = ?", new String[]{u, s});
            return false;
        }
        ContentValues cv = new ContentValues();
        cv.put("username", u);
        cv.put("sku", s);
        cv.put("created_at", nowStr());
        getWritableDatabase().insertWithOnConflict("wishlist", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        return true;
    }

    public Set<String> getWishlistedSkus(String username) {
        Set<String> set = new HashSet<>();
        if (username == null || username.trim().isEmpty()) return set;
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT sku FROM wishlist WHERE LOWER(username) = LOWER(?)", new String[]{username.trim()})) {
            while (c.moveToNext()) set.add(c.getString(0));
        }
        return set;
    }

    public static int getDiscountPercent(Product product) {
        if (product == null || product.getSku() == null) return 10;
        int[] presets = {10, 12, 15, 8, 14, 18, 11};
        return presets[Math.abs(product.getSku().hashCode()) % presets.length];
    }

    public static String getOriginalPriceFormatted(Product product) {
        if (product == null) return "";
        long current = CartManager.parsePrice(product.getPrice());
        if (current <= 0) return "";
        long original = Math.round((current * 100.0) / (100 - getDiscountPercent(product)) / 10000.0) * 10000L;
        return CartManager.formatPrice(original);
    }

    public static String getSoldCountText(Product product) {
        if (product == null || product.getSku() == null) return "Đã bán 85";
        return "Đã bán " + (48 + (Math.abs(product.getSku().hashCode()) % 240));
    }
}
