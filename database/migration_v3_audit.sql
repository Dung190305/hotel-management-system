-- =====================================================================
-- MIGRATION V3: BỔ SUNG AUDIT TRAIL CHO ĐẶT PHÒNG VÀ THANH TOÁN
-- Ghi nhận mã nhân viên Check-in, Check-out và nhân viên thu tiền
-- =====================================================================
USE hotel_db;

-- 1. Thêm nhân viên check-in và check-out vào bảng dat_phong
ALTER TABLE dat_phong
  ADD COLUMN ma_nv_checkin  VARCHAR(20) NULL,
  ADD COLUMN ma_nv_checkout VARCHAR(20) NULL,
  ADD CONSTRAINT fk_dp_nv_ci FOREIGN KEY (ma_nv_checkin)  REFERENCES nhan_vien(ma_nhan_vien) ON UPDATE CASCADE ON DELETE SET NULL,
  ADD CONSTRAINT fk_dp_nv_co FOREIGN KEY (ma_nv_checkout) REFERENCES nhan_vien(ma_nhan_vien) ON UPDATE CASCADE ON DELETE SET NULL;

-- 2. Thêm nhân viên thực hiện thu tiền vào bảng thanh_toan
ALTER TABLE thanh_toan
  ADD COLUMN ma_nhan_vien VARCHAR(20) NULL,
  ADD CONSTRAINT fk_tt_nv FOREIGN KEY (ma_nhan_vien) REFERENCES nhan_vien(ma_nhan_vien) ON UPDATE CASCADE ON DELETE SET NULL;

