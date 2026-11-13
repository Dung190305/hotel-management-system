-- =====================================================================
-- HỆ THỐNG QUẢN LÝ KHÁCH SẠN (HOTEL MANAGEMENT SYSTEM)
-- Script Nạp Dữ liệu Mẫu Ban Đầu (Database Seed DML)
-- Yêu cầu: Đã chạy file schema.sql trước khi chạy script này
-- =====================================================================

SET NAMES 'utf8mb4';
SET CHARACTER SET utf8mb4;
SET character_set_connection = 'utf8mb4';

USE hotel_db;

-- ---------------------------------------------------------------------
-- 1. KHỞI TẠO BẢNG TRÌNH TỰ SINH MÃ (id_sequence)
-- ---------------------------------------------------------------------
INSERT INTO id_sequence (prefix, `last_value`) VALUES 
('DP', 3),
('HD', 0),
('TT', 0),
('KH', 3),
('DV', 7),
('NV', 4)
ON DUPLICATE KEY UPDATE prefix = prefix;

-- ---------------------------------------------------------------------
-- 2. TÀI KHOẢN MẪU (Mật khẩu 'admin123', 'manager123' và 'letan123')
-- ---------------------------------------------------------------------
INSERT INTO tai_khoan (ten_dang_nhap, mat_khau_hash, vai_tro, trang_thai) VALUES
('admin', '$2a$10$Cf32SSS94kFugKgQvaOtP.ZrMYZ/B1MlmVa7XreRzdiaF77IHeb6i', 'ADMIN', TRUE),
('manager', '$2a$10$Cf32SSS94kFugKgQvaOtP.ZrMYZ/B1MlmVa7XreRzdiaF77IHeb6i', 'MANAGER', TRUE),
('letan01', '$2a$10$PkwUrV1LdJv8W7bb/DK2YusEc5LIMhpdfldhfqIR0MF0ZXGl0.I4G', 'LE_TAN', TRUE),
('letan02', '$2a$10$PkwUrV1LdJv8W7bb/DK2YusEc5LIMhpdfldhfqIR0MF0ZXGl0.I4G', 'LE_TAN', TRUE);

-- ---------------------------------------------------------------------
-- 3. NHÂN VIÊN MẪU (Quản trị viên, Quản lý điều hành và Lễ tân)
-- ---------------------------------------------------------------------
INSERT INTO nhan_vien (ma_nhan_vien, ten_dang_nhap, ho_ten, so_dien_thoai, email, chuc_vu, ngay_vao_lam, luong_co_ban, so_ca_truc, thuong_ca, he_so_trach_nhiem, thuong_hieu_qua, dang_hoat_dong) VALUES
('NV001', 'admin', 'Nguyễn Văn Quản Lý', '0901234567', 'quanly@hotel.com', 'QUAN_LY', '2023-01-15', 18000000.00, 0, 0.00, 1.50, 3000000.00, TRUE),
('NV002', 'letan01', 'Trần Thị Thảo Lễ Tân', '0912345678', 'thaotan@hotel.com', 'LE_TAN', '2023-06-01', 7000000.00, 26, 150000.00, 1.00, 0.00, TRUE),
('NV003', 'letan02', 'Lê Hoàng Nam Lễ Tân', '0923456789', 'namtan@hotel.com', 'LE_TAN', '2023-09-10', 7000000.00, 24, 150000.00, 1.00, 0.00, TRUE),
('NV004', 'manager', 'Đỗ Thanh Tùng Điều Hành', '0934567890', 'tungmanager@hotel.com', 'QUAN_LY', '2023-03-01', 14000000.00, 0, 0.00, 1.30, 2000000.00, TRUE);

-- ---------------------------------------------------------------------
-- 4. KHÁCH HÀNG MẪU
-- ---------------------------------------------------------------------
INSERT INTO khach_hang (ma_khach_hang, ho_ten, so_dien_thoai, email, so_cccd, ngay_dang_ky) VALUES
('KH001', 'Phạm Minh Tuấn', '0987654321', 'tuanpm@gmail.com', '001200001234', '2024-01-10'),
('KH002', 'Vũ Bích Phương', '0978123456', 'phuongvb@gmail.com', '034200005678', '2024-02-14'),
('KH003', 'Đặng Quốc Anh', '0965987654', 'anhdq@gmail.com', '048200009999', '2024-03-20');

-- ---------------------------------------------------------------------
-- 5. PHÒNG MẪU (Đúng 4 cấu hình phòng chuẩn)
-- ---------------------------------------------------------------------
INSERT INTO phong (ma_phong, so_phong, tang, loai_phong, loai_giuong, suc_chua_toi_da, don_gia_ngay, trang_thai_hien_tai, co_cua_so, co_ban_cong_view_bien, phi_dich_vu_vip, dang_kinh_doanh) VALUES
('P101', '101', 1, 'PHONG_DON_THUONG', 'DON', 2, 450000.00, 'AVAILABLE', TRUE, FALSE, 0.00, TRUE),
('P102', '102', 1, 'PHONG_DON_THUONG', 'DON', 2, 450000.00, 'AVAILABLE', TRUE, FALSE, 0.00, TRUE),
('P103', '103', 1, 'PHONG_DOI_THUONG', 'DOI', 4, 600000.00, 'CLEANING', FALSE, FALSE, 0.00, TRUE),
('P201', '201', 2, 'PHONG_DOI_THUONG', 'DOI', 4, 650000.00, 'OCCUPIED', TRUE, FALSE, 0.00, TRUE),
('P202', '202', 2, 'PHONG_DOI_THUONG', 'DOI', 4, 650000.00, 'RESERVED', TRUE, FALSE, 0.00, TRUE),
('P301', '301', 3, 'PHONG_DON_VIP', 'DON', 2, 1200000.00, 'AVAILABLE', TRUE, TRUE, 200000.00, TRUE),
('P302', '302', 3, 'PHONG_DOI_VIP', 'DOI', 4, 1800000.00, 'OCCUPIED', TRUE, TRUE, 300000.00, TRUE),
('P303', '303', 3, 'PHONG_DOI_VIP', 'DOI', 4, 1800000.00, 'MAINTENANCE', TRUE, FALSE, 150000.00, TRUE);

-- ---------------------------------------------------------------------
-- 6. DANH MỤC DỊCH VỤ MẪU
-- ---------------------------------------------------------------------
INSERT INTO dich_vu (ma_dich_vu, ten_dich_vu, don_gia_hien_tai, dang_kinh_doanh) VALUES
('DV001', 'Nước suối Aquafina 500ml', 15000.00, TRUE),
('DV002', 'Nước ngọt Coca Cola / Pepsi', 20000.00, TRUE),
('DV003', 'Bia Heineken lon', 35000.00, TRUE),
('DV004', 'Mì ly tôm chua cay', 25000.00, TRUE),
('DV005', 'Giặt ủi quần áo (kg)', 40000.00, TRUE),
('DV006', 'Dịch vụ ăn sáng tại phòng', 90000.00, TRUE),
('DV007', 'Thuê xe máy tay ga (ngày)', 150000.00, TRUE);

-- ---------------------------------------------------------------------
-- 7. ĐƠN ĐẶT PHÒNG MẪU (Đồng bộ chuẩn PMS với P202 'RESERVED', P201 'OCCUPIED', P302 'OCCUPIED')
-- ---------------------------------------------------------------------
INSERT INTO dat_phong (ma_dat_phong, ma_khach_hang, ma_phong, ma_nhan_vien, ngay_nhan_phong, ngay_tra_phong, thoi_gian_nhan_thuc_te, thoi_gian_tra_thuc_te, so_luong_khach, trang_thai, tien_coc, ma_nv_checkin, ma_nv_checkout, ngay_tao) VALUES
('DP001', 'KH001', 'P202', 'NV002', DATE_ADD(CURDATE(), INTERVAL 14 HOUR), DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 2 DAY), INTERVAL 12 HOUR), NULL, NULL, 2, 'DA_XAC_NHAN', 500000.00, NULL, NULL, NOW()),
('DP002', 'KH002', 'P201', 'NV002', DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 1 DAY), INTERVAL 14 HOUR), DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 1 DAY), INTERVAL 12 HOUR), DATE_ADD(DATE_SUB(CURDATE(), INTERVAL 1 DAY), INTERVAL 14 HOUR), NULL, 2, 'DANG_O', 650000.00, 'NV002', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),
('DP003', 'KH003', 'P302', 'NV003', DATE_ADD(CURDATE(), INTERVAL 10 HOUR), DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 3 DAY), INTERVAL 12 HOUR), DATE_ADD(CURDATE(), INTERVAL 10 HOUR), NULL, 2, 'DANG_O', 1800000.00, 'NV003', NULL, DATE_SUB(NOW(), INTERVAL 1 DAY));

-- ---------------------------------------------------------------------
-- 8. CHI TIẾT KHÁCH LƯU TRÚ MẪU
-- ---------------------------------------------------------------------
INSERT INTO chi_tiet_khach_dat_phong (ma_dat_phong, ho_ten, so_cccd, so_dien_thoai, vai_tro) VALUES
('DP001', 'Phạm Minh Tuấn', '001200001234', '0987654321', 'NGUOI_DAI_DIEN'),
('DP001', 'Lê Thu Hà', '001200005555', '0981112233', 'KHACH_DI_CUNG'),
('DP002', 'Vũ Bích Phương', '034200005678', '0978123456', 'NGUOI_DAI_DIEN'),
('DP002', 'Nguyễn Thu Trang', '034200007788', '0979998877', 'KHACH_DI_CUNG'),
('DP003', 'Đặng Quốc Anh', '048200009999', '0965987654', 'NGUOI_DAI_DIEN'),
('DP003', 'Hoàng Minh Châu', '048200008888', '0967776655', 'KHACH_DI_CUNG');

-- ---------------------------------------------------------------------
-- 9. CHI TIẾT DỊCH VỤ PHÁT SINH MẪU
-- ---------------------------------------------------------------------
INSERT INTO chi_tiet_dich_vu (ma_dat_phong, ma_dich_vu, so_luong, don_gia_luc_dung, thoi_gian_su_dung) VALUES
('DP002', 'DV001', 2, 15000.00, NOW()),
('DP002', 'DV003', 2, 35000.00, NOW()),
('DP003', 'DV006', 2, 90000.00, NOW());

