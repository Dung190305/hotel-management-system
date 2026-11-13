package com.qlks.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Đại diện cho đơn đặt dịch vụ tại phòng qua mã QR (In-Room QR Order).
 */
public class DonDichVuPhong {

    public enum TrangThaiOrder {
        CHO_TIEP_NHAN("Chờ tiếp nhận"),
        DA_TIEP_NHAN("Đã duyệt & Đang giao"),
        HOAN_TAT("Hoàn tất"),
        TU_CHOI("Từ chối / Hết hàng");

        private final String tenHienThi;

        TrangThaiOrder(String tenHienThi) {
            this.tenHienThi = tenHienThi;
        }

        public String getTenHienThi() {
            return tenHienThi;
        }
    }

    public enum HinhThucThanhToanOrder {
        GHI_VAO_HOA_DON("Ghi nợ vào phòng (Thanh toán khi trả phòng)"),
        CHUYEN_KHOAN_VIETQR("Chuyển khoản VietQR ngay");

        private final String tenHienThi;

        HinhThucThanhToanOrder(String tenHienThi) {
            this.tenHienThi = tenHienThi;
        }

        public String getTenHienThi() {
            return tenHienThi;
        }
    }

    public static class ChiTietMon {
        private String maDichVu;
        private String tenDichVu;
        private int soLuong;
        private BigDecimal donGia;

        public ChiTietMon(String maDichVu, String tenDichVu, int soLuong, BigDecimal donGia) {
            this.maDichVu = maDichVu;
            this.tenDichVu = tenDichVu;
            this.soLuong = soLuong;
            this.donGia = donGia != null ? donGia : BigDecimal.ZERO;
        }

        public String getMaDichVu() { return maDichVu; }
        public String getTenDichVu() { return tenDichVu; }
        public int getSoLuong() { return soLuong; }
        public BigDecimal getDonGia() { return donGia; }
        public BigDecimal getThanhTien() { return donGia.multiply(BigDecimal.valueOf(soLuong)); }
    }

    private String orderId;
    private String maPhong;
    private String soPhong;
    private String maDatPhong;
    private String tenKhach;
    private LocalDateTime thoiGianDat;
    private List<ChiTietMon> danhSachMon = new ArrayList<>();
    private String ghiChu;
    private TrangThaiOrder trangThai;
    private HinhThucThanhToanOrder hinhThucThanhToan;
    private String nguoiDuyet;
    private String lyDoTuChoi;

    public DonDichVuPhong() {
        this.thoiGianDat = LocalDateTime.now();
        this.trangThai = TrangThaiOrder.CHO_TIEP_NHAN;
        this.hinhThucThanhToan = HinhThucThanhToanOrder.GHI_VAO_HOA_DON;
    }

    public DonDichVuPhong(String orderId, String maPhong, String soPhong, String maDatPhong,
                          String tenKhach, List<ChiTietMon> danhSachMon, String ghiChu,
                          HinhThucThanhToanOrder hinhThucThanhToan) {
        this.orderId = orderId;
        this.maPhong = maPhong;
        this.soPhong = soPhong;
        this.maDatPhong = maDatPhong;
        this.tenKhach = tenKhach;
        this.thoiGianDat = LocalDateTime.now();
        this.danhSachMon = (danhSachMon != null) ? new ArrayList<>(danhSachMon) : new ArrayList<>();
        this.ghiChu = ghiChu;
        this.trangThai = TrangThaiOrder.CHO_TIEP_NHAN;
        this.hinhThucThanhToan = (hinhThucThanhToan != null) ? hinhThucThanhToan : HinhThucThanhToanOrder.GHI_VAO_HOA_DON;
    }

    public BigDecimal tinhTongTien() {
        BigDecimal tong = BigDecimal.ZERO;
        for (ChiTietMon mon : danhSachMon) {
            tong = tong.add(mon.getThanhTien());
        }
        return tong;
    }

    public String getTomTatMon() {
        if (danhSachMon.isEmpty()) return "(Chưa chọn món)";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < danhSachMon.size(); i++) {
            ChiTietMon m = danhSachMon.get(i);
            sb.append(m.getTenDichVu()).append(" x").append(m.getSoLuong());
            if (i < danhSachMon.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getMaPhong() { return maPhong; }
    public void setMaPhong(String maPhong) { this.maPhong = maPhong; }

    public String getSoPhong() { return soPhong; }
    public void setSoPhong(String soPhong) { this.soPhong = soPhong; }

    public String getMaDatPhong() { return maDatPhong; }
    public void setMaDatPhong(String maDatPhong) { this.maDatPhong = maDatPhong; }

    public String getTenKhach() { return tenKhach; }
    public void setTenKhach(String tenKhach) { this.tenKhach = tenKhach; }

    public LocalDateTime getThoiGianDat() { return thoiGianDat; }
    public void setThoiGianDat(LocalDateTime thoiGianDat) { this.thoiGianDat = thoiGianDat; }

    public List<ChiTietMon> getDanhSachMon() { return Collections.unmodifiableList(danhSachMon); }
    public void setDanhSachMon(List<ChiTietMon> danhSachMon) {
        this.danhSachMon = (danhSachMon != null) ? new ArrayList<>(danhSachMon) : new ArrayList<>();
    }
    public void addMon(ChiTietMon mon) {
        if (mon != null) this.danhSachMon.add(mon);
    }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public TrangThaiOrder getTrangThai() { return trangThai; }
    public void setTrangThai(TrangThaiOrder trangThai) { this.trangThai = trangThai; }

    public HinhThucThanhToanOrder getHinhThucThanhToan() { return hinhThucThanhToan; }
    public void setHinhThucThanhToan(HinhThucThanhToanOrder hinhThucThanhToan) { this.hinhThucThanhToan = hinhThucThanhToan; }

    public String getNguoiDuyet() { return nguoiDuyet; }
    public void setNguoiDuyet(String nguoiDuyet) { this.nguoiDuyet = nguoiDuyet; }

    public String getLyDoTuChoi() { return lyDoTuChoi; }
    public void setLyDoTuChoi(String lyDoTuChoi) { this.lyDoTuChoi = lyDoTuChoi; }
}

