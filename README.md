# Lập trình di động

Ứng dụng Android quản lý và bán lẻ thiết bị công nghệ, xây dựng bằng Java trên nền tảng Material Design 3, sử dụng SQLite kết hợp đồng bộ với Firebase Authentication và Cloud Firestore.

## 1. Tính năng 

### 1.1. Xác thực và phân quyền người dùng
- Đăng nhập và đăng ký tài khoản thông qua SQLite cục bộ và Firebase Authentication.
- Tự động duy trì phiên đăng nhập bằng `SharedPreferences`.
- Phân quyền tài khoản theo hai vai trò: Khách hàng (`user`) và Quản trị viên (`admin`).

### 1.2. Tra cứu và khám phá sản phẩm
- Hiển thị danh sách sản phẩm theo bố cục lưới 2 cột và danh sách sản phẩm nổi bật (Spotlight).
- Tìm kiếm sản phẩm theo thời gian thực dựa trên tên, mô tả hoặc danh mục.
- Lọc sản phẩm theo danh mục (Điện thoại, Máy tính bảng, Laptop, Thiết bị âm thanh, Phụ kiện), trạng thái tồn kho và danh sách yêu thích.
- Sắp xếp sản phẩm theo giá tăng dần hoặc giảm dần.
- Phân trang danh sách sản phẩm.

### 1.3. Chi tiết sản phẩm, đánh giá và yêu thích
- Hiển thị thông tin chi tiết sản phẩm: hình ảnh, mã SKU, giá bán, mức giảm giá, chế độ bảo hành và thông số kỹ thuật.
- Điều hướng trực tiếp sang sản phẩm trước hoặc tiếp theo trong danh mục.
- Gợi ý danh sách sản phẩm cùng danh mục ở cuối trang.
- Lưu hoặc bỏ lưu sản phẩm vào danh sách Yêu thích (Wishlist) gắn với từng tài khoản.
- Hệ thống đánh giá và bình luận sản phẩm:
  - Tính điểm đánh giá trung bình (thang điểm 1 đến 5 sao) và thống kê tỷ lệ theo từng mức sao.
  - Tự động gắn nhãn xác thực đã mua hàng đối với tài khoản có đơn hàng chứa sản phẩm tương ứng.
  - Cho phép gửi đánh giá mới, nhấn nút Hữu ích trên bình luận và xóa bình luận (dành cho tác giả hoặc Quản trị viên).

### 1.4. Giỏ hàng, mã giảm giá và đặt hàng
- Quản lý giỏ hàng riêng biệt cho từng tài khoản người dùng trong SQLite (`cart_items`).
- Thêm sản phẩm, cập nhật số lượng hoặc xóa sản phẩm khỏi giỏ hàng.
- Áp dụng mã giảm giá (Voucher) và tự động tính toán số tiền chiết khấu vào tổng giá trị thanh toán.
- Hỗ trợ chọn phương thức thanh toán: Tiền mặt khi nhận hàng (COD) hoặc Chuyển khoản ngân hàng (QR).
- Kiểm tra thông tin giao nhận qua hộp thoại xác nhận trước khi tạo đơn hàng.

### 1.5. Theo dõi và quản lý đơn hàng
- Lọc danh sách đơn hàng theo trạng thái: Tất cả, Đang xử lý / giao, Hoàn tất, Đã hủy.
- Theo dõi tiến trình đơn hàng qua 5 bước:
  1. Đang xử lý
  2. Đang đóng gói
  3. Đã giao cho đơn vị vận chuyển
  4. Đang giao hàng
  5. Giao thành công
- Cho phép khách hàng hủy đơn hàng kèm lý do khi đơn chưa bàn giao cho đơn vị vận chuyển.
- Hỗ trợ đặt lại đơn hàng đã hủy (tự động khôi phục danh sách sản phẩm vào giỏ hàng và điền sẵn thông tin người nhận).

### 1.6. Quản trị hệ thống (dành cho Admin)
- **Quản lý sản phẩm**: Thêm mới, cập nhật thông tin, điều chỉnh giá, thay đổi trạng thái tồn kho và xóa sản phẩm. Hỗ trợ chọn ảnh sản phẩm từ bộ nhớ thiết bị, tải từ đường dẫn URL hoặc dùng ảnh mẫu có sẵn.
- **Quản lý đơn hàng**: Xem toàn bộ đơn hàng trên hệ thống, cập nhật trạng thái vận chuyển theo 5 bước hoặc hủy đơn hàng kèm lý do.
- **Quản lý người dùng**: Xem danh sách tài khoản, thêm người dùng mới, cập nhật mật khẩu, thay đổi quyền hạn (`user` / `admin`) và xóa tài khoản.

## 2. Công nghệ và kiến trúc

| Thành phần | Chi tiết  |
| :--- | :--- |
| Ngôn ngữ | Java (JDK 11) |
| Android SDK | `minSdk 24` (Android 7.0), `targetSdk 37`, `compileSdk 37` |
| Giao diện | Material Design 3, AndroidX AppCompat, ConstraintLayout, RecyclerView, NestedScrollView |
| Cơ sở dữ liệu cục bộ | SQLite (`SQLiteOpenHelper`, database `TechStore.db` version 8) |
| Dịch vụ đám mây | Firebase Authentication, Cloud Firestore (`firebase-bom:33.7.0`) |

### Cấu trúc bảng SQLite (`TechStore.db`)
- `users`: Lưu thông tin đăng nhập và vai trò (`id`, `username`, `password`, `role`).
- `products`: Lưu danh mục sản phẩm, thông số, trạng thái tồn kho và dữ liệu hình ảnh (`id`, `name`, `description`, `price`, `image_res_id`, `category`, `sku`, `warranty`, `detailed_specs`, `in_stock`, `image_path`, `image_base64`, `image_key`).
- `orders`: Lưu thông tin đơn hàng và tiến trình vận chuyển (`id`, `order_code`, `username`, `receiver_name`, `receiver_phone`, `receiver_address`, `receiver_note`, `payment_method`, `total_price`, `item_count`, `created_at`, `status_step`, `items_summary`, `cancel_reason`, `items_raw`).
- `cart_items`: Lưu giỏ hàng theo tài khoản (`id`, `username`, `sku`, `product_name`, `price`, `quantity`).
- `product_reviews`: Lưu đánh giá và bình luận sản phẩm (`id`, `sku`, `username`, `rating`, `comment`, `created_at`, `verified`, `helpful_count`).
- `wishlist`: Lưu sản phẩm yêu thích theo tài khoản (`id`, `username`, `sku`, `created_at`).

## 3. Cấu trúc mã nguồn

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/example/myapplication/
│   ├── AdminDashboardActivity.java   # Màn hình quản trị sản phẩm, đơn hàng và người dùng
│   ├── CartItem.java                 # Model chi tiết món hàng trong giỏ
│   ├── CartManager.java              # Quản lý trạng thái và đồng bộ giỏ hàng
│   ├── DatabaseHelper.java           # Khởi tạo và truy vấn SQLite
│   ├── LoginActivity.java            # Màn hình đăng nhập
│   ├── Order.java                    # Model đơn hàng và định nghĩa các bước trạng thái
│   ├── OrderAdapter.java             # Adapter hiển thị danh sách đơn hàng
│   ├── OrderCancelHelper.java        # Xử lý hộp thoại hủy đơn hàng
│   ├── OrderDetailActivity.java      # Màn hình chi tiết đơn hàng và timeline vận chuyển
│   ├── OrderTrackingActivity.java    # Màn hình danh sách và lọc đơn hàng
│   ├── PaymentActivity.java          # Màn hình giỏ hàng, áp dụng voucher và thanh toán
│   ├── Product.java                  # Model sản phẩm và ánh xạ dữ liệu Firestore
│   ├── ProductAdapter.java           # Adapter hiển thị lưới sản phẩm
│   ├── ProductDetailActivity.java    # Màn hình chi tiết sản phẩm và đánh giá
│   ├── ProductEditorHelper.java      # Hộp thoại thêm/sửa sản phẩm cho Admin
│   ├── ProductImageHelper.java       # Tiện ích nạp và hiển thị ảnh sản phẩm
│   ├── ProductImagePicker.java       # Tiện ích chọn ảnh từ thiết bị hoặc tải từ URL
│   ├── ProductListActivity.java      # Màn hình chính, tìm kiếm, lọc và phân trang
│   ├── ProductReview.java            # Model đánh giá sản phẩm
│   ├── ProfileActivity.java          # Màn hình thông tin tài khoản
│   ├── RegisterActivity.java         # Màn hình đăng ký tài khoản
│   ├── SpotlightAdapter.java         # Adapter hiển thị sản phẩm nổi bật
│   ├── UiMotion.java                 # Tiện ích chuyển động giao diện
│   └── UserAccount.java              # Model tài khoản người dùng
└── res/
    ├── color/                        # Selector màu sắc cho thành phần giao diện
    ├── drawable/                     # Vector icons và hình ảnh sản phẩm
    ├── layout/                       # Các tệp giao diện XML của Activity, Item và Dialog
    └── values/                       # Định nghĩa màu sắc, chuỗi ký tự và theme
```

## 4. Thông tin cấu hình và kiểm thử

### Tài khoản Quản trị viên mặc định
- **Tên đăng nhập**: `admin`
- **Mật khẩu**: `123456`

### Mã giảm giá tích hợp sẵn
- `TECH500K`: Giảm 500.000đ cho đơn từ 10.000.000đ (giảm 200.000đ cho đơn dưới 10.000.000đ).
- `NEWMEMBER`: Giảm 5% giá trị đơn hàng (tối đa 1.000.000đ).
- `FREESHIP2H`: Giảm 150.000đ chi phí giao hàng.

## 5. Hướng dẫn cài đặt và biên dịch

1. Mở thư mục gốc của dự án bằng **Android Studio**.
2. Đảm bảo tệp cấu hình `app/google-services.json` hợp lệ trong thư mục module `app`.
3. Đồng bộ Gradle hoặc biên dịch trực tiếp qua cửa sổ dòng lệnh:
   ```powershell
   .\gradlew.bat assembleDebug
   ```
4. Cài đặt và khởi chạy ứng dụng trên thiết bị thật hoặc máy ảo Android (yêu cầu Android 7.0, API 24 trở lên).
