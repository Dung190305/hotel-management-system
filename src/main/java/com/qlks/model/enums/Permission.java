package com.qlks.model.enums;

/**
 * Các quyền hạn chi tiết trong hệ thống Quản lý Khách sạn (RBAC - Role-Based Access Control).
 * Được đối chiếu theo ma trận quyền chuẩn trong Đề cương dự án.
 */
public enum Permission {
    QUAN_LY_TAI_KHOAN("Quản lý tài khoản và nhân viên"),
    SUA_DANH_MUC("Sửa phòng và dịch vụ (danh mục)"),
    QUAN_LY_KHACH_HANG("Thêm và sửa khách hàng"),
    DAT_PHONG("Tạo, hủy đặt phòng; check-in/out"),
    GHI_NHAN_DICH_VU_THANH_TOAN("Ghi nhận dịch vụ và thanh toán"),
    XEM_BAO_CAO("Xem báo cáo và thống kê");

    private final String moTa;

    Permission(String moTa) {
        this.moTa = moTa;
    }

    public String getMoTa() {
        return moTa;
    }
}

