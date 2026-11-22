package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Màn hình theo dõi đơn hàng và tiến trình xử lý / giao hàng 5 bước.
 */
public class OrderTrackingActivity extends AppCompatActivity {

    private static final int FILTER_ALL = 0;
    private static final int FILTER_IN_PROGRESS = 1;
    private static final int FILTER_DELIVERED = 2;
    private static final int FILTER_CANCELLED = 3;

    private DatabaseHelper dbHelper;
    private String currentUser;
    private int currentFilter = FILTER_ALL;

    private final List<Order> allOrders = new ArrayList<>();
    private final List<Order> displayedOrders = new ArrayList<>();
    private OrderAdapter adapter;

    private RecyclerView rvOrders;
    private View layoutEmptyOrders;
    private TextView tvOrdersCount;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;
    private MaterialButton btnFilterAll;
    private MaterialButton btnFilterInProgress;
    private MaterialButton btnFilterDelivered;
    private MaterialButton btnFilterCancelled;

    private ListenerRegistration ordersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_tracking);

        View root = findViewById(R.id.rootOrderTracking);
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
        currentUser = session.getString("CURRENT_USER", "admin");

        bindViews();
        setupRecyclerView();
        setupListeners();
        loadLocalOrders();
        listenCloudOrders();
    }

    private void bindViews() {
        rvOrders = findViewById(R.id.rvOrders);
        layoutEmptyOrders = findViewById(R.id.layoutEmptyOrders);
        tvOrdersCount = findViewById(R.id.tvOrdersCount);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterInProgress = findViewById(R.id.btnFilterInProgress);
        btnFilterDelivered = findViewById(R.id.btnFilterDelivered);
        btnFilterCancelled = findViewById(R.id.btnFilterCancelled);

        UiMotion.bindTouchScale(
                findViewById(R.id.btnBack),
                findViewById(R.id.btnGoShopping),
                btnFilterAll,
                btnFilterInProgress,
                btnFilterDelivered,
                btnFilterCancelled
        );
    }

    private void setupRecyclerView() {
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(displayedOrders, this::openOrderDetail);
        rvOrders.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLocalOrders();
    }

    private void setupListeners() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnGoShopping).setOnClickListener(v -> {
            Intent intent = new Intent(OrderTrackingActivity.this, ProductListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnFilterAll.setOnClickListener(v -> setFilter(FILTER_ALL));
        btnFilterInProgress.setOnClickListener(v -> setFilter(FILTER_IN_PROGRESS));
        btnFilterDelivered.setOnClickListener(v -> setFilter(FILTER_DELIVERED));
        btnFilterCancelled.setOnClickListener(v -> setFilter(FILTER_CANCELLED));
    }

    private void setFilter(int filter) {
        currentFilter = filter;
        updateFilterButtonsUi();
        applyFilter();
        UiMotion.animateContentSwitch(rvOrders);
    }

    private void updateFilterButtonsUi() {
        styleFilterButton(btnFilterAll, currentFilter == FILTER_ALL);
        styleFilterButton(btnFilterInProgress, currentFilter == FILTER_IN_PROGRESS);
        styleFilterButton(btnFilterDelivered, currentFilter == FILTER_DELIVERED);
        styleFilterButton(btnFilterCancelled, currentFilter == FILTER_CANCELLED);
    }

    private void styleFilterButton(MaterialButton button, boolean selected) {
        if (selected) {
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

    private void loadLocalOrders() {
        allOrders.clear();
        allOrders.addAll(dbHelper.getOrdersByUser(currentUser));
        applyFilter();
    }

    private void applyFilter() {
        displayedOrders.clear();
        for (Order order : allOrders) {
            int step = order.getStatusStep();
            boolean cancelled = step == Order.STEP_CANCELLED;
            boolean delivered = step >= Order.STEP_DELIVERED;
            boolean inProgress = !cancelled && !delivered;
            if (currentFilter == FILTER_ALL
                    || (currentFilter == FILTER_IN_PROGRESS && inProgress)
                    || (currentFilter == FILTER_DELIVERED && delivered)
                    || (currentFilter == FILTER_CANCELLED && cancelled)) {
                displayedOrders.add(order);
            }
        }
        adapter.notifyDataSetChanged();
        tvOrdersCount.setText(allOrders.size() + " đơn");

        boolean isEmpty = displayedOrders.isEmpty();
        rvOrders.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        layoutEmptyOrders.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        if (isEmpty && !allOrders.isEmpty()) {
            tvEmptyTitle.setText("Không có đơn hàng ở mục này");
            tvEmptySubtitle.setText("Chọn \"Tất cả\" để xem toàn bộ lịch sử và tiến trình đơn hàng của bạn.");
        } else if (isEmpty) {
            tvEmptyTitle.setText("Chưa có đơn hàng nào");
            tvEmptySubtitle.setText("Đặt hàng ngay để theo dõi tiến trình xử lý, đóng gói và vận chuyển.");
        }
    }

    private void listenCloudOrders() {
        ordersListener = FirebaseFirestore.getInstance()
                .collection("orders")
                .whereEqualTo("userEmail", currentUser)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null || isFinishing() || isDestroyed()) {
                        return;
                    }

                    Map<String, Integer> localSteps = new HashMap<>();
                    for (Order local : dbHelper.getOrdersByUser(currentUser)) {
                        localSteps.put(local.getOrderCode(), local.getStatusStep());
                    }

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        String code = doc.getString("orderCode");
                        if (code == null || code.isEmpty()) {
                            code = doc.getId();
                        }
                        Long itemsLong = doc.getLong("totalItems");
                        int itemCount = itemsLong != null ? itemsLong.intValue() : 1;

                        int step;
                        Integer localStep = localSteps.get(code);
                        if (doc.contains("statusStep")) {
                            Long stepLong = doc.getLong("statusStep");
                            int cloudStep = stepLong != null ? Order.clampStep(stepLong.intValue()) : Order.STEP_PROCESSING;
                            if (doc.getMetadata().hasPendingWrites() && localStep != null) {
                                step = localStep;
                            } else {
                                step = cloudStep;
                            }
                        } else {
                            step = localStep != null ? localStep : Order.STEP_PROCESSING;
                        }

                        Order order = new Order(
                                code,
                                currentUser,
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
                        );
                        dbHelper.upsertOrderFromCloud(order);
                    }
                    loadLocalOrders();
                });
    }

    private void openOrderDetail(Order order) {
        Intent intent = new Intent(OrderTrackingActivity.this, OrderDetailActivity.class);
        intent.putExtra(OrderDetailActivity.EXTRA_ORDER_CODE, order.getOrderCode());
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ordersListener != null) {
            ordersListener.remove();
            ordersListener = null;
        }
    }
}
