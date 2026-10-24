package com.qlks.model;

import com.qlks.model.enums.VaiTro;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.mindrot.jbcrypt.BCrypt;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Lớp đại diện cho Tài khoản đăng nhập hệ thống.
 * Mật khẩu luôn được lưu dưới dạng băm BCrypt.
 */
public class TaiKhoan implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Tên đăng nhập không được để trống")
    @Size(min = 3, max = 50, message = "Tên đăng nhập phải từ 3 đến 50 ký tự")
    private String tenDangNhap;

    @NotBlank(message = "Mật khẩu không được để trống")
    private String matKhauHash;

    @NotNull(message = "Vai trò không được để trống")
    private VaiTro vaiTro;

    private boolean trangThai;
    private LocalDateTime ngayTao;

    public TaiKhoan() {
        this.trangThai = true;
        this.ngayTao = LocalDateTime.now();
    }

    public TaiKhoan(String tenDangNhap, String matKhauHash, VaiTro vaiTro, boolean trangThai) {
        this.tenDangNhap = tenDangNhap;
        this.matKhauHash = matKhauHash;
        this.vaiTro = vaiTro;
        this.trangThai = trangThai;
        this.ngayTao = LocalDateTime.now();
    }

    public TaiKhoan(String tenDangNhap, String matKhauHash, VaiTro vaiTro, boolean trangThai, LocalDateTime ngayTao) {
        this.tenDangNhap = tenDangNhap;
        this.matKhauHash = matKhauHash;
        this.vaiTro = vaiTro;
        this.trangThai = trangThai;
        this.ngayTao = ngayTao;
    }

    /**
     * Phương thức kiểm tra mật khẩu nhập vào so với mật khẩu đã băm.
     *
     * @param rawPassword Mật khẩu người dùng nhập vào ô Password
     * @return true nếu khớp, false nếu sai
     */
    public boolean kiemTraMatKhau(String rawPassword) {
        if (rawPassword == null || matKhauHash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, matKhauHash);
        } catch (Exception e) {
            return false;
        }
    }

    public String getTenDangNhap() {
        return tenDangNhap;
    }

    public void setTenDangNhap(String tenDangNhap) {
        this.tenDangNhap = tenDangNhap;
    }

    public String getMatKhauHash() {
        return matKhauHash;
    }

    public void setMatKhauHash(String matKhauHash) {
        this.matKhauHash = matKhauHash;
    }

    public VaiTro getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(VaiTro vaiTro) {
        this.vaiTro = vaiTro;
    }

    public boolean isTrangThai() {
        return trangThai;
    }

    public void setTrangThai(boolean trangThai) {
        this.trangThai = trangThai;
    }

    public LocalDateTime getNgayTao() {
        return ngayTao;
    }

    public void setNgayTao(LocalDateTime ngayTao) {
        this.ngayTao = ngayTao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaiKhoan taiKhoan = (TaiKhoan) o;
        return Objects.equals(tenDangNhap, taiKhoan.tenDangNhap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenDangNhap);
    }

    @Override
    public String toString() {
        return tenDangNhap + " (" + (vaiTro != null ? vaiTro.getTenHienThi() : "") + ")";
    }
}
