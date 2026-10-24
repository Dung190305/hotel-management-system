# ĐỒ ÁN MÔN HỌC: LẬP TRÌNH HƯỚNG ĐỐI TƯỢNG (OOP)
# HỆ THỐNG QUẢN LÝ KHÁCH SẠN (HOTEL MANAGEMENT SYSTEM)

**Ngôn ngữ:** Java 17 / 21 LTS  
**Giao diện Desktop:** Java Swing + FlatLaf  
**Cơ sở dữ liệu:** MySQL 8.0 (Kết nối JDBC thuần)  
**Kiểm tra dữ liệu:** Hibernate Validator (Jakarta Bean Validation 3.0)  
**Mã hóa bảo mật:** jBCrypt  
**Quản lý thư viện:** Apache Maven  
**Kiểm thử tự động:** JUnit 5  

---

## 1. HƯỚNG DẪN KHỞI CHẠY ỨNG DỤNG

### Bước 1: Khởi động MySQL & Kiểm tra Database
- Đảm bảo dịch vụ `MySQL80` trên máy đang ở trạng thái **Running**.
- Database `hotel_db` đã được nạp sẵn toàn bộ cấu trúc và dữ liệu mẫu từ file `database/hotel_management.sql`.
- File cấu hình kết nối nằm tại: `src/main/resources/database.properties`.

### Bước 2: Chạy ứng dụng từ Terminal / Command Prompt
Mở terminal tại thư mục gốc của dự án (`QLKS`) và gõ lệnh:
```bash
mvn exec:java
```
Ứng dụng sẽ tự động tải các tài nguyên và mở lên cửa sổ đăng nhập `LoginFrame`.

### Bước 3: Chạy toàn bộ bài kiểm thử tự động (Unit Test)
```bash
mvn test
```
Toàn bộ 13 bài test kiểm tra kế thừa, đa hình, Bean Validation, JDBC thực tế và các quy tắc nghiệp vụ sẽ được thực thi tự động.

---

## 2. TÀI KHOẢN ĐĂNG NHẬP MẪU

| Vai trò | Tên đăng nhập | Mật khẩu mặc định | Quyền hạn trong hệ thống |
| :--- | :---: | :---: | :--- |
| **Quản trị / Quản lý (Admin)** | `admin` | `admin123` | Toàn quyền: Xem báo cáo, quản lý danh mục phòng, khách hàng, đặt phòng, hóa đơn, **Quản lý nhân viên & Tính bảng lương**. |
| **Lễ tân (Receptionist)** | `letan01` | `letan123` | Nghiệp vụ tiếp tân: Sơ đồ phòng, Đặt phòng, Check-in, Check-out, Hóa đơn & Thu tiền dịch vụ. *(Ẩn tab Quản lý nhân viên)*. |
| **Lễ tân 2** | `letan02` | `letan123` | Tương tự lễ tân 1. |

---

## 3. CÁC KỊCH BẢN THAO TÁC MẪU (DEMO WORKFLOW)

### Kịch bản 1: Sơ đồ phòng 5 màu & Đổi trạng thái phòng
1. Đăng nhập bằng `letan01` / `letan123`.
2. Vào tab **"Sơ đồ & Danh mục Phòng"**:
   - Thấy sơ đồ phòng trực quan dạng thẻ màu:
     * 🟢 **Còn trống (AVAILABLE)**
     * 🔴 **Đang có khách (OCCUPIED)**
     * 🟡 **Đã đặt (RESERVED)**
     * 🔵 **Đang dọn dẹp (CLEANING)**
     * ⚪ **Đang bảo trì (MAINTENANCE)**
   - Click chuột phải hoặc click trực tiếp vào một phòng để đổi trạng thái nhanh (ví dụ: chuyển phòng sau khi buồng phòng dọn xong từ `CLEANING` về `AVAILABLE`).

### Kịch bản 2: Đặt phòng & Chống trùng lịch (Quy tắc nghiệp vụ cốt lõi)
1. Vào tab **"Đặt phòng (Booking)"** $\rightarrow$ Bấm nút **"+ Tạo đơn đặt phòng"**.
2. Chọn khách hàng (hoặc thêm khách mới), chọn ngày nhận phòng và ngày trả phòng.
3. Hộp thoại tự động tải danh sách **phòng trống thực tế** và tính số đêm + tạm tính tiền.
4. Nếu cố tình chọn phòng đã có khách đặt trong khoảng ngày đó, hệ thống sẽ từ chối và hiện thông báo lỗi tiếng Việt dễ hiểu.

### Kịch bản 3: Quy trình Check-in, Check-out & Tự động lập Hóa đơn
1. Vào tab **"Tiếp tân: Check-in / Out"**:
   - **Tab 1 (Check-in):** Chọn khách đến nhận phòng $\rightarrow$ Bấm **"Xác nhận Check-in"** $\rightarrow$ Phòng tự chuyển sang màu đỏ (`OCCUPIED`).
   - **Tab 2 (Check-out):** Chọn phòng đang có khách trả phòng $\rightarrow$ Bấm **"Trả phòng & Lập hóa đơn"** $\rightarrow$ Phòng tự động chuyển sang màu xanh dương (`CLEANING`) và sinh hóa đơn thanh toán.

### Kịch bản 4: Gọi dịch vụ phát sinh & Thanh toán chống trùng lặp
1. Vào tab **"Hóa đơn & Thanh toán"**:
   - Chọn hóa đơn vừa tạo $\rightarrow$ Bấm **"+ Thêm dịch vụ vào phòng"** (chọn nước ngọt, bia, giặt là...) $\rightarrow$ Hệ thống tự động cộng dồn tiền dịch vụ.
   - Bấm **"Xem chi tiết hóa đơn"** để xem phiếu tính tiền.
   - Bấm **"THANH TOÁN NGAY"** $\rightarrow$ Chọn Tiền mặt hoặc Chuyển khoản $\rightarrow$ Xác nhận thanh toán $\rightarrow$ Hóa đơn chuyển sang `ĐÃ THANH TOÁN`.
   - Nếu bấm thanh toán lại lần nữa trên hóa đơn này, hệ thống sẽ chặn đứng và báo lỗi: *"Hóa đơn đã được thanh toán đầy đủ trước đó!"* (Chống thanh toán trùng lặp).

### Kịch bản 5: Quản lý nhân viên & Tính bảng lương đa hình (OOP)
1. Đăng xuất và đăng nhập lại bằng tài khoản Quản lý: `admin` / `admin123`.
2. Xuất hiện thêm tab **"Nhân sự & Tính lương"**.
3. Bấm nút **"💰 BẢNG TÍNH LƯƠNG (OOP)"**:
   - Hệ thống tự động tính lương theo Đa hình:
     * Lễ tân tính theo ca trực: `Lương cơ bản + Số ca * Thưởng ca`
     * Quản lý tính theo trách nhiệm: `Lương cơ bản * Hệ số + Thưởng hiệu quả`
   - Hiển thị tổng quỹ lương cần chi trả của khách sạn.

---

## 4. HƯỚNG DẪN TRẢ LỜI PHẢN BIỆN KHI BẢO VỆ ĐỒ ÁN OOP

Khi thầy cô đặt câu hỏi, hãy sử dụng các luận điểm sau:

### Câu 1: Em hãy chỉ ra tính Kế thừa (Inheritance) trong đồ án?
- **Trả lời:** Em xây dựng phân cấp kế thừa từ lớp cha trừu tượng `NguoiDung`:
  + `NguoiDung` $\rightarrow$ `KhachHang` và `NhanVien`.
  + `NhanVien` $\rightarrow$ `LeTan` và `QuanLy`.
  + Lớp cha `Phong` $\rightarrow$ `PhongThuong` và `PhongVIP`.
  Toàn bộ thuộc tính chung (mã định danh, họ tên, số điện thoại, đơn giá ngày...) được tái sử dụng ở lớp cha, giúp tránh lặp mã nguồn (DRY).

### Câu 2: Tính Đa hình (Polymorphism) được thể hiện ở đoạn code nào?
- **Trả lời:** Em thể hiện đa hình ở 2 nơi quan trọng nhất:
  1. Phương thức trừu tượng `tinhLuong()` trong lớp cha `NhanVien`: Lớp con `LeTan` ghi đè tính theo số ca trực (`luongCoBan + soCa * thuongCa`), lớp con `QuanLy` ghi đè tính theo hệ số trách nhiệm (`luongCoBan * heSo + thuongHieuQua`). Khi tính bảng lương trong `NhanVienService`, em chỉ cần duyệt danh sách `List<NhanVien>` và gọi `nv.tinhLuong()` mà không cần bất kỳ câu lệnh `if-else` nào để kiểm tra loại nhân viên.
  2. Phương thức trừu tượng `tinhTienPhong(long soNgay)` trong lớp `Phong`: `PhongThuong` tính theo đơn giá và giảm 5% khi ở từ 7 ngày; `PhongVIP` có phụ phí VIP và giảm 10% khi ở từ 5 ngày. Khi đặt phòng, em chỉ cần gọi `phong.tinhTienPhong()`.

### Câu 3: Tính Đóng gói (Encapsulation) được bảo đảm như thế nào?
- **Trả lời:** Toàn bộ thuộc tính trong các lớp Model đều là `private`. Quyền truy xuất thông qua getter và setter. Đặc biệt, em tích hợp **Hibernate Validator (Jakarta Bean Validation)** với các annotation (`@NotBlank`, `@Pattern`, `@DecimalMin`...) để ngăn chặn dữ liệu bẩn ngay từ tầng đối tượng.

### Câu 4: Tính Trừu tượng (Abstraction) và Quan hệ đối tượng?
- **Trả lời:**
  - `NguoiDung`, `NhanVien`, `Phong` là các `abstract class`, không cho phép khởi tạo trực tiếp đối tượng chung chung.
  - Các `interface` trong tầng DAO (`BaseDAO<T, ID>`, `PhongDAO`, `DatPhongDAO`...) định nghĩa các hành vi mà không làm lộ chi tiết câu lệnh SQL.
  - Quan hệ **Association**: `DatPhong` liên kết với `KhachHang`, `Phong` và `NhanVien`.
  - Quan hệ **Composition**: `HoaDon` chứa danh sách các `ChiTietDichVu`. Chi tiết dịch vụ chỉ tồn tại khi gắn với một hóa đơn cụ thể.
