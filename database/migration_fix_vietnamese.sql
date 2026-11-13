-- =====================================================================
-- SCRIPT MIGRATION: KHẮC PHỤC TRIỆT ĐỂ LỖI DẤU HỎI ? VÀ CHUẨN HÓA DỮ LIỆU
-- Dự án: Quản lý Khách sạn (Hotel Management System)
-- Đảm bảo an toàn: Giữ nguyên dữ liệu hiện có (phòng mới, đặt phòng, hóa đơn)
-- =====================================================================

SET NAMES 'utf8mb4';
SET CHARACTER SET utf8mb4;
SET character_set_connection = 'utf8mb4';

USE hotel_db;

-- 1. Cập nhật lại tên tiếng Việt có dấu cho bảng nhân viên mẫu
UPDATE nhan_vien 
SET ho_ten = 'Nguyễn Văn Quản Lý' 
WHERE ma_nhan_vien = 'NV001';

UPDATE nhan_vien 
SET ho_ten = 'Trần Thị Thảo Lễ Tân' 
WHERE ma_nhan_vien = 'NV002';

UPDATE nhan_vien 
SET ho_ten = 'Lê Hoàng Nam Lễ Tân' 
WHERE ma_nhan_vien = 'NV003';

-- 2. Cập nhật lại tên tiếng Việt có dấu cho bảng khách hàng mẫu
UPDATE khach_hang 
SET ho_ten = 'Phạm Minh Tuấn' 
WHERE ma_khach_hang = 'KH001';

UPDATE khach_hang 
SET ho_ten = 'Vũ Bích Phương' 
WHERE ma_khach_hang = 'KH002';

UPDATE khach_hang 
SET ho_ten = 'Đặng Quốc Anh' 
WHERE ma_khach_hang = 'KH003';

-- 3. Cập nhật lại tên tiếng Việt có dấu cho danh mục dịch vụ mẫu
UPDATE dich_vu 
SET ten_dich_vu = 'Nước suối Aquafina 500ml' 
WHERE ma_dich_vu = 'DV001';

UPDATE dich_vu 
SET ten_dich_vu = 'Nước ngọt Coca Cola / Pepsi' 
WHERE ma_dich_vu = 'DV002';

UPDATE dich_vu 
SET ten_dich_vu = 'Bia Heineken lon' 
WHERE ma_dich_vu = 'DV003';

UPDATE dich_vu 
SET ten_dich_vu = 'Mì ly tôm chua cay' 
WHERE ma_dich_vu = 'DV004';

UPDATE dich_vu 
SET ten_dich_vu = 'Giặt ủi quần áo (kg)' 
WHERE ma_dich_vu = 'DV005';

UPDATE dich_vu 
SET ten_dich_vu = 'Dịch vụ ăn sáng tại phòng' 
WHERE ma_dich_vu = 'DV006';

UPDATE dich_vu 
SET ten_dich_vu = 'Thuê xe máy tay ga (ngày)' 
WHERE ma_dich_vu = 'DV007';

-- 4. Đảm bảo cấu hình bảng phòng luôn lưu loại phòng theo mã chuẩn
UPDATE phong SET loai_phong = 'PHONG_THUONG' WHERE loai_phong IN ('PHONG_THUONG', 'STANDARD', 'Standard');
UPDATE phong SET loai_phong = 'PHONG_VIP' WHERE loai_phong IN ('PHONG_VIP', 'VIP', 'Deluxe');
