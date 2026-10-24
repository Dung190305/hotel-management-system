package com.qlks.model.enums;

/**
 * Enum biểu diễn phân loại phòng khách sạn.
 */
public enum LoaiPhong {
    PHONG_THUONG("Phòng Thường (Standard)"),
    PHONG_VIP("Phòng VIP (Deluxe / Suite)");

    private final String tenHienThi;

    LoaiPhong(String tenHienThi) {
        this.tenHienThi = tenHienThi;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    @Override
    public String toString() {
        return tenHienThi;
    }

    public static LoaiPhong fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return PHONG_THUONG;
        }
        for (LoaiPhong lp : LoaiPhong.values()) {
            if (lp.name().equalsIgnoreCase(text) || lp.tenHienThi.equalsIgnoreCase(text)) {
                return lp;
            }
        }
        throw new IllegalArgumentException("Không tìm thấy loại phòng: " + text);
    }
}
