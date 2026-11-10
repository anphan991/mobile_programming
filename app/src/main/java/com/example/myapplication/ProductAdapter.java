package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.List;

// kết nối đối tượng Product với giao diện item_product.xml
public class ProductAdapter extends ArrayAdapter<Product> {

    public ProductAdapter(@NonNull Context context, @NonNull List<Product> productList) {
        super(context, 0, productList);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        // nạp giao diện item_product.xml nếu dòng chưa được tạo
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.item_product, parent, false);
        }

        // lấy sản phẩm ở vị trí hiện tại
        Product currentProduct = getItem(position);

        // ánh xạ các view trong item_product.xml
        ImageView imgProduct = convertView.findViewById(R.id.imgProduct);
        TextView tvName = convertView.findViewById(R.id.tvProductName);
        TextView tvDesc = convertView.findViewById(R.id.tvProductDesc);
        TextView tvPrice = convertView.findViewById(R.id.tvProductPrice);

        // gán dữ liệu vào từng ô
        if (currentProduct != null) {
            imgProduct.setImageResource(currentProduct.getImageResId());
            tvName.setText(currentProduct.getName());
            tvDesc.setText(currentProduct.getDescription());
            tvPrice.setText(currentProduct.getPrice());
        }

        return convertView;
    }
}
