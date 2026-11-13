# ĐỒ ÁN MÔN HỌC: LẬP TRÌNH HƯỚNG ĐỐI TƯỢNG (OOP)
# HỆ THỐNG QUẢN LÝ KHÁCH SẠN (HOTEL MANAGEMENT SYSTEM)

**Ngôn ngữ & Nền tảng:** Java 21 LTS  
**Giao diện Desktop:** Java Swing (FlatLaf Modern UI + MVC Controller + SwingWorker đa luồng)  
**Cơ sở dữ liệu:** MySQL 8.0 (Kết nối JDBC thuần, Transaction ACID & Khóa dòng `SELECT ... FOR UPDATE`)  
**Kiểm tra tính hợp lệ dữ liệu:** Hibernate Validator (Jakarta Bean Validation 3.0)  
**Mã hóa bảo mật mật khẩu:** BCrypt (jBCrypt)  
**Quản lý mã định danh:** `IdGenerator` nguyên tử qua bảng `id_sequence` với MySQL `LAST_INSERT_ID`  
**Quản lý thư viện & Build:** Apache Maven  
**Kiểm thử tự động:** JUnit 5 (38 bài Unit Test pass 100%)  

---

## 1. HƯỚNG DẪN KHỞI CHẠY ỨNG DỤNG

### Bước 1: Nạp Cơ sở dữ liệu MySQL
Hệ thống cung cấp 2 cách nạp cơ sở dữ liệu:
* **Cách 1 (Một lệnh duy nhất):** Mở MySQL Workbench hoặc CLI và chạy file:
  ```sql
  source database/hotel_management.sql;
  ```
* **Cách 2 (Phân tách chuẩn Schema & Seed):**
  1. Chạy cấu trúc bảng: `source database/schema.sql;`
  2. Chạy dữ liệu mẫu ban đầu: `source database/seed.sql;`

### Bước 2: Cấu hình kết nối Database & Biến môi trường
Hệ thống hỗ trợ 2 cơ chế cấu hình kết nối CSDL (ưu tiên biến môi trường để bảo mật):
1. **Biến môi trường (Khuyến nghị):**
   - `DB_URL`: `jdbc:mysql://localhost:3306/hotel_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh`
   - `DB_USER`: Tài khoản MySQL (mặc định: `root`)
   - `DB_PASSWORD`: Mật khẩu MySQL
2. **File cấu hình nội bộ (Fallback):**
   - Đặt tại `src/main/resources/database.properties` (file đã được đưa vào `.gitignore` để tránh lộ thông tin bảo mật).

### Bước 3: Chạy ứng dụng từ Terminal / Command Prompt
Mở terminal tại thư mục gốc của dự án (`QLKS`) và gõ lệnh:
```bash
mvn exec:java
```
Ứng dụng sẽ tự động tải các tài nguyên và mở lên cửa sổ đăng nhập `LoginFrame`.

### Bước 4: Chạy toàn bộ bài kiểm thử tự động (Unit Test)
```bash
# Chạy bộ 38 Unit Test kiểm tra toàn bộ nghiệp vụ (không phụ thuộc kết nối MySQL):
mvn test -Dtest="AppTest,ModelOopTest,ThongBaoServiceTest,IdGeneratorTest,AuthorizationTest,TransactionRollbackTest,CheckInBusinessRuleTest,DichVuGhiNhanTest"

# Hoặc chạy toàn bộ test suite:
mvn test
```

---

## 2. MA TRẬN PHÂN QUYỀN VÀ TÀI KHOẢN ĐĂNG NHẬP MẪU

Hệ thống triển khai mô hình kiểm soát truy cập dựa trên vai trò (RBAC) nghiêm ngặt tại tầng Service (`AuthorizationService.require(Permission)`):

| Chức năng nghiệp vụ | ADMIN (Quản trị) | MANAGER (Quản lý) | LE_TAN (Lễ tân) |
| :--- | :---: | :---: | :---: |
| Quản lý tài khoản và nhân viên |  | ❌ | ❌ |
| Sửa cấu hình phòng, dịch vụ (danh mục) |  |  | ❌ |
| Thêm và sửa khách hàng |  |  |  |
| Tạo, hủy đặt phòng; Check-in / Check-out |  |  |  |
| Ghi nhận dịch vụ và thanh toán hóa đơn |  |  |  |
| Xem báo cáo, thống kê doanh thu |  |  | ❌ |

### Danh sách tài khoản thử nghiệm ban đầu:
* **Quản trị viên (Admin):** `admin` / mật khẩu: `admin123` (Toàn quyền hệ thống).
* **Quản lý điều hành (Manager):** `manager` / mật khẩu: `manager123` (Quản lý danh mục, đặt phòng, hóa đơn và xem báo cáo).
* **Lễ tân 1 (Receptionist):** `letan01` / mật khẩu: `letan123` (Nghiệp vụ tiếp tân, check-in, check-out, hóa đơn).
* **Lễ tân 2:** `letan02` / mật khẩu: `letan123`.

---

## 3. CÁC ĐIỂM SÁNG KIẾN TRÚC & NGHIỆP VỤ OOP

1. **Đa hình (Polymorphism):**
   - Phân cấp `Phong` $\rightarrow$ `PhongThuong`, `PhongVIP`: đa hình phương thức `tinhTienPhong(soDem)` (phòng VIP tính thêm phụ phí dịch vụ và ban công view biển).
   - Phân cấp `NhanVien` $\rightarrow$ `LeTan`, `QuanLy`: đa hình phương thức `tinhLuongThucNhan()` (Lễ tân tính theo số ca trực, Quản lý tính theo hệ số trách nhiệm và thưởng hiệu quả).

2. **Gắn Dịch vụ theo Phiếu đặt phòng (Booking Service Binding):**
   - Dịch vụ phát sinh (`ChiTietDichVu`) gắn trực tiếp vào phiếu đặt phòng đang lưu trú (`ma_dat_phong`) với trạng thái `DANG_O`.
   - Lưu vết đơn giá tại thời điểm sử dụng (Snapshot Price): khi bảng giá dịch vụ thay đổi trong tương lai, chi tiết dịch vụ cũ không bao giờ bị tính sai giá.
   - Khi làm thủ tục trả phòng (Check-out), hệ thống tự động tổng hợp tiền dịch vụ vào hóa đơn.

3. **Toàn vẹn Transaction & Concurrency Control:**
   - Mọi luồng ghi nhiều bảng (`Check-in`, `Check-out`, `Thanh toán`) đều chạy trong 1 `Connection` với `conn.setAutoCommit(false)`, `commit()` và `rollback()` tự động.
   - Sử dụng `SELECT ... FOR UPDATE` để khóa dòng hóa đơn và dòng phòng, ngăn chặn hiện tượng 2 nhân viên cùng lúc thao tác gây trùng phòng hoặc thanh toán đè (Double Payment Prevention).

4. **Kiến trúc phân tầng Controller & SwingWorker:**
   - Tầng Giao diện (Swing View) hoàn toàn tách biệt khỏi tầng Nghiệp vụ (Service) thông qua các lớp `*Controller`.
   - Các tác vụ nặng hoặc gọi Service đều chạy trên background thread (`SwingWorker`), giữ giao diện luôn mượt mà và chống treo ứng dụng (No UI Freeze).

5. **Chuông thông báo trực quan theo đơn tới hạn:**
   - Biểu tượng chuông với dấu chấm than thông minh:
     * **Màu đỏ:** Có đơn tới hạn cần xử lý chưa đọc.
     * **Màu xanh:** Tất cả thông báo đã được xem.
     * **Không có dấu than:** Không có đơn nào tới hạn.
