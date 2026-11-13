package com.qlks.dao;

import com.qlks.model.KhachLuuTru;

import java.sql.Connection;
import java.util.List;

/**
 * Interface DAO dành cho bảng chi_tiet_khach_dat_phong.
 * Lưu trữ danh sách khách lưu trú thực tế kèm CCCD của từng đơn đặt phòng.
 */
public interface KhachLuuTruDAO {

    /**
     * Lấy danh sách khách lưu trú của một đơn đặt phòng.
     */
    List<KhachLuuTru> findByMaDatPhong(String maDatPhong);

    /**
     * Lấy danh sách khách lưu trú trong một transaction (Connection).
     */
    default List<KhachLuuTru> findByMaDatPhong(Connection conn, String maDatPhong) {
        return findByMaDatPhong(maDatPhong);
    }

    /**
     * Lưu một khách lưu trú.
     */
    default boolean save(KhachLuuTru khach) {
        return true;
    }

    /**
     * Lưu một khách lưu trú trong một transaction hiện hành.
     */
    boolean save(Connection conn, KhachLuuTru khach);

    /**
     * Lưu toàn bộ danh sách khách lưu trú của một đơn đặt phòng trong transaction.
     */
    boolean saveAll(Connection conn, String maDatPhong, List<KhachLuuTru> danhSachKhach);

    /**
     * Xóa danh sách khách lưu trú theo mã đặt phòng.
     */
    boolean deleteByMaDatPhong(Connection conn, String maDatPhong);
}
