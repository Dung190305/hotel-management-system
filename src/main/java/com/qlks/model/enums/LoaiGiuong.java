package com.qlks.model.enums;

/**
 * Phân loại số giường và sức chứa phòng khách sạn.
 * - Phòng đơn: 1 giường, sức chứa tối đa 2 người.
 * - Phòng đôi: 2 giường, sức chứa tối đa 4 người.
 */
public enum LoaiGiuong {
    DON("Phòng đơn", 1, 2),
    DOI("Phòng đôi", 2, 4);

    private final String tenHienThi;
    private final int soGiuong;
    private final int sucChuaToiDa;

    LoaiGiuong(String tenHienThi, int soGiuong, int sucChuaToiDa) {
        this.tenHienThi = tenHienThi;
        this.soGiuong = soGiuong;
        this.sucChuaToiDa = sucChuaToiDa;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    public int getSoGiuong() {
        return soGiuong;
    }

    public int getSucChuaToiDa() {
        return sucChuaToiDa;
    }

    @Override
    public String toString() {
        return tenHienThi;
    }

    public static LoaiGiuong fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return DON;
        }
        String clean = text.trim();
        for (LoaiGiuong lg : LoaiGiuong.values()) {
            if (lg.name().equalsIgnoreCase(clean) || lg.tenHienThi.equalsIgnoreCase(clean)) {
                return lg;
            }
        }
        if (clean.toLowerCase().contains("đôi") || clean.toLowerCase().contains("doi") || clean.toLowerCase().contains("double")) {
            return DOI;
        }
        return DON;
    }
}
