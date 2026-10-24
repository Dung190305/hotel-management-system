package com.qlks.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lớp đại diện cho Nhân viên Lễ tân.
 * Kế thừa từ NhanVien và hiện thực phương thức tinhLuong() theo ca trực.
 */
public class LeTan extends NhanVien {
    private static final long serialVersionUID = 1L;

    @Min(value = 0, message = "Số ca trực không được âm")
    private int soCaTruc;

    @NotNull(message = "Tiền thưởng mỗi ca không được để trống")
    @PositiveOrZero(message = "Tiền thưởng mỗi ca không được âm")
    private BigDecimal thuongCa;

    public LeTan() {
        super();
        this.soCaTruc = 0;
        this.thuongCa = BigDecimal.ZERO;
    }

    public LeTan(String maNhanVien, String hoTen, String soDienThoai, String email,
                 String tenDangNhap, LocalDate ngayVaoLam, BigDecimal luongCoBan, boolean dangHoatDong,
                 int soCaTruc, BigDecimal thuongCa) {
        super(maNhanVien, hoTen, soDienThoai, email, tenDangNhap, ngayVaoLam, luongCoBan, dangHoatDong);
        this.soCaTruc = soCaTruc;
        this.thuongCa = thuongCa != null ? thuongCa : BigDecimal.ZERO;
    }

    /**
     * Đa hình: Lương lễ tân = Lương cơ bản + (Số ca trực * Thưởng mỗi ca)
     */
    @Override
    public BigDecimal tinhLuong() {
        BigDecimal tongThuongCa = thuongCa.multiply(BigDecimal.valueOf(soCaTruc));
        return getLuongCoBan().add(tongThuongCa);
    }

    @Override
    public String getChucVu() {
        return "Lễ tân";
    }

    @Override
    public String getThongTinChiTiet() {
        return String.format("Lễ tân: %s | SĐT: %s | Ca trực: %d | Lương thực nhận: %s VNĐ",
                getHoTen(), getSoDienThoai(), soCaTruc, tinhLuong().toPlainString());
    }

    public int getSoCaTruc() {
        return soCaTruc;
    }

    public void setSoCaTruc(int soCaTruc) {
        this.soCaTruc = soCaTruc;
    }

    public BigDecimal getThuongCa() {
        return thuongCa;
    }

    public void setThuongCa(BigDecimal thuongCa) {
        this.thuongCa = thuongCa;
    }
}
