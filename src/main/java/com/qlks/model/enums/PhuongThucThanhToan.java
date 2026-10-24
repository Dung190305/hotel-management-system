package com.qlks.model.enums;

/**
 * Enum biểu diễn các phương thức thanh toán tiền mặt và chuyển khoản.
 */
public enum PhuongThucThanhToan {
    TIEN_MAT("Tiền mặt"),
    CHUYEN_KHOAN("Chuyển khoản");

    private final String tenHienThi;

    PhuongThucThanhToan(String tenHienThi) {
        this.tenHienThi = tenHienThi;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    @Override
    public String toString() {
        return tenHienThi;
    }

    public static PhuongThucThanhToan fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return TIEN_MAT;
        }
        for (PhuongThucThanhToan pt : PhuongThucThanhToan.values()) {
            if (pt.name().equalsIgnoreCase(text) || pt.tenHienThi.equalsIgnoreCase(text)) {
                return pt;
            }
        }
        throw new IllegalArgumentException("Không tìm thấy phương thức thanh toán: " + text);
    }
}
