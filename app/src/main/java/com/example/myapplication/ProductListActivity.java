package com.example.myapplication;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Set;

public class ProductListActivity extends AppCompatActivity {

    public static final String KEY_USER = "KEY_USER";
    public static ArrayList<Product> productList = new ArrayList<>();
    private final ArrayList<Product> filteredList = new ArrayList<>();
    private final ArrayList<Product> currentPageList = new ArrayList<>();
    private final ArrayList<Product> spotlightList = new ArrayList<>();

    private ProductAdapter adapter;
    private SpotlightAdapter spotlightAdapter;
    private ListenerRegistration firestoreListener;
    private int currentPage = 1, sortMode = 0;
    private final int itemsPerPage = 10;
    private boolean isMenuOpen = false, onlyInStock = false;
    private String currentFilter = "ALL", searchQuery = "";

    private View layoutMenu, btnClearSearch, cardAdminQuickBar, layoutMenuAdminSection;
    private RecyclerView rvSpotlight;
    private MaterialButton btnToggleMenu, btnPrev, btnNext;
    private TextView tvPage, btnPage1, btnPage2, tvProductCount, tvHeaderSubtitle, tvHeaderCartBadge, tvEmptySearch, tvSectionHeading, tvQuickWishlistLabel;
    private TextView menuSortPrice, menuInStockOnly;
    private EditText edtSearchProduct;
    private Chip chipFilterAll, chipFilterWishlist, chipFilterMobile, chipFilterTablet, chipFilterLaptop, chipFilterAccessory, chipFilterAudio, chipSortPrice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_list);
        UiMotion.applyInsets(findViewById(R.id.rootProductList));

        initViews();
        setupUserSession();
        setupMenuAndNavigation();
        loadLocalData();
        syncProductsWithFirestore();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CartManager.syncForCurrentUser(this);
        updateCartBadge();
        setupUserSession();
        updateWishlistQuickCount();
        ArrayList<Product> dbProducts = new DatabaseHelper(this).getAllProducts();
        if (!dbProducts.isEmpty()) {
            productList.clear();
            productList.addAll(dbProducts);
            updateSpotlightCarousel();
            refreshFilteredList();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (firestoreListener != null) firestoreListener.remove();
    }

    private void initViews() {
        layoutMenu = findViewById(R.id.layoutMenu);
        rvSpotlight = findViewById(R.id.rvSpotlight);
        tvSectionHeading = findViewById(R.id.tvSectionHeading);
        tvEmptySearch = findViewById(R.id.tvEmptySearch);
        tvQuickWishlistLabel = findViewById(R.id.tvQuickWishlistLabel);
        edtSearchProduct = findViewById(R.id.edtSearchProduct);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        btnToggleMenu = findViewById(R.id.btnToggleMenu);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        tvPage = findViewById(R.id.tvPage);
        btnPage1 = findViewById(R.id.btnPage1);
        btnPage2 = findViewById(R.id.btnPage2);
        tvProductCount = findViewById(R.id.tvProductCount);
        tvHeaderSubtitle = findViewById(R.id.tvHeaderSubtitle);
        tvHeaderCartBadge = findViewById(R.id.tvHeaderCartBadge);
        cardAdminQuickBar = findViewById(R.id.cardAdminQuickBar);
        layoutMenuAdminSection = findViewById(R.id.layoutMenuAdminSection);
        menuSortPrice = findViewById(R.id.menuSortPrice);
        menuInStockOnly = findViewById(R.id.menuInStockOnly);

        chipFilterAll = findViewById(R.id.chipFilterAll);
        chipFilterWishlist = findViewById(R.id.chipFilterWishlist);
        chipFilterMobile = findViewById(R.id.chipFilterMobile);
        chipFilterTablet = findViewById(R.id.chipFilterTablet);
        chipFilterLaptop = findViewById(R.id.chipFilterLaptop);
        chipFilterAccessory = findViewById(R.id.chipFilterAccessory);
        chipFilterAudio = findViewById(R.id.chipFilterAudio);
        chipSortPrice = findViewById(R.id.chipSortPrice);

        rvSpotlight.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        new LinearSnapHelper().attachToRecyclerView(rvSpotlight);
        spotlightAdapter = new SpotlightAdapter(spotlightList, this::openProductDetail);
        rvSpotlight.setAdapter(spotlightAdapter);

        RecyclerView rvProducts = findViewById(R.id.rvProducts);
        rvProducts.setLayoutManager(new GridLayoutManager(this, 2));
        rvProducts.setNestedScrollingEnabled(false);
        adapter = new ProductAdapter(currentPageList, new ProductAdapter.OnProductActionListener() {
            @Override public void onProductClick(Product product) { openProductDetail(product); }
            @Override public void onQuickAddCart(Product product) {
                CartManager.addToCart(product, 1);
                updateCartBadge();
                UiMotion.animateBadgePop(tvHeaderCartBadge);
                Toast.makeText(ProductListActivity.this, "Đã thêm " + product.getName() + " vào giỏ!", Toast.LENGTH_SHORT).show();
            }
            @Override public void onWishlistChanged() {
                updateWishlistQuickCount();
                if ("WISHLIST".equals(currentFilter)) refreshFilteredList();
            }
        });
        rvProducts.setAdapter(adapter);

        UiMotion.bindTouchScale(btnToggleMenu, findViewById(R.id.btnOpenCart), findViewById(R.id.btnHeaderProfile),
                btnPrev, btnNext, btnPage1, btnPage2, cardAdminQuickBar,
                findViewById(R.id.btnQuickFlashSale), findViewById(R.id.btnQuickWishlist),
                findViewById(R.id.btnQuickVoucher), findViewById(R.id.btnQuickOrders));
    }

    private String getActiveUser() {
        String user = getSharedPreferences("UserSession", MODE_PRIVATE).getString("CURRENT_USER", getIntent().getStringExtra(KEY_USER));
        return (user == null || user.trim().isEmpty()) ? "admin" : user.trim();
    }

    private void updateWishlistQuickCount() {
        int count = new DatabaseHelper(this).getWishlistedSkus(getActiveUser()).size();
        String label = count > 0 ? "Yêu thích (" + count + ")" : "Yêu thích";
        if (tvQuickWishlistLabel != null) tvQuickWishlistLabel.setText(label);
        if (chipFilterWishlist != null) chipFilterWishlist.setText(label);
    }

    private void openProductDetail(Product product) {
        closeMenu();
        Intent intent = new Intent(this, ProductDetailActivity.class);
        intent.putExtra(ProductDetailActivity.KEY_INDEX, Math.max(productList.indexOf(product), 0));
        startActivity(intent);
    }

    private void setupUserSession() {
        String user = getActiveUser();
        getSharedPreferences("UserSession", MODE_PRIVATE).edit().putString("CURRENT_USER", user).apply();
        boolean isAdmin = new DatabaseHelper(this).isAdminUser(user);

        TextView tvWelcome = findViewById(R.id.tvWelcomeUser);
        TextView tvBadge = findViewById(R.id.tvMenuAccountBadge);
        TextView tvAvatar = findViewById(R.id.tvHeaderAvatar);
        if (tvWelcome != null) tvWelcome.setText("Xin chào, " + user + (isAdmin ? " • Admin" : " • Thành viên"));
        if (tvBadge != null) tvBadge.setText("Tài khoản: " + user + (isAdmin ? " - Quản trị" : ""));
        if (tvAvatar != null) tvAvatar.setText(user.substring(0, 1).toUpperCase(Locale.ROOT));

        int vis = isAdmin ? View.VISIBLE : View.GONE;
        if (cardAdminQuickBar != null) cardAdminQuickBar.setVisibility(vis);
        if (layoutMenuAdminSection != null) layoutMenuAdminSection.setVisibility(vis);
    }

    private void setupMenuAndNavigation() {
        btnToggleMenu.setOnClickListener(v -> toggleMenu());
        findViewById(R.id.btnOpenCart).setOnClickListener(v -> openScreen(PaymentActivity.class));
        findViewById(R.id.menuCart).setOnClickListener(v -> openScreen(PaymentActivity.class));
        findViewById(R.id.menuOrders).setOnClickListener(v -> openScreen(OrderTrackingActivity.class));
        if (cardAdminQuickBar != null) cardAdminQuickBar.setOnClickListener(v -> openAdminTab(AdminDashboardActivity.TAB_PRODUCTS));
        findViewById(R.id.menuAdminProducts).setOnClickListener(v -> openAdminTab(AdminDashboardActivity.TAB_PRODUCTS));
        findViewById(R.id.menuAdminOrders).setOnClickListener(v -> openAdminTab(AdminDashboardActivity.TAB_ORDERS));
        findViewById(R.id.menuAdminUsers).setOnClickListener(v -> openAdminTab(AdminDashboardActivity.TAB_USERS));
        findViewById(R.id.menuProfile).setOnClickListener(v -> openScreen(ProfileActivity.class));
        findViewById(R.id.btnHeaderProfile).setOnClickListener(v -> openScreen(ProfileActivity.class));
        findViewById(R.id.tvMenuAccountBadge).setOnClickListener(v -> openScreen(ProfileActivity.class));

        findViewById(R.id.btnQuickFlashSale).setOnClickListener(v -> {
            sortMode = 0;
            applyCategoryFilter("ALL", "Săn Deal Giá Tốt Nhất");
            cycleSortMode();
            Toast.makeText(this, "Đã lọc sản phẩm Deal giá tốt nhất!", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnQuickWishlist).setOnClickListener(v -> {
            if ("WISHLIST".equals(currentFilter)) applyCategoryFilter("ALL", "Danh mục sản phẩm");
            else applyCategoryFilter("WISHLIST", "Sản phẩm Yêu thích đã lưu");
        });
        findViewById(R.id.btnQuickVoucher).setOnClickListener(v -> showVoucherCenterDialog());
        findViewById(R.id.btnQuickOrders).setOnClickListener(v -> openScreen(OrderTrackingActivity.class));

        edtSearchProduct.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s != null ? s.toString().trim().toLowerCase(Locale.ROOT) : "";
                btnClearSearch.setVisibility(searchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                refreshFilteredList();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        btnClearSearch.setOnClickListener(v -> edtSearchProduct.setText(""));

        menuSortPrice.setOnClickListener(v -> { closeMenu(); cycleSortMode(); });
        chipSortPrice.setOnClickListener(v -> cycleSortMode());

        menuInStockOnly.setOnClickListener(v -> {
            onlyInStock = !onlyInStock;
            menuInStockOnly.setText(onlyInStock ? "Tồn kho: Chỉ xem còn hàng" : "Tồn kho: Hiển thị tất cả");
            menuInStockOnly.setTextColor(ContextCompat.getColor(this, onlyInStock ? R.color.primary_dark : R.color.ink));
            menuInStockOnly.setTypeface(null, onlyInStock ? Typeface.BOLD : Typeface.NORMAL);
            closeMenu();
            refreshFilteredList();
        });

        chipFilterAll.setOnClickListener(v -> applyCategoryFilter("ALL", "Danh mục sản phẩm"));
        chipFilterWishlist.setOnClickListener(v -> applyCategoryFilter("WISHLIST", "Sản phẩm Yêu thích đã lưu"));
        chipFilterMobile.setOnClickListener(v -> applyCategoryFilter("MOBILE", "Điện thoại Flagship"));
        chipFilterTablet.setOnClickListener(v -> applyCategoryFilter("TABLET", "Máy tính bảng & Tablet"));
        chipFilterLaptop.setOnClickListener(v -> applyCategoryFilter("LAPTOP", "MacBook & Laptop"));
        chipFilterAccessory.setOnClickListener(v -> applyCategoryFilter("ACCESSORY", "Phụ kiện công nghệ"));
        chipFilterAudio.setOnClickListener(v -> applyCategoryFilter("AUDIO", "Thiết bị âm thanh"));

        findViewById(R.id.menuInfo).setOnClickListener(v -> {
            closeMenu();
            new MaterialAlertDialogBuilder(this).setTitle("Về TechZone Mall")
                    .setMessage("Phiên bản: 3.0 Pro Flagship\nHệ thống bán lẻ thiết bị công nghệ chính hãng, tích hợp đánh giá thực tế, Wishlist và Voucher ưu đãi.")
                    .setPositiveButton("Đóng", null).show();
        });
        findViewById(R.id.menuLogout).setOnClickListener(v -> UiMotion.logout(this));

        btnPrev.setOnClickListener(v -> { if (currentPage > 1) loadPageData(--currentPage); });
        btnNext.setOnClickListener(v -> { if (currentPage < getTotalPages()) loadPageData(++currentPage); });
        if (btnPage1 != null) btnPage1.setOnClickListener(v -> { if (currentPage != 1) loadPageData(currentPage = 1); });
        if (btnPage2 != null) btnPage2.setOnClickListener(v -> { if (getTotalPages() >= 2 && currentPage != 2) loadPageData(currentPage = 2); });
    }

    private void showVoucherCenterDialog() {
        String[] vouchers = {
                "TECH500K - Giảm ngay 500.000đ (Đơn từ 10.000.000đ)",
                "NEWMEMBER - Giảm 5% tối đa 1.000.000đ cho mọi đơn hàng",
                "FREESHIP2H - Miễn phí giao hỏa tốc 2h + giảm 150.000đ"
        };
        String[] codes = {"TECH500K", "NEWMEMBER", "FREESHIP2H"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Kho Mã Ưu Đãi TechZone")
                .setItems(vouchers, (dialog, which) -> {
                    getSharedPreferences("UserSession", MODE_PRIVATE).edit().putString("SAVED_VOUCHER", codes[which]).apply();
                    Toast.makeText(this, "Đã lưu mã " + codes[which] + " để tự động áp dụng khi thanh toán!", Toast.LENGTH_SHORT).show();
                })
                .setPositiveButton("Đến Giỏ hàng", (d, w) -> openScreen(PaymentActivity.class))
                .setNegativeButton("Đóng", null).show();
    }

    private void openScreen(Class<?> target) {
        closeMenu();
        startActivity(new Intent(this, target));
    }

    private void openAdminTab(int tab) {
        closeMenu();
        Intent intent = new Intent(this, AdminDashboardActivity.class);
        intent.putExtra(AdminDashboardActivity.EXTRA_INITIAL_TAB, tab);
        startActivity(intent);
    }

    private void loadLocalData() {
        ArrayList<Product> dbProducts = new DatabaseHelper(this).getAllProducts();
        productList.clear();
        productList.addAll(!dbProducts.isEmpty() ? dbProducts : createDefaultProducts());
        updateSpotlightCarousel();
        updateWishlistQuickCount();
        applyCategoryFilter("ALL", "Danh mục sản phẩm");
    }

    private void syncProductsWithFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        firestoreListener = db.collection("products").addSnapshotListener((snapshots, error) -> {
            if (snapshots == null || error != null || isFinishing() || isDestroyed()) return;
            if (snapshots.isEmpty()) {
                WriteBatch batch = db.batch();
                for (Product p : createDefaultProducts()) batch.set(db.collection("products").document(p.getSku()), p.toMap());
                batch.commit();
            } else {
                HashMap<String, String> localPaths = new HashMap<>();
                for (Product local : new DatabaseHelper(this).getAllProducts()) {
                    if (!local.getImagePath().isEmpty()) localPaths.put(local.getSku(), local.getImagePath());
                }
                productList.clear();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Product p = Product.fromFirestore(doc);
                    if (localPaths.containsKey(p.getSku())) p.setImagePath(localPaths.get(p.getSku()));
                    productList.add(p);
                }
                updateSpotlightCarousel();
                applyCategoryFilter(currentFilter, tvHeaderSubtitle != null ? tvHeaderSubtitle.getText().toString() : "Danh mục sản phẩm");
            }
        });
    }

    private void updateSpotlightCarousel() {
        spotlightList.clear();
        for (Product p : productList) {
            if (p.isInStock() && spotlightList.size() < 5) spotlightList.add(p);
        }
        if (spotlightList.isEmpty() && !productList.isEmpty()) spotlightList.add(productList.get(0));
        if (spotlightAdapter != null) spotlightAdapter.notifyDataSetChanged();
        updateSpotlightVisibility();
    }

    private void updateSpotlightVisibility() {
        if (rvSpotlight != null) {
            rvSpotlight.setVisibility((!spotlightList.isEmpty() && "ALL".equals(currentFilter) && searchQuery.isEmpty()) ? View.VISIBLE : View.GONE);
        }
    }

    private void cycleSortMode() {
        sortMode = (sortMode + 1) % 3;
        boolean active = sortMode != 0;
        menuSortPrice.setText(sortMode == 1 ? "Sắp xếp giá: Thấp đến cao" : (sortMode == 2 ? "Sắp xếp giá: Cao đến thấp" : "Sắp xếp giá: Mặc định"));
        menuSortPrice.setTextColor(ContextCompat.getColor(this, active ? R.color.primary_dark : R.color.ink));
        chipSortPrice.setText(sortMode == 1 ? "Giá: Thấp → Cao" : (sortMode == 2 ? "Giá: Cao → Thấp" : "Giá: Mặc định"));
        chipSortPrice.setChipBackgroundColorResource(active ? R.color.primary : R.color.surface);
        chipSortPrice.setTextColor(ContextCompat.getColor(this, active ? R.color.white : R.color.ink));
        chipSortPrice.setChipIconTintResource(active ? R.color.white : R.color.primary);
        refreshFilteredList();
    }

    private void applyCategoryFilter(String filter, String subtitle) {
        closeMenu();
        currentFilter = filter;
        if (tvHeaderSubtitle != null) tvHeaderSubtitle.setText(subtitle);
        if (tvSectionHeading != null) tvSectionHeading.setText(subtitle);

        chipFilterAll.setChecked("ALL".equals(filter));
        chipFilterWishlist.setChecked("WISHLIST".equals(filter));
        chipFilterMobile.setChecked("MOBILE".equals(filter));
        chipFilterTablet.setChecked("TABLET".equals(filter));
        chipFilterLaptop.setChecked("LAPTOP".equals(filter));
        chipFilterAccessory.setChecked("ACCESSORY".equals(filter));
        chipFilterAudio.setChecked("AUDIO".equals(filter));
        refreshFilteredList();
    }

    private void refreshFilteredList() {
        filteredList.clear();
        Set<String> wishlisted = "WISHLIST".equals(currentFilter) ? new DatabaseHelper(this).getWishlistedSkus(getActiveUser()) : null;
        for (Product p : productList) {
            boolean matchCat = "WISHLIST".equals(currentFilter)
                    ? (wishlisted != null && wishlisted.contains(p.getSku()))
                    : matchesCategory(p.getCategory(), currentFilter);
            if (matchCat && matchesSearch(p) && (!onlyInStock || p.isInStock())) {
                filteredList.add(p);
            }
        }
        if (sortMode != 0) {
            Collections.sort(filteredList, (a, b) -> {
                long pa = CartManager.parsePrice(a.getPrice()), pb = CartManager.parsePrice(b.getPrice());
                return sortMode == 1 ? Long.compare(pa, pb) : Long.compare(pb, pa);
            });
        }
        updateSpotlightVisibility();
        loadPageData(currentPage = 1);
    }

    private boolean matchesSearch(Product p) {
        return searchQuery.isEmpty()
                || p.getName().toLowerCase(Locale.ROOT).contains(searchQuery)
                || p.getDescription().toLowerCase(Locale.ROOT).contains(searchQuery)
                || p.getCategory().toLowerCase(Locale.ROOT).contains(searchQuery);
    }

    private boolean matchesCategory(String cat, String filter) {
        if ("ALL".equals(filter) || cat == null) return true;
        switch (filter) {
            case "MOBILE": return "Điện thoại".equals(cat);
            case "TABLET": return "Máy tính bảng".equals(cat) || "Tablet".equalsIgnoreCase(cat);
            case "LAPTOP": return "Máy tính xách tay".equals(cat) || "Laptop".equalsIgnoreCase(cat);
            case "AUDIO": return "Thiết bị âm thanh".equals(cat);
            case "ACCESSORY":
                return !matchesCategory(cat, "MOBILE") && !matchesCategory(cat, "TABLET")
                        && !matchesCategory(cat, "LAPTOP") && !matchesCategory(cat, "AUDIO");
            default: return true;
        }
    }

    private void loadPageData(int page) {
        currentPageList.clear();
        int total = filteredList.size();
        int start = (page - 1) * itemsPerPage, end = Math.min(start + itemsPerPage, total);
        for (int i = start; i < end; i++) currentPageList.add(filteredList.get(i));

        tvEmptySearch.setVisibility(total == 0 ? View.VISIBLE : View.GONE);
        tvEmptySearch.setText(("WISHLIST".equals(currentFilter) && total == 0)
                ? "Chưa có sản phẩm yêu thích nào. Hãy nhấn biểu tượng trái tim trên sản phẩm để lưu!"
                : "Không tìm thấy sản phẩm phù hợp.");

        int totalPages = getTotalPages();
        tvPage.setText("Trang " + page + "/" + totalPages);
        tvProductCount.setText(total == 0 ? "0 sản phẩm" : (start + 1) + "-" + end + " / " + total + " SP");

        stylePageDot(btnPage1, page == 1);
        if (btnPage2 != null) {
            btnPage2.setVisibility(totalPages >= 2 ? View.VISIBLE : View.GONE);
            btnPage2.setText(String.valueOf(Math.max(2, page)));
            stylePageDot(btnPage2, page >= 2);
        }

        btnPrev.setEnabled(page > 1);
        btnPrev.setAlpha(page > 1 ? 1f : 0.38f);
        btnNext.setEnabled(page < totalPages);
        btnNext.setAlpha(page < totalPages ? 1f : 0.38f);
        adapter.notifyDataSetChanged();
        UiMotion.animateContentSwitch(findViewById(R.id.rvProducts));
    }

    private void stylePageDot(TextView dot, boolean active) {
        if (dot == null) return;
        dot.setBackgroundResource(active ? R.drawable.bg_page_dot_active : R.drawable.bg_page_dot_inactive);
        dot.setTextColor(ContextCompat.getColor(this, active ? R.color.white : R.color.text_secondary));
    }

    private int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) filteredList.size() / itemsPerPage));
    }

    private void toggleMenu() {
        isMenuOpen = !isMenuOpen;
        btnToggleMenu.setIconResource(isMenuOpen ? R.drawable.ic_close_24 : R.drawable.ic_menu_24);
        UiMotion.togglePanel(layoutMenu, isMenuOpen);
    }

    private void closeMenu() {
        if (isMenuOpen) toggleMenu();
    }

    private void updateCartBadge() {
        int count = CartManager.getTotalItemCount();
        tvHeaderCartBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        if (count > 0) tvHeaderCartBadge.setText(String.valueOf(count));
    }

    public static ArrayList<Product> createDefaultProducts() {
        ArrayList<Product> list = new ArrayList<>();
        list.add(new Product("iPhone 16 Pro Max", "256GB - Titan Tự Nhiên - Chip A18 Pro", "34.990.000đ", R.drawable.img_iphone_16, "Điện thoại", "TZ-IP16PM-256", "12 tháng Apple VN", "• Màn hình: Super Retina XDR OLED 6.9 inch, ProMotion 120Hz\n• Vi xử lý: Apple A18 Pro tiến trình 3nm thế hệ thứ 2\n• Camera sau: Hệ thống 3 camera 48MP + Telephoto 5x\n• Chất liệu: Khung viền Titan cấp độ 5 bền nhẹ, cổng USB-C", true));
        list.add(new Product("Samsung Galaxy S24 Ultra", "512GB - Xám Titan - Tích hợp S-Pen", "28.990.000đ", R.drawable.img_samsung_s24, "Điện thoại", "TZ-S24U-512", "12 tháng chính hãng", "• Màn hình: Dynamic AMOLED 2X 6.8 inch QHD+, 120Hz\n• Vi xử lý: Snapdragon 8 Gen 3 for Galaxy\n• Camera sau: 200MP + Zoom quang học 5x 50MP\n• Tính năng: Khung viền Titan, bút S-Pen tích hợp sẵn", true));
        list.add(new Product("MacBook Air M3 13 inch", "RAM 16GB - SSD 512GB - Vỏ nhôm nguyên khối", "27.990.000đ", R.drawable.img_macbook_m3, "Máy tính xách tay", "TZ-MBA-M3-16", "12 tháng Apple VN", "• Màn hình: Liquid Retina 13.6 inch, độ sáng 500 nits, True Tone\n• Vi xử lý: Apple M3 (8 nhân CPU, 10 nhân GPU)\n• Bộ nhớ: RAM hợp nhất 16GB, ổ cứng SSD 512GB tốc độ cao\n• Thời lượng pin: Lên đến 18 giờ, sạc từ tính MagSafe 3", true));
        list.add(new Product("iPad Pro M4 11 inch", "256GB Wi-Fi - Màn hình Ultra Retina XDR", "26.490.000đ", R.drawable.img_ipad_pro_m4, "Máy tính bảng", "TZ-IPADP-M4", "12 tháng Apple VN", "• Màn hình: Tandem OLED Ultra Retina XDR 11 inch, ProMotion 120Hz\n• Vi xử lý: Chip Apple M4 hiệu năng đồ họa vượt trội\n• Thiết kế: Siêu mỏng 5.3 mm, trọng lượng chỉ 444g\n• Kết nối: Cổng Thunderbolt / USB 4, hỗ trợ Apple Pencil Pro", true));
        list.add(new Product("Tai nghe AirPods Pro 2", "Hộp sạc MagSafe USB-C - Chống ồn chủ động", "5.690.000đ", R.drawable.img_airpods_pro, "Thiết bị âm thanh", "TZ-APP2-USBC", "12 tháng chính hãng", "• Chip xử lý âm thanh: Apple H2 cho âm thanh chi tiết\n• Chống ồn: Chủ động (ANC) gấp 2 lần và chế độ Xuyên âm thích ứng\n• Kháng bụi và nước: Chuẩn IP54 cho cả tai nghe và hộp sạc\n• Thời lượng pin: Nghe nhạc lên đến 30 giờ kèm hộp sạc", false));
        list.add(new Product("Tai nghe Sony WH-1000XM5", "Chống ồn đa micro - Pin 30 giờ - Hi-Res Audio", "7.490.000đ", R.drawable.img_sony_wh1000xm5, "Thiết bị âm thanh", "TZ-SN-XM5", "12 tháng Sony VN", "• Công nghệ chống ồn: Bộ xử lý V1 + QN1 kết hợp 8 micro thu âm\n• Màng loa: Sợi carbon 30mm thiết kế đặc biệt cho độ chi tiết cao\n• Kết nối: Bluetooth 5.2 đa điểm (2 thiết bị cùng lúc), hỗ trợ LDAC\n• Sạc nhanh: Sạc 3 phút qua chuẩn USB-PD cho 3 giờ sử dụng", false));
        list.add(new Product("Apple Watch Series 10", "Viền Nhôm GPS 42mm - Màn hình OLED góc rộng", "10.990.000đ", R.drawable.img_apple_watch_series10, "Đồng hồ thông minh", "TZ-AWS10-42", "12 tháng Apple VN", "• Màn hình: Retina Luôn Bật LTPO3 OLED góc rộng, sáng hơn 40%\n• Vi xử lý: SiP S10 64-bit lõi kép với Neural Engine 4 lõi\n• Theo dõi sức khỏe: Nhịp tim quang học, giấc ngủ, độ sâu nước\n• Kháng nước: Độ sâu 50m, sạc nhanh 80% pin trong 30 phút", true));
        list.add(new Product("Bàn phím cơ Keychron K8 Pro", "Khung nhôm CNC - Hotswap - Bluetooth 5.1", "2.490.000đ", R.drawable.img_keychron, "Phụ kiện", "TZ-KC-K8PRO", "12 tháng chính hãng", "• Thiết kế: Layout TKL (80%), khung viền nhôm nguyên khối chắc chắn\n• Tùy biến: Hỗ trợ QMK/VIA, mạch Hotswap 5 pin thay switch dễ dàng\n• Keycap: PBT Double-shot OSA profile chống bóng, lót foam tiêu âm\n• Kết nối: Không dây Bluetooth 5.1 (3 thiết bị) và có dây Type-C", false));
        list.add(new Product("Chuột Logitech MX Master 3S", "Cảm biến Darkfield 8000 DPI - Click siêu êm", "2.190.000đ", R.drawable.img_logitech_mx, "Phụ kiện", "TZ-LG-MX3S", "24 tháng chính hãng", "• Cảm biến: Quang học Darkfield 8000 DPI hoạt động tốt trên mặt kính\n• Nút bấm: Công nghệ Quiet Click giảm 90% tiếng ồn khi thao tác\n• Con lăn: Điện từ MagSpeed cuộn 1.000 dòng mỗi giây\n• Kết nối: Đầu thu Logi Bolt USB hoặc Bluetooth Low Energy", true));
        list.add(new Product("Loa Marshall Emberton II", "Âm thanh đa hướng 360 độ - Pin hơn 30 giờ", "3.990.000đ", R.drawable.img_marshall_emberton, "Thiết bị âm thanh", "TZ-MS-EMB2", "12 tháng chính hãng", "• Công suất: 2 x 10W mạch khuếch đại Class D\n• Chuẩn kháng nước: IP67 chống bụi và nước cao cấp\n• Kết nối: Bluetooth 5.1, chế độ ghép đôi Stack Mode\n• Thời lượng pin: Lên đến hơn 30 giờ chơi nhạc liên tục", true));
        list.add(new Product("Tai nghe Marshall Major IV", "Pin kỷ lục 80 giờ - Sạc không dây tiện lợi", "3.190.000đ", R.drawable.img_marshall_major, "Thiết bị âm thanh", "TZ-MS-MJ4", "12 tháng chính hãng", "• Thiết kế: On-ear cổ điển đặc trưng Marshall, có thể gập gọn\n• Pin: Hơn 80 giờ nghe nhạc, sạc nhanh 15 phút nghe 15 giờ\n• Phím điều khiển: Núm xoay đa hướng Brass Control Knob\n• Kết nối: Bluetooth 5.0 và cổng AUX 3.5mm chia sẻ âm thanh", true));
        list.add(new Product("Loa JBL Charge 5", "Âm trầm Original Pro Sound - Kháng nước IP67", "3.490.000đ", R.drawable.img_jbl_charge5, "Thiết bị âm thanh", "TZ-JBL-CHG5", "12 tháng chính hãng", "• Âm thanh: Driver dạng racetrack cải tiến cho âm trầm uy lực\n• Tính năng sản phẩm: Cổng USB Type-A sạc điện thoại\n• Kết nối: PartyBoost kết nối nhiều loa JBL cùng lúc\n• Thời lượng pin: 20 giờ chơi nhạc bền bỉ mọi bữa tiệc", true));
        return list;
    }
}
