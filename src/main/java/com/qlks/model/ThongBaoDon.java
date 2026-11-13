package com.qlks.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Lớp đại diện cho thông báo đơn tới hạn (Đặt phòng / Trả phòng / Hóa đơn).
 */
public class ThongBaoDon implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum LoaiThongBao {
        QUA_HAN_CHECK_IN("Quá hạn nhận phòng", MucDo.NGUY_CAP),
        CHECK_IN_HOM_NAY("Tới hạn nhận phòng hôm nay", MucDo.CANH_BAO),
        QUA_HAN_CHECK_OUT("Quá hạn trả phòng", MucDo.NGUY_CAP),
        CHECK_OUT_HOM_NAY("Tới hạn trả phòng hôm nay", MucDo.CANH_BAO),
        HOA_DON_CHUA_THANH_TOAN("Hóa đơn chưa thanh toán", MucDo.THONG_TIN);

        private final String tenHienThi;
        private final MucDo mucDoMacDinh;

        LoaiThongBao(String tenHienThi, MucDo mucDoMacDinh) {
            this.tenHienThi = tenHienThi;
            this.mucDoMacDinh = mucDoMacDinh;
        }

        public String getTenHienThi() {
            return tenHienThi;
        }

        public MucDo getMucDoMacDinh() {
            return mucDoMacDinh;
        }

        @Override
        public String toString() {
            return tenHienThi;
        }
    }

    public enum MucDo {
        NGUY_CAP("Khẩn cấp", "#DC3545"), // Đỏ
        CANH_BAO("Cảnh báo", "#FD7E14"), // Cam
        THONG_TIN("Thông tin", "#0D6EFD"); // Xanh dương

        private final String tenHienThi;
        private final String maMauHex;

        MucDo(String tenHienThi, String maMauHex) {
            this.tenHienThi = tenHienThi;
            this.maMauHex = maMauHex;
        }

        public String getTenHienThi() {
            return tenHienThi;
        }

        public String getMaMauHex() {
            return maMauHex;
        }

        @Override
        public String toString() {
            return tenHienThi;
        }
    }

    private String maDon;
    private LoaiThongBao loaiThongBao;
    private MucDo mucDo;
    private String tenKhachHang;
    private String soDienThoai;
    private String soPhong;
    private LocalDateTime thoiHan;
    private String tinhTrangChiTiet;
    private boolean daDoc = false;

    public ThongBaoDon() {
    }

    public ThongBaoDon(String maDon, LoaiThongBao loaiThongBao, MucDo mucDo,
                       String tenKhachHang, String soDienThoai, String soPhong,
                       LocalDateTime thoiHan, String tinhTrangChiTiet) {
        this(maDon, loaiThongBao, mucDo, tenKhachHang, soDienThoai, soPhong, thoiHan, tinhTrangChiTiet, false);
    }

    public ThongBaoDon(String maDon, LoaiThongBao loaiThongBao, MucDo mucDo,
                       String tenKhachHang, String soDienThoai, String soPhong,
                       LocalDateTime thoiHan, String tinhTrangChiTiet, boolean daDoc) {
        this.maDon = maDon;
        this.loaiThongBao = loaiThongBao;
        this.mucDo = mucDo != null ? mucDo : (loaiThongBao != null ? loaiThongBao.getMucDoMacDinh() : MucDo.THONG_TIN);
        this.tenKhachHang = tenKhachHang;
        this.soDienThoai = soDienThoai;
        this.soPhong = soPhong;
        this.thoiHan = thoiHan;
        this.tinhTrangChiTiet = tinhTrangChiTiet;
        this.daDoc = daDoc;
    }

    public String getId() {
        return maDon + "_" + (loaiThongBao != null ? loaiThongBao.name() : "");
    }

    public boolean isDaDoc() {
        return daDoc;
    }

    public void setDaDoc(boolean daDoc) {
        this.daDoc = daDoc;
    }

    public String getMaDon() {
        return maDon;
    }

    public void setMaDon(String maDon) {
        this.maDon = maDon;
    }

    public LoaiThongBao getLoaiThongBao() {
        return loaiThongBao;
    }

    public void setLoaiThongBao(LoaiThongBao loaiThongBao) {
        this.loaiThongBao = loaiThongBao;
    }

    public MucDo getMucDo() {
        return mucDo;
    }

    public void setMucDo(MucDo mucDo) {
        this.mucDo = mucDo;
    }

    public String getTenKhachHang() {
        return tenKhachHang;
    }

    public void setTenKhachHang(String tenKhachHang) {
        this.tenKhachHang = tenKhachHang;
    }

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    public String getSoPhong() {
        return soPhong;
    }

    public void setSoPhong(String soPhong) {
        this.soPhong = soPhong;
    }

    public LocalDateTime getThoiHan() {
        return thoiHan;
    }

    public void setThoiHan(LocalDateTime thoiHan) {
        this.thoiHan = thoiHan;
    }

    public String getTinhTrangChiTiet() {
        return tinhTrangChiTiet;
    }

    public void setTinhTrangChiTiet(String tinhTrangChiTiet) {
        this.tinhTrangChiTiet = tinhTrangChiTiet;
    }
}

