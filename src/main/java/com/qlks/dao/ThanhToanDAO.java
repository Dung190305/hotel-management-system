package com.qlks.dao;

import com.qlks.model.ThanhToan;

import java.math.BigDecimal;
import java.util.List;

/**
 * Interface DAO dành cho bảng thanh_toan.
 */
public interface ThanhToanDAO {

    /**
     * Lấy toàn bộ lịch sử các giao dịch thanh toán của một hóa đơn.
     */
    List<ThanhToan> findByMaHoaDon(String maHoaDon);

    /**
     * Lưu giao dịch thanh toán mới.
     */
    boolean save(ThanhToan thanhToan);

    /**
     * Tính tổng số tiền khách đã thanh toán cho hóa đơn đó.
     */
    BigDecimal tinhTongTienDaThanhToan(String maHoaDon);
}
