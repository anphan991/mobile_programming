package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bảng điều khiển quản trị viên (chỉ dành cho tài khoản Admin):
 * - Quản lý Sản phẩm (thêm, sửa giá/thông tin/tồn kho, xóa)
 * - Quản lý Đơn hàng của toàn bộ khách hàng (xem chi tiết, cập nhật trạng thái 5 bước)
 * - Quản lý Người dùng (thêm, sửa thông tin/mật khẩu/phân quyền, xóa)
 */
public class AdminDashboardActivity extends AppCompatActivity {

    public static final String EXTRA_INITIAL_TAB = "EXTRA_INITIAL_TAB";
    public static final int TAB_PRODUCTS = 0;
    public static final int TAB_ORDERS = 1;
    public static final int TAB_USERS = 2;

    private DatabaseHelper dbHelper;
    private String currentUser;
    private int currentTab = TAB_PRODUCTS;
    private final ProductImagePicker imagePicker = new ProductImagePicker(this);

    private MaterialButton btnTabProducts, btnTabOrders, btnTabUsers, btnAdminAdd;
    private TextView tvAdminSectionCount, tvAdminEmpty;
    private RecyclerView rvAdminList;

    private final List<Product> products = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();
    private final List<UserAccount> users = new ArrayList<>();

    private ListenerRegistration allOrdersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        View root = findViewById(R.id.rootAdminDashboard);
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

        dbHelper = new DatabaseHelper(this);
        SharedPreferences session = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentUser = session.getString("CURRENT_USER", "");

        if (!dbHelper.isAdminUser(currentUser)) {
            Toast.makeText(this, "Chỉ quản trị viên mới có quyền truy cập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentTab = getIntent().getIntExtra(EXTRA_INITIAL_TAB, TAB_PRODUCTS);

        bindViews();
        setupListeners();
        listenAllCloudOrders();
        switchTab(currentTab);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCurrentTab();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (allOrdersListener != null) {
            allOrdersListener.remove();
            allOrdersListener = null;
        }
    }

    private void bindViews() {
        btnTabProducts = findViewById(R.id.btnTabProducts);
        btnTabOrders = findViewById(R.id.btnTabOrders);
        btnTabUsers = findViewById(R.id.btnTabUsers);
        btnAdminAdd = findViewById(R.id.btnAdminAdd);
        tvAdminSectionCount = findViewById(R.id.tvAdminSectionCount);
        tvAdminEmpty = findViewById(R.id.tvAdminEmpty);
        rvAdminList = findViewById(R.id.rvAdminList);
        rvAdminList.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnTabProducts.setOnClickListener(v -> switchTab(TAB_PRODUCTS));
        btnTabOrders.setOnClickListener(v -> switchTab(TAB_ORDERS));
        btnTabUsers.setOnClickListener(v -> switchTab(TAB_USERS));

        btnAdminAdd.setOnClickListener(v -> {
            if (currentTab == TAB_PRODUCTS) {
                ProductEditorHelper.showProductEditor(this, imagePicker, null, this::refreshCurrentTab);
            } else if (currentTab == TAB_USERS) {
                showUserEditorDialog(null);
            }
        });
    }

    private void switchTab(int tab) {
        currentTab = tab;
        styleTabButton(btnTabProducts, tab == TAB_PRODUCTS);
        styleTabButton(btnTabOrders, tab == TAB_ORDERS);
        styleTabButton(btnTabUsers, tab == TAB_USERS);

        if (tab == TAB_PRODUCTS) {
            btnAdminAdd.setVisibility(View.VISIBLE);
            btnAdminAdd.setText("+ Thêm sản phẩm");
        } else if (tab == TAB_USERS) {
            btnAdminAdd.setVisibility(View.VISIBLE);
            btnAdminAdd.setText("+ Thêm người dùng");
        } else {
            btnAdminAdd.setVisibility(View.GONE);
        }

        refreshCurrentTab();
    }

    private void styleTabButton(MaterialButton button, boolean active) {
        if (active) {
            button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.primary));
            button.setTextColor(ContextCompat.getColor(this, R.color.white));
            button.setStrokeWidth(0);
        } else {
            button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.surface));
            button.setTextColor(ContextCompat.getColor(this, R.color.ink));
            button.setStrokeColor(ContextCompat.getColorStateList(this, R.color.border));
            button.setStrokeWidth(Math.round(getResources().getDisplayMetrics().density));
        }
    }

    private void refreshCurrentTab() {
        if (currentTab == TAB_PRODUCTS) {
            products.clear();
            products.addAll(dbHelper.getAllProducts());
            ProductListActivity.productList.clear();
            ProductListActivity.productList.addAll(products);

            tvAdminSectionCount.setText("Tổng: " + products.size() + " sản phẩm");
            rvAdminList.setAdapter(new AdminProductAdapter());
            toggleEmpty(products.isEmpty(), "Chưa có sản phẩm nào");
        } else if (currentTab == TAB_ORDERS) {
            orders.clear();
            orders.addAll(dbHelper.getAllOrders());

            tvAdminSectionCount.setText("Tổng: " + orders.size() + " đơn hàng");
            rvAdminList.setAdapter(new AdminOrderAdapter());
            toggleEmpty(orders.isEmpty(), "Chưa có đơn hàng nào");
        } else {
            users.clear();
            users.addAll(dbHelper.getAllUsers());

            tvAdminSectionCount.setText("Tổng: " + users.size() + " tài khoản");
            rvAdminList.setAdapter(new AdminUserAdapter());
            toggleEmpty(users.isEmpty(), "Chưa có người dùng nào");
        }
    }

    private void toggleEmpty(boolean empty, String msg) {
        rvAdminList.setVisibility(empty ? View.GONE : View.VISIBLE);
        tvAdminEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        tvAdminEmpty.setText(msg);
    }

    private void listenAllCloudOrders() {
        allOrdersListener = FirebaseFirestore.getInstance()
                .collection("orders")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null || isFinishing() || isDestroyed()) {
                        return;
                    }
                    Map<String, Integer> localSteps = new HashMap<>();
                    for (Order o : dbHelper.getAllOrders()) {
                        localSteps.put(o.getOrderCode(), o.getStatusStep());
                    }

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String code = doc.getString("orderCode");
                        if (code == null || code.isEmpty()) code = doc.getId();
                        String owner = doc.getString("userEmail");
                        if (owner == null || owner.isEmpty()) owner = "user";

                        Long itemsLong = doc.getLong("totalItems");
                        int itemCount = itemsLong != null ? itemsLong.intValue() : 1;

                        int step = Order.STEP_PROCESSING;
                        Integer localStep = localSteps.get(code);
                        if (doc.contains("statusStep")) {
                            Long stepLong = doc.getLong("statusStep");
                            int cloudStep = stepLong != null ? Order.clampStep(stepLong.intValue()) : Order.STEP_PROCESSING;
                            if (doc.getMetadata().hasPendingWrites() && localStep != null) {
                                step = localStep;
                            } else {
                                step = cloudStep;
                            }
                        } else if (localStep != null) {
                            step = localStep;
                        }

                        dbHelper.upsertOrderFromCloud(new Order(
                                code,
                                owner,
                                doc.getString("receiverName"),
                                doc.getString("receiverPhone"),
                                doc.getString("receiverAddress"),
                                doc.getString("receiverNote"),
                                doc.getString("paymentMethod"),
                                doc.getString("totalPrice"),
                                itemCount,
                                doc.getString("createdAt"),
                                step,
                                doc.getString("itemsSummary"),
                                doc.getString("cancelReason"),
                                doc.getString("itemsRaw")
                        ));
                    }
                    if (currentTab == TAB_ORDERS) {
                        refreshCurrentTab();
                    }
                });
    }

    private void showOrderStatusPicker(Order order) {
        String[] stepTitles = new String[]{
                "1. " + Order.getStepTitle(Order.STEP_PROCESSING),
                "2. " + Order.getStepTitle(Order.STEP_PACKING),
                "3. " + Order.getStepTitle(Order.STEP_SHIPPED_CARRIER),
                "4. " + Order.getStepTitle(Order.STEP_OUT_FOR_DELIVERY),
                "5. " + Order.getStepTitle(Order.STEP_DELIVERED),
                "Hủy đơn hàng (nhập lý do)"
        };
        int checked = order.getStatusStep() == Order.STEP_CANCELLED ? 5 : order.getStatusStep();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Cập nhật đơn #" + order.getOrderCode())
                .setSingleChoiceItems(stepTitles, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 5) {
                        OrderCancelHelper.showCancelDialog(
                                AdminDashboardActivity.this,
                                order,
                                true,
                                reason -> refreshCurrentTab()
                        );
                        return;
                    }
                    int clamped = Order.clampStep(which);
                    dbHelper.updateOrderStatus(order.getOrderCode(), clamped);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("statusStep", clamped);
                    updates.put("status", Order.getStepTitle(clamped));
                    FirebaseFirestore.getInstance()
                            .collection("orders")
                            .document(order.getOrderCode())
                            .set(updates, SetOptions.merge());

                    Toast.makeText(this,
                            "Đơn #" + order.getOrderCode() + " -> " + Order.getStepTitle(clamped),
                            Toast.LENGTH_SHORT).show();
                    refreshCurrentTab();
                })
                .setNegativeButton("Đóng", null)
                .show();
    }

    private void showUserEditorDialog(UserAccount existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_admin_user, null);
        TextInputEditText edtUsername = view.findViewById(R.id.edtUserUsername);
        TextInputEditText edtPassword = view.findViewById(R.id.edtUserPassword);
        RadioButton rbUser = view.findViewById(R.id.rbRoleUser);
        RadioButton rbAdmin = view.findViewById(R.id.rbRoleAdmin);

        boolean isEdit = existing != null;
        boolean isRootAdmin = isEdit && "admin".equalsIgnoreCase(existing.getUsername());
        if (isEdit) {
            edtUsername.setText(existing.getUsername());
            edtPassword.setText(existing.getPassword());
            if (existing.isAdmin()) {
                rbAdmin.setChecked(true);
            } else {
                rbUser.setChecked(true);
            }
            if (isRootAdmin) {
                edtUsername.setEnabled(false);
                rbUser.setEnabled(false);
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(isEdit ? "Chỉnh sửa người dùng" : "Thêm người dùng mới")
                .setView(view)
                .setNegativeButton("Hủy", null)
                .setPositiveButton(isEdit ? "Lưu thay đổi" : "Thêm tài khoản", (dialog, which) -> {
                    String username = edtUsername.getText() != null ? edtUsername.getText().toString().trim() : "";
                    String password = edtPassword.getText() != null ? edtPassword.getText().toString().trim() : "";
                    String role = rbAdmin.isChecked() ? UserAccount.ROLE_ADMIN : UserAccount.ROLE_USER;

                    if (username.isEmpty() || password.length() < 6) {
                        Toast.makeText(this, "Tên đăng nhập không được trống và mật khẩu tối thiểu 6 ký tự!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (isEdit) {
                        if (!username.equalsIgnoreCase(existing.getUsername()) && dbHelper.checkUserExists(username)) {
                            Toast.makeText(this, "Tên đăng nhập đã tồn tại!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (dbHelper.updateUser(existing.getId(), username, password, role)) {
                            Toast.makeText(this, "Đã cập nhật tài khoản " + username, Toast.LENGTH_SHORT).show();
                            refreshCurrentTab();
                        } else {
                            Toast.makeText(this, "Không thể cập nhật tài khoản!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        if (dbHelper.checkUserExists(username)) {
                            Toast.makeText(this, "Tài khoản này đã tồn tại!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if (dbHelper.addUser(username, password, role)) {
                            Toast.makeText(this, "Đã tạo tài khoản " + username, Toast.LENGTH_SHORT).show();
                            refreshCurrentTab();
                        } else {
                            Toast.makeText(this, "Không thể tạo tài khoản!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    private void confirmDeleteUser(UserAccount user) {
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            Toast.makeText(this, "Không thể xóa tài khoản quản trị mặc định (admin)!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (user.getUsername().equalsIgnoreCase(currentUser)) {
            Toast.makeText(this, "Không thể xóa tài khoản đang đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa người dùng")
                .setMessage("Bạn có chắc muốn xóa tài khoản \"" + user.getUsername() + "\"?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    dbHelper.deleteUser(user.getId());
                    Toast.makeText(this, "Đã xóa người dùng " + user.getUsername(), Toast.LENGTH_SHORT).show();
                    refreshCurrentTab();
                })
                .show();
    }

    private class AdminProductAdapter extends RecyclerView.Adapter<AdminProductAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_product, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Product p = products.get(position);
            ProductImageHelper.bindImage(h.img, p);
            h.name.setText(p.getName());
            h.meta.setText(p.getSku() + " - " + p.getCategory());
            h.price.setText(p.getPrice());
            if (p.isInStock()) {
                h.stock.setText("Còn hàng");
                h.stock.setBackgroundResource(R.drawable.bg_badge_success);
                h.stock.setTextColor(ContextCompat.getColor(AdminDashboardActivity.this, R.color.success));
            } else {
                h.stock.setText("Hết hàng");
                h.stock.setBackgroundResource(R.drawable.bg_badge_danger);
                h.stock.setTextColor(ContextCompat.getColor(AdminDashboardActivity.this, R.color.danger));
            }
            h.btnEdit.setOnClickListener(v -> ProductEditorHelper.showProductEditor(
                    AdminDashboardActivity.this, imagePicker, p, AdminDashboardActivity.this::refreshCurrentTab));
            h.btnDelete.setOnClickListener(v -> ProductEditorHelper.confirmDeleteProduct(
                    AdminDashboardActivity.this, p, AdminDashboardActivity.this::refreshCurrentTab));
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final ImageView img;
            final TextView name, meta, price, stock;
            final MaterialButton btnEdit, btnDelete;

            VH(@NonNull View itemView) {
                super(itemView);
                img = itemView.findViewById(R.id.imgAdminProd);
                name = itemView.findViewById(R.id.tvAdminProdName);
                meta = itemView.findViewById(R.id.tvAdminProdMeta);
                price = itemView.findViewById(R.id.tvAdminProdPrice);
                stock = itemView.findViewById(R.id.tvAdminProdStock);
                btnEdit = itemView.findViewById(R.id.btnEditProduct);
                btnDelete = itemView.findViewById(R.id.btnDeleteProduct);
            }
        }
    }

    private class AdminOrderAdapter extends RecyclerView.Adapter<AdminOrderAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_order, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Order o = orders.get(position);
            int step = o.getStatusStep();
            boolean cancelled = step == Order.STEP_CANCELLED;
            boolean delivered = step >= Order.STEP_DELIVERED;

            h.code.setText("#" + o.getOrderCode());
            h.user.setText("Tài khoản: " + o.getUserEmail() + " - " + o.getCreatedAt());
            h.badge.setText(Order.getStepTitle(step));
            if (cancelled) {
                h.badge.setBackgroundResource(R.drawable.bg_badge_danger);
                h.badge.setTextColor(ContextCompat.getColor(AdminDashboardActivity.this, R.color.danger));
            } else if (delivered) {
                h.badge.setBackgroundResource(R.drawable.bg_badge_success);
                h.badge.setTextColor(ContextCompat.getColor(AdminDashboardActivity.this, R.color.success));
            } else {
                h.badge.setBackgroundResource(R.drawable.bg_badge_primary);
                h.badge.setTextColor(ContextCompat.getColor(AdminDashboardActivity.this, R.color.primary_dark));
            }
            h.info.setText(o.getReceiverName() + " (" + o.getReceiverPhone() + ") - " + o.getTotalPrice());
            if (cancelled && o.getCancelReason() != null && !o.getCancelReason().trim().isEmpty()) {
                h.address.setText("Lý do hủy: " + o.getCancelReason());
            } else {
                h.address.setText("Địa chỉ: " + o.getReceiverAddress());
            }

            h.btnDetail.setOnClickListener(v -> {
                Intent intent = new Intent(AdminDashboardActivity.this, OrderDetailActivity.class);
                intent.putExtra(OrderDetailActivity.EXTRA_ORDER_CODE, o.getOrderCode());
                startActivity(intent);
            });
            h.btnUpdate.setOnClickListener(v -> showOrderStatusPicker(o));
        }

        @Override
        public int getItemCount() {
            return orders.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView code, user, badge, info, address;
            final MaterialButton btnDetail, btnUpdate;

            VH(@NonNull View itemView) {
                super(itemView);
                code = itemView.findViewById(R.id.tvAdminOrderCode);
                user = itemView.findViewById(R.id.tvAdminOrderUser);
                badge = itemView.findViewById(R.id.tvAdminOrderBadge);
                info = itemView.findViewById(R.id.tvAdminOrderInfo);
                address = itemView.findViewById(R.id.tvAdminOrderAddress);
                btnDetail = itemView.findViewById(R.id.btnAdminOrderDetail);
                btnUpdate = itemView.findViewById(R.id.btnAdminUpdateStatus);
            }
        }
    }

    private class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.VH> {
        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            UserAccount u = users.get(position);
            String uname = u.getUsername();
            h.avatar.setText(uname.isEmpty() ? "U" : uname.substring(0, 1).toUpperCase(Locale.ROOT));
            h.name.setText(uname);
            h.pass.setText("Mật khẩu: " + u.getPassword());

            if (u.isAdmin()) {
                h.role.setText("ADMIN");
                h.role.setBackgroundResource(R.drawable.bg_badge_primary);
                h.role.setTextColor(ContextCompat.getColor(AdminDashboardActivity.this, R.color.primary_dark));
            } else {
                h.role.setText("USER");
                h.role.setBackgroundResource(R.drawable.bg_badge_success);
                h.role.setTextColor(ContextCompat.getColor(AdminDashboardActivity.this, R.color.success));
            }

            h.btnEdit.setOnClickListener(v -> showUserEditorDialog(u));
            h.btnDelete.setOnClickListener(v -> confirmDeleteUser(u));
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class VH extends RecyclerView.ViewHolder {
            final TextView avatar, name, pass, role;
            final MaterialButton btnEdit, btnDelete;

            VH(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.tvAdminUserAvatar);
                name = itemView.findViewById(R.id.tvAdminUserName);
                pass = itemView.findViewById(R.id.tvAdminUserPass);
                role = itemView.findViewById(R.id.tvAdminUserRole);
                btnEdit = itemView.findViewById(R.id.btnEditUser);
                btnDelete = itemView.findViewById(R.id.btnDeleteUser);
            }
        }
    }
}
