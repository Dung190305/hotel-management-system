package com.qlks.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lớp đại diện cho một lần sử dụng dịch vụ của khách gắn với hóa đơn.
 * Lưu đơn giá tại thời điểm dùng để không bị ảnh hưởng khi bảng giá dịch vụ thay đổi.
 */
public class ChiTietDichVu implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "Mã đặt phòng không được để trống")
    private String maDatPhong;

    @NotNull(message = "Dịch vụ không được để trống")
    private DichVu dichVu;

    @Min(value = 1, message = "Số lượng sử dụng phải từ 1 trở lên")
    private int soLuong;

    @NotNull(message = "Đơn giá lúc dùng không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Đơn giá lúc dùng phải lớn hơn 0")
    private BigDecimal donGiaLucDung;

    private LocalDateTime thoiGianSuDung;

    public ChiTietDichVu() {
        this.soLuong = 1;
        this.donGiaLucDung = BigDecimal.ZERO;
        this.thoiGianSuDung = LocalDateTime.now();
    }

    public ChiTietDichVu(Long id, String maDatPhong, DichVu dichVu, int soLuong,
                         BigDecimal donGiaLucDung, LocalDateTime thoiGianSuDung) {
        this.id = id;
        this.maDatPhong = maDatPhong;
        this.dichVu = dichVu;
        this.soLuong = soLuong;
        this.donGiaLucDung = donGiaLucDung != null ? donGiaLucDung : (dichVu != null ? dichVu.getDonGiaHienTai() : BigDecimal.ZERO);
        this.thoiGianSuDung = thoiGianSuDung != null ? thoiGianSuDung : LocalDateTime.now();
    }

    /**
     * Tính thành tiền = Số lượng * Đơn giá lúc dùng
     */
    public BigDecimal tinhThanhTien() {
        if (donGiaLucDung == null) return BigDecimal.ZERO;
        return donGiaLucDung.multiply(BigDecimal.valueOf(soLuong));
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMaDatPhong() {
        return maDatPhong;
    }

    public void setMaDatPhong(String maDatPhong) {
        this.maDatPhong = maDatPhong;
    }

    /**
     * Tương thích ngược: Mã hóa đơn liên kết theo mã đặt phòng
     */
    public String getMaHoaDon() {
        return maDatPhong;
    }

    public void setMaHoaDon(String maHoaDon) {
        this.maDatPhong = maHoaDon;
    }

    public DichVu getDichVu() {
        return dichVu;
    }

    public String getMaDichVu() {
        return dichVu != null ? dichVu.getMaDichVu() : null;
    }

    public void setDichVu(DichVu dichVu) {
        this.dichVu = dichVu;
        if (dichVu != null && this.donGiaLucDung.compareTo(BigDecimal.ZERO) == 0) {
            this.donGiaLucDung = dichVu.getDonGiaHienTai();
        }
    }

    public int getSoLuong() {
        return soLuong;
    }

    public void setSoLuong(int soLuong) {
        this.soLuong = soLuong;
    }

    public BigDecimal getDonGiaLucDung() {
        return donGiaLucDung;
    }

    public void setDonGiaLucDung(BigDecimal donGiaLucDung) {
        this.donGiaLucDung = donGiaLucDung;
    }

    public LocalDateTime getThoiGianSuDung() {
        return thoiGianSuDung;
    }

    public void setThoiGianSuDung(LocalDateTime thoiGianSuDung) {
        this.thoiGianSuDung = thoiGianSuDung;
    }

    @Override
    public String toString() {
        return (dichVu != null ? dichVu.getTenDichVu() : "Dịch vụ") + " x " + soLuong + " = " + tinhThanhTien().toPlainString() + " VNĐ";
    }
}
