package com.qlks.dao;

import com.qlks.model.HoaDon;
import com.qlks.model.enums.TrangThaiThanhToan;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Interface DAO dành cho bảng hoa_don.
 */
public interface HoaDonDAO extends BaseDAO<HoaDon, String> {

    /**
     * Tìm hóa đơn theo mã đặt phòng.
     */
    Optional<HoaDon> findByMaDatPhong(String maDatPhong);

    /**
     * Cập nhật trạng thái thanh toán của hóa đơn.
     */
    boolean updateTrangThai(String maHoaDon, TrangThaiThanhToan trangThaiMoi);

    /**
     * Cập nhật số tiền phòng, tiền dịch vụ, giảm giá và tổng tiền hóa đơn.
     */
    boolean updateTien(String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tongTien);
}
