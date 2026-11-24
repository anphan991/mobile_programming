package com.example.myapplication;

import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProductDetailActivity extends AppCompatActivity {

    public static final String KEY_INDEX = "KEY_INDEX";

    private ImageView imgDetailProduct, imgDetailWishlist;
    private View btnDetailWishlist;
    private TextView tvDetailName, tvDetailPrice, tvDetailOldPrice, tvDetailDiscountBadge, tvDetailHeroRating;
    private TextView tvDetailDesc, tvDetailPosition, tvDetailStockBadge, tvDetailSku, tvDetailWarranty, tvDetailSpecs, tvDetailSubtitle, tvDetailCartBadge;
    private TextView tvReviewAvgScore, tvReviewCountLabel, tvStar5Count, tvStar4Count, tvStar3Count, tvEmptyReviews;
    private ProgressBar pbStar5, pbStar4, pbStar3;
    private LinearLayout layoutReviewsList;
    private RecyclerView rvRelatedProducts;
    private NestedScrollView scrollProductDetail;
    private Button btnDetailPrev, btnDetailNext, btnAddToCart, btnBuyNow;

    private int currentIndex = 0;
    private String currentUser = "admin";
    private boolean isAdmin = false;
    private DatabaseHelper dbHelper;
    private final ProductImagePicker imagePicker = new ProductImagePicker(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);
        UiMotion.applyInsets(findViewById(R.id.rootProductDetail));

        dbHelper = new DatabaseHelper(this);
        if (ProductListActivity.productList == null || ProductListActivity.productList.isEmpty()) {
            ArrayList<Product> dbList = dbHelper.getAllProducts();
            ProductListActivity.productList = !dbList.isEmpty() ? dbList : ProductListActivity.createDefaultProducts();
        }

        scrollProductDetail = findViewById(R.id.scrollProductDetail);
        imgDetailProduct = findViewById(R.id.imgDetailProduct);
        btnDetailWishlist = findViewById(R.id.btnDetailWishlist);
        imgDetailWishlist = findViewById(R.id.imgDetailWishlist);
        tvDetailName = findViewById(R.id.tvDetailName);
        tvDetailPrice = findViewById(R.id.tvDetailPrice);
        tvDetailOldPrice = findViewById(R.id.tvDetailOldPrice);
        tvDetailDiscountBadge = findViewById(R.id.tvDetailDiscountBadge);
        tvDetailHeroRating = findViewById(R.id.tvDetailHeroRating);
        tvDetailDesc = findViewById(R.id.tvDetailDesc);
        tvDetailPosition = findViewById(R.id.tvDetailPosition);
        tvDetailStockBadge = findViewById(R.id.tvDetailStockBadge);
        tvDetailSku = findViewById(R.id.tvDetailSku);
        tvDetailWarranty = findViewById(R.id.tvDetailWarranty);
        tvDetailSpecs = findViewById(R.id.tvDetailSpecs);
        tvDetailSubtitle = findViewById(R.id.tvDetailSubtitle);

        tvReviewAvgScore = findViewById(R.id.tvReviewAvgScore);
        tvReviewCountLabel = findViewById(R.id.tvReviewCountLabel);
        tvStar5Count = findViewById(R.id.tvStar5Count);
        tvStar4Count = findViewById(R.id.tvStar4Count);
        tvStar3Count = findViewById(R.id.tvStar3Count);
        pbStar5 = findViewById(R.id.pbStar5);
        pbStar4 = findViewById(R.id.pbStar4);
        pbStar3 = findViewById(R.id.pbStar3);
        layoutReviewsList = findViewById(R.id.layoutReviewsList);
        tvEmptyReviews = findViewById(R.id.tvEmptyReviews);
        rvRelatedProducts = findViewById(R.id.rvRelatedProducts);
        rvRelatedProducts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        Button btnBackToList = findViewById(R.id.btnBackToList);
        Button btnOpenCartDetail = findViewById(R.id.btnOpenCartDetail);
        Button btnWriteReview = findViewById(R.id.btnWriteReview);
        btnDetailPrev = findViewById(R.id.btnDetailPrev);
        btnDetailNext = findViewById(R.id.btnDetailNext);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnBuyNow = findViewById(R.id.btnBuyNow);
        tvDetailCartBadge = findViewById(R.id.tvDetailCartBadge);

        UiMotion.bindTouchScale(btnBackToList, btnOpenCartDetail, btnDetailWishlist, btnAddToCart, btnBuyNow, btnDetailPrev, btnDetailNext, btnWriteReview);
        UiMotion.animateEntry(findViewById(R.id.cardDetailContent));

        btnBackToList.setOnClickListener(v -> finish());
        btnOpenCartDetail.setOnClickListener(v -> startActivity(new Intent(this, PaymentActivity.class)));

        CartManager.syncForCurrentUser(this);
        currentUser = UiMotion.currentUser(this);
        isAdmin = dbHelper.isAdminUser(currentUser);
        findViewById(R.id.layoutAdminProductActions).setVisibility(isAdmin ? View.VISIBLE : View.GONE);

        currentIndex = getIntent().getIntExtra(KEY_INDEX, 0);
        if (currentIndex < 0 || currentIndex >= ProductListActivity.productList.size()) currentIndex = 0;
        showProduct(currentIndex);

        btnDetailWishlist.setOnClickListener(v -> {
            Product current = ProductListActivity.productList.get(currentIndex);
            boolean saved = dbHelper.toggleWishlist(currentUser, current.getSku());
            imgDetailWishlist.setImageResource(saved ? R.drawable.ic_heart_filled_24 : R.drawable.ic_heart_outline_24);
            UiMotion.animateBadgePop(btnDetailWishlist);
            Toast.makeText(this, saved ? "Đã lưu " + current.getName() + " vào Yêu thích!" : "Đã bỏ khỏi Yêu thích", Toast.LENGTH_SHORT).show();
        });

        btnWriteReview.setOnClickListener(v -> showWriteReviewDialog(ProductListActivity.productList.get(currentIndex)));

        findViewById(R.id.btnDetailEditProduct).setOnClickListener(v -> {
            if (isAdmin) ProductEditorHelper.showProductEditor(this, imagePicker, ProductListActivity.productList.get(currentIndex), () -> showProduct(currentIndex));
        });
        findViewById(R.id.btnDetailDeleteProduct).setOnClickListener(v -> {
            if (isAdmin) ProductEditorHelper.confirmDeleteProduct(this, ProductListActivity.productList.get(currentIndex), this::finish);
        });

        btnAddToCart.setOnClickListener(v -> {
            Product current = ProductListActivity.productList.get(currentIndex);
            CartManager.addToCart(current, 1);
            updateDetailCartBadge();
            UiMotion.animateBadgePop(tvDetailCartBadge);
            Toast.makeText(this, "Đã thêm " + current.getName() + " vào giỏ hàng!", Toast.LENGTH_SHORT).show();
        });

        btnBuyNow.setOnClickListener(v -> {
            CartManager.addToCart(ProductListActivity.productList.get(currentIndex), 1);
            startActivity(new Intent(this, PaymentActivity.class));
        });

        btnDetailPrev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                showProduct(--currentIndex);
                UiMotion.animateContentSwitch(findViewById(R.id.cardDetailContent));
            }
        });
        btnDetailNext.setOnClickListener(v -> {
            if (currentIndex < ProductListActivity.productList.size() - 1) {
                showProduct(++currentIndex);
                UiMotion.animateContentSwitch(findViewById(R.id.cardDetailContent));
            }
        });
    }

    private void showProduct(int index) {
        if (index < 0 || index >= ProductListActivity.productList.size()) return;
        Product product = ProductListActivity.productList.get(index);
        ProductImageHelper.bindImage(imgDetailProduct, product);
        tvDetailName.setText(product.getName());
        tvDetailPrice.setText(product.getPrice());
        tvDetailDesc.setText(product.getDescription());
        tvDetailPosition.setText((index + 1) + "/" + ProductListActivity.productList.size());

        tvDetailDiscountBadge.setText("-" + DatabaseHelper.getDiscountPercent(product) + "%");
        tvDetailOldPrice.setText(DatabaseHelper.getOriginalPriceFormatted(product));
        tvDetailOldPrice.setPaintFlags(tvDetailOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        imgDetailWishlist.setImageResource(dbHelper.isWishlisted(currentUser, product.getSku())
                ? R.drawable.ic_heart_filled_24 : R.drawable.ic_heart_outline_24);

        boolean inStock = product.isInStock();
        UiMotion.bindStockBadge(this, tvDetailStockBadge, inStock);

        tvDetailSku.setText(product.getSku());
        tvDetailWarranty.setText(product.getWarranty());
        tvDetailSpecs.setText(product.getDetailedSpecs());
        tvDetailSubtitle.setText(product.getCategory() + " • Mã " + product.getSku());

        boolean hasPrev = index > 0, hasNext = index < ProductListActivity.productList.size() - 1;
        btnDetailPrev.setEnabled(hasPrev);
        btnDetailPrev.setAlpha(hasPrev ? 1f : 0.38f);
        btnDetailNext.setEnabled(hasNext);
        btnDetailNext.setAlpha(hasNext ? 1f : 0.38f);

        btnAddToCart.setEnabled(inStock);
        btnAddToCart.setAlpha(inStock ? 1f : 0.42f);
        btnAddToCart.setText(inStock ? "Thêm vào giỏ" : "Hết hàng");
        btnBuyNow.setEnabled(inStock);
        btnBuyNow.setAlpha(inStock ? 1f : 0.42f);
        btnBuyNow.setText(inStock ? "Mua ngay" : "Tạm hết hàng");

        renderReviewsForProduct(product);
        renderRelatedProducts(product);
        scrollProductDetail.scrollTo(0, 0);
    }

    private void renderReviewsForProduct(Product product) {
        String sku = product.getSku();
        List<ProductReview> reviews = dbHelper.getReviewsBySku(sku);
        float avg = dbHelper.getAverageRating(sku);
        int total = reviews.size(), denom = Math.max(1, total);
        int[] breakdown = dbHelper.getRatingBreakdown(sku);
        int c5 = breakdown[5], c4 = breakdown[4], c3 = breakdown[3] + breakdown[2] + breakdown[1];

        tvReviewAvgScore.setText(String.format(Locale.US, "%.1f", avg));
        tvReviewCountLabel.setText(total + " nhận xét");
        tvDetailHeroRating.setText(String.format(Locale.US, "%.1f (%d đánh giá) • %s",
                avg, Math.max(1, total), DatabaseHelper.getSoldCountText(product)));

        pbStar5.setProgress(Math.round(c5 * 100f / denom));
        pbStar4.setProgress(Math.round(c4 * 100f / denom));
        pbStar3.setProgress(Math.round(c3 * 100f / denom));
        tvStar5Count.setText(String.valueOf(c5));
        tvStar4Count.setText(String.valueOf(c4));
        tvStar3Count.setText(String.valueOf(c3));

        layoutReviewsList.removeAllViews();
        tvEmptyReviews.setVisibility(reviews.isEmpty() ? View.VISIBLE : View.GONE);

        LayoutInflater inflater = LayoutInflater.from(this);
        for (ProductReview r : reviews) {
            View v = inflater.inflate(R.layout.item_product_review, layoutReviewsList, false);
            String uname = r.getUsername();
            ((TextView) v.findViewById(R.id.tvReviewAvatar)).setText(uname.isEmpty() ? "K" : uname.substring(0, 1).toUpperCase(Locale.ROOT));
            ((TextView) v.findViewById(R.id.tvReviewUsername)).setText(uname);
            v.findViewById(R.id.tvReviewVerified).setVisibility(r.isVerifiedPurchase() ? View.VISIBLE : View.GONE);
            ((TextView) v.findViewById(R.id.tvReviewDate)).setText(r.getCreatedAt());
            ((TextView) v.findViewById(R.id.tvReviewRatingBadge)).setText(r.getRating() + ".0");
            ((TextView) v.findViewById(R.id.tvReviewComment)).setText(r.getComment());

            View btnHelpful = v.findViewById(R.id.btnReviewHelpful);
            TextView tvHelpful = v.findViewById(R.id.tvReviewHelpfulCount);
            tvHelpful.setText("Hữu ích (" + r.getHelpfulCount() + ")");
            btnHelpful.setOnClickListener(view -> {
                dbHelper.incrementReviewHelpful(r.getId());
                r.setHelpfulCount(r.getHelpfulCount() + 1);
                tvHelpful.setText("Hữu ích (" + r.getHelpfulCount() + ")");
                UiMotion.animateBadgePop(btnHelpful);
            });

            if (isAdmin || currentUser.equalsIgnoreCase(uname)) {
                View btnDel = v.findViewById(R.id.btnReviewDelete);
                btnDel.setVisibility(View.VISIBLE);
                btnDel.setOnClickListener(view -> new MaterialAlertDialogBuilder(this)
                        .setTitle("Xóa đánh giá")
                        .setMessage("Bạn có chắc muốn xóa nhận xét này?")
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Xóa", (d, w) -> {
                            dbHelper.deleteReview(r.getId());
                            Toast.makeText(this, "Đã xóa nhận xét!", Toast.LENGTH_SHORT).show();
                            renderReviewsForProduct(product);
                        }).show());
            }
            layoutReviewsList.addView(v);
        }
    }

    private void showWriteReviewDialog(Product product) {
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_write_review, null);
        ((TextView) v.findViewById(R.id.tvWriteReviewSubtitle)).setText(product.getName());

        ImageView[] stars = {
                v.findViewById(R.id.starPick1), v.findViewById(R.id.starPick2),
                v.findViewById(R.id.starPick3), v.findViewById(R.id.starPick4), v.findViewById(R.id.starPick5)
        };
        TextView tvStarLabel = v.findViewById(R.id.tvPickedStarLabel);
        Chip[] tagChips = {
                v.findViewById(R.id.chipTagAuthentic), v.findViewById(R.id.chipTagPerformance),
                v.findViewById(R.id.chipTagFast), v.findViewById(R.id.chipTagWorth)
        };
        TextInputLayout tilComment = v.findViewById(R.id.tilReviewComment);
        TextInputEditText edtComment = v.findViewById(R.id.edtReviewComment);

        final int[] picked = {5};
        final String[] labels = {"", "1 sao - Không hài lòng", "2 sao - Cần cải thiện", "3 sao - Tạm ổn", "4 sao - Hài lòng", "5 sao - Cực kỳ hài lòng"};
        for (int i = 0; i < stars.length; i++) {
            final int val = i + 1;
            stars[i].setOnClickListener(view -> {
                picked[0] = val;
                for (int j = 0; j < stars.length; j++) {
                    stars[j].setImageResource(j < val ? R.drawable.ic_star_filled_24 : R.drawable.ic_star_outline_24);
                }
                tvStarLabel.setText(labels[val]);
                UiMotion.animateBadgePop(view);
            });
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(v).setCancelable(true).create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        v.findViewById(R.id.btnCancelReview).setOnClickListener(view -> dialog.dismiss());
        v.findViewById(R.id.btnSubmitReview).setOnClickListener(view -> {
            String raw = edtComment.getText() != null ? edtComment.getText().toString().trim() : "";
            List<String> tags = new ArrayList<>();
            for (Chip chip : tagChips) {
                if (chip != null && chip.isChecked()) tags.add(chip.getText().toString());
            }
            if (raw.isEmpty() && tags.isEmpty()) {
                tilComment.setError("Vui lòng nhập nhận xét hoặc chọn điểm nổi bật!");
                return;
            }
            String fullComment = (tags.isEmpty() ? "" : "[" + String.join(" • ", tags) + "] ")
                    + (raw.isEmpty() ? "Sản phẩm chất lượng tốt, đúng mô tả của cửa hàng." : raw);
            boolean verified = dbHelper.hasUserPurchasedProduct(currentUser, product.getSku()) || isAdmin;
            dbHelper.addProductReview(product.getSku(), currentUser, picked[0], fullComment, verified);

            Map<String, Object> reviewMap = new HashMap<>();
            reviewMap.put("sku", product.getSku());
            reviewMap.put("username", currentUser);
            reviewMap.put("rating", picked[0]);
            reviewMap.put("comment", fullComment);
            reviewMap.put("verified", verified);
            reviewMap.put("createdAt", new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
            FirebaseFirestore.getInstance().collection("product_reviews").add(reviewMap);

            dialog.dismiss();
            renderReviewsForProduct(product);
            Toast.makeText(this, "Cảm ơn bạn đã gửi đánh giá sản phẩm!", Toast.LENGTH_SHORT).show();
        });
        dialog.show();
    }

    private void renderRelatedProducts(Product currentProduct) {
        List<Product> related = new ArrayList<>();
        for (Product p : ProductListActivity.productList) {
            if (!p.getSku().equals(currentProduct.getSku()) && p.getCategory().equalsIgnoreCase(currentProduct.getCategory())) {
                related.add(p);
            }
        }
        for (Product p : ProductListActivity.productList) {
            if (!p.getSku().equals(currentProduct.getSku()) && !related.contains(p) && related.size() < 6) {
                related.add(p);
            }
        }

        rvRelatedProducts.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                return new RecyclerView.ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_related_product, parent, false)) {};
            }
            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder h, int position) {
                Product p = related.get(position);
                View v = h.itemView;
                ProductImageHelper.bindImage(v.findViewById(R.id.imgRelatedProduct), p);
                ((TextView) v.findViewById(R.id.tvRelatedName)).setText(p.getName());
                ((TextView) v.findViewById(R.id.tvRelatedPrice)).setText(p.getPrice());
                ((TextView) v.findViewById(R.id.tvRelatedRating)).setText(String.format(Locale.US, "%.1f", dbHelper.getAverageRating(p.getSku())));
                v.setOnClickListener(view -> {
                    int idx = ProductListActivity.productList.indexOf(p);
                    if (idx >= 0) {
                        showProduct(currentIndex = idx);
                        UiMotion.animateContentSwitch(findViewById(R.id.cardDetailContent));
                    }
                });
                UiMotion.bindTouchScale(v);
            }
            @Override public int getItemCount() { return related.size(); }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        CartManager.syncForCurrentUser(this);
        updateDetailCartBadge();
    }

    private void updateDetailCartBadge() {
        int total = CartManager.getTotalItemCount();
        tvDetailCartBadge.setVisibility(total > 0 ? View.VISIBLE : View.GONE);
        if (total > 0) tvDetailCartBadge.setText(String.valueOf(total));
    }
}
