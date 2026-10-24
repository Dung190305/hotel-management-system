package com.qlks.model.enums;

/**
 * Enum biểu diễn trạng thái thanh toán của hóa đơn.
 */
public enum TrangThaiThanhToan {
    CHUA_THANH_TOAN("Chưa thanh toán"),
    DA_THANH_TOAN("Đã thanh toán");

    private final String tenHienThi;

    TrangThaiThanhToan(String tenHienThi) {
        this.tenHienThi = tenHienThi;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    @Override
    public String toString() {
        return tenHienThi;
    }

    public static TrangThaiThanhToan fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return CHUA_THANH_TOAN;
        }
        for (TrangThaiThanhToan tt : TrangThaiThanhToan.values()) {
            if (tt.name().equalsIgnoreCase(text) || tt.tenHienThi.equalsIgnoreCase(text)) {
                return tt;
            }
        }
        throw new IllegalArgumentException("Không tìm thấy trạng thái thanh toán: " + text);
    }
}
