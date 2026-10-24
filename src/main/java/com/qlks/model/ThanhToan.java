package com.qlks.model;

import com.qlks.model.enums.PhuongThucThanhToan;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Lớp đại diện cho Giao dịch thanh toán hóa đơn.
 */
public class ThanhToan implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Mã thanh toán không được để trống")
    private String maThanhToan;

    @NotBlank(message = "Mã hóa đơn không được để trống")
    private String maHoaDon;

    @NotNull(message = "Số tiền thanh toán không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Số tiền thanh toán phải lớn hơn 0")
    private BigDecimal soTien;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PhuongThucThanhToan phuongThuc;

    private LocalDateTime thoiGianGiaoDich;
    private String ghiChu;

    public ThanhToan() {
        this.phuongThuc = PhuongThucThanhToan.TIEN_MAT;
        this.thoiGianGiaoDich = LocalDateTime.now();
        this.soTien = BigDecimal.ZERO;
    }

    public ThanhToan(String maThanhToan, String maHoaDon, BigDecimal soTien,
                     PhuongThucThanhToan phuongThuc, LocalDateTime thoiGianGiaoDich, String ghiChu) {
        this.maThanhToan = maThanhToan;
        this.maHoaDon = maHoaDon;
        this.soTien = soTien != null ? soTien : BigDecimal.ZERO;
        this.phuongThuc = phuongThuc != null ? phuongThuc : PhuongThucThanhToan.TIEN_MAT;
        this.thoiGianGiaoDich = thoiGianGiaoDich != null ? thoiGianGiaoDich : LocalDateTime.now();
        this.ghiChu = ghiChu;
    }

    public String getMaThanhToan() {
        return maThanhToan;
    }

    public void setMaThanhToan(String maThanhToan) {
        this.maThanhToan = maThanhToan;
    }

    public String getMaHoaDon() {
        return maHoaDon;
    }

    public void setMaHoaDon(String maHoaDon) {
        this.maHoaDon = maHoaDon;
    }

    public BigDecimal getSoTien() {
        return soTien;
    }

    public void setSoTien(BigDecimal soTien) {
        this.soTien = soTien;
    }

    public PhuongThucThanhToan getPhuongThuc() {
        return phuongThuc;
    }

    public void setPhuongThuc(PhuongThucThanhToan phuongThuc) {
        this.phuongThuc = phuongThuc;
    }

    public LocalDateTime getThoiGianGiaoDich() {
        return thoiGianGiaoDich;
    }

    public void setThoiGianGiaoDich(LocalDateTime thoiGianGiaoDich) {
        this.thoiGianGiaoDich = thoiGianGiaoDich;
    }

    public String getGhiChu() {
        return ghiChu;
    }

    public void setGhiChu(String ghiChu) {
        this.ghiChu = ghiChu;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ThanhToan thanhToan = (ThanhToan) o;
        return Objects.equals(maThanhToan, thanhToan.maThanhToan);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maThanhToan);
    }

    @Override
    public String toString() {
        return maThanhToan + ": " + soTien.toPlainString() + " VNĐ (" + phuongThuc.getTenHienThi() + ")";
    }
}
