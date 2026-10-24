package com.qlks.dao;

import com.qlks.model.NhanVien;

import java.util.List;
import java.util.Optional;

/**
 * Interface DAO dành cho bảng nhan_vien.
 */
public interface NhanVienDAO extends BaseDAO<NhanVien, String> {

    /**
     * Tìm nhân viên liên kết với tài khoản đăng nhập.
     */
    Optional<NhanVien> findByTenDangNhap(String tenDangNhap);

    /**
     * Tìm kiếm nhân viên theo từ khóa (họ tên, sđt, email).
     */
    List<NhanVien> search(String keyword);

    /**
     * Cập nhật trạng thái ngừng hoạt động (Soft delete) của nhân viên.
     */
    boolean updateTrangThaiHoatDong(String maNhanVien, boolean dangHoatDong);
}
