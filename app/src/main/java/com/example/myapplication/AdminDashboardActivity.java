package com.example.myapplication;

import android.content.Intent;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {

    public static final String EXTRA_INITIAL_TAB = "EXTRA_INITIAL_TAB";
    public static final int TAB_PRODUCTS = 0, TAB_ORDERS = 1, TAB_USERS = 2;

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
        UiMotion.applyInsets(findViewById(R.id.rootAdminDashboard));

        dbHelper = new DatabaseHelper(this);
        currentUser = UiMotion.currentUser(this);
        if (!dbHelper.isAdminUser(currentUser)) {
            Toast.makeText(this, "Chỉ quản trị viên mới có quyền truy cập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentTab = getIntent().getIntExtra(EXTRA_INITIAL_TAB, TAB_PRODUCTS);
        btnTabProducts = findViewById(R.id.btnTabProducts);
        btnTabOrders = findViewById(R.id.btnTabOrders);
        btnTabUsers = findViewById(R.id.btnTabUsers);
        btnAdminAdd = findViewById(R.id.btnAdminAdd);
        tvAdminSectionCount = findViewById(R.id.tvAdminSectionCount);
        tvAdminEmpty = findViewById(R.id.tvAdminEmpty);
        rvAdminList = findViewById(R.id.rvAdminList);
        rvAdminList.setLayoutManager(new LinearLayoutManager(this));

        UiMotion.bindTouchScale(findViewById(R.id.btnBack), btnTabProducts, btnTabOrders, btnTabUsers, btnAdminAdd);

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

        listenAllCloudOrders();
        switchTab(currentTab);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCurrentTab();
        UiMotion.animateContentSwitch(rvAdminList);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (allOrdersListener != null) allOrdersListener.remove();
    }

    private void switchTab(int tab) {
        currentTab = tab;
        UiMotion.styleTabButton(this, btnTabProducts, tab == TAB_PRODUCTS);
        UiMotion.styleTabButton(this, btnTabOrders, tab == TAB_ORDERS);
        UiMotion.styleTabButton(this, btnTabUsers, tab == TAB_USERS);

        btnAdminAdd.setVisibility(tab == TAB_ORDERS ? View.GONE : View.VISIBLE);
        if (tab == TAB_PRODUCTS) btnAdminAdd.setText("+ Thêm sản phẩm");
        else if (tab == TAB_USERS) btnAdminAdd.setText("+ Thêm người dùng");

        refreshCurrentTab();
        UiMotion.animateContentSwitch(rvAdminList);
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
                    if (error != null || snapshots == null || isFinishing() || isDestroyed()) return;
                    Order.syncSnapshotsToLocal(dbHelper, snapshots, "user");
                    if (currentTab == TAB_ORDERS) refreshCurrentTab();
                });
    }

    private void showUserEditorDialog(UserAccount existing) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_admin_user, null);
        TextInputEditText edtUsername = view.findViewById(R.id.edtUserUsername);
        TextInputEditText edtPassword = view.findViewById(R.id.edtUserPassword);
        RadioButton rbUser = view.findViewById(R.id.rbRoleUser);
        RadioButton rbAdmin = view.findViewById(R.id.rbRoleAdmin);

        boolean isEdit = existing != null;
        if (isEdit) {
            edtUsername.setText(existing.getUsername());
            edtPassword.setText(existing.getPassword());
            if (existing.isAdmin()) rbAdmin.setChecked(true); else rbUser.setChecked(true);
            if ("admin".equalsIgnoreCase(existing.getUsername())) {
                edtUsername.setEnabled(false);
                rbUser.setEnabled(false);
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(isEdit ? "Chỉnh sửa người dùng" : "Thêm người dùng mới")
                .setView(view)
                .setNegativeButton("Hủy", null)
                .setPositiveButton(isEdit ? "Lưu thay đổi" : "Thêm tài khoản", (dialog, which) -> {
                    String uname = edtUsername.getText() != null ? edtUsername.getText().toString().trim() : "";
                    String pass = edtPassword.getText() != null ? edtPassword.getText().toString().trim() : "";
                    String role = rbAdmin.isChecked() ? UserAccount.ROLE_ADMIN : UserAccount.ROLE_USER;
                    if (uname.isEmpty() || pass.length() < 6) {
                        Toast.makeText(this, "Tên đăng nhập không được trống và mật khẩu tối thiểu 6 ký tự!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if ((!isEdit || !uname.equalsIgnoreCase(existing.getUsername())) && dbHelper.checkUserExists(uname)) {
                        Toast.makeText(this, "Tên đăng nhập đã tồn tại!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    boolean ok = isEdit ? dbHelper.updateUser(existing.getId(), uname, pass, role) : dbHelper.addUser(uname, pass, role);
                    if (ok) {
                        Toast.makeText(this, (isEdit ? "Đã cập nhật tài khoản " : "Đã tạo tài khoản ") + uname, Toast.LENGTH_SHORT).show();
                        refreshCurrentTab();
                    }
                }).show();
    }

    private void confirmDeleteUser(UserAccount user) {
        if ("admin".equalsIgnoreCase(user.getUsername()) || user.getUsername().equalsIgnoreCase(currentUser)) {
            Toast.makeText(this, "Không thể xóa tài khoản quản trị mặc định hoặc tài khoản đang đăng nhập!", Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Xóa người dùng")
                .setMessage("Bạn có chắc muốn xóa tài khoản \"" + user.getUsername() + "\"?")
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Xóa", (d, w) -> {
                    dbHelper.deleteUser(user.getId());
                    Toast.makeText(this, "Đã xóa người dùng " + user.getUsername(), Toast.LENGTH_SHORT).show();
                    refreshCurrentTab();
                }).show();
    }

    private class AdminProductAdapter extends RecyclerView.Adapter<AdminProductAdapter.VH> {
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_admin_product, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Product p = products.get(pos);
            ProductImageHelper.bindImage(h.img, p);
            h.name.setText(p.getName());
            h.meta.setText(p.getSku() + " - " + p.getCategory());
            h.price.setText(p.getPrice());
            UiMotion.bindStockBadge(AdminDashboardActivity.this, h.stock, p.isInStock());
            h.btnEdit.setOnClickListener(v -> ProductEditorHelper.showProductEditor(AdminDashboardActivity.this, imagePicker, p, AdminDashboardActivity.this::refreshCurrentTab));
            h.btnDelete.setOnClickListener(v -> ProductEditorHelper.confirmDeleteProduct(AdminDashboardActivity.this, p, AdminDashboardActivity.this::refreshCurrentTab));
        }
        @Override public int getItemCount() { return products.size(); }
        class VH extends RecyclerView.ViewHolder {
            final ImageView img;
            final TextView name, meta, price, stock;
            final MaterialButton btnEdit, btnDelete;
            VH(@NonNull View v) {
                super(v);
                img = v.findViewById(R.id.imgAdminProd);
                name = v.findViewById(R.id.tvAdminProdName);
                meta = v.findViewById(R.id.tvAdminProdMeta);
                price = v.findViewById(R.id.tvAdminProdPrice);
                stock = v.findViewById(R.id.tvAdminProdStock);
                btnEdit = v.findViewById(R.id.btnEditProduct);
                btnDelete = v.findViewById(R.id.btnDeleteProduct);
            }
        }
    }

    private class AdminOrderAdapter extends RecyclerView.Adapter<AdminOrderAdapter.VH> {
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_admin_order, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            Order o = orders.get(pos);
            int step = o.getStatusStep();
            boolean cancelled = step == Order.STEP_CANCELLED;
            h.code.setText("#" + o.getOrderCode());
            h.user.setText("Tài khoản: " + o.getUserEmail() + " - " + o.getCreatedAt());
            UiMotion.bindStatusBadge(AdminDashboardActivity.this, h.badge, step);
            h.info.setText(o.getReceiverName() + " (" + o.getReceiverPhone() + ") - " + o.getTotalPrice());
            h.address.setText(cancelled && o.getCancelReason() != null && !o.getCancelReason().trim().isEmpty()
                    ? "Lý do hủy: " + o.getCancelReason() : "Địa chỉ: " + o.getReceiverAddress());
            h.btnDetail.setOnClickListener(v -> {
                Intent intent = new Intent(AdminDashboardActivity.this, OrderDetailActivity.class);
                intent.putExtra(OrderDetailActivity.EXTRA_ORDER_CODE, o.getOrderCode());
                startActivity(intent);
            });
            h.btnUpdate.setOnClickListener(v -> Order.showStepPicker(AdminDashboardActivity.this, o, AdminDashboardActivity.this::refreshCurrentTab));
        }
        @Override public int getItemCount() { return orders.size(); }
        class VH extends RecyclerView.ViewHolder {
            final TextView code, user, badge, info, address;
            final MaterialButton btnDetail, btnUpdate;
            VH(@NonNull View v) {
                super(v);
                code = v.findViewById(R.id.tvAdminOrderCode);
                user = v.findViewById(R.id.tvAdminOrderUser);
                badge = v.findViewById(R.id.tvAdminOrderBadge);
                info = v.findViewById(R.id.tvAdminOrderInfo);
                address = v.findViewById(R.id.tvAdminOrderAddress);
                btnDetail = v.findViewById(R.id.btnAdminOrderDetail);
                btnUpdate = v.findViewById(R.id.btnAdminUpdateStatus);
            }
        }
    }

    private class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.VH> {
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup p, int vt) {
            return new VH(LayoutInflater.from(p.getContext()).inflate(R.layout.item_admin_user, p, false));
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            UserAccount u = users.get(pos);
            String uname = u.getUsername();
            h.avatar.setText(uname.isEmpty() ? "U" : uname.substring(0, 1).toUpperCase(Locale.ROOT));
            h.name.setText(uname);
            h.pass.setText("Mật khẩu: " + u.getPassword());
            h.role.setText(u.isAdmin() ? "ADMIN" : "USER");
            h.role.setBackgroundResource(u.isAdmin() ? R.drawable.bg_badge_primary : R.drawable.bg_badge_success);
            h.role.setTextColor(ContextCompat.getColor(AdminDashboardActivity.this, u.isAdmin() ? R.color.primary_dark : R.color.success));
            h.btnEdit.setOnClickListener(v -> showUserEditorDialog(u));
            h.btnDelete.setOnClickListener(v -> confirmDeleteUser(u));
        }
        @Override public int getItemCount() { return users.size(); }
        class VH extends RecyclerView.ViewHolder {
            final TextView avatar, name, pass, role;
            final MaterialButton btnEdit, btnDelete;
            VH(@NonNull View v) {
                super(v);
                avatar = v.findViewById(R.id.tvAdminUserAvatar);
                name = v.findViewById(R.id.tvAdminUserName);
                pass = v.findViewById(R.id.tvAdminUserPass);
                role = v.findViewById(R.id.tvAdminUserRole);
                btnEdit = v.findViewById(R.id.btnEditUser);
                btnDelete = v.findViewById(R.id.btnDeleteUser);
            }
        }
    }
}
