package com.qlks.dao;

import com.qlks.model.ChiTietDichVu;

import java.util.List;

/**
 * Interface DAO dành cho bảng chi_tiet_dich_vu.
 */
public interface ChiTietDichVuDAO {

    /**
     * Lấy danh sách dịch vụ đã dùng theo mã hóa đơn.
     */
    List<ChiTietDichVu> findByMaHoaDon(String maHoaDon);

    /**
     * Lưu một chi tiết sử dụng dịch vụ mới.
     */
    boolean save(ChiTietDichVu ctdv);

    /**
     * Xóa một mục dịch vụ đã dùng theo ID.
     */
    boolean delete(Long id);

    /**
     * Xóa toàn bộ dịch vụ của một hóa đơn.
     */
    boolean deleteByMaHoaDon(String maHoaDon);
}
