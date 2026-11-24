package com.example.myapplication;

import android.content.Context;
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
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Product p = productList.get(position);
        Context ctx = h.itemView.getContext();
        DatabaseHelper db = new DatabaseHelper(ctx);
        String user = UiMotion.currentUser(ctx);

        ProductImageHelper.bindImage(h.imgProduct, p);
        h.tvCategory.setText(p.getCategory());
        h.tvName.setText(p.getName());
        h.tvDesc.setText(p.getDescription());
        h.tvPrice.setText(p.getPrice());

        if (h.tvDiscount != null) h.tvDiscount.setText("-" + DatabaseHelper.getDiscountPercent(p) + "%");
        if (h.tvOldPrice != null) {
            h.tvOldPrice.setText(DatabaseHelper.getOriginalPriceFormatted(p));
            h.tvOldPrice.setPaintFlags(h.tvOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }
        if (h.tvRating != null) {
            h.tvRating.setText(String.format(Locale.US, "%.1f (%d)",
                    db.getAverageRating(p.getSku()), Math.max(1, db.getReviewCount(p.getSku()))));
        }
        if (h.tvSold != null) h.tvSold.setText(DatabaseHelper.getSoldCountText(p));

        if (h.imgWishlist != null) {
            h.imgWishlist.setImageResource(db.isWishlisted(user, p.getSku())
                    ? R.drawable.ic_heart_filled_24 : R.drawable.ic_heart_outline_24);
        }
        if (h.btnWishlist != null) {
            h.btnWishlist.setOnClickListener(v -> {
                boolean saved = db.toggleWishlist(user, p.getSku());
                if (h.imgWishlist != null) {
                    h.imgWishlist.setImageResource(saved ? R.drawable.ic_heart_filled_24 : R.drawable.ic_heart_outline_24);
                }
                UiMotion.animateBadgePop(h.btnWishlist);
                Toast.makeText(ctx, saved ? "Đã lưu " + p.getName() + " vào Yêu thích" : "Đã bỏ khỏi Yêu thích", Toast.LENGTH_SHORT).show();
                listener.onWishlistChanged();
            });
        }

        boolean inStock = p.isInStock();
        h.tvOutOfStock.setVisibility(inStock ? View.GONE : View.VISIBLE);
        h.btnQuickAdd.setEnabled(inStock);
        h.btnQuickAdd.setAlpha(inStock ? 1f : 0.35f);
        h.cardQuickAddBg.setCardBackgroundColor(ContextCompat.getColor(ctx, inStock ? R.color.primary : R.color.text_tertiary));

        h.itemView.setOnClickListener(v -> listener.onProductClick(p));
        h.btnQuickAdd.setOnClickListener(v -> { if (inStock) listener.onQuickAddCart(p); });
        UiMotion.bindTouchScale(h.itemView, h.btnQuickAdd, h.btnWishlist);
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

        ViewHolder(@NonNull View v) {
            super(v);
            imgProduct = v.findViewById(R.id.imgProduct);
            tvCategory = v.findViewById(R.id.tvProductCategory);
            tvName = v.findViewById(R.id.tvProductName);
            tvDesc = v.findViewById(R.id.tvProductDesc);
            tvPrice = v.findViewById(R.id.tvProductPrice);
            tvOldPrice = v.findViewById(R.id.tvProductOldPrice);
            tvDiscount = v.findViewById(R.id.tvProductDiscount);
            tvRating = v.findViewById(R.id.tvProductRating);
            tvSold = v.findViewById(R.id.tvProductSold);
            tvOutOfStock = v.findViewById(R.id.tvItemOutOfStockBadge);
            btnQuickAdd = v.findViewById(R.id.btnQuickAddCart);
            btnWishlist = v.findViewById(R.id.btnItemWishlist);
            imgWishlist = v.findViewById(R.id.imgItemWishlist);
            cardQuickAddBg = v.findViewById(R.id.cardQuickAddBg);
        }
    }
}
