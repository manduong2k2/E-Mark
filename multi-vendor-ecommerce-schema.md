# Thiết kế Hệ thống Thương mại điện tử Multi-vendor
## Theo mô hình Bounded Context (DDD)

---

## 0. Tổng quan kiến trúc

Hệ thống được chia thành **11 bounded context**, mỗi context có thể triển khai như một service độc lập với database riêng (Database per Service). Giao tiếp giữa các context chủ yếu qua:
- **Domain Events** (async, qua message broker: Kafka/RabbitMQ) — ví dụ `OrderPlaced`, `PaymentCompleted`, `StockReserved`
- **ID reference** — context này chỉ lưu `user_id`, `vendor_id`, `product_id`... dạng UUID, không JOIN trực tiếp qua DB

```
┌─────────────┐   ┌──────────────┐   ┌─────────────┐
│   Identity  │   │    Vendor    │   │   Catalog   │
│  & Access   │   │  Management  │   │  (Product)  │
└─────────────┘   └──────────────┘   └─────────────┘
       │                  │                  │
       ▼                  ▼                  ▼
┌─────────────┐   ┌──────────────┐   ┌─────────────┐
│Cart/Checkout│──▶│    Order     │◀──│  Inventory  │
└─────────────┘   └──────────────┘   └─────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        ▼                 ▼                 ▼
┌─────────────┐   ┌──────────────┐   ┌─────────────┐
│   Payment   │   │   Shipping   │   │Review/Rating│
│  & Payout   │   │ (Fulfillment)│   │             │
└─────────────┘   └──────────────┘   └─────────────┘

┌─────────────┐   ┌──────────────┐
│  Pricing &  │   │Notification/ │
│  Promotion  │   │Messaging     │
└─────────────┘   └──────────────┘
```

---

## 1. Identity & Access Context (IAM)
*Quản lý người dùng, xác thực, phân quyền cho cả buyer, vendor-staff, admin*

| Bảng | Mô tả | Cột chính |
|---|---|---|
| `users` | Tài khoản người dùng gốc | id, email, phone, password_hash, status (active/banned), user_type (buyer/vendor_staff/admin), created_at |
| `user_profiles` | Thông tin cá nhân | user_id, full_name, avatar_url, gender, dob |
| `addresses` | Sổ địa chỉ (giao hàng, nhận hàng) | id, user_id, recipient_name, phone, province, district, ward, detail, is_default |
| `roles` | Vai trò hệ thống | id, name (buyer, vendor_owner, vendor_staff, admin, cs_agent) |
| `permissions` | Danh sách quyền hạn | id, code, description |
| `role_permissions` | Map role ↔ permission | role_id, permission_id |
| `user_roles` | Map user ↔ role | user_id, role_id |
| `auth_sessions` | Phiên đăng nhập / refresh token | id, user_id, device_info, ip, refresh_token, expires_at |
| `oauth_accounts` | Đăng nhập MXH (Google/FB) | user_id, provider, provider_user_id |

---

## 2. Vendor Management Context
*Quản lý gian hàng (shop), hồ sơ pháp lý, nhân sự vendor*

| Bảng | Mô tả | Cột chính |
|---|---|---|
| `vendors` | Gian hàng | id, owner_user_id, shop_name, slug, status (pending/active/suspended), tier (standard/premium) |
| `vendor_profiles` | Hồ sơ shop | vendor_id, logo_url, banner_url, description, business_type |
| `vendor_verification_docs` | Giấy tờ pháp lý, KYC | id, vendor_id, doc_type (business_license, tax_id, id_card), file_url, status, reviewed_by |
| `vendor_bank_accounts` | Tài khoản nhận tiền | id, vendor_id, bank_name, account_number, account_holder, is_verified |
| `vendor_staff` | Nhân sự thuộc shop | id, vendor_id, user_id, role (owner/manager/staff) |
| `vendor_settings` | Cấu hình vận hành shop | vendor_id, auto_confirm_order, return_policy, warehouse_default_id |
| `vendor_categories_registered` | Ngành hàng shop được phép bán | vendor_id, category_id, commission_rate |
| `vendor_metrics` | Chỉ số tổng hợp (đọc-only, cache) | vendor_id, avg_rating, total_orders, response_rate, on_time_ship_rate |

---

## 3. Catalog (Product) Context
*Danh mục sản phẩm — vendor tự quản lý sản phẩm của mình*

| Bảng | Mô tả | Cột chính |
|---|---|---|
| `categories` | Cây danh mục (nền tảng quản lý) | id, parent_id, name, slug, level |
| `brands` | Thương hiệu | id, name, logo_url |
| `products` | Sản phẩm gốc (spu) | id, vendor_id, category_id, brand_id, name, slug, description, status (draft/active/hidden), product_type |
| `product_variants` | Biến thể / SKU | id, product_id, sku, price, compare_at_price, weight, dimensions |
| `product_attributes` | Thuộc tính (màu, size...) | id, product_id, attribute_name |
| `product_attribute_values` | Giá trị thuộc tính | id, attribute_id, value |
| `product_images` | Hình ảnh | id, product_id, variant_id (nullable), url, sort_order |
| `product_tags` | Gắn thẻ (tìm kiếm/SEO) | product_id, tag |
| `product_moderation_logs` | Lịch sử duyệt sản phẩm | id, product_id, action, reviewer_id, note |

> Ghi chú: Search/Filter nên đẩy dữ liệu sang Elasticsearch/OpenSearch, không query trực tiếp bảng này cho tìm kiếm full-text.

---

## 4. Inventory Context
*Tồn kho — tách riêng để hỗ trợ nhiều kho / nhiều vendor, tránh oversell*

| Bảng | Mô tả | Cột chính |
|---|---|---|
| `warehouses` | Kho hàng (của vendor hoặc 3PL) | id, vendor_id, name, address, type (self/3pl) |
| `inventory_items` | Tồn kho theo SKU-kho | id, warehouse_id, variant_id (ref), quantity_on_hand, quantity_reserved |
| `inventory_reservations` | Giữ hàng tạm khi checkout | id, order_id (ref), variant_id, warehouse_id, quantity, expires_at, status |
| `inventory_transactions` | Log xuất/nhập/điều chỉnh kho | id, warehouse_id, variant_id, type (in/out/adjust/return), quantity, reference_id, created_at |
| `stock_alerts` | Cảnh báo hết hàng | id, variant_id, warehouse_id, threshold, triggered_at |

---

## 5. Pricing & Promotion Context
*Giá bán, khuyến mãi, hoa hồng nền tảng*

| Bảng | Mô tả | Cột chính |
|---|---|---|
| `product_prices` | Giá theo kênh/thời điểm (nếu cần price history) | id, variant_id (ref), price, currency, effective_from, effective_to |
| `discounts` | Chương trình giảm giá của vendor | id, vendor_id, name, type (percentage/fixed), value, start_at, end_at, applies_to |
| `coupons` | Mã giảm giá | id, code, scope (platform/vendor), vendor_id (nullable), discount_type, value, usage_limit, min_order_value |
| `coupon_usages` | Lịch sử dùng mã | id, coupon_id, user_id, order_id (ref), used_at |
| `flash_sales` | Chương trình flash sale | id, name, start_at, end_at |
| `flash_sale_items` | SKU tham gia flash sale | flash_sale_id, variant_id, flash_price, quota |
| `platform_commission_rules` | Quy tắc chiết khấu nền tảng theo ngành hàng | id, category_id, vendor_tier, commission_rate |

---

## 6. Cart & Checkout Context
*Giỏ hàng — nên tách nhẹ, có thể lưu Redis + đồng bộ DB*

| Bảng | Mô tả | Cột chính |
|---|---|---|
| `carts` | Giỏ hàng | id, user_id, status (active/converted/abandoned) |
| `cart_items` | Sản phẩm trong giỏ | id, cart_id, vendor_id (ref), variant_id (ref), quantity, price_snapshot |
| `wishlists` | Danh sách yêu thích | id, user_id, name |
| `wishlist_items` | Sản phẩm trong wishlist | wishlist_id, variant_id |

---

## 7. Order Context
*Lõi giao dịch — 1 order của buyer có thể tách thành nhiều sub-order theo vendor*

| Bảng | Mô tả | Cột chính |
|---|---|---|
| `orders` | Đơn hàng tổng (buyer) | id, user_id, order_number, total_amount, status, payment_status, shipping_address_snapshot, created_at |
| `sub_orders` | Đơn con theo từng vendor (để vendor xử lý độc lập) | id, order_id, vendor_id, status (pending/confirmed/shipped/delivered/cancelled), subtotal, shipping_fee |
| `order_items` | Chi tiết sản phẩm trong sub-order | id, sub_order_id, variant_id, product_name_snapshot, sku_snapshot, quantity, unit_price, total_price |
| `order_status_history` | Lịch sử trạng thái | id, sub_order_id, from_status, to_status, changed_by, changed_at |
| `order_cancellations` | Yêu cầu hủy | id, sub_order_id, reason, requested_by, status |
| `return_requests` | Yêu cầu trả hàng/hoàn tiền | id, sub_order_id, order_item_id, reason, type (return/refund/exchange), status, evidence_urls |
| `return_status_history` | Lịch sử xử lý return | id, return_request_id, status, note, changed_at |

---

## 8. Payment & Payout Context
*Thanh toán từ buyer + đối soát/thanh toán cho vendor*

| Bảng | Mô tả | Cột chính |
|---|---|---|
| `payment_methods` | Phương thức thanh toán của user | id, user_id, type (card/e-wallet/bank), token_provider, masked_info |
| `payments` | Giao dịch thanh toán của order | id, order_id (ref), amount, method, gateway (VNPay/Momo/Stripe...), status, gateway_transaction_id |
| `refunds` | Hoàn tiền cho buyer | id, payment_id, return_request_id (ref), amount, status, processed_at |
| `vendor_wallets` | Ví số dư của vendor trên sàn | id, vendor_id, balance, pending_balance |
| `wallet_transactions` | Lịch sử biến động ví | id, wallet_id, type (order_income/commission_fee/withdrawal/refund_deduction), amount, reference_id |
| `payout_requests` | Yêu cầu rút tiền của vendor | id, vendor_id, amount, bank_account_id (ref), status |
| `payout_batches` | Đợt thanh toán hàng loạt (nền tảng chạy định kỳ) | id, period_start, period_end, status |
| `payout_batch_items` | Chi tiết từng vendor trong đợt | batch_id, vendor_id, gross_amount, commission_deducted, net_amount |

---

## 9. Shipping / Fulfillment Context
*Vận chuyển — tích hợp đơn vị vận chuyển thứ ba (GHN, GHTK, J&T...)*

| Bảng | Mô tả | Cột chính |
|---|---|---|
| `shipping_carriers` | Đơn vị vận chuyển | id, name, api_config |
| `shipping_zones` | Vùng giao hàng & phí | id, name, provinces |
| `shipping_rates` | Bảng giá ship theo vendor/zone/trọng lượng | id, vendor_id, zone_id, weight_from, weight_to, fee |
| `shipments` | Vận đơn cho mỗi sub-order | id, sub_order_id (ref), carrier_id, tracking_number, status (created/picked_up/in_transit/delivered/failed) |
| `shipment_items` | Sản phẩm trong vận đơn (hỗ trợ tách kiện) | shipment_id, order_item_id, quantity |
| `tracking_events` | Log hành trình đơn hàng | id, shipment_id, status, location, timestamp, raw_payload |

---

## 10. Review & Rating Context

| Bảng | Mô tả | Cột chính |
|---|---|---|
| `reviews` | Đánh giá sản phẩm | id, order_item_id (ref), user_id, product_id (ref), vendor_id (ref), rating (1-5), comment, status |
| `review_images` | Ảnh/video kèm review | id, review_id, url |
| `review_replies` | Phản hồi của vendor | id, review_id, vendor_staff_id, content |
| `review_votes` | Vote "hữu ích" | review_id, user_id, is_helpful |

---

## 11. Notification & Messaging Context

| Bảng | Mô tả | Cột chính |
|---|---|---|
| `notifications` | Thông báo đẩy tới user/vendor | id, recipient_id, recipient_type, type, title, body, is_read, sent_at |
| `notification_templates` | Mẫu thông báo (email/SMS/push) | id, code, channel, subject_template, body_template |
| `notification_preferences` | Cài đặt nhận thông báo | user_id, channel, is_enabled |
| `conversations` | Hội thoại buyer ↔ vendor (chat hỏi đáp) | id, buyer_id, vendor_id, last_message_at |
| `messages` | Tin nhắn | id, conversation_id, sender_id, content, attachment_url, sent_at |
| `support_tickets` | Khiếu nại/hỗ trợ với sàn | id, user_id, order_id (ref), category, status, assigned_agent_id |

---

## Nguyên tắc thiết kế quan trọng

1. **Không FK xuyên context**: `sub_orders.vendor_id` chỉ là ID tham chiếu logic, không JOIN cross-database. Validate qua API/event.
2. **Snapshot dữ liệu tại thời điểm giao dịch**: `order_items` lưu `product_name_snapshot`, `sku_snapshot`, `unit_price` — vì sản phẩm/giá có thể đổi sau này, order không được thay đổi theo.
3. **Tách Order thành `orders` + `sub_orders`**: bắt buộc với multi-vendor vì mỗi vendor xử lý fulfillment độc lập (đóng gói, ship, trạng thái riêng), nhưng buyer vẫn thấy 1 đơn hàng tổng.
4. **Saga pattern cho luồng đặt hàng**: `Checkout → Reserve Inventory → Create Order → Charge Payment → Confirm/Release Stock`, dùng event để rollback nếu bước nào fail (tránh 2-phase-commit xuyên service).
5. **Đối soát commission**: tính hoa hồng tại thời điểm `sub_order` hoàn tất (delivered + hết thời gian khiếu nại), không tính ngay lúc đặt hàng.
6. **Idempotency**: các bảng giao dịch tiền (`payments`, `wallet_transactions`, `payout_batch_items`) cần cột `idempotency_key` để tránh double-processing khi retry webhook.
7. **Đọc nhanh cho hiển thị**: cân nhắc bảng/cache tổng hợp kiểu CQRS (`vendor_metrics`, product rating trung bình) thay vì aggregate real-time mỗi lần load trang.

---

## Gợi ý mở rộng (nếu cần)
- **Search Context**: đồng bộ `products` + `vendor_metrics` sang Elasticsearch, không thuộc RDBMS.
- **Analytics/Reporting Context**: data warehouse riêng (star schema), ETL từ các context trên, không ảnh hưởng OLTP.
- **Advertising Context**: nếu có sponsored listing cho vendor — tách riêng `ad_campaigns`, `ad_bids`, `ad_impressions`.
