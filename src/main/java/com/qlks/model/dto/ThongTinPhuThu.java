package com.qlks.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO đóng gói thông tin Phụ thu nhận phòng sớm (Early Check-in)
 * hoặc Phụ thu trả phòng trễ (Late Check-out).
 */
public class ThongTinPhuThu {

    public enum LoaiPhuThu {
        EARLY_CHECK_IN("Nhận phòng sớm"),
        LATE_CHECK_OUT("Trả phòng trễ"),
        KHONG_PHU_THU("Không phụ thu");

        private final String tenHienThi;

        LoaiPhuThu(String tenHienThi) {
            this.tenHienThi = tenHienThi;
        }

        public String getTenHienThi() {
            return tenHienThi;
        }
    }

    private final LoaiPhuThu loaiPhuThu;
    private final BigDecimal tyLePhuThu;     // Ví dụ: 0.3 (30%), 0.5 (50%), 1.0 (100%)
    private final BigDecimal soTienPhuThu;   // Số tiền phụ thu tính bằng VNĐ
    private final LocalDateTime thoiGianThucTe;
    private final String moTa;

    public ThongTinPhuThu(LoaiPhuThu loaiPhuThu, BigDecimal tyLePhuThu, BigDecimal soTienPhuThu,
                          LocalDateTime thoiGianThucTe, String moTa) {
        this.loaiPhuThu = loaiPhuThu != null ? loaiPhuThu : LoaiPhuThu.KHONG_PHU_THU;
        this.tyLePhuThu = tyLePhuThu != null ? tyLePhuThu : BigDecimal.ZERO;
        this.soTienPhuThu = soTienPhuThu != null ? soTienPhuThu : BigDecimal.ZERO;
        this.thoiGianThucTe = thoiGianThucTe;
        this.moTa = moTa != null ? moTa : "";
    }

    public static ThongTinPhuThu khongPhuThu(LocalDateTime thoiGianThucTe, String moTa) {
        return new ThongTinPhuThu(LoaiPhuThu.KHONG_PHU_THU, BigDecimal.ZERO, BigDecimal.ZERO, thoiGianThucTe, moTa);
    }

    public boolean coPhuThu() {
        return soTienPhuThu.compareTo(BigDecimal.ZERO) > 0;
    }

    public LoaiPhuThu getLoaiPhuThu() {
        return loaiPhuThu;
    }

    public BigDecimal getTyLePhuThu() {
        return tyLePhuThu;
    }

    public int getPhanTramPhuThu() {
        return tyLePhuThu.multiply(BigDecimal.valueOf(100)).intValue();
    }

    public BigDecimal getSoTienPhuThu() {
        return soTienPhuThu;
    }

    public LocalDateTime getThoiGianThucTe() {
        return thoiGianThucTe;
    }

    public String getMoTa() {
        return moTa;
    }

    @Override
    public String toString() {
        return "ThongTinPhuThu{" +
                "loai=" + loaiPhuThu +
                ", tyLe=" + getPhanTramPhuThu() + "%" +
                ", soTien=" + soTienPhuThu +
                ", thoiGian=" + thoiGianThucTe +
                ", moTa='" + moTa + '\'' +
                '}';
    }
}

