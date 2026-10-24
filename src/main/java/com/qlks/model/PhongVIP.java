package com.qlks.model;

import com.qlks.model.enums.TrangThaiPhong;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Lớp đại diện cho Phòng VIP (Deluxe / Suite).
 * Kế thừa từ Phong, có thêm ban công hướng biển và phụ phí dịch vụ VIP kèm đồ uống chào mừng.
 */
public class PhongVIP extends Phong {
    private static final long serialVersionUID = 1L;

    private boolean coBanCongViewBien;

    @NotNull(message = "Phụ phí dịch vụ VIP không được để trống")
    @PositiveOrZero(message = "Phụ phí dịch vụ VIP không được âm")
    private BigDecimal phiDichVuVip;

    public PhongVIP() {
        super();
        this.coBanCongViewBien = true;
        this.phiDichVuVip = BigDecimal.ZERO;
    }

    public PhongVIP(String maPhong, String soPhong, int tang, BigDecimal donGiaNgay,
                    TrangThaiPhong trangThaiHienTai, boolean dangKinhDoanh,
                    boolean coBanCongViewBien, BigDecimal phiDichVuVip) {
        super(maPhong, soPhong, tang, donGiaNgay, trangThaiHienTai, dangKinhDoanh);
        this.coBanCongViewBien = coBanCongViewBien;
        this.phiDichVuVip = phiDichVuVip != null ? phiDichVuVip : BigDecimal.ZERO;
    }

    /**
     * Đa hình: Tính tiền phòng VIP.
     * Tiền phòng = (Đơn giá ngày + Phí dịch vụ VIP) * Số ngày lưu trú.
     * Ưu đãi: Khách ở từ 5 ngày trở lên được giảm giá 10% tổng tiền phòng.
     */
    @Override
    public BigDecimal tinhTienPhong(long soNgay) {
        long soNgayThucTe = Math.max(1, soNgay);
        BigDecimal giaMoiNgay = getDonGiaNgay().add(phiDichVuVip);
        BigDecimal tong = giaMoiNgay.multiply(BigDecimal.valueOf(soNgayThucTe));

        if (soNgayThucTe >= 5) {
            BigDecimal giamGia = tong.multiply(new BigDecimal("0.10"));
            tong = tong.subtract(giamGia);
        }
        return tong.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getLoaiPhong() {
        return "PHONG_VIP";
    }

    @Override
    public String getTenLoaiPhongHienThi() {
        return "Phòng VIP";
    }

    public boolean isCoBanCongViewBien() {
        return coBanCongViewBien;
    }

    public void setCoBanCongViewBien(boolean coBanCongViewBien) {
        this.coBanCongViewBien = coBanCongViewBien;
    }

    public BigDecimal getPhiDichVuVip() {
        return phiDichVuVip;
    }

    public void setPhiDichVuVip(BigDecimal phiDichVuVip) {
        this.phiDichVuVip = phiDichVuVip;
    }
}
