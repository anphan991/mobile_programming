package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;
import java.util.Locale;

// Kết nối danh sách Product với giao diện thẻ Bento Grid Flagship (item_product.xml)
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    public interface OnProductActionListener {
        void onProductClick(Product product);
        void onQuickAddCart(Product product);
        default void onWishlistChanged() {}
    }

    private final List<Product> productList;
    private final OnProductActionListener listener;

    public ProductAdapter(@NonNull List<Product> productList, @NonNull OnProductActionListener listener) {
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);
        Context ctx = holder.itemView.getContext();
        DatabaseHelper db = new DatabaseHelper(ctx);
        SharedPreferences pref = ctx.getSharedPreferences("UserSession", Context.MODE_PRIVATE);
        String currentUser = pref.getString("CURRENT_USER", "admin");

        ProductImageHelper.bindImage(holder.imgProduct, product);
        holder.tvCategory.setText(product.getCategory());
        holder.tvName.setText(product.getName());
        holder.tvDesc.setText(product.getDescription());
        holder.tvPrice.setText(product.getPrice());

        // % giảm giá & Giá gốc gạch ngang
        int discount = DatabaseHelper.getDiscountPercent(product);
        if (holder.tvDiscount != null) {
            holder.tvDiscount.setText("-" + discount + "%");
        }
        if (holder.tvOldPrice != null) {
            holder.tvOldPrice.setText(DatabaseHelper.getOriginalPriceFormatted(product));
            holder.tvOldPrice.setPaintFlags(holder.tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        // Điểm sao đánh giá thực tế từ SQLite + số lượt bán
        if (holder.tvRating != null) {
            float avg = db.getAverageRating(product.getSku());
            int count = db.getReviewCount(product.getSku());
            holder.tvRating.setText(String.format(Locale.US, "%.1f (%d)", avg, Math.max(1, count)));
        }
        if (holder.tvSold != null) {
            holder.tvSold.setText(DatabaseHelper.getSoldCountText(product));
        }

        // Trạng thái Yêu thích (Wishlist)
        boolean wishlisted = db.isWishlisted(currentUser, product.getSku());
        if (holder.imgWishlist != null) {
            holder.imgWishlist.setImageResource(wishlisted
                    ? R.drawable.ic_heart_filled_24
                    : R.drawable.ic_heart_outline_24);
        }
        if (holder.btnWishlist != null) {
            holder.btnWishlist.setOnClickListener(v -> {
                boolean nowWishlisted = db.toggleWishlist(currentUser, product.getSku());
                if (holder.imgWishlist != null) {
                    holder.imgWishlist.setImageResource(nowWishlisted
                            ? R.drawable.ic_heart_filled_24
                            : R.drawable.ic_heart_outline_24);
                }
                UiMotion.animateBadgePop(holder.btnWishlist);
                Toast.makeText(ctx,
                        nowWishlisted
                                ? "Đã lưu " + product.getName() + " vào Yêu thích"
                                : "Đã bỏ khỏi danh sách Yêu thích",
                        Toast.LENGTH_SHORT).show();
                listener.onWishlistChanged();
            });
        }

        boolean inStock = product.isInStock();
        holder.tvOutOfStock.setVisibility(inStock ? View.GONE : View.VISIBLE);

        // Nút + thêm nhanh vào giỏ (làm mờ và vô hiệu hóa nếu hết hàng)
        holder.btnQuickAdd.setEnabled(inStock);
        holder.btnQuickAdd.setAlpha(inStock ? 1.0f : 0.35f);
        holder.cardQuickAddBg.setCardBackgroundColor(ContextCompat.getColor(
                ctx,
                inStock ? R.color.primary : R.color.text_tertiary
        ));

        holder.itemView.setOnClickListener(v -> listener.onProductClick(product));
        holder.btnQuickAdd.setOnClickListener(v -> {
            if (inStock) listener.onQuickAddCart(product);
        });
        UiMotion.bindTouchScale(holder.itemView, holder.btnQuickAdd, holder.btnWishlist);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgProduct, imgWishlist;
        final TextView tvCategory, tvName, tvDesc, tvPrice, tvOldPrice, tvDiscount, tvRating, tvSold, tvOutOfStock;
        final View btnQuickAdd, btnWishlist;
        final MaterialCardView cardQuickAddBg;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvCategory = itemView.findViewById(R.id.tvProductCategory);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvDesc = itemView.findViewById(R.id.tvProductDesc);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvOldPrice = itemView.findViewById(R.id.tvProductOldPrice);
            tvDiscount = itemView.findViewById(R.id.tvProductDiscount);
            tvRating = itemView.findViewById(R.id.tvProductRating);
            tvSold = itemView.findViewById(R.id.tvProductSold);
            tvOutOfStock = itemView.findViewById(R.id.tvItemOutOfStockBadge);
            btnQuickAdd = itemView.findViewById(R.id.btnQuickAddCart);
            btnWishlist = itemView.findViewById(R.id.btnItemWishlist);
            imgWishlist = itemView.findViewById(R.id.imgItemWishlist);
            cardQuickAddBg = itemView.findViewById(R.id.cardQuickAddBg);
        }
    }
}
