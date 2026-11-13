
-- =====================================================================
-- HỆ THỐNG QUẢN LÝ KHÁCH SẠN (HOTEL MANAGEMENT SYSTEM)
-- Script tạo Database, Bảng, Ràng buộc và Dữ liệu mẫu ban đầu
-- Phiên bản MySQL tương thích: MySQL 8.0+
-- =====================================================================

-- Đảm bảo toàn bộ phiên làm việc của MySQL Client sử dụng mã hóa utf8mb4 (tránh lỗi ký tự tiếng Việt ?)
SET NAMES 'utf8mb4';
SET CHARACTER SET utf8mb4;
SET character_set_connection = 'utf8mb4';

DROP DATABASE IF EXISTS hotel_db;
CREATE DATABASE hotel_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE hotel_db;

-- ---------------------------------------------------------------------
-- 1. BẢNG TÀI KHOẢN (tai_khoan)
-- Lưu tài khoản đăng nhập hệ thống, mật khẩu mã hóa BCrypt
-- ---------------------------------------------------------------------
CREATE TABLE tai_khoan (
    ten_dang_nhap VARCHAR(50) NOT NULL PRIMARY KEY,
    mat_khau_hash VARCHAR(255) NOT NULL,
    vai_tro VARCHAR(20) NOT NULL, -- 'ADMIN', 'MANAGER', 'LE_TAN'
    trang_thai BOOLEAN NOT NULL DEFAULT TRUE,
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 2. BẢNG NHÂN VIÊN (nhan_vien)
-- Lưu thông tin nhân viên, thể hiện kế thừa Quản lý & Lễ tân
-- ---------------------------------------------------------------------
CREATE TABLE nhan_vien (
    ma_nhan_vien VARCHAR(20) NOT NULL PRIMARY KEY,
    ten_dang_nhap VARCHAR(50) UNIQUE,
    ho_ten VARCHAR(100) NOT NULL,
    so_dien_thoai VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    chuc_vu VARCHAR(20) NOT NULL, -- 'QUAN_LY', 'LE_TAN'
    ngay_vao_lam DATE NOT NULL,
    luong_co_ban DECIMAL(15, 2) NOT NULL,
    so_ca_truc INT DEFAULT 0, -- Dành cho Lễ tân
    thuong_ca DECIMAL(15, 2) DEFAULT 0.00, -- Dành cho Lễ tân
    he_so_trach_nhiem DECIMAL(4, 2) DEFAULT 1.00, -- Dành cho Quản lý
    thuong_hieu_qua DECIMAL(15, 2) DEFAULT 0.00, -- Dành cho Quản lý
    dang_hoat_dong BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_nhanvien_taikhoan FOREIGN KEY (ten_dang_nhap) 
        REFERENCES tai_khoan (ten_dang_nhap) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 3. BẢNG KHÁCH HÀNG (khach_hang)
-- ---------------------------------------------------------------------
CREATE TABLE khach_hang (
    ma_khach_hang VARCHAR(20) NOT NULL PRIMARY KEY,
    ho_ten VARCHAR(100) NOT NULL,
    so_dien_thoai VARCHAR(15) NOT NULL UNIQUE,
    email VARCHAR(100),
    so_cccd VARCHAR(20) NOT NULL UNIQUE,
    ngay_dang_ky DATE NOT NULL
) ENGINE=InnoDB;

CREATE INDEX idx_khachhang_sdt ON khach_hang (so_dien_thoai);
CREATE INDEX idx_khachhang_cccd ON khach_hang (so_cccd);

-- ---------------------------------------------------------------------
-- 4. BẢNG PHÒNG (phong)
-- Thể hiện kế thừa Phòng Thường và Phòng VIP
-- ---------------------------------------------------------------------
CREATE TABLE phong (
    ma_phong VARCHAR(20) NOT NULL PRIMARY KEY,
    so_phong VARCHAR(10) NOT NULL UNIQUE,
    tang INT NOT NULL,
    loai_phong VARCHAR(30) NOT NULL, -- 'PHONG_DON_THUONG', 'PHONG_DON_VIP', 'PHONG_DOI_THUONG', 'PHONG_DOI_VIP'
    loai_giuong VARCHAR(20) NOT NULL DEFAULT 'DON', -- 'DON', 'DOI'
    suc_chua_toi_da INT NOT NULL DEFAULT 2, -- 2 cho phòng đơn, 4 cho phòng đôi
    don_gia_ngay DECIMAL(15, 2) NOT NULL,
    trang_thai_hien_tai VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', 
    -- 5 trạng thái chuẩn: 'AVAILABLE', 'OCCUPIED', 'RESERVED', 'CLEANING', 'MAINTENANCE'
    co_cua_so BOOLEAN DEFAULT TRUE, -- Đặc trưng phòng thường
    co_ban_cong_view_bien BOOLEAN DEFAULT FALSE, -- Đặc trưng phòng VIP
    phi_dich_vu_vip DECIMAL(15, 2) DEFAULT 0.00, -- Phụ phí phòng VIP
    dang_kinh_doanh BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE INDEX idx_phong_trangthai ON phong (trang_thai_hien_tai);
CREATE INDEX idx_phong_loai ON phong (loai_phong);

-- ---------------------------------------------------------------------
-- 5. BẢNG ĐẶT PHÒNG (dat_phong)
-- Liên kết Khách hàng - Phòng - Nhân viên
-- ---------------------------------------------------------------------
CREATE TABLE dat_phong (
    ma_dat_phong VARCHAR(20) NOT NULL PRIMARY KEY,
    ma_khach_hang VARCHAR(20) NOT NULL,
    ma_phong VARCHAR(20) NOT NULL,
    ma_nhan_vien VARCHAR(20) NOT NULL,
    ngay_nhan_phong DATETIME NOT NULL,
    ngay_tra_phong DATETIME NOT NULL,
    thoi_gian_nhan_thuc_te DATETIME NULL,
    thoi_gian_tra_thuc_te DATETIME NULL,
    so_luong_khach INT NOT NULL DEFAULT 1,
    trang_thai VARCHAR(20) NOT NULL DEFAULT 'DA_XAC_NHAN',
    -- 'CHO_XAC_NHAN', 'DA_XAC_NHAN', 'DANG_O', 'DA_TRA_PHONG', 'DA_HUY'
    tien_coc DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    ma_nv_checkin VARCHAR(20) NULL,
    ma_nv_checkout VARCHAR(20) NULL,
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_datphong_khachhang FOREIGN KEY (ma_khach_hang) 
        REFERENCES khach_hang (ma_khach_hang) ON DELETE RESTRICT,
    CONSTRAINT fk_datphong_phong FOREIGN KEY (ma_phong) 
        REFERENCES phong (ma_phong) ON DELETE RESTRICT,
    CONSTRAINT fk_datphong_nhanvien FOREIGN KEY (ma_nhan_vien) 
        REFERENCES nhan_vien (ma_nhan_vien) ON DELETE RESTRICT,
    CONSTRAINT fk_dp_nv_ci FOREIGN KEY (ma_nv_checkin) 
        REFERENCES nhan_vien (ma_nhan_vien) ON UPDATE CASCADE ON DELETE SET NULL,
    CONSTRAINT fk_dp_nv_co FOREIGN KEY (ma_nv_checkout) 
        REFERENCES nhan_vien (ma_nhan_vien) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

-- Index tối ưu thuật toán kiểm tra trùng lịch phòng
CREATE INDEX idx_datphong_lich ON dat_phong (ma_phong, ngay_nhan_phong, ngay_tra_phong, trang_thai);

-- ---------------------------------------------------------------------
-- 5.1. BẢNG CHI TIẾT KHÁCH LƯU TRÚ (chi_tiet_khach_dat_phong)
-- Bắt buộc ghi nhận CCCD của tất cả khách lưu trú trong đơn đặt phòng
-- ---------------------------------------------------------------------
CREATE TABLE chi_tiet_khach_dat_phong (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma_dat_phong VARCHAR(20) NOT NULL,
    ho_ten VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    so_cccd VARCHAR(12) NOT NULL,
    so_dien_thoai VARCHAR(20) NULL,
    vai_tro VARCHAR(30) NOT NULL DEFAULT 'KHACH_DI_CUNG', -- 'NGUOI_DAI_DIEN', 'KHACH_DI_CUNG'
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_khach_dat_phong FOREIGN KEY (ma_dat_phong) 
        REFERENCES dat_phong (ma_dat_phong) ON DELETE CASCADE,
    CONSTRAINT uq_dat_phong_cccd UNIQUE (ma_dat_phong, so_cccd),
    INDEX idx_khach_cccd (so_cccd),
    INDEX idx_khach_dat_phong (ma_dat_phong)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 6. BẢNG DỊCH VỤ (dich_vu)
-- ---------------------------------------------------------------------
CREATE TABLE dich_vu (
    ma_dich_vu VARCHAR(20) NOT NULL PRIMARY KEY,
    ten_dich_vu VARCHAR(100) NOT NULL,
    don_gia_hien_tai DECIMAL(15, 2) NOT NULL,
    dang_kinh_doanh BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 7. BẢNG HÓA ĐƠN (hoa_don)
-- Gắn liền với phiếu đặt phòng khi thanh toán
-- ---------------------------------------------------------------------
CREATE TABLE hoa_don (
    ma_hoa_don VARCHAR(20) NOT NULL PRIMARY KEY,
    ma_dat_phong VARCHAR(20) NOT NULL UNIQUE,
    ma_nhan_vien_lap VARCHAR(20) NOT NULL,
    ngay_lap DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tien_phong DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    tien_dich_vu DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    tien_giam_gia DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    tien_coc DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    tong_tien DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    trang_thai VARCHAR(20) NOT NULL DEFAULT 'CHUA_THANH_TOAN', 
    -- 'CHUA_THANH_TOAN', 'DA_THANH_TOAN'
    CONSTRAINT fk_hoadon_datphong FOREIGN KEY (ma_dat_phong) 
        REFERENCES dat_phong (ma_dat_phong) ON DELETE RESTRICT,
    CONSTRAINT fk_hoadon_nhanvien FOREIGN KEY (ma_nhan_vien_lap) 
        REFERENCES nhan_vien (ma_nhan_vien) ON DELETE RESTRICT
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- 8. BẢNG CHI TIẾT DỊCH VỤ (chi_tiet_dich_vu)
-- Dịch vụ gắn theo phiếu đặt phòng; lưu đơn giá tại thời điểm dùng
-- ---------------------------------------------------------------------
CREATE TABLE chi_tiet_dich_vu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma_dat_phong VARCHAR(20) NOT NULL,
    ma_dich_vu VARCHAR(20) NOT NULL,
    so_luong INT NOT NULL DEFAULT 1,
    don_gia_luc_dung DECIMAL(15, 2) NOT NULL,
    thoi_gian_su_dung DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ctdv_datphong FOREIGN KEY (ma_dat_phong) 
        REFERENCES dat_phong (ma_dat_phong) ON DELETE CASCADE,
    CONSTRAINT fk_ctdv_dichvu FOREIGN KEY (ma_dich_vu) 
        REFERENCES dich_vu (ma_dich_vu) ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE INDEX idx_ctdv_datphong ON chi_tiet_dich_vu (ma_dat_phong);

-- ---------------------------------------------------------------------
-- 9. BẢNG THANH TOÁN (thanh_toan)
-- Ghi nhận lịch sử giao dịch tiền mặt hoặc chuyển khoản
-- ---------------------------------------------------------------------
CREATE TABLE thanh_toan (
    ma_thanh_toan VARCHAR(20) NOT NULL PRIMARY KEY,
    ma_hoa_don VARCHAR(20) NOT NULL,
    so_tien DECIMAL(15, 2) NOT NULL,
    phuong_thuc VARCHAR(30) NOT NULL, -- 'TIEN_MAT', 'CHUYEN_KHOAN'
    thoi_gian_giao_dich DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ma_nhan_vien VARCHAR(20) NULL,
    ghi_chu VARCHAR(255),
    CONSTRAINT fk_thanhtoan_hoadon FOREIGN KEY (ma_hoa_don) 
        REFERENCES hoa_don (ma_hoa_don) ON DELETE RESTRICT,
    CONSTRAINT fk_tt_nv FOREIGN KEY (ma_nhan_vien) 
        REFERENCES nhan_vien (ma_nhan_vien) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE INDEX idx_thanhtoan_hoadon ON thanh_toan (ma_hoa_don);

-- ---------------------------------------------------------------------
-- 11. BẢNG TRÌNH TỰ SINH MÃ ĐỊNH DANH (id_sequence)
-- Quản lý chuỗi số tăng dần nguyên tử qua LAST_INSERT_ID, chống trùng lặp đa luồng
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS id_sequence (
    prefix VARCHAR(10) NOT NULL PRIMARY KEY,
    `last_value` BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

INSERT INTO id_sequence (prefix, `last_value`) VALUES 
('DP', 3),
('HD', 0),
('TT', 0),
('KH', 3),
('DV', 7),
('NV', 4)
ON DUPLICATE KEY UPDATE prefix = prefix;

-- =====================================================================
-- DỮ LIỆU MẪU BAN ĐẦU ĐỂ KIỂM THỬ NGHIỆP VỤ (SEED DATA)
-- Mật khẩu mặc định:
--   - Tài khoản 'admin': mật khẩu là 'admin123'
--   - Tài khoản 'letan01': mật khẩu là 'letan123'
--   - Tài khoản 'letan02': mật khẩu là 'letan123'
-- Chuỗi băm BCrypt tương ứng:
--   'admin123' -> '$2a$10$tZ8QdY2/qKz0KjV0N62Z0u787c88b0uYd3hFvFjK5Q1sB9zK3R4.W' (sẽ dùng BCrypt thực tế)
-- =====================================================================

-- 1. Tài khoản mẫu (Mật khẩu 'admin123', 'manager123' và 'letan123')
INSERT INTO tai_khoan (ten_dang_nhap, mat_khau_hash, vai_tro, trang_thai) VALUES
('admin', '$2a$10$Cf32SSS94kFugKgQvaOtP.ZrMYZ/B1MlmVa7XreRzdiaF77IHeb6i', 'ADMIN', TRUE),
('manager', '$2a$10$Cf32SSS94kFugKgQvaOtP.ZrMYZ/B1MlmVa7XreRzdiaF77IHeb6i', 'MANAGER', TRUE),
('letan01', '$2a$10$PkwUrV1LdJv8W7bb/DK2YusEc5LIMhpdfldhfqIR0MF0ZXGl0.I4G', 'LE_TAN', TRUE),
('letan02', '$2a$10$PkwUrV1LdJv8W7bb/DK2YusEc5LIMhpdfldhfqIR0MF0ZXGl0.I4G', 'LE_TAN', TRUE);

-- 2. Nhân viên mẫu (Quản trị, Quản lý và Lễ tân)
INSERT INTO nhan_vien (ma_nhan_vien, ten_dang_nhap, ho_ten, so_dien_thoai, email, chuc_vu, ngay_vao_lam, luong_co_ban, so_ca_truc, thuong_ca, he_so_trach_nhiem, thuong_hieu_qua, dang_hoat_dong) VALUES
('NV001', 'admin', 'Nguyễn Văn Quản Lý', '0901234567', 'quanly@hotel.com', 'QUAN_LY', '2023-01-15', 18000000.00, 0, 0.00, 1.50, 3000000.00, TRUE),
('NV002', 'letan01', 'Trần Thị Thảo Lễ Tân', '0912345678', 'thaotan@hotel.com', 'LE_TAN', '2023-06-01', 7000000.00, 26, 150000.00, 1.00, 0.00, TRUE),
('NV003', 'letan02', 'Lê Hoàng Nam Lễ Tân', '0923456789', 'namtan@hotel.com', 'LE_TAN', '2023-09-10', 7000000.00, 24, 150000.00, 1.00, 0.00, TRUE),
('NV004', 'manager', 'Đỗ Thanh Tùng Điều Hành', '0934567890', 'tungmanager@hotel.com', 'QUAN_LY', '2023-03-01', 14000000.00, 0, 0.00, 1.30, 2000000.00, TRUE);

-- 3. Khách hàng mẫu
INSERT INTO khach_hang (ma_khach_hang, ho_ten, so_dien_thoai, email, so_cccd, ngay_dang_ky) VALUES
('KH001', 'Phạm Minh Tuấn', '0987654321', 'tuanpm@gmail.com', '001200001234', '2024-01-10'),
('KH002', 'Vũ Bích Phương', '0978123456', 'phuongvb@gmail.com', '034200005678', '2024-02-14'),
('KH003', 'Đặng Quốc Anh', '0965987654', 'anhdq@gmail.com', '048200009999', '2024-03-20');

-- 4. Phòng mẫu (Đúng 4 cấu hình chuẩn với đủ các trạng thái vận hành)
INSERT INTO phong (ma_phong, so_phong, tang, loai_phong, loai_giuong, suc_chua_toi_da, don_gia_ngay, trang_thai_hien_tai, co_cua_so, co_ban_cong_view_bien, phi_dich_vu_vip, dang_kinh_doanh) VALUES
('P101', '101', 1, 'PHONG_DON_THUONG', 'DON', 2, 450000.00, 'AVAILABLE', TRUE, FALSE, 0.00, TRUE),
('P102', '102', 1, 'PHONG_DON_THUONG', 'DON', 2, 450000.00, 'AVAILABLE', TRUE, FALSE, 0.00, TRUE),
('P103', '103', 1, 'PHONG_DOI_THUONG', 'DOI', 4, 600000.00, 'CLEANING', FALSE, FALSE, 0.00, TRUE),
('P201', '201', 2, 'PHONG_DOI_THUONG', 'DOI', 4, 650000.00, 'OCCUPIED', TRUE, FALSE, 0.00, TRUE),
('P202', '202', 2, 'PHONG_DOI_THUONG', 'DOI', 4, 650000.00, 'RESERVED', TRUE, FALSE, 0.00, TRUE),
('P301', '301', 3, 'PHONG_DON_VIP', 'DON', 2, 1200000.00, 'AVAILABLE', TRUE, TRUE, 200000.00, TRUE),
('P302', '302', 3, 'PHONG_DOI_VIP', 'DOI', 4, 1800000.00, 'OCCUPIED', TRUE, TRUE, 300000.00, TRUE),
('P303', '303', 3, 'PHONG_DOI_VIP', 'DOI', 4, 1800000.00, 'MAINTENANCE', TRUE, FALSE, 150000.00, TRUE);

-- 5. Dịch vụ mẫu
INSERT INTO dich_vu (ma_dich_vu, ten_dich_vu, don_gia_hien_tai, dang_kinh_doanh) VALUES
('DV001', 'Nước suối Aquafina 500ml', 15000.00, TRUE),
('DV002', 'Nước ngọt Coca Cola / Pepsi', 20000.00, TRUE),
('DV003', 'Bia Heineken lon', 35000.00, TRUE),
('DV004', 'Mì ly tôm chua cay', 25000.00, TRUE),
('DV005', 'Giặt ủi quần áo (kg)', 40000.00, TRUE),
('DV006', 'Dịch vụ ăn sáng tại phòng', 90000.00, TRUE),
('DV007', 'Thuê xe máy tay ga (ngày)', 150000.00, TRUE);

-- 6. Đơn đặt phòng mẫu (Đồng bộ chuẩn PMS: P202 'RESERVED', P201 'OCCUPIED', P302 'OCCUPIED')
INSERT INTO dat_phong (ma_dat_phong, ma_khach_hang, ma_phong, ma_nhan_vien, ngay_nhan_phong, ngay_tra_phong, thoi_gian_nhan_thuc_te, thoi_gian_tra_thuc_te, so_luong_khach, trang_thai, tien_coc, ma_nv_checkin, ma_nv_checkout, ngay_tao) VALUES
('DP001', 'KH001', 'P202', 'NV002', DATE_ADD(CURDATE(), INTERVAL 14 HOUR), DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 2 DAY), INTERVAL 12 HOUR), NULL, NULL, 2, 'DA_XAC_NHAN', 500000.00, NULL, NULL, NOW()),
('DP002', 'KH002', 'P201', 'NV002', DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 1 DAY), INTERVAL 14 HOUR), DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 1 DAY), INTERVAL 12 HOUR), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 1 DAY), INTERVAL 14 HOUR), NULL, 2, 'DANG_O', 650000.00, 'NV002', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
('DP003', 'KH003', 'P302', 'NV003', DATE_ADD(CURDATE(), INTERVAL 10 HOUR), DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 3 DAY), INTERVAL 12 HOUR), DATE_ADD(CURDATE(), INTERVAL 10 HOUR), NULL, 2, 'DANG_O', 1800000.00, 'NV003', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- 7. Chi tiết khách lưu trú mẫu (CCCD & Khách đi cùng)
INSERT INTO chi_tiet_khach_dat_phong (ma_dat_phong, ho_ten, so_cccd, so_dien_thoai, vai_tro) VALUES
('DP001', 'Phạm Minh Tuấn', '001200001234', '0987654321', 'NGUOI_DAI_DIEN'),
('DP001', 'Lê Thu Hà', '001200005555', '0981112233', 'KHACH_DI_CUNG'),
('DP002', 'Vũ Bích Phương', '034200005678', '0978123456', 'NGUOI_DAI_DIEN'),
('DP002', 'Nguyễn Thu Trang', '034200007788', '0979998877', 'KHACH_DI_CUNG'),
('DP003', 'Đặng Quốc Anh', '048200009999', '0965987654', 'NGUOI_DAI_DIEN'),
('DP003', 'Hoàng Minh Châu', '048200008888', '0967776655', 'KHACH_DI_CUNG');

-- 8. Chi tiết dịch vụ đã sử dụng trong kỳ lưu trú
INSERT INTO chi_tiet_dich_vu (ma_dat_phong, ma_dich_vu, so_luong, don_gia_luc_dung, thoi_gian_su_dung) VALUES
('DP002', 'DV001', 2, 15000.00, NOW()),
('DP002', 'DV003', 2, 35000.00, NOW()),
('DP003', 'DV006', 2, 90000.00, NOW());
