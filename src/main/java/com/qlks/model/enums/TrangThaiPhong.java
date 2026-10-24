package com.qlks.model.enums;

/**
 * Enum đại diện cho 5 trạng thái vật lý của phòng trong khách sạn.
 * Thể hiện tính đóng gói OOP: thuộc tính private, constructor và getter.
 */
public enum TrangThaiPhong {
    AVAILABLE("Còn trống", "Phòng sẵn sàng đón khách"),
    OCCUPIED("Đang có khách", "Khách đã làm thủ tục check-in và đang lưu trú"),
    RESERVED("Đã đặt", "Phòng đã có booking được xác nhận trước"),
    CLEANING("Đang dọn dẹp", "Khách vừa check-out hoặc phòng đang được vệ sinh"),
    MAINTENANCE("Đang bảo trì", "Phòng hỏng hóc hoặc đang sửa chữa thiết bị");

    private final String tenHienThi;
    private final String moTa;

    TrangThaiPhong(String tenHienThi, String moTa) {
        this.tenHienThi = tenHienThi;
        this.moTa = moTa;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    public String getMoTa() {
        return moTa;
    }

    @Override
    public String toString() {
        return tenHienThi;
    }

    public static TrangThaiPhong fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return AVAILABLE;
        }
        for (TrangThaiPhong tt : TrangThaiPhong.values()) {
            if (tt.name().equalsIgnoreCase(text) || tt.tenHienThi.equalsIgnoreCase(text)) {
                return tt;
            }
        }
        throw new IllegalArgumentException("Không tìm thấy trạng thái phòng: " + text);
    }
}
