# Database migrations (Liquibase)

## Cấu trúc

Changelog tổng: `src/main/resources/db/changelog/db.changelog-master.yaml`.

Mỗi module có folder `src/main/java/com/Marketplace_Management/<Module>/Migration`:

- `db.changelog.yaml` khai báo thứ tự chạy các file SQL của module.
- Mỗi file `NNN-create-<table>.sql` là một Liquibase formatted SQL changeset, có rollback.
- Maven sao chép các file `.yaml` và `.sql` trong folder này vào classpath khi build.

| Module | Số bảng |
| --- | ---: |
| Shared | 1 |
| Auth | 6 |
| Vendor | 2 |
| Catalog | 9 |
| Delivery | 7 |
| Cart | 2 |
| Order | 2 |
| Assistant | 3 |
| Merchandising | 0 |

Merchandising chưa có Entity nên changelog hiện tại rỗng. Tổng cộng 29 Entity và 3 bảng nối do `@JoinTable` khai báo.

## Database mới

Cấu hình datasource PostgreSQL trong `application.yml` hoặc qua biến môi trường, rồi dùng:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.yaml
```

Khởi động backend bằng `mvn spring-boot:run`. Liquibase tạo bảng trước khi Hibernate kiểm tra schema. Không dùng `ddl-auto: update` song song với Liquibase. Các file `application.example.yml` và `application.example.properties` đã có cấu hình tương ứng.

Spring Boot 4 cần `spring-boot-starter-liquibase` để tự động chạy migration; dependency này đã được cấu hình trong `pom.xml`. Xem [Spring Boot: Database Initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html#howto.data-initialization.migration-tool.liquibase).

UUID do Hibernate tạo bằng UUID v7; các ID dạng số dùng PostgreSQL identity. `created_at`, `updated_at` do Hibernate điền. `deleted_at` là nullable `timestamp(6) with time zone`, theo `@SoftDelete(TIMESTAMP)`; ba Entity có khóa riêng (`EmailVerifyToken`, `OAuthInfo`, `UserSession`), `Province`, `Ward` và `ProductCollection` không kế thừa các cột audit này.

Các cột và quan hệ bám theo JPA mapping: `@Column` quyết định độ dài SQL, `@Size` là validation dữ liệu. `OrderItem.productImages` là `varchar(255)[]`. Các UUID chỉ khai báo như cột thông thường (`userId`, `vendorId`, v.v.) không tự động được thêm khóa ngoại. `files.entity_id` là quan hệ đa hình nên không có khóa ngoại tới một bảng cụ thể.

Migration `Delivery/Migration/008-insert-vn-units.sql` tự động nhập 34 tỉnh/thành và 3.321 phường/xã từ bản sao UTF-8 của `postgres_ImportData_vn_units.sql`, sau khi tạo bảng Delivery. Liquibase chỉ chạy changeset này một lần; không cần chạy thủ công hai file `postgres_CreateTables_vn_units.sql` và `postgres_ImportData_vn_units.sql`. Tổng cộng có 32 changeset tạo bảng và 1 changeset nhập dữ liệu.

Rollback của changeset nhập dữ liệu xóa các ID đã nhập, theo thứ tự phường/xã trước, tỉnh/thành sau. Rollback sẽ bị khóa ngoại chặn nếu dữ liệu này đang được địa chỉ giao hàng tham chiếu. File nguồn ở thư mục Backend được giữ nguyên; thay đổi dữ liệu sau này cần changeset mới thay vì sửa file migration đã chạy.

Các bảng có `deleted_at` dùng partial unique index với điều kiện `WHERE deleted_at IS NULL`, khai báo bên ngoài `CREATE TABLE`. Liquibase quản lý các index này; không khai báo lại unique toàn bảng trong JPA. Những kiểm tra `@Unique` tương ứng dùng `deletedAtColumn = "deleted_at"` để bỏ qua bản ghi đã xóa mềm. Các bảng không có `deleted_at` vẫn giữ unique constraint thông thường.

## Database đã có dữ liệu

Đây là bộ migration khởi tạo cho schema mới, chưa phải script nâng cấp một database cũ cụ thể. Không chạy thẳng vào database đã có các bảng cùng tên. Không dùng `IF NOT EXISTS` hoặc `MARK_RAN` để bỏ qua sai lệch schema.

Trước khi chuyển database cũ sang Liquibase:

1. Sao lưu database và làm việc trên bản sao.
2. Tạo database trống bằng bộ migration này, rồi đối chiếu schema với bản sao database cũ.
3. Viết changeset nâng cấp riêng cho mọi sai lệch, gồm kiểu ID, cột, nullability, index, constraint và dữ liệu cần chuyển đổi. Đặc biệt kiểm tra `product_collection.product_id`/`collection_id` (UUID), các index đã sửa tên cột và các cột soft delete.
4. Chỉ dùng `changelog-sync` để đánh dấu baseline khi đã xác nhận toàn bộ schema tương ứng thực sự tồn tại. Lệnh này chỉ ghi lịch sử, không kiểm tra hoặc sửa cấu trúc database.
5. Chạy và kiểm thử changeset nâng cấp trên bản sao trước khi triển khai.

Hai ApplicationRunner cũ `OAuthInfoMigration` và `AddressDetailMigration` vẫn giữ nguyên để bảo toàn luồng chuyển dữ liệu legacy. Bộ migration mới không xóa cột hoặc dữ liệu legacy.

## Thêm migration mới

Tạo file SQL mới trong module, dùng changeset ID duy nhất, thêm rollback phù hợp, rồi thêm `include` vào cuối `db.changelog.yaml` của module:

```yaml
  - include:
      file: 010-add-product-field.sql
      relativeToChangelogFile: true
```

Không sửa changeset đã chạy ở môi trường khác vì Liquibase kiểm tra checksum. Nếu migration mới phụ thuộc vào thay đổi mới của module khác, đăng ký các file mới ở cuối changelog tổng theo đúng thứ tự phụ thuộc; không chỉ dựa vào thứ tự module ban đầu.

## Kiểm tra

Kiểm tra changelog đóng gói có đủ mọi Entity/bảng nối và có rollback, không kết nối database:

```powershell
mvn -Dtest=LiquibaseMigrationTest test
```

Kiểm tra trên PostgreSQL (tài khoản cần quyền tạo schema):

```powershell
mvn -Dtest=LiquibaseMigrationTest '-Dmigration.test.url=jdbc:postgresql://localhost:5432/migration_test' '-Dmigration.test.user=migration_test' '-Dmigration.test.password=your-password' test
```

Test PostgreSQL tạo schema có tên ngẫu nhiên trong database được chỉ định, chạy migration, đối chiếu với Hibernate, kiểm tra constraint, chạy lại, rollback và migrate lại; cuối cùng xóa schema test. Test này không chạy khi thiếu `migration.test.url` và không đọc datasource của ứng dụng.
