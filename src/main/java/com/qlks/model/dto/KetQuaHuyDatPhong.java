package com.qlks.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO chứa kết quả xử lý hủy đặt phòng và quyết toán hoàn/phạt tiền cọc.
 */
public class KetQuaHuyDatPhong {

    private final String maDatPhong;
    private final BigDecimal tienCocBanDau;
    private final BigDecimal tyLeHoan; // 1.0 (100%) hoặc 0.5 (50%)
    private final BigDecimal tienHoanLai;
    private final BigDecimal phiPhatHuy;
    private final LocalDateTime thoiDiemHuy;
    private final long soGioConLai;
    private final String maHoaDonQuyetToan;
    private final String maThanhToanHoan;
    private final String lyDoHuy;

    public KetQuaHuyDatPhong(String maDatPhong, BigDecimal tienCocBanDau, BigDecimal tyLeHoan,
                             BigDecimal tienHoanLai, BigDecimal phiPhatHuy, LocalDateTime thoiDiemHuy,
                             long soGioConLai, String maHoaDonQuyetToan, String maThanhToanHoan, String lyDoHuy) {
        this.maDatPhong = maDatPhong;
        this.tienCocBanDau = tienCocBanDau != null ? tienCocBanDau : BigDecimal.ZERO;
        this.tyLeHoan = tyLeHoan != null ? tyLeHoan : BigDecimal.ZERO;
        this.tienHoanLai = tienHoanLai != null ? tienHoanLai : BigDecimal.ZERO;
        this.phiPhatHuy = phiPhatHuy != null ? phiPhatHuy : BigDecimal.ZERO;
        this.thoiDiemHuy = thoiDiemHuy != null ? thoiDiemHuy : LocalDateTime.now();
        this.soGioConLai = soGioConLai;
        this.maHoaDonQuyetToan = maHoaDonQuyetToan;
        this.maThanhToanHoan = maThanhToanHoan;
        this.lyDoHuy = lyDoHuy;
    }

    public String getMaDatPhong() {
        return maDatPhong;
    }

    public BigDecimal getTienCocBanDau() {
        return tienCocBanDau;
    }

    public BigDecimal getTyLeHoan() {
        return tyLeHoan;
    }

    public BigDecimal getTienHoanLai() {
        return tienHoanLai;
    }

    public BigDecimal getPhiPhatHuy() {
        return phiPhatHuy;
    }

    public LocalDateTime getThoiDiemHuy() {
        return thoiDiemHuy;
    }

    public long getSoGioConLai() {
        return soGioConLai;
    }

    public String getMaHoaDonQuyetToan() {
        return maHoaDonQuyetToan;
    }

    public String getMaThanhToanHoan() {
        return maThanhToanHoan;
    }

    public String getLyDoHuy() {
        return lyDoHuy;
    }
}

