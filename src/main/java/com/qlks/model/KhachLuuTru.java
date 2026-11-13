package com.qlks.model;

import com.qlks.model.enums.VaiTroKhachLuuTru;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Thực thể biểu diễn thông tin khách lưu trú thực tế trong đơn đặt phòng.
 * Bắt buộc ghi nhận CCCD của tất cả khách (người đại diện và khách đi cùng).
 */
public class KhachLuuTru implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String REGEX_CCCD_VN = "^\\d{12}$";

    private Long id;

    @NotBlank(message = "Mã đặt phòng không được để trống")
    private String maDatPhong;

    @NotBlank(message = "Họ tên khách lưu trú không được để trống")
    private String hoTen;

    @NotBlank(message = "Số CCCD không được để trống")
    @Pattern(regexp = REGEX_CCCD_VN, message = "Số CCCD phải gồm đúng 12 chữ số, không chứa chữ cái hay ký tự đặc biệt")
    private String soCCCD;

    private String soDienThoai;

    @NotNull(message = "Vai trò không được để trống")
    private VaiTroKhachLuuTru vaiTro;

    private LocalDateTime ngayTao;

    public KhachLuuTru() {
        this.vaiTro = VaiTroKhachLuuTru.KHACH_DI_CUNG;
        this.ngayTao = LocalDateTime.now();
    }

    public KhachLuuTru(String maDatPhong, String hoTen, String soCCCD, String soDienThoai, VaiTroKhachLuuTru vaiTro) {
        this.maDatPhong = maDatPhong;
        this.hoTen = hoTen != null ? hoTen.trim() : null;
        this.soCCCD = soCCCD != null ? soCCCD.trim() : null;
        this.soDienThoai = soDienThoai != null ? soDienThoai.trim() : null;
        this.vaiTro = vaiTro != null ? vaiTro : VaiTroKhachLuuTru.KHACH_DI_CUNG;
        this.ngayTao = LocalDateTime.now();
    }

    public KhachLuuTru(Long id, String maDatPhong, String hoTen, String soCCCD, String soDienThoai,
                       VaiTroKhachLuuTru vaiTro, LocalDateTime ngayTao) {
        this.id = id;
        this.maDatPhong = maDatPhong;
        this.hoTen = hoTen != null ? hoTen.trim() : null;
        this.soCCCD = soCCCD != null ? soCCCD.trim() : null;
        this.soDienThoai = soDienThoai != null ? soDienThoai.trim() : null;
        this.vaiTro = vaiTro != null ? vaiTro : VaiTroKhachLuuTru.KHACH_DI_CUNG;
        this.ngayTao = ngayTao != null ? ngayTao : LocalDateTime.now();
    }

    /**
     * Kiểm tra tính hợp lệ của số CCCD Việt Nam (đúng 12 chữ số, không chứa chữ cái, khoảng trắng hay ký tự đặc biệt).
     */
    public static boolean kiemTraCCCD(String cccd) {
        if (cccd == null) {
            return false;
        }
        String clean = cccd.trim();
        return clean.matches(REGEX_CCCD_VN);
    }

    /**
     * Trả về số CCCD đã che bớt các số ở giữa để bảo vệ quyền riêng tư cá nhân (Ví dụ: 001******123).
     */
    public String getSoCCCDChe() {
        if (soCCCD == null || soCCCD.trim().length() < 6) {
            return "******";
        }
        String clean = soCCCD.trim();
        if (clean.length() == 12) {
            return clean.substring(0, 3) + "******" + clean.substring(9);
        }
        return clean.substring(0, 2) + "****" + clean.substring(clean.length() - 2);
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

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen != null ? hoTen.trim() : null;
    }

    public String getSoCCCD() {
        return soCCCD;
    }

    public void setSoCCCD(String soCCCD) {
        this.soCCCD = soCCCD != null ? soCCCD.trim() : null;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai != null ? soDienThoai.trim() : null;
    }

    public VaiTroKhachLuuTru getVaiTro() {
        return vaiTro;
    }

    public void setVaiTro(VaiTroKhachLuuTru vaiTro) {
        this.vaiTro = vaiTro;
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
        KhachLuuTru that = (KhachLuuTru) o;
        return Objects.equals(maDatPhong, that.maDatPhong) && Objects.equals(soCCCD, that.soCCCD);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maDatPhong, soCCCD);
    }

    @Override
    public String toString() {
        return hoTen + " (" + (vaiTro != null ? vaiTro.getTenHienThi() : "") + " - CCCD: " + getSoCCCDChe() + ")";
    }
}
