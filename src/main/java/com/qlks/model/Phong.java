package com.qlks.model;

import com.qlks.model.enums.CauHinhLoaiPhong;
import com.qlks.model.enums.HangPhong;
import com.qlks.model.enums.LoaiGiuong;
import com.qlks.model.enums.TrangThaiPhong;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Lớp trừu tượng đại diện cho một căn phòng trong khách sạn.
 * Thể hiện tính TRỪU TƯỢNG (Abstract class) và ĐÓNG GÓI (Encapsulation).
 */
public abstract class Phong implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Mã phòng không được để trống")
    private String maPhong;

    @NotBlank(message = "Số phòng không được để trống")
    private String soPhong;

    @Min(value = 1, message = "Số tầng phải từ 1 trở lên")
    private int tang;

    @NotNull(message = "Đơn giá ngày không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Đơn giá ngày phải lớn hơn 0")
    private BigDecimal donGiaNgay;

    @NotNull(message = "Trạng thái phòng không được để trống")
    private TrangThaiPhong trangThaiHienTai;

    @NotNull(message = "Loại giường không được để trống")
    private LoaiGiuong loaiGiuong;

    private boolean dangKinhDoanh;

    public Phong() {
        this.trangThaiHienTai = TrangThaiPhong.AVAILABLE;
        this.loaiGiuong = LoaiGiuong.DON;
        this.dangKinhDoanh = true;
        this.donGiaNgay = BigDecimal.ZERO;
    }

    public Phong(String maPhong, String soPhong, int tang, BigDecimal donGiaNgay,
                 TrangThaiPhong trangThaiHienTai, boolean dangKinhDoanh) {
        this(maPhong, soPhong, tang, donGiaNgay, trangThaiHienTai, LoaiGiuong.DON, dangKinhDoanh);
    }

    public Phong(String maPhong, String soPhong, int tang, BigDecimal donGiaNgay,
                 TrangThaiPhong trangThaiHienTai, LoaiGiuong loaiGiuong, boolean dangKinhDoanh) {
        this.maPhong = maPhong;
        this.soPhong = soPhong;
        this.tang = tang;
        this.donGiaNgay = donGiaNgay != null ? donGiaNgay : BigDecimal.ZERO;
        this.trangThaiHienTai = trangThaiHienTai != null ? trangThaiHienTai : TrangThaiPhong.AVAILABLE;
        this.loaiGiuong = loaiGiuong != null ? loaiGiuong : LoaiGiuong.DON;
        this.dangKinhDoanh = dangKinhDoanh;
    }

    /**
     * Phương thức trừu tượng tính tiền phòng theo số ngày lưu trú.
     * Thể hiện tính ĐA HÌNH (Polymorphism): Mỗi loại phòng có chính sách giá khác nhau.
     *
     * @param soNgay Số ngày/đêm khách lưu trú
     * @return Tổng tiền phòng (BigDecimal)
     */
    public abstract BigDecimal tinhTienPhong(long soNgay);

    /**
     * Trả về hạng phòng (THUONG hoặc VIP).
     */
    public abstract HangPhong getHangPhong();

    /**
     * Trả về 1 trong đúng 4 cấu hình phòng chuẩn của hệ thống.
     */
    public abstract CauHinhLoaiPhong getCauHinh();

    /**
     * Trả về danh mục tiện nghi đầy đủ tương ứng với hạng phòng.
     */
    public abstract List<String> getDanhSachTienNghi();

    /**
     * Trả về tên định danh loại phòng (PHONG_DON_THUONG, PHONG_DON_VIP, PHONG_DOI_THUONG, PHONG_DOI_VIP)
     */
    public abstract String getLoaiPhong();

    /**
     * Trả về tên hiển thị loại phòng tiếng Việt chuẩn ("Phòng đơn thường", "Phòng đơn VIP", ...)
     */
    public abstract String getTenLoaiPhongHienThi();

    public int getSoGiuong() {
        return loaiGiuong != null ? loaiGiuong.getSoGiuong() : 1;
    }

    public int getSucChuaToiDa() {
        return loaiGiuong != null ? loaiGiuong.getSucChuaToiDa() : 2;
    }

    public LoaiGiuong getLoaiGiuong() {
        return loaiGiuong;
    }

    public void setLoaiGiuong(LoaiGiuong loaiGiuong) {
        this.loaiGiuong = loaiGiuong != null ? loaiGiuong : LoaiGiuong.DON;
    }

    public String getMaPhong() {
        return maPhong;
    }

    public void setMaPhong(String maPhong) {
        this.maPhong = maPhong;
    }

    public String getSoPhong() {
        return soPhong;
    }

    public void setSoPhong(String soPhong) {
        this.soPhong = soPhong;
    }

    public int getTang() {
        return tang;
    }

    public void setTang(int tang) {
        this.tang = tang;
    }

    public BigDecimal getDonGiaNgay() {
        return donGiaNgay;
    }

    public void setDonGiaNgay(BigDecimal donGiaNgay) {
        this.donGiaNgay = donGiaNgay;
    }

    public TrangThaiPhong getTrangThaiHienTai() {
        return trangThaiHienTai;
    }

    public void setTrangThaiHienTai(TrangThaiPhong trangThaiHienTai) {
        this.trangThaiHienTai = trangThaiHienTai;
    }

    public boolean isDangKinhDoanh() {
        return dangKinhDoanh;
    }

    public void setDangKinhDoanh(boolean dangKinhDoanh) {
        this.dangKinhDoanh = dangKinhDoanh;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Phong phong = (Phong) o;
        return Objects.equals(maPhong, phong.maPhong);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maPhong);
    }

    @Override
    public String toString() {
        return "Phòng " + soPhong + " (" + getTenLoaiPhongHienThi() + " - " + trangThaiHienTai.getTenHienThi() + ")";
    }
}
