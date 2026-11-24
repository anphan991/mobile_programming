package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

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
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_spotlight, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Product p = featuredList.get(position);
        h.tvCategory.setText(p.getCategory());
        h.tvName.setText(p.getName());
        h.tvDesc.setText(p.getDescription());
        h.tvPrice.setText(p.getPrice());
        if (h.tvRating != null) {
            h.tvRating.setText(String.format(Locale.US, "%.1f", new DatabaseHelper(h.itemView.getContext()).getAverageRating(p.getSku())));
        }
        ProductImageHelper.bindImage(h.imgProduct, p);
        h.itemView.setOnClickListener(v -> listener.onSpotlightClick(p));
        UiMotion.bindTouchScale(h.itemView);
    }

    @Override
    public int getItemCount() {
        return featuredList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView imgProduct;
        final TextView tvCategory, tvName, tvDesc, tvPrice, tvRating;

        ViewHolder(@NonNull View v) {
            super(v);
            imgProduct = v.findViewById(R.id.imgSpotlight);
            tvCategory = v.findViewById(R.id.tvSpotlightCategory);
            tvName = v.findViewById(R.id.tvSpotlightName);
            tvDesc = v.findViewById(R.id.tvSpotlightDesc);
            tvPrice = v.findViewById(R.id.tvSpotlightPrice);
            tvRating = v.findViewById(R.id.tvSpotlightRating);
        }
    }
}
