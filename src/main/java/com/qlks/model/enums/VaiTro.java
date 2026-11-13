package com.qlks.model.enums;

/**
 * Enum biểu diễn vai trò của người dùng trong hệ thống quản lý khách sạn.
 */
public enum VaiTro {
    ADMIN("Quản trị viên"),
    MANAGER("Quản lý"),
    LE_TAN("Nhân viên Lễ tân");

    private final String tenHienThi;

    VaiTro(String tenHienThi) {
        this.tenHienThi = tenHienThi;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    @Override
    public String toString() {
        return tenHienThi;
    }

    public static VaiTro fromString(String text) {
        for (VaiTro vt : VaiTro.values()) {
            if (vt.name().equalsIgnoreCase(text) || vt.tenHienThi.equalsIgnoreCase(text)) {
                return vt;
            }
        }
        throw new IllegalArgumentException("Không tìm thấy vai trò: " + text);
    }
}
