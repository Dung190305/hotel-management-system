-- ============================================================================
-- MIGRATION V2: NÂNG CẤP 4 CẤU HÌNH PHÒNG, CCCD KHÁCH LƯU TRÚ VÀ TIỀN CỌC HÓA ĐƠN
-- Dự án: Hệ thống quản lý khách sạn (OOP Java Swing + MySQL)
-- ============================================================================

SET NAMES 'utf8mb4';
SET CHARACTER SET utf8mb4;

USE hotel_db;

-- 1. Bổ sung các cột cấu hình vào bảng phong (nếu chưa có)
-- loai_giuong: 'DON' (1 giường) hoặc 'DOI' (2 giường)
-- suc_chua_toi_da: 2 (phòng đơn) hoặc 4 (phòng đôi)
-- loai_phong: lưu mã cấu hình PHONG_DON_THUONG, PHONG_DON_VIP, PHONG_DOI_THUONG, PHONG_DOI_VIP

SET @col_loai_giuong := (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'hotel_db' AND TABLE_NAME = 'phong' AND COLUMN_NAME = 'loai_giuong');
SET @sql_add_giuong := IF(@col_loai_giuong = 0, 'ALTER TABLE phong ADD COLUMN loai_giuong VARCHAR(20) NOT NULL DEFAULT ''DON'' AFTER loai_phong', 'SELECT 1');
PREPARE stmt FROM @sql_add_giuong;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_suc_chua := (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'hotel_db' AND TABLE_NAME = 'phong' AND COLUMN_NAME = 'suc_chua_toi_da');
SET @sql_add_succhua := IF(@col_suc_chua = 0, 'ALTER TABLE phong ADD COLUMN suc_chua_toi_da INT NOT NULL DEFAULT 2 AFTER loai_giuong', 'SELECT 1');
PREPARE stmt FROM @sql_add_succhua;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Mở rộng độ dài cột loai_phong lên VARCHAR(30) nếu cần
ALTER TABLE phong MODIFY COLUMN loai_phong VARCHAR(30) NOT NULL;

-- 2. Cập nhật dữ liệu phòng hiện có sang đúng 4 cấu hình chuẩn
-- Phòng đơn thường (1 giường, sức chứa 2)
UPDATE phong 
SET loai_phong = 'PHONG_DON_THUONG', loai_giuong = 'DON', suc_chua_toi_da = 2 
WHERE ma_phong IN ('P101', 'P102');

-- Phòng đôi thường (2 giường, sức chứa 4)
UPDATE phong 
SET loai_phong = 'PHONG_DOI_THUONG', loai_giuong = 'DOI', suc_chua_toi_da = 4 
WHERE ma_phong IN ('P103', 'P201', 'P202');

-- Phòng đơn VIP (1 giường, sức chứa 2)
UPDATE phong 
SET loai_phong = 'PHONG_DON_VIP', loai_giuong = 'DON', suc_chua_toi_da = 2 
WHERE ma_phong IN ('P301', 'P.203');

-- Phòng đôi VIP (2 giường, sức chứa 4)
UPDATE phong 
SET loai_phong = 'PHONG_DOI_VIP', loai_giuong = 'DOI', suc_chua_toi_da = 4 
WHERE ma_phong IN ('P302', 'P303');

-- Cập nhật bất kỳ phòng nào còn sót
UPDATE phong 
SET loai_phong = 'PHONG_DON_THUONG', loai_giuong = 'DON', suc_chua_toi_da = 2
WHERE loai_phong = 'PHONG_THUONG';

UPDATE phong 
SET loai_phong = 'PHONG_DON_VIP', loai_giuong = 'DON', suc_chua_toi_da = 2
WHERE loai_phong = 'PHONG_VIP';

-- 3. Tạo bảng chi_tiet_khach_dat_phong lưu trữ đầy đủ CCCD của tất cả khách lưu trú
CREATE TABLE IF NOT EXISTS chi_tiet_khach_dat_phong (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ma_dat_phong VARCHAR(20) NOT NULL,
    ho_ten VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    so_cccd VARCHAR(12) NOT NULL,
    so_dien_thoai VARCHAR(20) NULL,
    vai_tro VARCHAR(30) NOT NULL DEFAULT 'KHACH_DI_CUNG',
    ngay_tao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_khach_dat_phong FOREIGN KEY (ma_dat_phong) 
        REFERENCES dat_phong(ma_dat_phong) ON DELETE CASCADE,
    CONSTRAINT uq_dat_phong_cccd UNIQUE (ma_dat_phong, so_cccd),
    INDEX idx_khach_cccd (so_cccd),
    INDEX idx_khach_dat_phong (ma_dat_phong)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Di chuyển dữ liệu khách đại diện cũ vào chi_tiet_khach_dat_phong nếu chưa có
INSERT IGNORE INTO chi_tiet_khach_dat_phong (ma_dat_phong, ho_ten, so_cccd, so_dien_thoai, vai_tro)
SELECT dp.ma_dat_phong, kh.ho_ten, kh.so_cccd, kh.so_dien_thoai, 'NGUOI_DAI_DIEN'
FROM dat_phong dp
JOIN khach_hang kh ON dp.ma_khach_hang = kh.ma_khach_hang;

-- 4. Bổ sung cột tien_coc vào bảng hoa_don (nếu chưa có)
SET @col_tien_coc := (SELECT COUNT(*) FROM information_schema.COLUMNS 
    WHERE TABLE_SCHEMA = 'hotel_db' AND TABLE_NAME = 'hoa_don' AND COLUMN_NAME = 'tien_coc');
SET @sql_add_tiencoc := IF(@col_tien_coc = 0, 'ALTER TABLE hoa_don ADD COLUMN tien_coc DECIMAL(15,2) NOT NULL DEFAULT 0.00 AFTER tien_giam_gia', 'SELECT 1');
PREPARE stmt FROM @sql_add_tiencoc;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Đồng bộ tiền cọc từ đơn đặt phòng sang hóa đơn nếu hóa đơn chưa có tiền cọc
UPDATE hoa_don hd
JOIN dat_phong dp ON hd.ma_dat_phong = dp.ma_dat_phong
SET hd.tien_coc = dp.tien_coc
WHERE hd.tien_coc = 0.00 AND dp.tien_coc > 0;
