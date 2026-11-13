package com.qlks.model.enums;

/**
 * Vai trò của khách lưu trú trong đơn đặt phòng:
 * - NGUOI_DAI_DIEN: Người đại diện đặt phòng (chịu trách nhiệm chính).
 * - KHACH_DI_CUNG: Khách đi cùng đoàn / phòng.
 */
public enum VaiTroKhachLuuTru {
    NGUOI_DAI_DIEN("Người đại diện"),
    KHACH_DI_CUNG("Khách đi cùng");

    private final String tenHienThi;

    VaiTroKhachLuuTru(String tenHienThi) {
        this.tenHienThi = tenHienThi;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    @Override
    public String toString() {
        return tenHienThi;
    }

    public static VaiTroKhachLuuTru fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return KHACH_DI_CUNG;
        }
        String clean = text.trim();
        for (VaiTroKhachLuuTru vt : VaiTroKhachLuuTru.values()) {
            if (vt.name().equalsIgnoreCase(clean) || vt.tenHienThi.equalsIgnoreCase(clean)) {
                return vt;
            }
        }
        if (clean.toLowerCase().contains("đại diện") || clean.toLowerCase().contains("dai dien")) {
            return NGUOI_DAI_DIEN;
        }
        return KHACH_DI_CUNG;
    }
}
