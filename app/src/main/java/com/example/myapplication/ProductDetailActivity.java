package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
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
    private TextView tvDetailDesc, tvDetailPosition;
    private TextView tvDetailStockBadge, tvDetailSku, tvDetailWarranty, tvDetailSpecs, tvDetailSubtitle, tvDetailCartBadge;
    private TextView tvReviewAvgScore, tvReviewCountLabel, tvStar5Count, tvStar4Count, tvStar3Count, tvEmptyReviews;
    private ProgressBar pbStar5, pbStar4, pbStar3;
    private LinearLayout layoutReviewsList;
    private RecyclerView rvRelatedProducts;
    private NestedScrollView scrollProductDetail;
    private Button btnBackToList, btnDetailPrev, btnDetailNext, btnAddToCart, btnBuyNow, btnOpenCartDetail, btnWriteReview;

    private int currentIndex = 0;
    private String currentUser = "admin";
    private boolean isAdmin = false;
    private DatabaseHelper dbHelper;
    private final ProductImagePicker imagePicker = new ProductImagePicker(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        View rootView = findViewById(R.id.rootProductDetail);
        final int basePaddingLeft = rootView.getPaddingLeft();
        final int basePaddingTop = rootView.getPaddingTop();
        final int basePaddingRight = rootView.getPaddingRight();
        final int basePaddingBottom = rootView.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            v.setPadding(
                    basePaddingLeft + systemBars.left,
                    basePaddingTop + systemBars.top,
                    basePaddingRight + systemBars.right,
                    basePaddingBottom + systemBars.bottom
            );
            return insets;
        });

        dbHelper = new DatabaseHelper(this);
        if (ProductListActivity.productList == null || ProductListActivity.productList.isEmpty()) {
            ArrayList<Product> dbList = dbHelper.getAllProducts();
            ProductListActivity.productList = (dbList != null && !dbList.isEmpty())
                    ? dbList
                    : ProductListActivity.createDefaultProducts();
        }

        // Ánh xạ view
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
        btnWriteReview = findViewById(R.id.btnWriteReview);
        rvRelatedProducts = findViewById(R.id.rvRelatedProducts);
        if (rvRelatedProducts != null) {
            rvRelatedProducts.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        }

        btnBackToList = findViewById(R.id.btnBackToList);
        btnDetailPrev = findViewById(R.id.btnDetailPrev);
        btnDetailNext = findViewById(R.id.btnDetailNext);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnBuyNow = findViewById(R.id.btnBuyNow);
        btnOpenCartDetail = findViewById(R.id.btnOpenCartDetail);
        tvDetailCartBadge = findViewById(R.id.tvDetailCartBadge);

        UiMotion.bindTouchScale(
                btnBackToList,
                btnOpenCartDetail,
                btnDetailWishlist,
                btnAddToCart,
                btnBuyNow,
                btnDetailPrev,
                btnDetailNext,
                btnWriteReview
        );
        UiMotion.animateEntry(findViewById(R.id.cardDetailContent));

        // Sự kiện mở giỏ hàng từ nút trên Header
        if (btnOpenCartDetail != null) {
            btnOpenCartDetail.setOnClickListener(v -> {
                Intent intent = new Intent(ProductDetailActivity.this, PaymentActivity.class);
                startActivity(intent);
            });
        }

        // Đồng bộ giỏ hàng theo tài khoản đang đăng nhập
        CartManager.syncForCurrentUser(this);

        SharedPreferences session = getSharedPreferences("UserSession", MODE_PRIVATE);
        currentUser = session.getString("CURRENT_USER", "admin");
        if (currentUser == null || currentUser.trim().isEmpty()) currentUser = "admin";
        isAdmin = dbHelper.isAdminUser(currentUser);
        View layoutAdminActions = findViewById(R.id.layoutAdminProductActions);
        if (layoutAdminActions != null) {
            layoutAdminActions.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        }

        // Nhận vị trí sản phẩm được chọn từ ProductListActivity
        currentIndex = getIntent().getIntExtra(KEY_INDEX, 0);
        if (currentIndex < 0 || currentIndex >= ProductListActivity.productList.size()) {
            currentIndex = 0;
        }
        showProduct(currentIndex);

        // Nút Lưu Yêu thích (Wishlist)
        if (btnDetailWishlist != null) {
            btnDetailWishlist.setOnClickListener(v -> {
                if (currentIndex >= 0 && currentIndex < ProductListActivity.productList.size()) {
                    Product current = ProductListActivity.productList.get(currentIndex);
                    boolean nowWishlisted = dbHelper.toggleWishlist(currentUser, current.getSku());
                    updateWishlistIcon(nowWishlisted);
                    UiMotion.animateBadgePop(btnDetailWishlist);
                    Toast.makeText(this,
                            nowWishlisted
                                    ? "Đã lưu " + current.getName() + " vào Yêu thích!"
                                    : "Đã bỏ khỏi danh sách Yêu thích",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Nút Viết đánh giá & bình luận
        if (btnWriteReview != null) {
            btnWriteReview.setOnClickListener(v -> {
                if (currentIndex >= 0 && currentIndex < ProductListActivity.productList.size()) {
                    showWriteReviewDialog(ProductListActivity.productList.get(currentIndex));
                }
            });
        }

        View btnEditProd = findViewById(R.id.btnDetailEditProduct);
        if (btnEditProd != null) {
            btnEditProd.setOnClickListener(v -> {
                if (isAdmin && currentIndex >= 0 && currentIndex < ProductListActivity.productList.size()) {
                    Product current = ProductListActivity.productList.get(currentIndex);
                    ProductEditorHelper.showProductEditor(this, imagePicker, current, () -> showProduct(currentIndex));
                }
            });
        }
        View btnDeleteProd = findViewById(R.id.btnDetailDeleteProduct);
        if (btnDeleteProd != null) {
            btnDeleteProd.setOnClickListener(v -> {
                if (isAdmin && currentIndex >= 0 && currentIndex < ProductListActivity.productList.size()) {
                    Product current = ProductListActivity.productList.get(currentIndex);
                    ProductEditorHelper.confirmDeleteProduct(this, current, this::finish);
                }
            });
        }

        // Nút quay lại danh sách
        btnBackToList.setOnClickListener(v -> finish());

        // Nút Thêm vào giỏ hàng
        if (btnAddToCart != null) {
            btnAddToCart.setOnClickListener(v -> {
                if (currentIndex >= 0 && currentIndex < ProductListActivity.productList.size()) {
                    Product current = ProductListActivity.productList.get(currentIndex);
                    CartManager.addToCart(current, 1);
                    updateDetailCartBadge();
                    UiMotion.animateBadgePop(tvDetailCartBadge);
                    Toast.makeText(this, "Đã thêm " + current.getName() + " vào giỏ hàng!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Nút Mua ngay (Chuyển sang trang Thanh toán)
        if (btnBuyNow != null) {
            btnBuyNow.setOnClickListener(v -> {
                if (currentIndex >= 0 && currentIndex < ProductListActivity.productList.size()) {
                    Product current = ProductListActivity.productList.get(currentIndex);
                    CartManager.addToCart(current, 1);
                    Intent intent = new Intent(ProductDetailActivity.this, PaymentActivity.class);
                    startActivity(intent);
                }
            });
        }

        // Nút xem sản phẩm liền trước
        btnDetailPrev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                showProduct(currentIndex);
                UiMotion.animateContentSwitch(findViewById(R.id.cardDetailContent));
            } else {
                Toast.makeText(this, "Đang xem sản phẩm đầu tiên", Toast.LENGTH_SHORT).show();
            }
        });

        // Nút xem sản phẩm tiếp theo
        btnDetailNext.setOnClickListener(v -> {
            if (currentIndex < ProductListActivity.productList.size() - 1) {
                currentIndex++;
                showProduct(currentIndex);
                UiMotion.animateContentSwitch(findViewById(R.id.cardDetailContent));
            } else {
                Toast.makeText(this, "Đang xem sản phẩm cuối cùng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateWishlistIcon(boolean wishlisted) {
        if (imgDetailWishlist != null) {
            imgDetailWishlist.setImageResource(wishlisted
                    ? R.drawable.ic_heart_filled_24
                    : R.drawable.ic_heart_outline_24);
        }
    }

    // Hiển thị thông tin sản phẩm theo vị trí
    private void showProduct(int index) {
        if (ProductListActivity.productList != null && index >= 0 && index < ProductListActivity.productList.size()) {
            Product product = ProductListActivity.productList.get(index);
            ProductImageHelper.bindImage(imgDetailProduct, product);
            tvDetailName.setText(product.getName());
            tvDetailPrice.setText(product.getPrice());
            tvDetailDesc.setText(product.getDescription());
            tvDetailPosition.setText((index + 1) + "/" + ProductListActivity.productList.size());

            int discount = DatabaseHelper.getDiscountPercent(product);
            if (tvDetailDiscountBadge != null) {
                tvDetailDiscountBadge.setText("-" + discount + "%");
            }
            if (tvDetailOldPrice != null) {
                tvDetailOldPrice.setText(DatabaseHelper.getOriginalPriceFormatted(product));
                tvDetailOldPrice.setPaintFlags(tvDetailOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            }

            updateWishlistIcon(dbHelper.isWishlisted(currentUser, product.getSku()));

            if (tvDetailStockBadge != null) {
                if (product.isInStock()) {
                    tvDetailStockBadge.setText("Còn hàng");
                    tvDetailStockBadge.setBackgroundResource(R.drawable.bg_badge_success);
                    tvDetailStockBadge.setTextColor(ContextCompat.getColor(this, R.color.success));
                } else {
                    tvDetailStockBadge.setText("Hết hàng");
                    tvDetailStockBadge.setBackgroundResource(R.drawable.bg_badge_danger);
                    tvDetailStockBadge.setTextColor(ContextCompat.getColor(this, R.color.danger));
                }
            }
            if (tvDetailSku != null) {
                tvDetailSku.setText(product.getSku());
            }
            if (tvDetailWarranty != null) {
                tvDetailWarranty.setText(product.getWarranty());
            }
            if (tvDetailSpecs != null) {
                tvDetailSpecs.setText(product.getDetailedSpecs());
            }
            if (tvDetailSubtitle != null) {
                tvDetailSubtitle.setText(product.getCategory() + " • Mã " + product.getSku());
            }

            boolean hasPrevious = index > 0;
            boolean hasNext = index < ProductListActivity.productList.size() - 1;
            btnDetailPrev.setEnabled(hasPrevious);
            btnDetailNext.setEnabled(hasNext);
            btnDetailPrev.setAlpha(hasPrevious ? 1f : 0.38f);
            btnDetailNext.setAlpha(hasNext ? 1f : 0.38f);

            // Cập nhật trạng thái nút Mua hàng & Thêm giỏ theo tình trạng tồn kho
            if (btnAddToCart != null && btnBuyNow != null) {
                if (product.isInStock()) {
                    btnAddToCart.setEnabled(true);
                    btnAddToCart.setAlpha(1.0f);
                    btnAddToCart.setText("Thêm vào giỏ");

                    btnBuyNow.setEnabled(true);
                    btnBuyNow.setAlpha(1.0f);
                    btnBuyNow.setText("Mua ngay");
                } else {
                    btnAddToCart.setEnabled(false);
                    btnAddToCart.setAlpha(0.42f);
                    btnAddToCart.setText("Hết hàng");

                    btnBuyNow.setEnabled(false);
                    btnBuyNow.setAlpha(0.42f);
                    btnBuyNow.setText("Tạm hết hàng");
                }
            }

            // Nạp đánh giá & bình luận thực tế + sản phẩm tương tự
            renderReviewsForProduct(product);
            renderRelatedProducts(product);

            if (scrollProductDetail != null) {
                scrollProductDetail.scrollTo(0, 0);
            }
        }
    }

    private void renderReviewsForProduct(Product product) {
        if (product == null || layoutReviewsList == null) return;
        String sku = product.getSku();
        List<ProductReview> reviews = dbHelper.getReviewsBySku(sku);
        float avg = dbHelper.getAverageRating(sku);
        int total = reviews.size();
        int[] breakdown = dbHelper.getRatingBreakdown(sku);

        if (tvReviewAvgScore != null) {
            tvReviewAvgScore.setText(String.format(Locale.US, "%.1f", avg));
        }
        if (tvReviewCountLabel != null) {
            tvReviewCountLabel.setText(total + " nhận xét");
        }
        if (tvDetailHeroRating != null) {
            tvDetailHeroRating.setText(String.format(Locale.US, "%.1f (%d đánh giá) • %s",
                    avg, Math.max(1, total), DatabaseHelper.getSoldCountText(product)));
        }

        int denom = Math.max(1, total);
        int c5 = breakdown[5];
        int c4 = breakdown[4];
        int c3 = breakdown[3] + breakdown[2] + breakdown[1];

        if (pbStar5 != null) pbStar5.setProgress(Math.round(c5 * 100f / denom));
        if (pbStar4 != null) pbStar4.setProgress(Math.round(c4 * 100f / denom));
        if (pbStar3 != null) pbStar3.setProgress(Math.round(c3 * 100f / denom));
        if (tvStar5Count != null) tvStar5Count.setText(String.valueOf(c5));
        if (tvStar4Count != null) tvStar4Count.setText(String.valueOf(c4));
        if (tvStar3Count != null) tvStar3Count.setText(String.valueOf(c3));

        layoutReviewsList.removeAllViews();
        if (tvEmptyReviews != null) {
            tvEmptyReviews.setVisibility(reviews.isEmpty() ? View.VISIBLE : View.GONE);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (ProductReview review : reviews) {
            View itemView = inflater.inflate(R.layout.item_product_review, layoutReviewsList, false);
            TextView tvAvatar = itemView.findViewById(R.id.tvReviewAvatar);
            TextView tvUsername = itemView.findViewById(R.id.tvReviewUsername);
            TextView tvVerified = itemView.findViewById(R.id.tvReviewVerified);
            TextView tvDate = itemView.findViewById(R.id.tvReviewDate);
            TextView tvRatingBadge = itemView.findViewById(R.id.tvReviewRatingBadge);
            TextView tvComment = itemView.findViewById(R.id.tvReviewComment);
            View btnHelpful = itemView.findViewById(R.id.btnReviewHelpful);
            TextView tvHelpfulCount = itemView.findViewById(R.id.tvReviewHelpfulCount);
            TextView btnDelete = itemView.findViewById(R.id.btnReviewDelete);

            String uname = review.getUsername();
            tvAvatar.setText(uname.isEmpty() ? "K" : uname.substring(0, 1).toUpperCase(Locale.ROOT));
            tvUsername.setText(uname);
            tvVerified.setVisibility(review.isVerifiedPurchase() ? View.VISIBLE : View.GONE);
            tvDate.setText(review.getCreatedAt());
            tvRatingBadge.setText(review.getRating() + ".0");
            tvComment.setText(review.getComment());
            tvHelpfulCount.setText("Hữu ích (" + review.getHelpfulCount() + ")");

            btnHelpful.setOnClickListener(v -> {
                dbHelper.incrementReviewHelpful(review.getId());
                review.setHelpfulCount(review.getHelpfulCount() + 1);
                tvHelpfulCount.setText("Hữu ích (" + review.getHelpfulCount() + ")");
                UiMotion.animateBadgePop(btnHelpful);
            });

            boolean canDelete = isAdmin || currentUser.equalsIgnoreCase(uname);
            if (canDelete) {
                btnDelete.setVisibility(View.VISIBLE);
                btnDelete.setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
                        .setTitle("Xóa đánh giá")
                        .setMessage("Bạn có chắc muốn xóa nhận xét này?")
                        .setNegativeButton("Hủy", null)
                        .setPositiveButton("Xóa", (d, w) -> {
                            dbHelper.deleteReview(review.getId());
                            Toast.makeText(this, "Đã xóa nhận xét!", Toast.LENGTH_SHORT).show();
                            renderReviewsForProduct(product);
                        })
                        .show());
            }

            layoutReviewsList.addView(itemView);
        }
    }

    private void showWriteReviewDialog(Product product) {
        if (product == null) return;
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_write_review, null);

        TextView tvSubtitle = dialogView.findViewById(R.id.tvWriteReviewSubtitle);
        tvSubtitle.setText(product.getName());

        ImageView[] stars = new ImageView[]{
                dialogView.findViewById(R.id.starPick1),
                dialogView.findViewById(R.id.starPick2),
                dialogView.findViewById(R.id.starPick3),
                dialogView.findViewById(R.id.starPick4),
                dialogView.findViewById(R.id.starPick5)
        };
        TextView tvStarLabel = dialogView.findViewById(R.id.tvPickedStarLabel);
        Chip chipAuthentic = dialogView.findViewById(R.id.chipTagAuthentic);
        Chip chipPerformance = dialogView.findViewById(R.id.chipTagPerformance);
        Chip chipFast = dialogView.findViewById(R.id.chipTagFast);
        Chip chipWorth = dialogView.findViewById(R.id.chipTagWorth);
        TextInputLayout tilComment = dialogView.findViewById(R.id.tilReviewComment);
        TextInputEditText edtComment = dialogView.findViewById(R.id.edtReviewComment);
        MaterialButton btnCancel = dialogView.findViewById(R.id.btnCancelReview);
        MaterialButton btnSubmit = dialogView.findViewById(R.id.btnSubmitReview);

        final int[] pickedRating = {5};
        final String[] starDescriptions = new String[]{
                "",
                "1 sao - Không hài lòng",
                "2 sao - Cần cải thiện",
                "3 sao - Tạm ổn",
                "4 sao - Hài lòng",
                "5 sao - Cực kỳ hài lòng"
        };

        for (int i = 0; i < stars.length; i++) {
            final int starValue = i + 1;
            stars[i].setOnClickListener(v -> {
                pickedRating[0] = starValue;
                for (int j = 0; j < stars.length; j++) {
                    stars[j].setImageResource(j < starValue
                            ? R.drawable.ic_star_filled_24
                            : R.drawable.ic_star_outline_24);
                }
                tvStarLabel.setText(starDescriptions[starValue]);
                UiMotion.animateBadgePop(v);
            });
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnSubmit.setOnClickListener(v -> {
            String rawText = edtComment.getText() != null ? edtComment.getText().toString().trim() : "";
            List<String> selectedTags = new ArrayList<>();
            if (chipAuthentic != null && chipAuthentic.isChecked()) selectedTags.add("Hàng chính hãng");
            if (chipPerformance != null && chipPerformance.isChecked()) selectedTags.add("Hiệu năng đỉnh");
            if (chipFast != null && chipFast.isChecked()) selectedTags.add("Giao hàng nhanh");
            if (chipWorth != null && chipWorth.isChecked()) selectedTags.add("Rất đáng tiền");

            if (rawText.isEmpty() && selectedTags.isEmpty()) {
                tilComment.setError("Vui lòng nhập nhận xét hoặc chọn điểm nổi bật!");
                return;
            }

            StringBuilder fullComment = new StringBuilder();
            if (!selectedTags.isEmpty()) {
                fullComment.append("[").append(String.join(" • ", selectedTags)).append("] ");
            }
            if (rawText.isEmpty()) {
                fullComment.append("Sản phẩm chất lượng tốt, đúng mô tả của cửa hàng.");
            } else {
                fullComment.append(rawText);
            }

            boolean verified = dbHelper.hasUserPurchasedProduct(currentUser, product.getSku()) || isAdmin;
            dbHelper.addProductReview(product.getSku(), currentUser, pickedRating[0], fullComment.toString(), verified);

            // Đồng bộ đánh giá lên Cloud Firestore
            Map<String, Object> reviewMap = new HashMap<>();
            reviewMap.put("sku", product.getSku());
            reviewMap.put("username", currentUser);
            reviewMap.put("rating", pickedRating[0]);
            reviewMap.put("comment", fullComment.toString());
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
        if (rvRelatedProducts == null || ProductListActivity.productList == null) return;
        List<Product> related = new ArrayList<>();
        // Ưu tiên sản phẩm cùng danh mục trước, sau đó đến sản phẩm nổi bật khác
        for (Product p : ProductListActivity.productList) {
            if (!p.getSku().equals(currentProduct.getSku())
                    && p.getCategory().equalsIgnoreCase(currentProduct.getCategory())) {
                related.add(p);
            }
        }
        for (Product p : ProductListActivity.productList) {
            if (!p.getSku().equals(currentProduct.getSku()) && !related.contains(p) && related.size() < 6) {
                related.add(p);
            }
        }

        rvRelatedProducts.setAdapter(new RecyclerView.Adapter<RelatedVH>() {
            @NonNull
            @Override
            public RelatedVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_related_product, parent, false);
                return new RelatedVH(v);
            }

            @Override
            public void onBindViewHolder(@NonNull RelatedVH h, int position) {
                Product p = related.get(position);
                ProductImageHelper.bindImage(h.img, p);
                h.name.setText(p.getName());
                h.price.setText(p.getPrice());
                float avg = dbHelper.getAverageRating(p.getSku());
                h.rating.setText(String.format(Locale.US, "%.1f", avg));
                h.itemView.setOnClickListener(v -> {
                    int idx = ProductListActivity.productList.indexOf(p);
                    if (idx >= 0) {
                        currentIndex = idx;
                        showProduct(currentIndex);
                        UiMotion.animateContentSwitch(findViewById(R.id.cardDetailContent));
                    }
                });
                UiMotion.bindTouchScale(h.itemView);
            }

            @Override
            public int getItemCount() {
                return related.size();
            }
        });
    }

    private static class RelatedVH extends RecyclerView.ViewHolder {
        final ImageView img;
        final TextView name, price, rating;

        RelatedVH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgRelatedProduct);
            name = itemView.findViewById(R.id.tvRelatedName);
            price = itemView.findViewById(R.id.tvRelatedPrice);
            rating = itemView.findViewById(R.id.tvRelatedRating);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        CartManager.syncForCurrentUser(this);
        updateDetailCartBadge();
    }

    private void updateDetailCartBadge() {
        if (tvDetailCartBadge != null) {
            int total = CartManager.getTotalItemCount();
            if (total > 0) {
                tvDetailCartBadge.setVisibility(View.VISIBLE);
                tvDetailCartBadge.setText(String.valueOf(total));
            } else {
                tvDetailCartBadge.setVisibility(View.GONE);
            }
        }
    }
}
