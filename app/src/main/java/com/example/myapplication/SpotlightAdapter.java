package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

// Adapter cho thanh cuộn ngang sản phẩm Flagship nổi bật
public class SpotlightAdapter extends RecyclerView.Adapter<SpotlightAdapter.ViewHolder> {

    public interface OnSpotlightClickListener {
        void onSpotlightClick(Product product);
    }

    private final List<Product> featuredList;
    private final OnSpotlightClickListener listener;

    public SpotlightAdapter(@NonNull List<Product> featuredList, @NonNull OnSpotlightClickListener listener) {
        this.featuredList = featuredList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_spotlight, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = featuredList.get(position);
        holder.tvCategory.setText(product.getCategory());
        holder.tvName.setText(product.getName());
        holder.tvDesc.setText(product.getDescription());
        holder.tvPrice.setText(product.getPrice());
        ProductImageHelper.bindImage(holder.imgProduct, product);

        holder.itemView.setOnClickListener(v -> listener.onSpotlightClick(product));
    }

    @Override
    public int getItemCount() {
        return featuredList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgProduct;
        final TextView tvCategory, tvName, tvDesc, tvPrice;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgProduct = itemView.findViewById(R.id.imgSpotlight);
            tvCategory = itemView.findViewById(R.id.tvSpotlightCategory);
            tvName = itemView.findViewById(R.id.tvSpotlightName);
            tvDesc = itemView.findViewById(R.id.tvSpotlightDesc);
            tvPrice = itemView.findViewById(R.id.tvSpotlightPrice);
        }
    }
}
