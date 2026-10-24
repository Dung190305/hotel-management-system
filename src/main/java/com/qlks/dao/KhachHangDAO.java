package com.qlks.dao;

import com.qlks.model.KhachHang;

import java.util.List;
import java.util.Optional;

/**
 * Interface DAO dành cho bảng khach_hang.
 */
public interface KhachHangDAO extends BaseDAO<KhachHang, String> {

    /**
     * Tìm khách hàng theo số CCCD / Hộ chiếu.
     */
    Optional<KhachHang> findBySoCCCD(String soCCCD);

    /**
     * Tìm khách hàng theo số điện thoại.
     */
    Optional<KhachHang> findBySoDienThoai(String soDienThoai);

    /**
     * Tìm kiếm khách hàng theo từ khóa (họ tên, sđt hoặc cccd).
     */
    List<KhachHang> search(String keyword);
}
