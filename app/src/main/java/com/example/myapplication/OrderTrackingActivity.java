package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;

public class OrderTrackingActivity extends AppCompatActivity {

    private static final int FILTER_ALL = 0, FILTER_IN_PROGRESS = 1, FILTER_DELIVERED = 2, FILTER_CANCELLED = 3;

    private DatabaseHelper dbHelper;
    private String currentUser;
    private int currentFilter = FILTER_ALL;

    private final List<Order> allOrders = new ArrayList<>();
    private final List<Order> displayedOrders = new ArrayList<>();
    private OrderAdapter adapter;

    private RecyclerView rvOrders;
    private View layoutEmptyOrders;
    private TextView tvOrdersCount, tvEmptyTitle, tvEmptySubtitle;
    private MaterialButton btnFilterAll, btnFilterInProgress, btnFilterDelivered, btnFilterCancelled;
    private ListenerRegistration ordersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_tracking);
        UiMotion.applyInsets(findViewById(R.id.rootOrderTracking));

        dbHelper = new DatabaseHelper(this);
        currentUser = UiMotion.currentUser(this);

        rvOrders = findViewById(R.id.rvOrders);
        layoutEmptyOrders = findViewById(R.id.layoutEmptyOrders);
        tvOrdersCount = findViewById(R.id.tvOrdersCount);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        btnFilterAll = findViewById(R.id.btnFilterAll);
        btnFilterInProgress = findViewById(R.id.btnFilterInProgress);
        btnFilterDelivered = findViewById(R.id.btnFilterDelivered);
        btnFilterCancelled = findViewById(R.id.btnFilterCancelled);

        UiMotion.bindTouchScale(findViewById(R.id.btnBack), findViewById(R.id.btnGoShopping),
                btnFilterAll, btnFilterInProgress, btnFilterDelivered, btnFilterCancelled);

        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(displayedOrders, order -> {
            Intent intent = new Intent(this, OrderDetailActivity.class);
            intent.putExtra(OrderDetailActivity.EXTRA_ORDER_CODE, order.getOrderCode());
            startActivity(intent);
        });
        rvOrders.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnGoShopping).setOnClickListener(v -> {
            Intent intent = new Intent(this, ProductListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
        btnFilterAll.setOnClickListener(v -> setFilter(FILTER_ALL));
        btnFilterInProgress.setOnClickListener(v -> setFilter(FILTER_IN_PROGRESS));
        btnFilterDelivered.setOnClickListener(v -> setFilter(FILTER_DELIVERED));
        btnFilterCancelled.setOnClickListener(v -> setFilter(FILTER_CANCELLED));

        loadLocalOrders();
        listenCloudOrders();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadLocalOrders();
    }

    private void setFilter(int filter) {
        currentFilter = filter;
        UiMotion.styleTabButton(this, btnFilterAll, filter == FILTER_ALL);
        UiMotion.styleTabButton(this, btnFilterInProgress, filter == FILTER_IN_PROGRESS);
        UiMotion.styleTabButton(this, btnFilterDelivered, filter == FILTER_DELIVERED);
        UiMotion.styleTabButton(this, btnFilterCancelled, filter == FILTER_CANCELLED);
        applyFilter();
        UiMotion.animateContentSwitch(rvOrders);
    }

    private void loadLocalOrders() {
        allOrders.clear();
        allOrders.addAll(dbHelper.getOrdersByUser(currentUser));
        applyFilter();
    }

    private void applyFilter() {
        displayedOrders.clear();
        for (Order o : allOrders) {
            int s = o.getStatusStep();
            boolean cancelled = s == Order.STEP_CANCELLED, delivered = s >= Order.STEP_DELIVERED;
            if (currentFilter == FILTER_ALL
                    || (currentFilter == FILTER_IN_PROGRESS && !cancelled && !delivered)
                    || (currentFilter == FILTER_DELIVERED && delivered)
                    || (currentFilter == FILTER_CANCELLED && cancelled)) {
                displayedOrders.add(o);
            }
        }
        adapter.notifyDataSetChanged();
        tvOrdersCount.setText(allOrders.size() + " đơn");

        boolean empty = displayedOrders.isEmpty();
        rvOrders.setVisibility(empty ? View.GONE : View.VISIBLE);
        layoutEmptyOrders.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            tvEmptyTitle.setText(!allOrders.isEmpty() ? "Không có đơn hàng ở mục này" : "Chưa có đơn hàng nào");
            tvEmptySubtitle.setText(!allOrders.isEmpty()
                    ? "Chọn \"Tất cả\" để xem toàn bộ lịch sử và tiến trình đơn hàng của bạn."
                    : "Đặt hàng ngay để theo dõi tiến trình xử lý, đóng gói và vận chuyển.");
        }
    }

    private void listenCloudOrders() {
        ordersListener = FirebaseFirestore.getInstance()
                .collection("orders")
                .whereEqualTo("userEmail", currentUser)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null || isFinishing() || isDestroyed()) return;
                    Order.syncSnapshotsToLocal(dbHelper, snapshots, currentUser);
                    loadLocalOrders();
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ordersListener != null) ordersListener.remove();
    }
}
