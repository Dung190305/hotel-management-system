package com.qlks.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lớp đại diện cho Quản lý khách sạn.
 * Kế thừa từ NhanVien và hiện thực phương thức tinhLuong() theo hệ số trách nhiệm và thưởng hiệu quả.
 */
public class QuanLy extends NhanVien {
    private static final long serialVersionUID = 1L;

    @DecimalMin(value = "1.0", message = "Hệ số trách nhiệm của quản lý tối thiểu là 1.0")
    private double heSoTrachNhiem;

    @NotNull(message = "Thưởng hiệu quả không được để trống")
    @PositiveOrZero(message = "Thưởng hiệu quả không được âm")
    private BigDecimal thuongHieuQua;

    public QuanLy() {
        super();
        this.heSoTrachNhiem = 1.0;
        this.thuongHieuQua = BigDecimal.ZERO;
    }

    public QuanLy(String maNhanVien, String hoTen, String soDienThoai, String email,
                  String tenDangNhap, LocalDate ngayVaoLam, BigDecimal luongCoBan, boolean dangHoatDong,
                  double heSoTrachNhiem, BigDecimal thuongHieuQua) {
        super(maNhanVien, hoTen, soDienThoai, email, tenDangNhap, ngayVaoLam, luongCoBan, dangHoatDong);
        this.heSoTrachNhiem = heSoTrachNhiem;
        this.thuongHieuQua = thuongHieuQua != null ? thuongHieuQua : BigDecimal.ZERO;
    }

    /**
     * Đa hình: Lương quản lý = (Lương cơ bản * Hệ số trách nhiệm) + Thưởng hiệu quả
     */
    @Override
    public BigDecimal tinhLuong() {
        BigDecimal luongHeSo = getLuongCoBan().multiply(BigDecimal.valueOf(heSoTrachNhiem));
        return luongHeSo.add(thuongHieuQua);
    }

    @Override
    public String getChucVu() {
        return "Quản lý";
    }

    @Override
    public String getThongTinChiTiet() {
        return String.format("Quản lý: %s | SĐT: %s | Hệ số: %.2f | Lương thực nhận: %s VNĐ",
                getHoTen(), getSoDienThoai(), heSoTrachNhiem, tinhLuong().toPlainString());
    }

    public double getHeSoTrachNhiem() {
        return heSoTrachNhiem;
    }

    public void setHeSoTrachNhiem(double heSoTrachNhiem) {
        this.heSoTrachNhiem = heSoTrachNhiem;
    }

    public BigDecimal getThuongHieuQua() {
        return thuongHieuQua;
    }

    public void setThuongHieuQua(BigDecimal thuongHieuQua) {
        this.thuongHieuQua = thuongHieuQua;
    }
}
