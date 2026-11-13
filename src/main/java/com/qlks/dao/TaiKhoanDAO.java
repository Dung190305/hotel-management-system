package com.qlks.dao;

import com.qlks.model.TaiKhoan;

import java.util.Optional;

/**
 * Interface DAO dành cho bảng tai_khoan.
 */
public interface TaiKhoanDAO extends BaseDAO<TaiKhoan, String> {

    /**
     * Tìm tài khoản theo tên đăng nhập.
     *
     * @param tenDangNhap Tên đăng nhập
     * @return Optional<TaiKhoan>
     */
    Optional<TaiKhoan> findByTenDangNhap(String tenDangNhap);

    /**
     * Cập nhật mật khẩu mới (chuỗi băm BCrypt) cho tài khoản.
     *
     * @param tenDangNhap    Tên tài khoản
     * @param matKhauHashMoi Chuỗi băm mới
     * @return true nếu thành công
     */
    boolean updateMatKhau(String tenDangNhap, String matKhauHashMoi);

    /**
     * Cập nhật trạng thái khóa/mở tài khoản.
     *
     * @param tenDangNhap Tên tài khoản
     * @param trangThai   true = kích hoạt, false = khóa
     * @return true nếu thành công
     */
    boolean updateTrangThai(String tenDangNhap, boolean trangThai);

    /**
     * Thêm mới tài khoản trong một Transaction hiện hành.
     */
    default boolean save(java.sql.Connection conn, TaiKhoan taiKhoan) {
        return save(taiKhoan);
    }
}
