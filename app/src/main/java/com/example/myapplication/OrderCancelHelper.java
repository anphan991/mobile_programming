package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * Hộp thoại hủy đơn hàng dùng chung cho User và Admin, hỗ trợ chọn hoặc nhập lý do hủy.
 */
public final class OrderCancelHelper {

    public interface OnCancelledListener {
        void onOrderCancelled(String reason);
    }

    private OrderCancelHelper() {}

    public static void showCancelDialog(Context context, Order order, boolean isAdmin, OnCancelledListener listener) {
        if (order == null) return;

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_cancel_order, null);
        TextView tvTitle = view.findViewById(R.id.tvCancelDialogTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvCancelDialogSubtitle);
        RadioGroup rgReasons = view.findViewById(R.id.rgCancelReasons);
        RadioButton rb1 = view.findViewById(R.id.rbReason1);
        RadioButton rb2 = view.findViewById(R.id.rbReason2);
        RadioButton rb3 = view.findViewById(R.id.rbReason3);
        RadioButton rb4 = view.findViewById(R.id.rbReason4);
        RadioButton rbOther = view.findViewById(R.id.rbReasonOther);
        TextInputLayout tilCustom = view.findViewById(R.id.tilCustomCancelReason);
        TextInputEditText edtCustom = view.findViewById(R.id.edtCustomCancelReason);
        MaterialButton btnKeep = view.findViewById(R.id.btnCancelKeep);
        MaterialButton btnConfirm = view.findViewById(R.id.btnCancelConfirm);

        tvTitle.setText("Hủy đơn #" + order.getOrderCode());
        if (isAdmin) {
            tvSubtitle.setText("Ghi nhận lý do hủy để khách hàng nắm thông tin");
            rb1.setText("Hết hàng / không đủ tồn kho");
            rb2.setText("Không liên hệ được người nhận");
            rb3.setText("Địa chỉ giao hàng không hợp lệ");
            rb4.setText("Hủy theo yêu cầu của khách hàng");
        } else {
            tvSubtitle.setText("Sau khi hủy, bạn có thể sửa lại SĐT hoặc địa chỉ và đặt lại đơn");
            rb1.setText("Sai số điện thoại / thông tin người nhận");
            rb2.setText("Sai hoặc cần đổi địa chỉ giao hàng");
            rb3.setText("Muốn đổi sản phẩm / số lượng");
            rb4.setText("Đổi ý / không còn nhu cầu");
        }

        rgReasons.setOnCheckedChangeListener((group, checkedId) -> {
            tilCustom.setError(null);
            tilCustom.setHint(checkedId == R.id.rbReasonOther
                    ? "Nhập lý do hủy đơn *"
                    : "Ghi chú thêm (Tùy chọn)");
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(view)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnKeep.setOnClickListener(v -> dialog.dismiss());
        btnConfirm.setOnClickListener(v -> {
            int checkedId = rgReasons.getCheckedRadioButtonId();
            String customText = edtCustom.getText() != null ? edtCustom.getText().toString().trim() : "";
            String finalReason;

            if (checkedId == R.id.rbReasonOther) {
                if (customText.isEmpty()) {
                    tilCustom.setError("Vui lòng nhập lý do hủy đơn!");
                    return;
                }
                finalReason = customText;
            } else {
                RadioButton selected = view.findViewById(checkedId);
                String preset = selected != null ? selected.getText().toString() : "";
                finalReason = customText.isEmpty() ? preset : (preset + " - " + customText);
            }

            if (isAdmin && !finalReason.startsWith("Admin:")) {
                finalReason = "Admin: " + finalReason;
            }

            new DatabaseHelper(context).updateOrderStatusWithReason(
                    order.getOrderCode(), Order.STEP_CANCELLED, finalReason);

            Map<String, Object> updates = new HashMap<>();
            updates.put("statusStep", Order.STEP_CANCELLED);
            updates.put("status", Order.getStepTitle(Order.STEP_CANCELLED));
            updates.put("cancelReason", finalReason);
            updates.put("cancelledBy", isAdmin ? "admin" : "user");
            FirebaseFirestore.getInstance()
                    .collection("orders")
                    .document(order.getOrderCode())
                    .set(updates, SetOptions.merge());

            Toast.makeText(context,
                    "Đã hủy đơn #" + order.getOrderCode(),
                    Toast.LENGTH_SHORT).show();
            dialog.dismiss();

            if (listener != null) {
                listener.onOrderCancelled(finalReason);
            }
        });

        dialog.show();
    }
}
