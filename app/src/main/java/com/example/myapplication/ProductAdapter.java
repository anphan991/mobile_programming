package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

// Kết nối danh sách Product với giao diện thẻ Bento Grid (item_product.xml)
public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    public interface OnProductActionListener {
        void onProductClick(Product product);
        void onQuickAddCart(Product product);
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
        ProductImageHelper.bindImage(holder.imgProduct, product);
        holder.tvCategory.setText(product.getCategory());
        holder.tvName.setText(product.getName());
        holder.tvDesc.setText(product.getDescription());
        holder.tvPrice.setText(product.getPrice());

        boolean inStock = product.isInStock();
        holder.tvOutOfStock.setVisibility(inStock ? View.GONE : View.VISIBLE);

        // Nút + thêm nhanh vào giỏ (làm mờ và vô hiệu hóa nếu hết hàng)
        holder.btnQuickAdd.setEnabled(inStock);
        holder.btnQuickAdd.setAlpha(inStock ? 1.0f : 0.35f);
        holder.cardQuickAddBg.setCardBackgroundColor(ContextCompat.getColor(
                holder.itemView.getContext(),
                inStock ? R.color.primary : R.color.text_tertiary
        ));

        holder.itemView.setOnClickListener(v -> listener.onProductClick(product));
        holder.btnQuickAdd.setOnClickListener(v -> {
            if (inStock) listener.onQuickAddCart(product);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgProduct;
        final TextView tvCategory, tvName, tvDesc, tvPrice, tvOutOfStock;
        final View btnQuickAdd;
        final MaterialCardView cardQuickAddBg;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            tvCategory = itemView.findViewById(R.id.tvProductCategory);
            tvName = itemView.findViewById(R.id.tvProductName);
            tvDesc = itemView.findViewById(R.id.tvProductDesc);
            tvPrice = itemView.findViewById(R.id.tvProductPrice);
            tvOutOfStock = itemView.findViewById(R.id.tvItemOutOfStockBadge);
            btnQuickAdd = itemView.findViewById(R.id.btnQuickAddCart);
            cardQuickAddBg = itemView.findViewById(R.id.cardQuickAddBg);
        }
    }
}
