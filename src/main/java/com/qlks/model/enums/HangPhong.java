package com.qlks.model.enums;

/**
 * Hạng phòng khách sạn: Phòng thường và Phòng VIP.
 */
public enum HangPhong {
    THUONG("Phòng thường"),
    VIP("Phòng VIP");

    private final String tenHienThi;

    HangPhong(String tenHienThi) {
        this.tenHienThi = tenHienThi;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    @Override
    public String toString() {
        return tenHienThi;
    }

    public static HangPhong fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return THUONG;
        }
        String clean = text.trim();
        for (HangPhong hp : HangPhong.values()) {
            if (hp.name().equalsIgnoreCase(clean) || hp.tenHienThi.equalsIgnoreCase(clean)) {
                return hp;
            }
        }
        if (clean.toLowerCase().contains("vip")) {
            return VIP;
        }
        return THUONG;
    }
}
