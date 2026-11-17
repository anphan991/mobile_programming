package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * ProductListActivity: Màn hình chính hiển thị danh sách sản phẩm.
 * Hỗ trợ phân trang, lọc danh mục, đồng bộ Cloud Firestore và xác thực Firebase.
 */
public class ProductListActivity extends AppCompatActivity {

    public static final String KEY_USER = "KEY_USER";
    private static final String PREF_USER_SESSION = "UserSession";
    private static final String KEY_CURRENT_USER = "CURRENT_USER";
    private static final String DEFAULT_USER = "admin";
    private static final String FIRESTORE_COLLECTION_PRODUCTS = "products";
    private static final int ITEMS_PER_PAGE = 10;

    // Mã lọc danh mục
    public static final String FILTER_ALL = "ALL";
    public static final String FILTER_MOBILE = "MOBILE";
    public static final String FILTER_ACCESSORY = "ACCESSORY";
    public static final String FILTER_AUDIO = "AUDIO";


    // Giữ static để tương thích với ProductDetailActivity và PaymentActivity
    public static ArrayList<Product> productList = new ArrayList<>();
    private final ArrayList<Product> filteredList = new ArrayList<>();
    private final ArrayList<Product> currentPageList = new ArrayList<>();

    private ProductAdapter adapter;
    private int currentPage = 1;
    private boolean isMenuOpen = false;
    private String currentFilterCode = FILTER_ALL;

    private ListView listView;
    private LinearLayout layoutMenu;
    private MaterialButton btnToggleMenu, btnPrev, btnNext, btnOpenCart;
    private TextView tvPage, tvProductCount, tvHeaderSubtitle, tvHeaderCartBadge;
    private TextView tvWelcomeUser, tvMenuAccountBadge;
    private TextView menuHome, menuFilterMobile, menuFilterAccessory, menuFilterAudio;
    private TextView menuProfile, menuCart, menuInfo, menuLogout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        setupEdgeToEdgeInsets();
        initViews();
        setupUserSession();
        setupListViewAndAdapter();
        setupNavigationAndMenu();
        loadLocalData();
        syncProductsWithFirestore();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Đồng bộ giỏ hàng và cập nhật số lượng badge khi quay lại màn hình
        CartManager.syncForCurrentUser(this);
        updateCartBadge();
    }

    private void setupEdgeToEdgeInsets() {
        View root = findViewById(R.id.rootProductList);
        if (root != null) {
            final int pl = root.getPaddingLeft();
            final int pt = root.getPaddingTop();
            final int pr = root.getPaddingRight();
            final int pb = root.getPaddingBottom();
            ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                Insets bars = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
                );
                v.setPadding(pl + bars.left, pt + bars.top, pr + bars.right, pb + bars.bottom);
                return insets;
            });
        }
    }


     //Ánh xạ các view của Activity tại một nơi tập trung.
    private void initViews() {
        listView = findViewById(R.id.listView);
        layoutMenu = findViewById(R.id.layoutMenu);

        btnToggleMenu = findViewById(R.id.btnToggleMenu);
        btnOpenCart = findViewById(R.id.btnOpenCart);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);

        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        tvMenuAccountBadge = findViewById(R.id.tvMenuAccountBadge);
        tvProductCount = findViewById(R.id.tvProductCount);
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);
        tvHeaderCartBadge = findViewById(R.id.tvHeaderCartBadge);
        tvPage = findViewById(R.id.tvPage);

        // Menu items
        menuHome = findViewById(R.id.menuHome);
        menuFilterMobile = findViewById(R.id.menuFilterMobile);
        menuFilterAccessory = findViewById(R.id.menuFilterAccessory);
        menuFilterAudio = findViewById(R.id.menuFilterAudio);
        menuProfile = findViewById(R.id.menuProfile);
        menuCart = findViewById(R.id.menuCart);
        menuInfo = findViewById(R.id.menuInfo);
        menuLogout = findViewById(R.id.menuLogout);
    }


     //Khởi tạo thông tin phiên người dùng và hiển thị lời chào.
    private void setupUserSession() {
        SharedPreferences session = getSharedPreferences(PREF_USER_SESSION, MODE_PRIVATE);
        String username = session.getString(KEY_CURRENT_USER, getIntent().getStringExtra(KEY_USER));
        if (username == null || username.trim().isEmpty()) {
            username = DEFAULT_USER;
        }
        session.edit().putString(KEY_CURRENT_USER, username).apply();

        if (tvWelcomeUser != null) {
            tvWelcomeUser.setText("Xin chào, " + username);
        }
        if (tvMenuAccountBadge != null) {
            tvMenuAccountBadge.setText("Tài khoản: " + username);
        }

        CartManager.syncForCurrentUser(this);
    }


    // Cài đặt Adapter và sự kiện click xem chi tiết sản phẩm.
    private void setupListViewAndAdapter() {
        adapter = new ProductAdapter(this, currentPageList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            closeMenu();
            Product item = currentPageList.get(position);
            int realIndex = productList.indexOf(item);
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra(ProductDetailActivity.KEY_INDEX, realIndex >= 0 ? realIndex : position);
            startActivity(intent);
        });
    }


     // Cài đặt tất cả sự kiện bấm nút và menu điều hướng.
    private void setupNavigationAndMenu() {
        // Nút toggle menu & nút mở giỏ hàng
        btnToggleMenu.setOnClickListener(v -> toggleMenu());
        btnOpenCart.setOnClickListener(v -> {
            closeMenu();
            startActivity(new Intent(this, PaymentActivity.class));
        });

        // Nút phân trang
        btnPrev.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                loadPageData(currentPage);
            }
        });
        btnNext.setOnClickListener(v -> {
            if (currentPage < getTotalPages()) {
                currentPage++;
                loadPageData(currentPage);
            }
        });

        // Bộ lọc danh mục
        menuHome.setOnClickListener(v -> applyCategoryFilter(FILTER_ALL, "Danh mục thiết bị công nghệ", menuHome));
        menuFilterMobile.setOnClickListener(v -> applyCategoryFilter(FILTER_MOBILE, "Điện thoại & Máy tính bảng", menuFilterMobile));
        menuFilterAccessory.setOnClickListener(v -> applyCategoryFilter(FILTER_ACCESSORY, "Laptop & Phụ kiện công nghệ", menuFilterAccessory));
        menuFilterAudio.setOnClickListener(v -> applyCategoryFilter(FILTER_AUDIO, "Thiết bị âm thanh chính hãng", menuFilterAudio));

        // Các mục chức năng trong menu
        View.OnClickListener openProfile = v -> {
            closeMenu();
            startActivity(new Intent(this, ProfileActivity.class));
        };
        menuProfile.setOnClickListener(openProfile);
        if (tvMenuAccountBadge != null) {
            tvMenuAccountBadge.setOnClickListener(openProfile);
        }

        menuCart.setOnClickListener(v -> {
            closeMenu();
            startActivity(new Intent(this, PaymentActivity.class));
        });

        menuInfo.setOnClickListener(v -> {
            closeMenu();
            showAppInfoDialog();
        });

        menuLogout.setOnClickListener(v -> performLogout());
    }

     // data ban đầu từ SQLite
    private void loadLocalData() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        ArrayList<Product> dbProducts = dbHelper.getAllProducts();
        productList.clear();
        if (dbProducts != null && !dbProducts.isEmpty()) {
            productList.addAll(dbProducts);
        } else {
            productList.addAll(createDefaultProducts());
        }

        filteredList.clear();
        filteredList.addAll(productList);
        updateMenuBadgeCount();
        loadPageData(currentPage);
    }

    private void syncProductsWithFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(FIRESTORE_COLLECTION_PRODUCTS).get().addOnSuccessListener(snapshots -> {
            if (snapshots.isEmpty()) {
                for (Product p : createDefaultProducts()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", p.getName());
                    map.put("price", p.getPrice());
                    map.put("category", p.getCategory());
                    map.put("sku", p.getSku());
                    map.put("description", p.getDescription());
                    map.put("warranty", p.getWarranty());
                    map.put("detailedSpecs", p.getDetailedSpecs());
                    map.put("inStock", p.isInStock());
                    db.collection(FIRESTORE_COLLECTION_PRODUCTS).document(p.getSku()).set(map);
                }
            } else {
                productList.clear();
                for (QueryDocumentSnapshot doc : snapshots) {
                    productList.add(new Product(
                            doc.getString("name"), doc.getString("description"), doc.getString("price"),
                            DatabaseHelper.getImageForProduct(doc.getString("name")),
                            doc.getString("category"), doc.getString("sku"),
                            doc.getString("warranty"), doc.getString("detailedSpecs"),
                            Boolean.TRUE.equals(doc.getBoolean("inStock"))
                    ));
                }
                applyCategoryFilter(currentFilterCode, "Danh mục thiết bị công nghệ", menuHome);
                updateMenuBadgeCount();
            }
        });
    }
    private void applyCategoryFilter(String filterCode, String subtitle, TextView selectedView) {
        closeMenu();
        currentFilterCode = filterCode;
        filteredList.clear();

        for (Product p : productList) {
            if (matchesCategory(p.getCategory(), filterCode)) {
                filteredList.add(p);
            }
        }

        if (tvHeaderSubtitle != null) {
            tvHeaderSubtitle.setText(subtitle);
        }

        highlightActiveMenuItem(selectedView);
        currentPage = 1;
        loadPageData(currentPage);
    }


    private boolean matchesCategory(String category, String filterCode) {
        if (FILTER_ALL.equals(filterCode)) return true;
        if (category == null) return false;

        switch (filterCode) {
            case FILTER_MOBILE:
                return "Điện thoại".equals(category) || "Máy tính bảng".equals(category);
            case FILTER_AUDIO:
                return "Thiết bị âm thanh".equals(category);
            case FILTER_ACCESSORY:
                return !"Điện thoại".equals(category) && !"Máy tính bảng".equals(category) && !"Thiết bị âm thanh".equals(category);
            default:
                return true;
        }
    }

    private void highlightActiveMenuItem(TextView selectedView) {
        TextView[] menuItems = new TextView[]{menuHome, menuFilterMobile, menuFilterAccessory, menuFilterAudio};
        for (TextView item : menuItems) {
            if (item == null) continue;
            boolean active = (item == selectedView);
            item.setTextColor(ContextCompat.getColor(this, active ? R.color.primary_dark : R.color.ink));
            item.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private void loadPageData(int page) {
        currentPageList.clear();
        int total = filteredList.size();
        int start = (page - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, total);

        for (int i = start; i < end; i++) {
            currentPageList.add(filteredList.get(i));
        }

        int totalPages = getTotalPages();
        if (tvPage != null) {
            tvPage.setText(page + " / " + totalPages);
        }
        if (tvProductCount != null) {
            tvProductCount.setText(total == 0 ? "0 sản phẩm" : "Hiển thị " + (start + 1) + "–" + end + " / " + total + " sản phẩm");
        }

        updatePaginationButtons(page, totalPages);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) filteredList.size() / ITEMS_PER_PAGE));
    }

    private void updatePaginationButtons(int page, int totalPages) {
        if (btnPrev != null) {
            boolean canPrev = page > 1;
            btnPrev.setEnabled(canPrev);
            btnPrev.setAlpha(canPrev ? 1.0f : 0.42f);
        }
        if (btnNext != null) {
            boolean canNext = page < totalPages;
            btnNext.setEnabled(canNext);
            btnNext.setAlpha(canNext ? 1.0f : 0.42f);
        }
    }

    private void updateMenuBadgeCount() {
        if (menuHome != null) {
            menuHome.setText("Tất cả sản phẩm (" + productList.size() + ")");
        }
    }


    private void toggleMenu() {
        isMenuOpen = !isMenuOpen;
        btnToggleMenu.setIconResource(isMenuOpen ? R.drawable.ic_close_24 : R.drawable.ic_menu_24);
        layoutMenu.setVisibility(isMenuOpen ? View.VISIBLE : View.GONE);
    }

    private void closeMenu() {
        if (isMenuOpen) {
            toggleMenu();
        }
    }

    private void updateCartBadge() {
        if (tvHeaderCartBadge != null) {
            int total = CartManager.getTotalItemCount();
            tvHeaderCartBadge.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
            if (total > 0) {
                tvHeaderCartBadge.setText(String.valueOf(total));
            }
        }
    }

    private void showAppInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Về Test Store")
                .setMessage("Phiên bản: 1.2.0\nHệ thống tra cứu và đặt mua thiết bị công nghệ.")
                .setPositiveButton("Đóng", null)
                .show();
    }


     // Đăng xuất tài khoản khỏi Firebase và hệ thống cục bộ
    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        CartManager.onLogout();
        getSharedPreferences(PREF_USER_SESSION, MODE_PRIVATE).edit().clear().apply();
        Toast.makeText(this, "Đã đăng xuất tài khoản!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }



    //các sản phẩm mặc định được sử dụng để khởi tạo dữ liệu.
    public static ArrayList<Product> createDefaultProducts() {
        ArrayList<Product> list = new ArrayList<>();
        list.add(new Product("iPhone 16 Pro Max", "256GB · Titan Tự Nhiên · Chip A18 Pro", "34.990.000đ", R.drawable.img_iphone_16, "Điện thoại", "TZ-IP16PM-256", "12 tháng Apple VN", "• Màn hình: Super Retina XDR OLED 6.9 inch, ProMotion 120Hz\n• Vi xử lý: Apple A18 Pro tiến trình 3nm thế hệ thứ 2\n• Camera sau: Hệ thống 3 camera 48MP + Telephoto 5x\n• Chất liệu: Khung viền Titan cấp độ 5 bền nhẹ, cổng USB-C", true));
        list.add(new Product("Samsung Galaxy S24 Ultra", "512GB · Xám Titan · Tích hợp S-Pen", "28.990.000đ", R.drawable.img_samsung_s24, "Điện thoại", "TZ-S24U-512", "12 tháng chính hãng", "• Màn hình: Dynamic AMOLED 2X 6.8 inch QHD+, 120Hz\n• Vi xử lý: Snapdragon 8 Gen 3 for Galaxy\n• Camera sau: 200MP + Zoom quang học 5x 50MP\n• Tính năng: Khung viền Titan, bút S-Pen tích hợp sẵn", true));
        list.add(new Product("MacBook Air M3 13 inch", "RAM 16GB · SSD 512GB · Vỏ nhôm nguyên khối", "27.990.000đ", R.drawable.img_macbook_m3, "Máy tính xách tay", "TZ-MBA-M3-16", "12 tháng Apple VN", "• Màn hình: Liquid Retina 13.6 inch, độ sáng 500 nits, True Tone\n• Vi xử lý: Apple M3 (8 nhân CPU, 10 nhân GPU)\n• Bộ nhớ: RAM hợp nhất 16GB, ổ cứng SSD 512GB tốc độ cao\n• Thời lượng pin: Lên đến 18 giờ, sạc từ tính MagSafe 3", true));
        list.add(new Product("iPad Pro M4 11 inch", "256GB Wi-Fi · Màn hình Ultra Retina XDR", "26.490.000đ", R.drawable.img_ipad_pro_m4, "Máy tính bảng", "TZ-IPADP-M4", "12 tháng Apple VN", "• Màn hình: Tandem OLED Ultra Retina XDR 11 inch, ProMotion 120Hz\n• Vi xử lý: Chip Apple M4 hiệu năng đồ họa vượt trội\n• Thiết kế: Siêu mỏng 5.3 mm, trọng lượng chỉ 444g\n• Kết nối: Cổng Thunderbolt / USB 4, hỗ trợ Apple Pencil Pro", true));
        list.add(new Product("Tai nghe AirPods Pro 2", "Hộp sạc MagSafe USB-C · Chống ồn chủ động", "5.690.000đ", R.drawable.img_airpods_pro, "Thiết bị âm thanh", "TZ-APP2-USBC", "12 tháng chính hãng", "• Chip xử lý âm thanh: Apple H2 cho âm thanh chi tiết\n• Chống ồn: Chủ động (ANC) gấp 2 lần và chế độ Xuyên âm thích ứng\n• Kháng bụi & nước: Chuẩn IP54 cho cả tai nghe và hộp sạc\n• Thời lượng pin: Nghe nhạc lên đến 30 giờ kèm hộp sạc", false));
        list.add(new Product("Tai nghe Sony WH-1000XM5", "Chống ồn đa micro · Pin 30 giờ · Hi-Res Audio", "7.490.000đ", R.drawable.img_sony_wh1000xm5, "Thiết bị âm thanh", "TZ-SN-XM5", "12 tháng Sony VN", "• Công nghệ chống ồn: Bộ xử lý V1 + QN1 kết hợp 8 micro thu âm\n• Màng loa: Sợi carbon 30mm thiết kế đặc biệt cho độ chi tiết cao\n• Kết nối: Bluetooth 5.2 đa điểm (2 thiết bị cùng lúc), hỗ trợ LDAC\n• Sạc nhanh: Sạc 3 phút qua chuẩn USB-PD cho 3 giờ sử dụng", false));
        list.add(new Product("Apple Watch Series 10", "Viền Nhôm GPS 42mm · Màn hình OLED góc rộng", "10.990.000đ", R.drawable.img_apple_watch_series10, "Đồng hồ thông minh", "TZ-AWS10-42", "12 tháng Apple VN", "• Màn hình: Retina Luôn Bật LTPO3 OLED góc rộng, sáng hơn 40%\n• Vi xử lý: SiP S10 64-bit lõi kép với Neural Engine 4 lõi\n• Theo dõi sức khỏe: Nhịp tim quang học, giấc ngủ, độ sâu nước\n• Kháng nước: Độ sâu 50m, sạc nhanh 80% pin trong 30 phút", true));
        list.add(new Product("Bàn phím cơ Keychron K8 Pro", "Khung nhôm CNC · Hotswap · Bluetooth 5.1", "2.490.000đ", R.drawable.img_keychron, "Phụ kiện", "TZ-KC-K8PRO", "12 tháng chính hãng", "• Thiết kế: Layout TKL (80%), khung viền nhôm nguyên khối chắc chắn\n• Tùy biến: Hỗ trợ QMK/VIA, mạch Hotswap 5 pin thay switch dễ dàng\n• Keycap: PBT Double-shot OSA profile chống bóng, lót foam tiêu âm\n• Kết nối: Không dây Bluetooth 5.1 (3 thiết bị) và có dây Type-C", false));
        list.add(new Product("Chuột Logitech MX Master 3S", "Cảm biến Darkfield 8000 DPI · Click siêu êm", "2.190.000đ", R.drawable.img_logitech_mx, "Phụ kiện", "TZ-LG-MX3S", "24 tháng chính hãng", "• Cảm biến: Quang học Darkfield 8000 DPI hoạt động tốt trên mặt kính\n• Nút bấm: Công nghệ Quiet Click giảm 90% tiếng ồn khi thao tác\n• Con lăn: Điện từ MagSpeed cuộn 1.000 dòng mỗi giây\n• Kết nối: Đầu thu Logi Bolt USB hoặc Bluetooth Low Energy", true));
        list.add(new Product("Loa Marshall Emberton II", "Âm thanh đa hướng 360 độ · Pin hơn 30 giờ", "3.990.000đ", R.drawable.img_marshall_emberton, "Thiết bị âm thanh", "TZ-MS-EMB2", "12 tháng chính hãng", "• Công suất: 2 x 10W mạch khuếch đại Class D\n• Chuẩn kháng nước: IP67 chống bụi và nước cao cấp\n• Kết nối: Bluetooth 5.1, chế độ ghép đôi Stack Mode\n• Thời lượng pin: Lên đến hơn 30 giờ chơi nhạc liên tục", true));
        list.add(new Product("Tai nghe Marshall Major IV", "Pin kỷ lục 80 giờ · Sạc không dây tiện lợi", "3.190.000đ", R.drawable.img_marshall_major, "Thiết bị âm thanh", "TZ-MS-MJ4", "12 tháng chính hãng", "• Thiết kế: On-ear cổ điển đặc trưng Marshall, có thể gập gọn\n• Pin: Hơn 80 giờ nghe nhạc, sạc nhanh 15 phút nghe 15 giờ\n• Phím điều khiển: Núm xoay đa hướng Brass Control Knob\n• Kết nối: Bluetooth 5.0 và cổng AUX 3.5mm chia sẻ âm thanh", true));
        list.add(new Product("Loa JBL Charge 5", "Âm trầm Original Pro Sound · Kháng nước IP67", "3.490.000đ", R.drawable.img_jbl_charge5, "Thiết bị âm thanh", "TZ-JBL-CHG5", "12 tháng chính hãng", "• Âm thanh: Driver dạng racetrack cải tiến cho âm trầm uy lực\n• Tính năng sản phẩm: Cổng USB Type-A sạc điện thoại\n• Kết nối: PartyBoost kết nối nhiều loa JBL cùng lúc\n• Thời lượng pin: 20 giờ chơi nhạc bền bỉ mọi bữa tiệc", true));
        return list;
    }
}
