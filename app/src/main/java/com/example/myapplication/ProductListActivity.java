package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;

public class ProductListActivity extends AppCompatActivity {

    // key chung truyền nhận username
    public static final String KEY_USER = "KEY_USER";

    // danh sách tổng các sản phẩm
    public static ArrayList<Product> productList = new ArrayList<>();

    // danh sách sau khi lọc theo danh mục trên menu
    private ArrayList<Product> filteredList = new ArrayList<>();

    // danh sách sản phẩm hiển thị theo trang
    private ArrayList<Product> currentPageList = new ArrayList<>();
    private ProductAdapter adapter;
    private int currentPage = 1;
    private final int itemsPerPage = 10; //số lượng sản phẩm hiển thị trong trang

    private LinearLayout layoutMenu;
    private MaterialButton btnToggleMenu, btnPrev, btnNext, btnOpenCart;
    private TextView tvPage, tvProductCount, tvHeaderSubtitle, tvHeaderCartBadge;
    private TextView menuHome, menuFilterMobile, menuFilterAccessory, menuFilterAudio;
    private boolean isMenuOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);

        View rootView = findViewById(R.id.rootProductList);
        final int basePaddingLeft = rootView.getPaddingLeft();
        final int basePaddingTop = rootView.getPaddingTop();
        final int basePaddingRight = rootView.getPaddingRight();
        final int basePaddingBottom = rootView.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(
                    basePaddingLeft + systemBars.left,
                    basePaddingTop + systemBars.top,
                    basePaddingRight + systemBars.right,
                    basePaddingBottom + systemBars.bottom
            );
            return insets;
        });

        // Khởi tạo danh sách sản phẩm
        initProductData();

        // Mở sẵn kết nối SQLite Database để Database Inspector nhận diện ngay lập tức
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        dbHelper.getReadableDatabase();

        // Ánh xạ View
        ListView listView = findViewById(R.id.listView);
        TextView tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        TextView tvMenuAccountBadge = findViewById(R.id.tvMenuAccountBadge);
        tvProductCount = findViewById(R.id.tvProductCount);
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);

        btnToggleMenu = findViewById(R.id.btnToggleMenu);
        btnOpenCart = findViewById(R.id.btnOpenCart);
        tvHeaderCartBadge = findViewById(R.id.tvHeaderCartBadge);
        layoutMenu = findViewById(R.id.layoutMenu);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        tvPage = findViewById(R.id.tvPage);

        // Sự kiện mở giỏ hàng từ nút trên Header
        if (btnOpenCart != null) {
            btnOpenCart.setOnClickListener(v -> {
                if (isMenuOpen) closeMenu();
                Intent intent = new Intent(ProductListActivity.this, PaymentActivity.class);
                startActivity(intent);
            });
        }

        menuHome = findViewById(R.id.menuHome);
        if (menuHome != null) {
            menuHome.setText("Tất cả sản phẩm (" + productList.size() + ")");
        }
        menuFilterMobile = findViewById(R.id.menuFilterMobile);
        menuFilterAccessory = findViewById(R.id.menuFilterAccessory);
        menuFilterAudio = findViewById(R.id.menuFilterAudio);
        TextView menuProfile = findViewById(R.id.menuProfile);
        TextView menuCart = findViewById(R.id.menuCart);
        TextView menuInfo = findViewById(R.id.menuInfo);
        TextView menuLogout = findViewById(R.id.menuLogout);

        // Nhận username từ SharedPreferences lưu trữ trong UserSession
        SharedPreferences sessionPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        String username = sessionPref.getString("CURRENT_USER", null);
        if (username == null || username.trim().isEmpty()) {
            username = getIntent().getStringExtra(KEY_USER);
            if (username == null || username.trim().isEmpty()) {
                username = "admin";
            }
            sessionPref.edit().putString("CURRENT_USER", username).apply();
        }
        // Đồng bộ giỏ hàng riêng của tài khoản đang đăng nhập
        CartManager.syncForCurrentUser(this);

        tvWelcomeUser.setText("Xin chào, " + username);
        if (tvMenuAccountBadge != null) {
            tvMenuAccountBadge.setText("Tài khoản: " + username);
        }

        // Mặc định hiển thị toàn bộ danh sách sản phẩm
        filteredList.clear();
        filteredList.addAll(productList);

        // Tạo adapter và gắn vào ListView
        adapter = new ProductAdapter(this, currentPageList);
        listView.setAdapter(adapter);
        loadPageData(currentPage);

        // Sự kiện bấm nút menu ẩn/hiện ở góc trên bên trái
        btnToggleMenu.setOnClickListener(v -> toggleMenu());

        // Sự kiện mục "Tất cả sản phẩm" trong menu
        menuHome.setOnClickListener(v -> {
            applyCategoryFilter("ALL", "Danh mục thiết bị công nghệ", menuHome);
            closeMenu();
        });

        // Sự kiện mục lọc "Điện thoại & Máy tính bảng" trong menu
        menuFilterMobile.setOnClickListener(v -> {
            applyCategoryFilter("MOBILE", "Điện thoại & Máy tính bảng", menuFilterMobile);
            closeMenu();
        });

        // Sự kiện mục lọc "Laptop & Phụ kiện" trong menu
        menuFilterAccessory.setOnClickListener(v -> {
            applyCategoryFilter("ACCESSORY", "Laptop & Phụ kiện công nghệ", menuFilterAccessory);
            closeMenu();
        });

        // Sự kiện mục lọc thiết bị âm thanh
        menuFilterAudio.setOnClickListener(v -> {
            applyCategoryFilter("AUDIO", "Thiết bị âm thanh chính hãng", menuFilterAudio);
            closeMenu();
        });

        // Sự kiện mở trang Hồ sơ cá nhân (từ mục menu hoặc badge tài khoản)
        View.OnClickListener openProfileListener = v -> {
            closeMenu();
            Intent intent = new Intent(ProductListActivity.this, ProfileActivity.class);
            startActivity(intent);
        };
        if (menuProfile != null) {
            menuProfile.setOnClickListener(openProfileListener);
        }
        if (tvMenuAccountBadge != null) {
            tvMenuAccountBadge.setOnClickListener(openProfileListener);
        }

        // Sự kiện mục "Giỏ hàng & Đặt mua" trong menu
        if (menuCart != null) {
            menuCart.setOnClickListener(v -> {
                closeMenu();
                Intent intent = new Intent(ProductListActivity.this, PaymentActivity.class);
                startActivity(intent);
            });
        }

        // Sự kiện mục "Thông tin ứng dụng" trong menu
        menuInfo.setOnClickListener(v -> {
            closeMenu();
            new AlertDialog.Builder(this)
                    .setTitle("Về Test Store")
                    .setMessage("Phiên bản: 1.2.0\nHệ thống quản lý và tra cứu danh mục sản phẩm công nghệ.\n\nHỗ trợ phân trang, lọc danh mục và xem chi tiết cấu hình sản phẩm.")
                    .setPositiveButton("Đóng", null)
                    .show();
        });

        // Sự kiện đăng xuất từ trong menu
        if (menuLogout != null) {
            menuLogout.setOnClickListener(v -> performLogout());
        }

        // Sự kiện bấm chọn 1 dòng sản phẩm mở trang ProductDetailActivity
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (isMenuOpen) {
                closeMenu();
            }
            if (position >= 0 && position < currentPageList.size()) {
                Product selectedProduct = currentPageList.get(position);
                int realIndex = productList.indexOf(selectedProduct);
                if (realIndex < 0) {
                    realIndex = (currentPage - 1) * itemsPerPage + position;
                }
                Intent intent = new Intent(ProductListActivity.this, ProductDetailActivity.class);
                intent.putExtra(ProductDetailActivity.KEY_INDEX, realIndex);
                startActivity(intent);
            }
        });

        // Sự kiện nút Trang trước (Back ở bottom)
        btnPrev.setOnClickListener(v -> {
            if (currentPage > 1) {
                currentPage--;
                loadPageData(currentPage);
                listView.smoothScrollToPosition(0);
            } else {
                Toast.makeText(this, "Đang ở trang đầu tiên", Toast.LENGTH_SHORT).show();
            }
        });

        // Sự kiện nút Trang tiếp (Next ở bottom)
        btnNext.setOnClickListener(v -> {
            int totalPages = getTotalPages();
            if (currentPage < totalPages) {
                currentPage++;
                loadPageData(currentPage);
                listView.smoothScrollToPosition(0);
            } else {
                Toast.makeText(this, "Đang ở trang cuối cùng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initProductData() {
        // Đọc danh mục sản phẩm trực tiếp từ cơ sở dữ liệu SQLite (bảng products)
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        ArrayList<Product> dbProducts = dbHelper.getAllProducts();
        if (dbProducts != null && !dbProducts.isEmpty()) {
            productList.clear();
            productList.addAll(dbProducts);
            return;
        }

        // Dự phòng khởi tạo nếu cơ sở dữ liệu chưa sẵn sàng
        productList.clear();
        productList.add(new Product(
                "iPhone 16 Pro Max",
                "256GB · Titan Tự Nhiên · Chip A18 Pro",
                "34.990.000đ",
                R.drawable.img_iphone_16,
                "Điện thoại",
                "TZ-IP16PM-256",
                "12 tháng Apple VN",
                "• Màn hình: Super Retina XDR OLED 6.9 inch, ProMotion 120Hz\n"
                        + "• Vi xử lý: Apple A18 Pro tiến trình 3nm thế hệ thứ 2\n"
                        + "• Camera sau: Hệ thống 3 camera 48MP + Telephoto 5x\n"
                        + "• Chất liệu: Khung viền Titan cấp độ 5 bền nhẹ, cổng USB-C"
        ));
        productList.add(new Product(
                "Samsung Galaxy S24 Ultra",
                "512GB · Xám Titan · Tích hợp S-Pen",
                "28.990.000đ",
                R.drawable.img_samsung_s24,
                "Điện thoại",
                "TZ-S24U-512",
                "12 tháng chính hãng",
                "• Màn hình: Dynamic AMOLED 2X 6.8 inch QHD+, 120Hz\n"
                        + "• Vi xử lý: Snapdragon 8 Gen 3 for Galaxy\n"
                        + "• Camera sau: 200MP + Zoom quang học 5x 50MP\n"
                        + "• Tính năng: Khung viền Titan, bút S-Pen tích hợp sẵn"
        ));
        productList.add(new Product(
                "MacBook Air M3 13 inch",
                "RAM 16GB · SSD 512GB · Vỏ nhôm nguyên khối",
                "27.990.000đ",
                R.drawable.img_macbook_m3,
                "Máy tính xách tay",
                "TZ-MBA-M3-16",
                "12 tháng Apple VN",
                "• Màn hình: Liquid Retina 13.6 inch, độ sáng 500 nits, True Tone\n"
                        + "• Vi xử lý: Apple M3 (8 nhân CPU, 10 nhân GPU)\n"
                        + "• Bộ nhớ: RAM hợp nhất 16GB, ổ cứng SSD 512GB tốc độ cao\n"
                        + "• Thời lượng pin: Lên đến 18 giờ, sạc từ tính MagSafe 3"
        ));
        productList.add(new Product(
                "iPad Pro M4 11 inch",
                "256GB Wi-Fi · Màn hình Ultra Retina XDR",
                "26.490.000đ",
                R.drawable.img_ipad_pro_m4,
                "Máy tính bảng",
                "TZ-IPADP-M4",
                "12 tháng Apple VN",
                "• Màn hình: Tandem OLED Ultra Retina XDR 11 inch, ProMotion 120Hz\n"
                        + "• Vi xử lý: Chip Apple M4 hiệu năng đồ họa vượt trội\n"
                        + "• Thiết kế: Siêu mỏng 5.3 mm, trọng lượng chỉ 444g\n"
                        + "• Kết nối: Cổng Thunderbolt / USB 4, hỗ trợ Apple Pencil Pro",
                true // còn hàng
        ));
        productList.add(new Product(
                "Tai nghe AirPods Pro 2",
                "Hộp sạc MagSafe USB-C · Chống ồn chủ động",
                "5.690.000đ",
                R.drawable.img_airpods_pro,
                "Thiết bị âm thanh",
                "TZ-APP2-USBC",
                "12 tháng chính hãng",
                "• Chip xử lý âm thanh: Apple H2 cho âm thanh chi tiết\n"
                        + "• Chống ồn: Chủ động (ANC) gấp 2 lần và chế độ Xuyên âm thích ứng\n"
                        + "• Kháng bụi & nước: Chuẩn IP54 cho cả tai nghe và hộp sạc\n"
                        + "• Thời lượng pin: Nghe nhạc lên đến 30 giờ kèm hộp sạc",
                false // hết hàng
        ));
        productList.add(new Product(
                "Tai nghe Sony WH-1000XM5",
                "Chống ồn đa micro · Pin 30 giờ · Hi-Res Audio",
                "7.490.000đ",
                R.drawable.img_sony_wh1000xm5,
                "Thiết bị âm thanh",
                "TZ-SN-XM5",
                "12 tháng Sony VN",
                "• Công nghệ chống ồn: Bộ xử lý V1 + QN1 kết hợp 8 micro thu âm\n"
                        + "• Màng loa: Sợ carbon 30mm thiết kế đặc biệt cho độ chi tiết cao\n"
                        + "• Kết nối: Bluetooth 5.2 đa điểm (2 thiết bị cùng lúc), hỗ trợ LDAC\n"
                        + "• Sạc nhanh: Sạc 3 phút qua chuẩn USB-PD cho 3 giờ sử dụng",
                false // Hết hàng
        ));
        productList.add(new Product(
                "Apple Watch Series 10",
                "Viền Nhôm GPS 42mm · Màn hình OLED góc rộng",
                "10.990.000đ",
                R.drawable.img_apple_watch_series10,
                "Đồng hồ thông minh",
                "TZ-AWS10-42",
                "12 tháng Apple VN",
                "• Màn hình: Retina Luôn Bật LTPO3 OLED góc rộng, sáng hơn 40%\n"
                        + "• Vi xử lý: SiP S10 64-bit lõi kép với Neural Engine 4 lõi\n"
                        + "• Theo dõi sức khỏe: Nhịp tim quang học, giấc ngủ, độ sâu nước\n"
                        + "• Kháng nước: Độ sâu 50m, sạc nhanh 80% pin trong 30 phút",
                true
        ));
        productList.add(new Product(
                "Bàn phím cơ Keychron K8 Pro",
                "Khung nhôm CNC · Hotswap · Bluetooth 5.1",
                "2.490.000đ",
                R.drawable.img_keychron,
                "Phụ kiện",
                "TZ-KC-K8PRO",
                "12 tháng chính hãng",
                "• Thiết kế: Layout TKL (80%), khung viền nhôm nguyên khối chắc chắn\n"
                        + "• Tùy biến: Hỗ trợ QMK/VIA, mạch Hotswap 5 pin thay switch dễ dàng\n"
                        + "• Keycap: PBT Double-shot OSA profile chống bóng, lót foam tiêu âm\n"
                        + "• Kết nối: Không dây Bluetooth 5.1 (3 thiết bị) và có dây Type-C",
                false // Hết hàng
        ));
        productList.add(new Product(
                "Chuột Logitech MX Master 3S",
                "Cảm biến Darkfield 8000 DPI · Click siêu êm",
                "2.190.000đ",
                R.drawable.img_logitech_mx,
                "Phụ kiện",
                "TZ-LG-MX3S",
                "24 tháng chính hãng",
                "• Cảm biến: Quang học Darkfield 8000 DPI hoạt động tốt trên mặt kính\n"
                        + "• Nút bấm: Công nghệ Quiet Click giảm 90% tiếng ồn khi thao tác\n"
                        + "• Con lăn: Điện từ MagSpeed cuộn 1.000 dòng mỗi giây\n"
                        + "• Kết nối: Đầu thu Logi Bolt USB hoặc Bluetooth Low Energy"
        ));


        productList.add(new Product(
                "Loa Marshall Emberton II",
                "Âm thanh đa hướng 360 độ · Pin hơn 30 giờ",
                "3.990.000đ",
                R.drawable.img_marshall_emberton,
                "Thiết bị âm thanh",
                "TZ-MS-EMB2",
                "12 tháng chính hãng",
                "• Công suất: 2 x 10W mạch khuếch đại Class D\n"
                        + "• Chuẩn kháng nước: IP67 chống bụi và nước cao cấp\n"
                        + "• Kết nối: Bluetooth 5.1, chế độ ghép đôi Stack Mode\n"
                        + "• Thời lượng pin: Lên đến hơn 30 giờ chơi nhạc liên tục"
        ));

        productList.add(new Product(
                "Tai nghe Marshall Major IV",
                "Pin kỷ lục 80 giờ · Sạc không dây tiện lợi",
                "3.190.000đ",
                R.drawable.img_marshall_major,
                "Thiết bị âm thanh",
                "TZ-MS-MJ4",
                "12 tháng chính hãng",
                "• Thiết kế: On-ear cổ điển đặc trưng Marshall, có thể gập gọn\n"
                        + "• Pin: Hơn 80 giờ nghe nhạc, sạc nhanh 15 phút nghe 15 giờ\n"
                        + "• Phím điều khiển: Núm xoay đa hướng Brass Control Knob\n"
                        + "• Kết nối: Bluetooth 5.0 và cổng AUX 3.5mm chia sẻ âm thanh"
        ));

        productList.add(new Product(
                "Loa JBL Charge 5",
                "Âm trầm Original Pro Sound · Kháng nước IP67",
                "3.490.000đ",
                R.drawable.img_jbl_charge5,
                "Thiết bị âm thanh",
                "TZ-JBL-CHG5",
                "12 tháng chính hãng",
                "• Âm thanh: Driver dạng racetrack cải tiến cho âm trầm uy lực\n"
                        + "• Tính năng sạc dự phòng: Cổng USB Type-A sạc điện thoại\n"
                        + "• Kết nối: PartyBoost kết nối nhiều loa JBL cùng lúc\n"
                        + "• Thời lượng pin: 20 giờ chơi nhạc bền bỉ mọi bữa tiệc"
        ));
    }


    private void applyCategoryFilter(String filterCode, String subtitle, TextView selectedMenuView) {
        filteredList.clear();
        for (Product p : productList) {
            String cat = p.getCategory();
            if ("ALL".equals(filterCode)) {
                filteredList.add(p);
            } else if ("MOBILE".equals(filterCode)) {
                if ("Điện thoại".equals(cat) || "Máy tính bảng".equals(cat)) {
                    filteredList.add(p);
                }
            } else if ("AUDIO".equals(filterCode)) {
                if ("Thiết bị âm thanh".equals(cat)) {
                    filteredList.add(p);
                }
            } else if ("ACCESSORY".equals(filterCode)) {
                if (!"Điện thoại".equals(cat) && !"Máy tính bảng".equals(cat) && !"Thiết bị âm thanh".equals(cat)) {
                    filteredList.add(p);
                }
            }
        }

        if (tvHeaderSubtitle != null) {
            tvHeaderSubtitle.setText(subtitle);
        }

        // Cập nhật trạng thái nổi bật của mục đang chọn trong menu
        TextView[] menuItems = new TextView[]{menuHome, menuFilterMobile, menuFilterAccessory, menuFilterAudio};
        for (TextView item : menuItems) {
            if (item == null) continue;
            boolean isSelected = (item == selectedMenuView);
            item.setTextColor(ContextCompat.getColor(this, isSelected ? R.color.primary_dark : R.color.ink));
            item.setTypeface(null, isSelected ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }

        currentPage = 1;
        loadPageData(currentPage);
    }

    private int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) filteredList.size() / itemsPerPage));
    }

    // Tải dữ liệu cho từng trang
    private void loadPageData(int page) {
        currentPageList.clear();
        int totalItems = filteredList.size();
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);
        for (int i = startIndex; i < endIndex; i++) {
            currentPageList.add(filteredList.get(i));
        }

        int totalPages = getTotalPages();
        if (tvPage != null) {
            tvPage.setText(page + " / " + totalPages);
        }
        if (tvProductCount != null) {
            if (totalItems == 0) {
                tvProductCount.setText("0 sản phẩm");
            } else {
                tvProductCount.setText("Hiển thị " + (startIndex + 1) + "–" + endIndex + " / " + totalItems + " sản phẩm");
            }
        }
        if (btnPrev != null && btnNext != null) {
            boolean hasPrevious = page > 1;
            boolean hasNext = page < totalPages;
            btnPrev.setEnabled(hasPrevious);
            btnNext.setEnabled(hasNext);
            btnPrev.setAlpha(hasPrevious ? 1f : 0.42f);
            btnNext.setAlpha(hasNext ? 1f : 0.42f);
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void toggleMenu() {
        if (isMenuOpen) {
            closeMenu();
        } else {
            openMenu();
        }
    }

    private void openMenu() {
        isMenuOpen = true;
        btnToggleMenu.setIconResource(R.drawable.ic_close_24);
        layoutMenu.animate().cancel();
        layoutMenu.setAlpha(0f);
        layoutMenu.setTranslationY(-10f);
        layoutMenu.setVisibility(View.VISIBLE);
        layoutMenu.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(180L)
                .start();
    }

    private void closeMenu() {
        if (!isMenuOpen) return;
        isMenuOpen = false;
        btnToggleMenu.setIconResource(R.drawable.ic_menu_24);
        layoutMenu.animate().cancel();
        layoutMenu.animate()
                .alpha(0f)
                .translationY(-8f)
                .setDuration(140L)
                .withEndAction(() -> {
                    layoutMenu.setVisibility(View.GONE);
                    layoutMenu.setTranslationY(0f);
                })
                .start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CartManager.syncForCurrentUser(this);
        updateCartBadge();
    }

    private void updateCartBadge() {
        if (tvHeaderCartBadge != null) {
            int total = CartManager.getTotalItemCount();
            if (total > 0) {
                tvHeaderCartBadge.setVisibility(View.VISIBLE);
                tvHeaderCartBadge.setText(String.valueOf(total));
            } else {
                tvHeaderCartBadge.setVisibility(View.GONE);
            }
        }
    }

    private void performLogout() {
        // Xóa bộ nhớ tạm giỏ hàng của phiên hiện tại
        CartManager.onLogout();

        // Xóa phiên trong SharedPreferences
        SharedPreferences sessionPref = getSharedPreferences("UserSession", MODE_PRIVATE);
        sessionPref.edit().clear().apply();

        Intent intent = new Intent(ProductListActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
