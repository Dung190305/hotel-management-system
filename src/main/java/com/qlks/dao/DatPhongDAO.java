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
     * Tìm phiếu đặt phòng hiện đang có khách lưu trú (DANG_O) tại phòng chỉ định.
     */
    default java.util.Optional<DatPhong> findDangOByPhong(String maPhong) {
        return findByPhong(maPhong).stream()
                .filter(dp -> dp.getTrangThai() == TrangThaiDatPhong.DANG_O)
                .findFirst();
    }

    /**
     * Tìm phiếu đặt phòng hiện đang có khách lưu trú (DANG_O) tại phòng chỉ định trong Transaction.
     */
    default java.util.Optional<DatPhong> findDangOByPhong(java.sql.Connection conn, String maPhong) {
        return findDangOByPhong(maPhong);
    }

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

    /**
     * Kiểm tra xem phòng có lượt đặt phòng nào đã xác nhận (DA_XAC_NHAN) trong ngày chỉ định hay không.
     */
    boolean hasDatPhongXacNhanTrongNgay(String maPhong, java.time.LocalDate ngay);

    /**
     * Kiểm tra xem phòng có lượt đặt phòng nào đã xác nhận (DA_XAC_NHAN) trong ngày chỉ định hay không
     * trong một Transaction hiện hành, loại trừ mã đặt phòng đang hủy để chống Dirty Read.
     */
    default boolean hasDatPhongXacNhanTrongNgay(java.sql.Connection conn, String maPhong, java.time.LocalDate ngay, String excludeMaDatPhong) {
        return hasDatPhongXacNhanTrongNgay(maPhong, ngay);
    }

    /**
     * Tìm phiếu đặt phòng theo mã trong một Transaction hiện hành.
     */
    default java.util.Optional<DatPhong> findById(java.sql.Connection conn, String maDatPhong) {
        return findById(maDatPhong);
    }

    /**
     * Khóa dòng phiếu đặt phòng bằng SELECT ... FOR UPDATE trong Transaction
     * để chống xung đột tương tranh khi Check-in, Check-out hoặc Hủy phòng.
     */
    default java.util.Optional<DatPhong> findByIdForUpdate(java.sql.Connection conn, String maDatPhong) {
        return findById(conn, maDatPhong);
    }

    /**
     * Lưu phiếu đặt phòng trong một Transaction hiện hành.
     */
    default boolean save(java.sql.Connection conn, DatPhong datPhong) {
        return save(datPhong);
    }

    /**
     * Cập nhật trạng thái đặt phòng trong một Transaction hiện hành.
     */
    default boolean updateTrangThai(java.sql.Connection conn, String maDatPhong, TrangThaiDatPhong trangThaiMoi) {
        return updateTrangThai(maDatPhong, trangThaiMoi);
    }

    /**
     * Xác nhận Check-in trong một Transaction hiện hành kèm ghi nhận nhân viên thực hiện (Audit Trail).
     */
    default boolean xacNhanCheckIn(java.sql.Connection conn, String maDatPhong, LocalDateTime thoiGianCheckInThucTe, String maNvCheckIn) {
        return xacNhanCheckIn(conn, maDatPhong, thoiGianCheckInThucTe);
    }

    /**
     * Xác nhận Check-in trong một Transaction hiện hành.
     */
    default boolean xacNhanCheckIn(java.sql.Connection conn, String maDatPhong, LocalDateTime thoiGianCheckInThucTe) {
        return xacNhanCheckIn(maDatPhong, thoiGianCheckInThucTe);
    }

    /**
     * Xác nhận Check-out trong một Transaction hiện hành kèm ghi nhận nhân viên thực hiện (Audit Trail).
     */
    default boolean xacNhanCheckOut(java.sql.Connection conn, String maDatPhong, LocalDateTime thoiGianCheckOutThucTe, String maNvCheckOut) {
        return xacNhanCheckOut(conn, maDatPhong, thoiGianCheckOutThucTe);
    }

    /**
     * Xác nhận Check-out trong một Transaction hiện hành.
     */
    default boolean xacNhanCheckOut(java.sql.Connection conn, String maDatPhong, LocalDateTime thoiGianCheckOutThucTe) {
        return xacNhanCheckOut(maDatPhong, thoiGianCheckOutThucTe);
    }

    /**
     * Kiểm tra trùng lịch phòng với khóa dòng FOR UPDATE trong Transaction để chống đặt phòng đồng thời.
     */
    boolean isPhongBiTrungLich(java.sql.Connection conn, String maPhong, LocalDateTime ngayNhanMoi, LocalDateTime ngayTraMoi, String excludeMaDatPhong);

    /**
     * Kiểm tra xem phòng hiện tại có đơn đặt phòng nào khác đang ở trạng thái DANG_O hay không.
     * Sử dụng trong transaction Check-in để chống hai đơn khác nhau cùng Check-in vào 1 phòng.
     */
    default boolean isPhongDangCoNguoiO(java.sql.Connection conn, String maPhong, String excludeMaDatPhong) {
        return false;
    }
}
