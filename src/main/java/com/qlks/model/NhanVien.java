package com.qlks.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lớp trừu tượng đại diện cho Nhân viên khách sạn.
 * Thể hiện tính KẾ THỪA (từ NguoiDung) và ĐA HÌNH (phương thức trừu tượng tinhLuong()).
 */
public abstract class NhanVien extends NguoiDung {
    private static final long serialVersionUID = 1L;

    private String tenDangNhap; // Khóa ngoại liên kết với TaiKhoan

    @NotNull(message = "Ngày vào làm không được để trống")
    private LocalDate ngayVaoLam;

    @NotNull(message = "Lương cơ bản không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Lương cơ bản phải lớn hơn 0")
    private BigDecimal luongCoBan;

    private boolean dangHoatDong;

    public NhanVien() {
        super();
        this.dangHoatDong = true;
        this.luongCoBan = BigDecimal.ZERO;
    }

    public NhanVien(String maNhanVien, String hoTen, String soDienThoai, String email,
                    String tenDangNhap, LocalDate ngayVaoLam, BigDecimal luongCoBan, boolean dangHoatDong) {
        super(maNhanVien, hoTen, soDienThoai, email);
        this.tenDangNhap = tenDangNhap;
        this.ngayVaoLam = ngayVaoLam;
        this.luongCoBan = luongCoBan != null ? luongCoBan : BigDecimal.ZERO;
        this.dangHoatDong = dangHoatDong;
    }

    /**
     * Phương thức trừu tượng tính tiền lương thực nhận của nhân viên.
     * Mỗi vị trí (Lễ tân, Quản lý) sẽ có công thức tính lương riêng biệt (Đa hình).
     *
     * @return Tiền lương thực nhận (BigDecimal)
     */
    public abstract BigDecimal tinhLuong();

    /**
     * Lấy chức vụ dạng chuỗi hiển thị.
     */
    public abstract String getChucVu();

    public String getMaNhanVien() {
        return getMaDinhDanh();
    }

    public void setMaNhanVien(String maNhanVien) {
        setMaDinhDanh(maNhanVien);
    }

    public String getTenDangNhap() {
        return tenDangNhap;
    }

    public void setTenDangNhap(String tenDangNhap) {
        this.tenDangNhap = tenDangNhap;
    }

    public LocalDate getNgayVaoLam() {
        return ngayVaoLam;
    }

    public void setNgayVaoLam(LocalDate ngayVaoLam) {
        this.ngayVaoLam = ngayVaoLam;
    }

    public BigDecimal getLuongCoBan() {
        return luongCoBan;
    }

    public void setLuongCoBan(BigDecimal luongCoBan) {
        this.luongCoBan = luongCoBan;
    }

    public boolean isDangHoatDong() {
        return dangHoatDong;
    }

    public void setDangHoatDong(boolean dangHoatDong) {
        this.dangHoatDong = dangHoatDong;
    }
}
