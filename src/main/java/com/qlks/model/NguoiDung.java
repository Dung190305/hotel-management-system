package com.qlks.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.Objects;

/**
 * Lớp trừu tượng đại diện cho một người trong hệ thống (Khách hàng hoặc Nhân viên).
 * Minh họa tính TRỪU TƯỢNG (Abstract class) và ĐÓNG GÓI (Encapsulation).
 */
public abstract class NguoiDung implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Mã định danh không được để trống")
    @Size(max = 20, message = "Mã định danh không quá 20 ký tự")
    private String maDinhDanh;

    @NotBlank(message = "Họ và tên không được để trống")
    @Size(min = 2, max = 100, message = "Họ tên phải từ 2 đến 100 ký tự")
    private String hoTen;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)[3|5|7|8|9][0-9]{8}$", message = "Số điện thoại không đúng định dạng Việt Nam")
    private String soDienThoai;

    @Email(message = "Địa chỉ email không hợp lệ")
    private String email;

    // Default constructor
    public NguoiDung() {
    }

    // Constructor đầy đủ tham số
    public NguoiDung(String maDinhDanh, String hoTen, String soDienThoai, String email) {
        this.maDinhDanh = maDinhDanh;
        this.hoTen = hoTen;
        this.soDienThoai = soDienThoai;
        this.email = email;
    }

    // Phương thức trừu tượng: các lớp con bắt buộc phải ghi đè
    public abstract String getThongTinChiTiet();

    // Getters and Setters có kiểm tra hợp lệ
    public String getMaDinhDanh() {
        return maDinhDanh;
    }

    public void setMaDinhDanh(String maDinhDanh) {
        this.maDinhDanh = maDinhDanh;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NguoiDung nguoiDung = (NguoiDung) o;
        return Objects.equals(maDinhDanh, nguoiDung.maDinhDanh);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maDinhDanh);
    }

    @Override
    public String toString() {
        return hoTen + " (" + soDienThoai + ")";
    }
}
