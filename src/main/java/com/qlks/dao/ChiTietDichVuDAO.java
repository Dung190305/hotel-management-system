package com.qlks.dao;

import com.qlks.model.ChiTietDichVu;

import java.util.List;

/**
 * Interface DAO dành cho bảng chi_tiet_dich_vu.
 */
public interface ChiTietDichVuDAO {

    /**
     * Lấy danh sách dịch vụ đã dùng theo mã đặt phòng.
     */
    List<ChiTietDichVu> findByMaDatPhong(String maDatPhong);

    /**
     * Lấy danh sách dịch vụ đã dùng theo mã đặt phòng trong transaction.
     */
    List<ChiTietDichVu> findByMaDatPhong(java.sql.Connection conn, String maDatPhong);

    /**
     * Tương thích ngược: Lấy danh sách dịch vụ đã dùng theo mã hóa đơn.
     */
    default List<ChiTietDichVu> findByMaHoaDon(String maHoaDon) {
        return findByMaDatPhong(maHoaDon);
    }

    /**
     * Tương thích ngược: Lấy danh sách dịch vụ đã dùng theo mã hóa đơn trong transaction.
     */
    default List<ChiTietDichVu> findByMaHoaDon(java.sql.Connection conn, String maHoaDon) {
        return findByMaDatPhong(conn, maHoaDon);
    }

    /**
     * Lấy danh sách dịch vụ đã dùng kèm khóa dòng SELECT ... FOR UPDATE trong transaction
     * để chống Race Condition khi tính tiền quyết toán Check-out (Current Read).
     */
    default List<ChiTietDichVu> findByMaDatPhongForUpdate(java.sql.Connection conn, String maDatPhong) {
        return findByMaDatPhong(conn, maDatPhong);
    }

    /**
     * Lưu một chi tiết sử dụng dịch vụ mới.
     */
    boolean save(ChiTietDichVu ctdv);

    /**
     * Lưu một chi tiết sử dụng dịch vụ mới trong transaction.
     */
    default boolean save(java.sql.Connection conn, ChiTietDichVu ctdv) {
        return save(ctdv);
    }

    /**
     * Xóa một mục dịch vụ đã dùng theo ID.
     */
    boolean delete(Long id);

    /**
     * Xóa toàn bộ dịch vụ của một phiếu đặt phòng.
     */
    boolean deleteByMaDatPhong(String maDatPhong);

    /**
     * Tương thích ngược: Xóa toàn bộ dịch vụ của một hóa đơn.
     */
    default boolean deleteByMaHoaDon(String maHoaDon) {
        return deleteByMaDatPhong(maHoaDon);
    }
}
