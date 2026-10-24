package com.qlks.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * Lớp đại diện cho khách hàng lưu trú tại khách sạn.
 * Kế thừa từ NguoiDung.
 */
public class KhachHang extends NguoiDung {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Số CCCD/Hộ chiếu không được để trống")
    @Pattern(regexp = "^[0-9]{9,12}$", message = "Số CCCD phải gồm 9 đến 12 chữ số")
    private String soCCCD;

    @NotNull(message = "Ngày đăng ký không được để trống")
    private LocalDate ngayDangKy;

    public KhachHang() {
        super();
        this.ngayDangKy = LocalDate.now();
    }

    public KhachHang(String maKhachHang, String hoTen, String soDienThoai, String email, String soCCCD, LocalDate ngayDangKy) {
        super(maKhachHang, hoTen, soDienThoai, email);
        this.soCCCD = soCCCD;
        this.ngayDangKy = ngayDangKy != null ? ngayDangKy : LocalDate.now();
    }

    public String getMaKhachHang() {
        return getMaDinhDanh();
    }

    public void setMaKhachHang(String maKhachHang) {
        setMaDinhDanh(maKhachHang);
    }

    public String getSoCCCD() {
        return soCCCD;
    }

    public void setSoCCCD(String soCCCD) {
        this.soCCCD = soCCCD;
    }

    public LocalDate getNgayDangKy() {
        return ngayDangKy;
    }

    public void setNgayDangKy(LocalDate ngayDangKy) {
        this.ngayDangKy = ngayDangKy;
    }

    @Override
    public String getThongTinChiTiet() {
        return String.format("Khách hàng: %s | CCCD: %s | SĐT: %s | Ngày ĐK: %s",
                getHoTen(), soCCCD, getSoDienThoai(), ngayDangKy);
    }
}
