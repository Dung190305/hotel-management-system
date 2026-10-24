package com.qlks.model.enums;

/**
 * Enum biểu diễn vòng đời trạng thái của một đơn đặt phòng (Booking Status).
 */
public enum TrangThaiDatPhong {
    CHO_XAC_NHAN("Chờ xác nhận"),
    DA_XAC_NHAN("Đã xác nhận"),
    DANG_O("Đang ở"),
    DA_TRA_PHONG("Đã trả phòng"),
    DA_HUY("Đã hủy");

    private final String tenHienThi;

    TrangThaiDatPhong(String tenHienThi) {
        this.tenHienThi = tenHienThi;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    @Override
    public String toString() {
        return tenHienThi;
    }

    public static TrangThaiDatPhong fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return DA_XAC_NHAN;
        }
        for (TrangThaiDatPhong tt : TrangThaiDatPhong.values()) {
            if (tt.name().equalsIgnoreCase(text) || tt.tenHienThi.equalsIgnoreCase(text)) {
                return tt;
            }
        }
        throw new IllegalArgumentException("Không tìm thấy trạng thái đặt phòng: " + text);
    }
}
