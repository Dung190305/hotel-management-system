package com.qlks.model;

import com.qlks.model.enums.TrangThaiDatPhong;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Lớp đại diện cho Đơn đặt phòng (Booking).
 * Thể hiện quan hệ Association giữa KhachHang, Phong và NhanVien.
 */
public class DatPhong implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Mã đặt phòng không được để trống")
    private String maDatPhong;

    @NotNull(message = "Khách hàng không được để trống")
    private KhachHang khachHang;

    @NotNull(message = "Phòng không được để trống")
    private Phong phong;

    @NotNull(message = "Nhân viên lập không được để trống")
    private NhanVien nhanVienLap;

    @NotNull(message = "Ngày nhận phòng dự kiến không được để trống")
    private LocalDateTime ngayNhanPhong;

    @NotNull(message = "Ngày trả phòng dự kiến không được để trống")
    private LocalDateTime ngayTraPhong;

    private LocalDateTime thoiGianNhanThucTe;
    private LocalDateTime thoiGianTraThucTe;

    @Min(value = 1, message = "Số lượng khách tối thiểu là 1 người")
    private int soLuongKhach;

    @NotNull(message = "Trạng thái đặt phòng không được để trống")
    private TrangThaiDatPhong trangThai;

    @NotNull(message = "Tiền cọc không được để trống")
    @PositiveOrZero(message = "Tiền cọc không được âm")
    private BigDecimal tienCoc;

    private LocalDateTime ngayTao;

    public DatPhong() {
        this.soLuongKhach = 1;
        this.trangThai = TrangThaiDatPhong.DA_XAC_NHAN;
        this.tienCoc = BigDecimal.ZERO;
        this.ngayTao = LocalDateTime.now();
    }

    public DatPhong(String maDatPhong, KhachHang khachHang, Phong phong, NhanVien nhanVienLap,
                    LocalDateTime ngayNhanPhong, LocalDateTime ngayTraPhong,
                    LocalDateTime thoiGianNhanThucTe, LocalDateTime thoiGianTraThucTe,
                    int soLuongKhach, TrangThaiDatPhong trangThai, BigDecimal tienCoc, LocalDateTime ngayTao) {
        this.maDatPhong = maDatPhong;
        this.khachHang = khachHang;
        this.phong = phong;
        this.nhanVienLap = nhanVienLap;
        this.ngayNhanPhong = ngayNhanPhong;
        this.ngayTraPhong = ngayTraPhong;
        this.thoiGianNhanThucTe = thoiGianNhanThucTe;
        this.thoiGianTraThucTe = thoiGianTraThucTe;
        this.soLuongKhach = soLuongKhach;
        this.trangThai = trangThai != null ? trangThai : TrangThaiDatPhong.DA_XAC_NHAN;
        this.tienCoc = tienCoc != null ? tienCoc : BigDecimal.ZERO;
        this.ngayTao = ngayTao != null ? ngayTao : LocalDateTime.now();
    }

    /**
     * Tính số đêm lưu trú giữa ngày nhận và ngày trả phòng.
     * Quy tắc nghiệp vụ: Tối thiểu là 1 đêm (kể cả nhận và trả trong cùng ngày).
     */
    public long tinhSoDem() {
        if (ngayNhanPhong == null || ngayTraPhong == null) {
            return 1;
        }
        long soDem = ChronoUnit.DAYS.between(ngayNhanPhong.toLocalDate(), ngayTraPhong.toLocalDate());
        return Math.max(1, soDem);
    }

    /**
     * Gọi đa hình phương thức tinhTienPhong() của đối tượng Phong.
     */
    public BigDecimal tinhTienTamTinh() {
        if (phong == null) {
            return BigDecimal.ZERO;
        }
        return phong.tinhTienPhong(tinhSoDem());
    }

    public String getMaDatPhong() {
        return maDatPhong;
    }

    public void setMaDatPhong(String maDatPhong) {
        this.maDatPhong = maDatPhong;
    }

    public KhachHang getKhachHang() {
        return khachHang;
    }

    public void setKhachHang(KhachHang khachHang) {
        this.khachHang = khachHang;
    }

    public Phong getPhong() {
        return phong;
    }

    public void setPhong(Phong phong) {
        this.phong = phong;
    }

    public NhanVien getNhanVienLap() {
        return nhanVienLap;
    }

    public void setNhanVienLap(NhanVien nhanVienLap) {
        this.nhanVienLap = nhanVienLap;
    }

    public LocalDateTime getNgayNhanPhong() {
        return ngayNhanPhong;
    }

    public void setNgayNhanPhong(LocalDateTime ngayNhanPhong) {
        this.ngayNhanPhong = ngayNhanPhong;
    }

    public LocalDateTime getNgayTraPhong() {
        return ngayTraPhong;
    }

    public void setNgayTraPhong(LocalDateTime ngayTraPhong) {
        this.ngayTraPhong = ngayTraPhong;
    }

    public LocalDateTime getThoiGianNhanThucTe() {
        return thoiGianNhanThucTe;
    }

    public void setThoiGianNhanThucTe(LocalDateTime thoiGianNhanThucTe) {
        this.thoiGianNhanThucTe = thoiGianNhanThucTe;
    }

    public LocalDateTime getThoiGianTraThucTe() {
        return thoiGianTraThucTe;
    }

    public void setThoiGianTraThucTe(LocalDateTime thoiGianTraThucTe) {
        this.thoiGianTraThucTe = thoiGianTraThucTe;
    }

    public int getSoLuongKhach() {
        return soLuongKhach;
    }

    public void setSoLuongKhach(int soLuongKhach) {
        this.soLuongKhach = soLuongKhach;
    }

    public TrangThaiDatPhong getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(TrangThaiDatPhong trangThai) {
        this.trangThai = trangThai;
    }

    public BigDecimal getTienCoc() {
        return tienCoc;
    }

    public void setTienCoc(BigDecimal tienCoc) {
        this.tienCoc = tienCoc;
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
        DatPhong datPhong = (DatPhong) o;
        return Objects.equals(maDatPhong, datPhong.maDatPhong);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maDatPhong);
    }

    @Override
    public String toString() {
        return maDatPhong + " [" + (phong != null ? phong.getSoPhong() : "") + "] - " + (khachHang != null ? khachHang.getHoTen() : "");
    }
}
