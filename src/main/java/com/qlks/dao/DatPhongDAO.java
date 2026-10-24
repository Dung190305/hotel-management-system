package com.qlks.dao;

import com.qlks.model.DatPhong;
import com.qlks.model.enums.TrangThaiDatPhong;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interface DAO dành cho bảng dat_phong.
 */
public interface DatPhongDAO extends BaseDAO<DatPhong, String> {

    /**
     * Lấy danh sách đặt phòng theo mã khách hàng.
     */
    List<DatPhong> findByKhachHang(String maKhachHang);

    /**
     * Lấy danh sách đặt phòng theo mã phòng.
     */
    List<DatPhong> findByPhong(String maPhong);

    /**
     * Lấy danh sách đặt phòng theo trạng thái (ví dụ: DANG_O cho màn hình check-out).
     */
    List<DatPhong> findByTrangThai(TrangThaiDatPhong trangThai);

    /**
     * Cập nhật trạng thái đặt phòng.
     */
    boolean updateTrangThai(String maDatPhong, TrangThaiDatPhong trangThaiMoi);

    /**
     * Cập nhật thời gian nhận phòng thực tế khi làm thủ tục Check-in.
     */
    boolean xacNhanCheckIn(String maDatPhong, LocalDateTime thoiGianCheckInThucTe);

    /**
     * Cập nhật thời gian trả phòng thực tế khi làm thủ tục Check-out.
     */
    boolean xacNhanCheckOut(String maDatPhong, LocalDateTime thoiGianCheckOutThucTe);

    /**
     * Kiểm tra xem phòng có bị trùng lịch trong khoảng thời gian [ngayNhanMoi, ngayTraMoi] hay không.
     * Quy tắc nghiệp vụ chống trùng phòng:
     * Hai khoảng thời gian trùng nhau nếu:
     * ngayNhanMoi < ngayTraCu AND ngayTraMoi > ngayNhanCu
     * (Không tính các booking có trạng thái DA_HUY hoặc DA_TRA_PHONG).
     *
     * @param maPhong            Mã phòng cần kiểm tra
     * @param ngayNhanMoi        Ngày giờ nhận phòng mới
     * @param ngayTraMoi         Ngày giờ trả phòng mới
     * @param excludeMaDatPhong  Mã đặt phòng cần bỏ qua khi cập nhật (có thể null nếu tạo mới)
     * @return true nếu BỊ TRÙNG (không thể đặt), false nếu PHÒNG TRỐNG (có thể đặt)
     */
    boolean isPhongBiTrungLich(String maPhong, LocalDateTime ngayNhanMoi, LocalDateTime ngayTraMoi, String excludeMaDatPhong);
}
