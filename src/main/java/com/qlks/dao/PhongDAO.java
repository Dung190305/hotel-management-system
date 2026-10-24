package com.qlks.dao;

import com.qlks.model.Phong;
import com.qlks.model.enums.TrangThaiPhong;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Interface DAO dành cho bảng phong.
 */
public interface PhongDAO extends BaseDAO<Phong, String> {

    /**
     * Tìm phòng theo số phòng (ví dụ: 101, 102, 301).
     */
    Optional<Phong> findBySoPhong(String soPhong);

    /**
     * Lọc danh sách phòng theo trạng thái vật lý (AVAILABLE, OCCUPIED, RESERVED, CLEANING, MAINTENANCE).
     */
    List<Phong> findByTrangThai(TrangThaiPhong trangThai);

    /**
     * Cập nhật trạng thái vật lý của phòng.
     */
    boolean updateTrangThai(String maPhong, TrangThaiPhong trangThaiMoi);

    /**
     * Tìm tất cả các phòng còn trống trong một khoảng thời gian dự kiến (không bị trùng lịch với bất kỳ booking hợp lệ nào).
     *
     * @param checkIn  Thời gian nhận phòng
     * @param checkOut Thời gian trả phòng
     * @return Danh sách các phòng sẵn sàng đặt
     */
    List<Phong> findPhongTrong(LocalDateTime checkIn, LocalDateTime checkOut);

    /**
     * Cập nhật trạng thái kinh doanh của phòng (Bật/tắt kinh doanh).
     */
    boolean updateKinhDoanh(String maPhong, boolean dangKinhDoanh);
}
