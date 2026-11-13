-- =====================================================================
-- MIGRATION V4: DỊCH VỤ GẮN THEO PHIẾU ĐẶT PHÒNG (BOOKING SERVICE BINDING)
-- Chuyển quan hệ chi_tiet_dich_vu từ hóa đơn sang phiếu đặt phòng
-- =====================================================================
USE hotel_db;

-- 1. Thêm cột ma_dat_phong
ALTER TABLE chi_tiet_dich_vu ADD COLUMN ma_dat_phong VARCHAR(20) NULL;

-- 2. Chuyển đổi dữ liệu cũ từ bảng hoa_don sang dat_phong
UPDATE chi_tiet_dich_vu c 
JOIN hoa_don h ON c.ma_hoa_don = h.ma_hoa_don
SET c.ma_dat_phong = h.ma_dat_phong;

-- 3. Xóa khóa ngoại và cột ma_hoa_don cũ
ALTER TABLE chi_tiet_dich_vu DROP FOREIGN KEY fk_ctdv_hoadon;
ALTER TABLE chi_tiet_dich_vu DROP COLUMN ma_hoa_don;

-- 4. Đặt ràng buộc NOT NULL, FOREIGN KEY và INDEX cho ma_dat_phong
ALTER TABLE chi_tiet_dich_vu
  MODIFY ma_dat_phong VARCHAR(20) NOT NULL,
  ADD CONSTRAINT fk_ctdv_datphong FOREIGN KEY (ma_dat_phong) REFERENCES dat_phong(ma_dat_phong) ON DELETE CASCADE,
  ADD INDEX idx_ctdv_datphong (ma_dat_phong);

