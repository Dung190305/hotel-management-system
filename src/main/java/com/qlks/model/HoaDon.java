package com.qlks.model;

import com.qlks.model.enums.TrangThaiThanhToan;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Lớp đại diện cho Hóa đơn thanh toán.
 * Thể hiện quan hệ Composition: Một hóa đơn chứa phiếu đặt phòng và danh sách các chi tiết dịch vụ.
 */
public class HoaDon implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Mã hóa đơn không được để trống")
    private String maHoaDon;

    @NotNull(message = "Phiếu đặt phòng không được để trống")
    private DatPhong datPhong;

    @NotNull(message = "Nhân viên lập hóa đơn không được để trống")
    private NhanVien nhanVienLap;

    private LocalDateTime ngayLap;

    @NotNull(message = "Tiền phòng không được để trống")
    @PositiveOrZero(message = "Tiền phòng không được âm")
    private BigDecimal tienPhong;

    @NotNull(message = "Tiền dịch vụ không được để trống")
    @PositiveOrZero(message = "Tiền dịch vụ không được âm")
    private BigDecimal tienDichVu;

    @NotNull(message = "Tiền giảm giá không được để trống")
    @PositiveOrZero(message = "Tiền giảm giá không được âm")
    private BigDecimal tienGiamGia;

    @NotNull(message = "Tiền cọc không được để trống")
    @PositiveOrZero(message = "Tiền cọc không được âm")
    private BigDecimal tienCoc;

    @NotNull(message = "Tổng tiền không được để trống")
    @PositiveOrZero(message = "Tổng tiền không được âm")
    private BigDecimal tongTien;

    @NotNull(message = "Trạng thái thanh toán không được để trống")
    private TrangThaiThanhToan trangThai;

    private List<ChiTietDichVu> danhSachDichVu;

    public HoaDon() {
        this.ngayLap = LocalDateTime.now();
        this.tienPhong = BigDecimal.ZERO;
        this.tienDichVu = BigDecimal.ZERO;
        this.tienGiamGia = BigDecimal.ZERO;
        this.tienCoc = BigDecimal.ZERO;
        this.tongTien = BigDecimal.ZERO;
        this.trangThai = TrangThaiThanhToan.CHUA_THANH_TOAN;
        this.danhSachDichVu = new ArrayList<>();
    }

    public HoaDon(String maHoaDon, DatPhong datPhong, NhanVien nhanVienLap, LocalDateTime ngayLap,
                  BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia,
                  BigDecimal tongTien, TrangThaiThanhToan trangThai) {
        this(maHoaDon, datPhong, nhanVienLap, ngayLap, tienPhong, tienDichVu, tienGiamGia,
                datPhong != null ? datPhong.getTienCoc() : BigDecimal.ZERO, tongTien, trangThai);
    }

    public HoaDon(String maHoaDon, DatPhong datPhong, NhanVien nhanVienLap, LocalDateTime ngayLap,
                  BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc,
                  BigDecimal tongTien, TrangThaiThanhToan trangThai) {
        this.maHoaDon = maHoaDon;
        this.datPhong = datPhong;
        this.nhanVienLap = nhanVienLap;
        this.ngayLap = ngayLap != null ? ngayLap : LocalDateTime.now();
        this.tienPhong = tienPhong != null ? tienPhong : BigDecimal.ZERO;
        this.tienDichVu = tienDichVu != null ? tienDichVu : BigDecimal.ZERO;
        this.tienGiamGia = tienGiamGia != null ? tienGiamGia : BigDecimal.ZERO;
        this.tienCoc = tienCoc != null ? tienCoc : BigDecimal.ZERO;
        this.tongTien = tongTien != null ? tongTien : BigDecimal.ZERO;
        this.trangThai = trangThai != null ? trangThai : TrangThaiThanhToan.CHUA_THANH_TOAN;
        this.danhSachDichVu = new ArrayList<>();
    }

    /**
     * Tính toán tổng chi phí lưu trú thực tế:
     * Tổng chi phí = Tiền phòng + Tiền dịch vụ - Giảm giá.
     */
    public BigDecimal getTongChiPhi() {
        BigDecimal tong = tienPhong.add(tienDichVu).subtract(tienGiamGia);
        return tong.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : tong;
    }

    /**
     * Tính số tiền thực tế khách CÒN LẠI PHẢI THANH TOÁN (sau khi đã trừ tiền cọc):
     * Còn lại phải thu = max(0, Tổng chi phí - Tiền cọc đã nhận).
     */
    public BigDecimal getConLaiPhaiThu() {
        BigDecimal tongChiPhi = getTongChiPhi();
        BigDecimal conLai = tongChiPhi.subtract(tienCoc);
        return conLai.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : conLai;
    }

    /**
     * Tính toán lại tổng tiền hóa đơn:
     * Quy ước tổng tiền tương ứng với tổng chi phí dịch vụ và phòng.
     */
    public BigDecimal tinhTongTien() {
        return getTongChiPhi();
    }

    /**
     * Thêm dịch vụ vào hóa đơn và tự động cộng dồn tiền dịch vụ.
     */
    public void themChiTietDichVu(ChiTietDichVu ctdv) {
        if (ctdv != null) {
            this.danhSachDichVu.add(ctdv);
            this.tienDichVu = this.tienDichVu.add(ctdv.tinhThanhTien());
            this.tongTien = tinhTongTien();
        }
    }

    public String getMaHoaDon() {
        return maHoaDon;
    }

    public void setMaHoaDon(String maHoaDon) {
        this.maHoaDon = maHoaDon;
    }

    public DatPhong getDatPhong() {
        return datPhong;
    }

    public void setDatPhong(DatPhong datPhong) {
        this.datPhong = datPhong;
    }

    public NhanVien getNhanVienLap() {
        return nhanVienLap;
    }

    public void setNhanVienLap(NhanVien nhanVienLap) {
        this.nhanVienLap = nhanVienLap;
    }

    public LocalDateTime getNgayLap() {
        return ngayLap;
    }

    public void setNgayLap(LocalDateTime ngayLap) {
        this.ngayLap = ngayLap;
    }

    public BigDecimal getTienPhong() {
        return tienPhong;
    }

    public void setTienPhong(BigDecimal tienPhong) {
        this.tienPhong = tienPhong;
        this.tongTien = tinhTongTien();
    }

    public BigDecimal getTienDichVu() {
        return tienDichVu;
    }

    public void setTienDichVu(BigDecimal tienDichVu) {
        this.tienDichVu = tienDichVu;
        this.tongTien = tinhTongTien();
    }

    public BigDecimal getTienGiamGia() {
        return tienGiamGia;
    }

    public void setTienGiamGia(BigDecimal tienGiamGia) {
        this.tienGiamGia = tienGiamGia;
        this.tongTien = tinhTongTien();
    }

    public BigDecimal getTienCoc() {
        return tienCoc;
    }

    public void setTienCoc(BigDecimal tienCoc) {
        this.tienCoc = tienCoc != null ? tienCoc : BigDecimal.ZERO;
    }

    public BigDecimal getTongTien() {
        return tongTien;
    }

    public void setTongTien(BigDecimal tongTien) {
        this.tongTien = tongTien;
    }

    public TrangThaiThanhToan getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(TrangThaiThanhToan trangThai) {
        this.trangThai = trangThai;
    }

    public List<ChiTietDichVu> getDanhSachDichVu() {
        return danhSachDichVu;
    }

    public void setDanhSachDichVu(List<ChiTietDichVu> danhSachDichVu) {
        this.danhSachDichVu = danhSachDichVu != null ? danhSachDichVu : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HoaDon hoaDon = (HoaDon) o;
        return Objects.equals(maHoaDon, hoaDon.maHoaDon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maHoaDon);
    }

    @Override
    public String toString() {
        return "Hóa đơn " + maHoaDon + " - Tổng: " + tongTien.toPlainString() + " VNĐ (" + trangThai.getTenHienThi() + ")";
    }
}
