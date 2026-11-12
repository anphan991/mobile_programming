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
    private static final int DATABASE_VERSION = 3; // Tăng version lên 3 để bổ sung bảng giỏ hàng theo từng tài khoản

    // BẢNG USERS
    public static final String TABLE_USERS = "users";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";

    //  BẢNG PRODUCTS
    public static final String TABLE_PRODUCTS = "products";
    public static final String COLUMN_PROD_ID = "id";
    public static final String COLUMN_PROD_NAME = "name";
    public static final String COLUMN_PROD_DESC = "description";
    public static final String COLUMN_PROD_PRICE = "price";
    public static final String COLUMN_PROD_IMAGE = "image_res_id";
    public static final String COLUMN_PROD_CATEGORY = "category";
    public static final String COLUMN_PROD_SKU = "sku";
    public static final String COLUMN_PROD_WARRANTY = "warranty";
    public static final String COLUMN_PROD_SPECS = "detailed_specs";
    public static final String COLUMN_PROD_INSTOCK = "in_stock";

    // BẢNG ORDERS
    public static final String TABLE_ORDERS = "orders";
    public static final String COLUMN_ORDER_ID = "id";
    public static final String COLUMN_ORDER_CODE = "order_code";
    public static final String COLUMN_ORDER_USERNAME = "username";
    public static final String COLUMN_ORDER_RECEIVER = "receiver_name";
    public static final String COLUMN_ORDER_PHONE = "receiver_phone";
    public static final String COLUMN_ORDER_ADDRESS = "receiver_address";
    public static final String COLUMN_ORDER_NOTE = "receiver_note";
    public static final String COLUMN_ORDER_PAYMENT = "payment_method";
    public static final String COLUMN_ORDER_TOTAL = "total_price";
    public static final String COLUMN_ORDER_ITEM_COUNT = "item_count";
    public static final String COLUMN_ORDER_CREATED_AT = "created_at";

    // BẢNG CART_ITEMS
    public static final String TABLE_CART = "cart_items";
    public static final String COLUMN_CART_ID = "id";
    public static final String COLUMN_CART_USERNAME = "username";
    public static final String COLUMN_CART_SKU = "sku";
    public static final String COLUMN_CART_NAME = "product_name";
    public static final String COLUMN_CART_PRICE = "price";
    public static final String COLUMN_CART_QUANTITY = "quantity";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tạo bảng users
        String createTableUsers = "CREATE TABLE IF NOT EXISTS " + TABLE_USERS + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_USERNAME + " TEXT UNIQUE, "
                + COLUMN_PASSWORD + " TEXT"
                + ");";
        db.execSQL(createTableUsers);

        // admin
        ContentValues adminValues = new ContentValues();
        adminValues.put(COLUMN_USERNAME, "admin");
        adminValues.put(COLUMN_PASSWORD, "123456");
        db.insertWithOnConflict(TABLE_USERS, null, adminValues, SQLiteDatabase.CONFLICT_IGNORE);

        // Tạo bảng products
        String createTableProducts = "CREATE TABLE IF NOT EXISTS " + TABLE_PRODUCTS + " ("
                + COLUMN_PROD_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_PROD_NAME + " TEXT, "
                + COLUMN_PROD_DESC + " TEXT, "
                + COLUMN_PROD_PRICE + " TEXT, "
                + COLUMN_PROD_IMAGE + " INTEGER, "
                + COLUMN_PROD_CATEGORY + " TEXT, "
                + COLUMN_PROD_SKU + " TEXT UNIQUE, "
                + COLUMN_PROD_WARRANTY + " TEXT, "
                + COLUMN_PROD_SPECS + " TEXT, "
                + COLUMN_PROD_INSTOCK + " INTEGER"
                + ");";
        db.execSQL(createTableProducts);

        // Chèn sẵn danh mục
        seedDefaultProducts(db);

        // Tạo bảng orders
        String createTableOrders = "CREATE TABLE IF NOT EXISTS " + TABLE_ORDERS + " ("
                + COLUMN_ORDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_ORDER_CODE + " TEXT UNIQUE, "
                + COLUMN_ORDER_USERNAME + " TEXT, "
                + COLUMN_ORDER_RECEIVER + " TEXT, "
                + COLUMN_ORDER_PHONE + " TEXT, "
                + COLUMN_ORDER_ADDRESS + " TEXT, "
                + COLUMN_ORDER_NOTE + " TEXT, "
                + COLUMN_ORDER_PAYMENT + " TEXT, "
                + COLUMN_ORDER_TOTAL + " TEXT, "
                + COLUMN_ORDER_ITEM_COUNT + " INTEGER, "
                + COLUMN_ORDER_CREATED_AT + " TEXT"
                + ");";
        db.execSQL(createTableOrders);

        // Tạo bảng cart_items
        String createTableCart = "CREATE TABLE IF NOT EXISTS " + TABLE_CART + " ("
                + COLUMN_CART_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_CART_USERNAME + " TEXT, "
                + COLUMN_CART_SKU + " TEXT, "
                + COLUMN_CART_NAME + " TEXT, "
                + COLUMN_CART_PRICE + " TEXT, "
                + COLUMN_CART_QUANTITY + " INTEGER, "
                + "UNIQUE(" + COLUMN_CART_USERNAME + ", " + COLUMN_CART_SKU + ")"
                + ");";
        db.execSQL(createTableCart);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Giữ nguyên dữ liệu tài khoản và đơn hàng đã có, chỉ bổ sung bảng mới nếu chưa tồn tại
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        onCreate(db);
    }

    // Nạp danh sách vào bảng products
    private void seedDefaultProducts(SQLiteDatabase db) {
        insertProductIfNotExist(db, "iPhone 16 Pro Max", "256GB · Titan Tự Nhiên · Chip A18 Pro", "34.990.000đ",
                R.drawable.img_iphone_16, "Điện thoại", "TZ-IP16PM-256", "12 tháng Apple VN",
                "• Màn hình: Super Retina XDR OLED 6.9 inch, ProMotion 120Hz\n• Vi xử lý: Apple A18 Pro tiến trình 3nm thế hệ thứ 2\n• Camera sau: Hệ thống 3 camera 48MP + Telephoto 5x\n• Chất liệu: Khung viền Titan cấp độ 5 bền nhẹ, cổng USB-C", 1);

        insertProductIfNotExist(db, "Samsung Galaxy S24 Ultra", "512GB · Xám Titan · Tích hợp S-Pen", "28.990.000đ",
                R.drawable.img_samsung_s24, "Điện thoại", "TZ-S24U-512", "12 tháng chính hãng",
                "• Màn hình: Dynamic AMOLED 2X 6.8 inch QHD+, 120Hz\n• Vi xử lý: Snapdragon 8 Gen 3 for Galaxy\n• Camera sau: 200MP + Zoom quang học 5x 50MP\n• Tính năng: Khung viền Titan, bút S-Pen tích hợp sẵn", 1);

        insertProductIfNotExist(db, "MacBook Air M3 13 inch", "RAM 16GB · SSD 512GB · Vỏ nhôm nguyên khối", "27.990.000đ",
                R.drawable.img_macbook_m3, "Máy tính xách tay", "TZ-MBA-M3-16", "12 tháng Apple VN",
                "• Màn hình: Liquid Retina 13.6 inch, độ sáng 500 nits, True Tone\n• Vi xử lý: Apple M3 (8 nhân CPU, 10 nhân GPU)\n• Bộ nhớ: RAM hợp nhất 16GB, ổ cứng SSD 512GB tốc độ cao\n• Thời lượng pin: Lên đến 18 giờ, sạc từ tính MagSafe 3", 1);

        insertProductIfNotExist(db, "iPad Pro M4 11 inch", "256GB Wi-Fi · Màn hình Ultra Retina XDR", "26.490.000đ",
                R.drawable.img_ipad_pro_m4, "Máy tính bảng", "TZ-IPADP-M4", "12 tháng Apple VN",
                "• Màn hình: Tandem OLED Ultra Retina XDR 11 inch, ProMotion 120Hz\n• Vi xử lý: Chip Apple M4 hiệu năng đồ họa vượt trội\n• Thiết kế: Siêu mỏng 5.3 mm, trọng lượng chỉ 444g\n• Kết nối: Cổng Thunderbolt / USB 4, hỗ trợ Apple Pencil Pro", 1);

        insertProductIfNotExist(db, "Tai nghe AirPods Pro 2", "Hộp sạc MagSafe USB-C · Chống ồn chủ động", "5.690.000đ",
                R.drawable.img_airpods_pro, "Thiết bị âm thanh", "TZ-APP2-USBC", "12 tháng chính hãng",
                "• Chip xử lý âm thanh: Apple H2 cho âm thanh chi tiết\n• Chống ồn: Chủ động (ANC) gấp 2 lần và chế độ Xuyên âm thích ứng\n• Kháng bụi & nước: Chuẩn IP54 cho cả tai nghe và hộp sạc\n• Thời lượng pin: Nghe nhạc lên đến 30 giờ kèm hộp sạc", 0);

        insertProductIfNotExist(db, "Tai nghe Sony WH-1000XM5", "Chống ồn đa micro · Pin 30 giờ · Hi-Res Audio", "7.490.000đ",
                R.drawable.img_sony_wh1000xm5, "Thiết bị âm thanh", "TZ-SN-XM5", "12 tháng Sony VN",
                "• Công nghệ chống ồn: Bộ xử lý V1 + QN1 kết hợp 8 micro thu âm\n• Màng loa: Sợi carbon 30mm thiết kế đặc biệt cho độ chi tiết cao\n• Kết nối: Bluetooth 5.2 đa điểm (2 thiết bị cùng lúc), hỗ trợ LDAC\n• Sạc nhanh: Sạc 3 phút qua chuẩn USB-PD cho 3 giờ sử dụng", 0);

        insertProductIfNotExist(db, "Apple Watch Series 10", "Viền Nhôm GPS 42mm · Màn hình OLED góc rộng", "10.990.000đ",
                R.drawable.img_apple_watch_series10, "Đồng hồ thông minh", "TZ-AWS10-42", "12 tháng Apple VN",
                "• Màn hình: Retina Luôn Bật LTPO3 OLED góc rộng, sáng hơn 40%\n• Vi xử lý: SiP S10 64-bit lõi kép với Neural Engine 4 lõi\n• Theo dõi sức khỏe: Nhịp tim quang học, giấc ngủ, độ sâu nước\n• Kháng nước: Độ sâu 50m, sạc nhanh 80% pin trong 30 phút", 1);

        insertProductIfNotExist(db, "Bàn phím cơ Keychron K8 Pro", "Khung nhôm CNC · Hotswap · Bluetooth 5.1", "2.490.000đ",
                R.drawable.img_keychron, "Phụ kiện", "TZ-KC-K8PRO", "12 tháng chính hãng",
                "• Thiết kế: Layout TKL (80%), khung viền nhôm nguyên khối chắc chắn\n• Tùy biến: Hỗ trợ QMK/VIA, mạch Hotswap 5 pin thay switch dễ dàng\n• Keycap: PBT Double-shot OSA profile chống bóng, lót foam tiêu âm\n• Kết nối: Không dây Bluetooth 5.1 (3 thiết bị) và có dây Type-C", 0);

        insertProductIfNotExist(db, "Chuột Logitech MX Master 3S", "Cảm biến Darkfield 8000 DPI · Click siêu êm", "2.190.000đ",
                R.drawable.img_logitech_mx, "Phụ kiện", "TZ-LG-MX3S", "24 tháng chính hãng",
                "• Cảm biến: Quang học Darkfield 8000 DPI hoạt động tốt trên mặt kính\n• Nút bấm: Công nghệ Quiet Click giảm 90% tiếng ồn khi thao tác\n• Con lăn: Điện từ MagSpeed cuộn 1.000 dòng mỗi giây\n• Kết nối: Đầu thu Logi Bolt USB hoặc Bluetooth Low Energy", 1);

        insertProductIfNotExist(db, "Loa Marshall Emberton II", "Âm thanh đa hướng 360 độ · Pin hơn 30 giờ", "3.990.000đ",
                R.drawable.img_marshall_emberton, "Thiết bị âm thanh", "TZ-MS-EMB2", "12 tháng chính hãng",
                "• Công suất: 2 x 10W mạch khuếch đại Class D\n• Chuẩn kháng nước: IP67 chống bụi và nước cao cấp\n• Kết nối: Bluetooth 5.1, chế độ ghép đôi Stack Mode\n• Thời lượng pin: Lên đến hơn 30 giờ chơi nhạc liên tục", 1);

        insertProductIfNotExist(db, "Tai nghe Marshall Major IV", "Pin kỷ lục 80 giờ · Sạc không dây tiện lợi", "3.190.000đ",
                R.drawable.img_marshall_major, "Thiết bị âm thanh", "TZ-MS-MJ4", "12 tháng chính hãng",
                "• Thiết kế: On-ear cổ điển đặc trưng Marshall, có thể gập gọn\n• Pin: Hơn 80 giờ nghe nhạc, sạc nhanh 15 phút nghe 15 giờ\n• Phím điều khiển: Núm xoay đa hướng Brass Control Knob\n• Kết nối: Bluetooth 5.0 và cổng AUX 3.5mm chia sẻ âm thanh", 1);

        insertProductIfNotExist(db, "Loa JBL Charge 5", "Âm trầm Original Pro Sound · Kháng nước IP67", "3.490.000đ",
                R.drawable.img_jbl_charge5, "Thiết bị âm thanh", "TZ-JBL-CHG5", "12 tháng chính hãng",
                "• Âm thanh: Driver dạng racetrack cải tiến cho âm trầm uy lực\n• Tính năng sạc dự phòng: Cổng USB Type-A sạc điện thoại\n• Kết nối: PartyBoost kết nối nhiều loa JBL cùng lúc\n• Thời lượng pin: 20 giờ chơi nhạc bền bỉ mọi bữa tiệc", 1);
    }

    private void insertProductIfNotExist(SQLiteDatabase db, String name, String desc, String price,
                                         int imageResId, String category, String sku, String warranty,
                                         String specs, int inStock) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROD_NAME, name);
        values.put(COLUMN_PROD_DESC, desc);
        values.put(COLUMN_PROD_PRICE, price);
        values.put(COLUMN_PROD_IMAGE, imageResId);
        values.put(COLUMN_PROD_CATEGORY, category);
        values.put(COLUMN_PROD_SKU, sku);
        values.put(COLUMN_PROD_WARRANTY, warranty);
        values.put(COLUMN_PROD_SPECS, specs);
        values.put(COLUMN_PROD_INSTOCK, inStock);
        db.insertWithOnConflict(TABLE_PRODUCTS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    // Đọc toàn bộ danh sách sản phẩm từ bảng products của SQLite
    public ArrayList<Product> getAllProducts() {
        ArrayList<Product> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS + " ORDER BY " + COLUMN_PROD_ID + " ASC", null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROD_NAME));
                    String desc = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROD_DESC));
                    String price = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROD_PRICE));
                    int imageResId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROD_IMAGE));
                    String category = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROD_CATEGORY));
                    String sku = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROD_SKU));
                    String warranty = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROD_WARRANTY));
                    String specs = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PROD_SPECS));
                    int inStockInt = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_PROD_INSTOCK));
                    boolean inStock = (inStockInt == 1);

                    list.add(new Product(name, desc, price, imageResId, category, sku, warranty, specs, inStock));
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return list;
    }

    // Lưu thông tin đơn hàng mới vào bảng orders
    public long insertOrder(String orderCode, String username, String receiverName,
                            String phone, String address, String note,
                            String paymentMethod, String totalPrice, int itemCount) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ORDER_CODE, orderCode);
        values.put(COLUMN_ORDER_USERNAME, username);
        values.put(COLUMN_ORDER_RECEIVER, receiverName);
        values.put(COLUMN_ORDER_PHONE, phone);
        values.put(COLUMN_ORDER_ADDRESS, address);
        values.put(COLUMN_ORDER_NOTE, note);
        values.put(COLUMN_ORDER_PAYMENT, paymentMethod);
        values.put(COLUMN_ORDER_TOTAL, totalPrice);
        values.put(COLUMN_ORDER_ITEM_COUNT, itemCount);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        values.put(COLUMN_ORDER_CREATED_AT, currentTime);

        return db.insert(TABLE_ORDERS, null, values);
    }

    // Lưu hoặc cập nhật sản phẩm trong giỏ hàng của một tài khoản cụ thể
    public void saveCartItem(String username, Product product, int quantity) {
        if (username == null || username.trim().isEmpty() || product == null) return;
        SQLiteDatabase db = this.getWritableDatabase();
        if (quantity <= 0) {
            db.delete(TABLE_CART, COLUMN_CART_USERNAME + " = ? AND " + COLUMN_CART_SKU + " = ?",
                    new String[]{username, product.getSku()});
            return;
        }
        ContentValues values = new ContentValues();
        values.put(COLUMN_CART_USERNAME, username);
        values.put(COLUMN_CART_SKU, product.getSku());
        values.put(COLUMN_CART_NAME, product.getName());
        values.put(COLUMN_CART_PRICE, product.getPrice());
        values.put(COLUMN_CART_QUANTITY, quantity);
        db.insertWithOnConflict(TABLE_CART, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // Đọc danh sách giỏ hàng riêng của một tài khoản từ SQLite
    public ArrayList<CartItem> getCartItemsByUser(String username) {
        ArrayList<CartItem> result = new ArrayList<>();
        if (username == null || username.trim().isEmpty()) return result;

        ArrayList<Product> allProducts = getAllProducts();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COLUMN_CART_SKU + ", " + COLUMN_CART_QUANTITY
                        + " FROM " + TABLE_CART
                        + " WHERE " + COLUMN_CART_USERNAME + " = ? ORDER BY " + COLUMN_CART_ID + " ASC",
                new String[]{username}
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    String sku = cursor.getString(0);
                    int qty = cursor.getInt(1);
                    Product matched = null;
                    for (Product p : allProducts) {
                        if (p.getSku().equals(sku)) {
                            matched = p;
                            break;
                        }
                    }
                    if (matched != null && qty > 0) {
                        result.add(new CartItem(matched, qty));
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return result;
    }

    // Xóa toàn bộ giỏ hàng của một tài khoản cụ thể (khi đặt hàng xong)
    public void clearUserCart(String username) {
        if (username == null || username.trim().isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CART, COLUMN_CART_USERNAME + " = ?", new String[]{username});
    }

    // Đăng ký tài khoản mới vào SQLite
    public boolean registerUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_USERNAME, username);
        values.put(COLUMN_PASSWORD, password);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    // Kiểm tra tên người dùng đã tồn tại trong cơ sở dữ liệu chưa
    public boolean checkUserExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COLUMN_ID + " FROM " + TABLE_USERS + " WHERE " + COLUMN_USERNAME + " = ?",
                new String[]{username}
        );
        boolean exists = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) {
            cursor.close();
        }
        return exists;
    }

    // Kiểm tra thông tin đăng nhập đúng username và password
    public boolean checkLogin(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + COLUMN_ID + " FROM " + TABLE_USERS + " WHERE " + COLUMN_USERNAME + " = ? AND " + COLUMN_PASSWORD + " = ?",
                new String[]{username, password}
        );
        boolean isValid = (cursor != null && cursor.getCount() > 0);
        if (cursor != null) {
            cursor.close();
        }
        return isValid;
    }
}