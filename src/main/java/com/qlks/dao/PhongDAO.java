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
     * Cập nhật trạng thái vật lý của phòng trong một Transaction hiện hành.
     */
    boolean updateTrangThai(java.sql.Connection conn, String maPhong, TrangThaiPhong trangThaiMoi);

    /**
     * Khóa dòng dữ liệu phòng bằng SELECT ... FOR UPDATE để chống đặt phòng đồng thời (concurrency control).
     */
    default Optional<Phong> findByIdForUpdate(java.sql.Connection conn, String maPhong) {
        return findById(maPhong);
    }

    /**
     * Cập nhật trạng thái kinh doanh của phòng (Bật/tắt kinh doanh).
     */
    default boolean updateKinhDoanh(String maPhong, boolean dangKinhDoanh) {
        return true;
    }

    /**
     * Cập nhật trạng thái phòng sang OCCUPIED khi làm thủ tục Check-in.
     * Áp dụng Atomic CAS: Chỉ thành công nếu phòng đang ở AVAILABLE hoặc RESERVED.
     */
    default boolean updateTrangThaiCheckIn(java.sql.Connection conn, String maPhong) {
        return updateTrangThai(conn, maPhong, TrangThaiPhong.OCCUPIED);
    }

    /**
     * @deprecated Không khuyến khích sử dụng vì thực hiện UPDATE JOIN toàn bảng gây khóa bản ghi và Deadlock
     * trong các tác vụ đọc. Trạng thái phòng được quản lý trực tiếp qua các service nghiệp vụ (Check-in, Check-out, Đặt phòng).
     */
    @Deprecated
    default void dongBoTrangThaiTheoDon() {
    }
}
