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
     * Tìm hóa đơn theo mã trong transaction.
     */
    default Optional<HoaDon> findById(java.sql.Connection conn, String maHoaDon) {
        return findById(maHoaDon);
    }

    /**
     * Tìm hóa đơn theo mã và khóa dòng bằng SELECT ... FOR UPDATE chống thanh toán đồng thời.
     */
    default Optional<HoaDon> findByIdForUpdate(java.sql.Connection conn, String maHoaDon) {
        return findById(conn, maHoaDon);
    }

    /**
     * Tìm hóa đơn theo mã đặt phòng.
     */
    Optional<HoaDon> findByMaDatPhong(String maDatPhong);

    /**
     * Tìm hóa đơn theo mã đặt phòng trong một Transaction hiện hành.
     */
    Optional<HoaDon> findByMaDatPhong(java.sql.Connection conn, String maDatPhong);

    /**
     * Lưu hóa đơn trong một Transaction hiện hành.
     */
    boolean save(java.sql.Connection conn, HoaDon hoaDon);

    /**
     * Cập nhật thông tin hóa đơn trong transaction.
     */
    default boolean update(java.sql.Connection conn, HoaDon hoaDon) {
        return update(hoaDon);
    }

    /**
     * Cập nhật trạng thái thanh toán của hóa đơn.
     */
    boolean updateTrangThai(String maHoaDon, TrangThaiThanhToan trangThaiMoi);

    /**
     * Cập nhật trạng thái thanh toán trong một Transaction hiện hành.
     */
    boolean updateTrangThai(java.sql.Connection conn, String maHoaDon, TrangThaiThanhToan trangThaiMoi);

    /**
     * Cập nhật số tiền phòng, tiền dịch vụ, giảm giá, tiền cọc và tổng tiền hóa đơn.
     */
    boolean updateTien(String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien);

    /**
     * Tương thích ngược: Cập nhật số tiền hóa đơn khi không truyền tiền cọc riêng.
     */
    default boolean updateTien(String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tongTien) {
        return updateTien(maHoaDon, tienPhong, tienDichVu, tienGiamGia, BigDecimal.ZERO, tongTien);
    }

    /**
     * Cập nhật số tiền hóa đơn trong một Transaction hiện hành.
     */
    default boolean updateTien(java.sql.Connection conn, String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) {
        return updateTien(maHoaDon, tienPhong, tienDichVu, tienGiamGia, tienCoc, tongTien);
    }
}
