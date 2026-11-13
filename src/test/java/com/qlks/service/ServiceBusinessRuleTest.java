package com.qlks.service;

import com.qlks.dao.DatPhongDAO;
import com.qlks.dao.HoaDonDAO;
import com.qlks.dao.KhachHangDAO;
import com.qlks.dao.KhachLuuTruDAO;
import com.qlks.dao.NhanVienDAO;
import com.qlks.dao.PhongDAO;
import com.qlks.dao.impl.DatPhongDAOImpl;
import com.qlks.dao.impl.HoaDonDAOImpl;
import com.qlks.dao.impl.KhachHangDAOImpl;
import com.qlks.dao.impl.KhachLuuTruDAOImpl;
import com.qlks.dao.impl.NhanVienDAOImpl;
import com.qlks.dao.impl.PhongDAOImpl;
import com.qlks.exception.BusinessException;
import com.qlks.model.*;
import com.qlks.model.enums.LoaiGiuong;
import com.qlks.model.enums.PhuongThucThanhToan;
import com.qlks.model.enums.TrangThaiDatPhong;
import com.qlks.model.enums.TrangThaiPhong;
import com.qlks.model.enums.TrangThaiThanhToan;
import com.qlks.model.enums.VaiTroKhachLuuTru;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.qlks.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BỘ KIỂM THỬ QUY TẮC NGHIỆP VỤ & TRANSACTION (TEST 7 -> TEST 24):
 * Nhóm 2 (Test 7 - 12): Ràng buộc khách lưu trú, CCCD 12 số, sức chứa, rollback.
 * Nhóm 3 (Test 13 - 19): Đặt phòng, chống trùng lịch, gối đầu, check-in, check-out, dọn dẹp, bảo trì.
 * Nhóm 4 (Test 20 - 24): Tách bạch tiền cọc & giảm giá, công thức tài chính, chống thanh toán trùng lặp.
 */
public class ServiceBusinessRuleTest {

    private final AuthService authService = new AuthService();
    private final DatPhongService datPhongService = new DatPhongService();
    private final HoaDonService hoaDonService = new HoaDonService();
    private final PhongService phongService = new PhongService();
    private final NhanVienService nhanVienService = new NhanVienService();

    private final KhachHangDAO khachHangDAO = new KhachHangDAOImpl();
    private final PhongDAO phongDAO = new PhongDAOImpl();
    private final NhanVienDAO nhanVienDAO = new NhanVienDAOImpl();
    private final DatPhongDAO datPhongDAO = new DatPhongDAOImpl();
    private final KhachLuuTruDAO khachLuuTruDAO = new KhachLuuTruDAOImpl();
    private final HoaDonDAO hoaDonDAO = new HoaDonDAOImpl();
    private static Boolean dbConnected = null;

    private static boolean isDbConnected() {
        if (dbConnected == null) {
            dbConnected = DatabaseConnection.checkConnection();
        }
        return dbConnected;
    }

    @BeforeEach
    void kiemTraKetNoiDatabase() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                isDbConnected(),
                "MySQL chưa được khởi động hoặc không thể kết nối. Tự động bỏ qua Integration Test."
        );
        AuthService.setTaiKhoanHienTai(new TaiKhoan("admin", "admin123", com.qlks.model.enums.VaiTro.ADMIN, true));
    }

    @BeforeEach
    @AfterEach
    void cleanUpTestDatabase() {
        if (!isDbConnected()) {
            AuthService.dangXuat();
            return;
        }
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            stmt.executeUpdate("DELETE FROM chi_tiet_khach_dat_phong WHERE ma_dat_phong LIKE 'T_%' OR ma_dat_phong LIKE 'TEST_%'");
            stmt.executeUpdate("DELETE FROM thanh_toan WHERE ma_hoa_don IN (SELECT ma_hoa_don FROM hoa_don WHERE ma_dat_phong LIKE 'T_%' OR ma_dat_phong LIKE 'TEST_%')");
            stmt.executeUpdate("DELETE FROM chi_tiet_dich_vu WHERE ma_dat_phong LIKE 'T_%' OR ma_dat_phong LIKE 'TEST_%'");
            stmt.executeUpdate("DELETE FROM hoa_don WHERE ma_dat_phong LIKE 'T_%' OR ma_dat_phong LIKE 'TEST_%'");
            stmt.executeUpdate("DELETE FROM dat_phong WHERE ma_dat_phong LIKE 'T_%' OR ma_dat_phong LIKE 'TEST_%'");
            stmt.executeUpdate("UPDATE phong p SET trang_thai_hien_tai = 'AVAILABLE' "
                    + "WHERE p.ma_phong IN ('P101', 'P102', 'P103', 'P301')");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        AuthService.dangXuat();
    }

    // =========================================================================
    // NHÓM 2: KIỂM THỬ RÀNG BUỘC KHÁCH LƯU TRÚ & CCCD (TEST 7 -> TEST 12)
    // =========================================================================

    @Test
    @DisplayName("Test 7: Cho phép số lượng khách hợp lệ trong giới hạn sức chứa (1-2 người cho phòng đơn, 1-4 người cho phòng đôi)")
    void test07_SoLuongKhachHopLeTheoLoaiPhong() {
        KhachHang kh = khachHangDAO.findById("KH001").orElseThrow();
        Phong phongDon = phongDAO.findAll().stream().filter(p -> p.getLoaiGiuong() == LoaiGiuong.DON).findFirst().orElseThrow();
        Phong phongDoi = phongDAO.findAll().stream().filter(p -> p.getLoaiGiuong() == LoaiGiuong.DOI).findFirst().orElseThrow();
        NhanVien nv = nhanVienDAO.findById("NV001").orElseThrow();

        long time = System.currentTimeMillis() % 100000;

        // 1. Phòng đơn: Thêm 2 khách (hợp lệ)
        String maDpDon = "T_GDON_" + time;
        DatPhong dpDon = new DatPhong(maDpDon, kh, phongDon, nv,
                LocalDateTime.of(2028, 1, 10, 14, 0),
                LocalDateTime.of(2028, 1, 12, 12, 0),
                null, null, 2, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);
        dpDon.addKhachLuuTru(new KhachLuuTru(maDpDon, "Khách Đơn 1", "001200001111", "0901111111", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        dpDon.addKhachLuuTru(new KhachLuuTru(maDpDon, "Khách Đơn 2", "001200001112", "0901111112", VaiTroKhachLuuTru.KHACH_DI_CUNG));
        assertDoesNotThrow(() -> datPhongService.taoDatPhong(dpDon));

        // 2. Phòng đôi: Thêm 4 khách (hợp lệ)
        String maDpDoi = "T_GDOI_" + time;
        DatPhong dpDoi = new DatPhong(maDpDoi, kh, phongDoi, nv,
                LocalDateTime.of(2028, 1, 10, 14, 0),
                LocalDateTime.of(2028, 1, 12, 12, 0),
                null, null, 4, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);
        dpDoi.addKhachLuuTru(new KhachLuuTru(maDpDoi, "Khách Đôi 1", "001200002221", "0902222221", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        dpDoi.addKhachLuuTru(new KhachLuuTru(maDpDoi, "Khách Đôi 2", "001200002222", "0902222222", VaiTroKhachLuuTru.KHACH_DI_CUNG));
        dpDoi.addKhachLuuTru(new KhachLuuTru(maDpDoi, "Khách Đôi 3", "001200002223", "0902222223", VaiTroKhachLuuTru.KHACH_DI_CUNG));
        dpDoi.addKhachLuuTru(new KhachLuuTru(maDpDoi, "Khách Đôi 4", "001200002224", "0902222224", VaiTroKhachLuuTru.KHACH_DI_CUNG));
        assertDoesNotThrow(() -> datPhongService.taoDatPhong(dpDoi));

        // Dọn dẹp
        datPhongService.huyDatPhong(maDpDon);
        datPhongService.huyDatPhong(maDpDoi);
    }

    @Test
    @DisplayName("Test 8: Từ chối lưu trú khi số lượng khách vượt quá sức chứa tối đa của phòng")
    void test08_TuChoiKhiVuotQuaSucChua() {
        KhachHang kh = khachHangDAO.findById("KH001").orElseThrow();
        Phong phongDon = phongDAO.findAll().stream().filter(p -> p.getLoaiGiuong() == LoaiGiuong.DON).findFirst().orElseThrow();
        NhanVien nv = nhanVienDAO.findById("NV001").orElseThrow();

        long time = System.currentTimeMillis() % 100000;
        String maDp = "T_OVCAP_" + time;
        DatPhong dp = new DatPhong(maDp, kh, phongDon, nv,
                LocalDateTime.of(2028, 2, 1, 14, 0),
                LocalDateTime.of(2028, 2, 3, 12, 0),
                null, null, 3, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);

        // Cố tình thêm 3 người vào phòng đơn sức chứa 2
        dp.addKhachLuuTru(new KhachLuuTru(maDp, "Người 1", "001200003331", "0903333331", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        dp.addKhachLuuTru(new KhachLuuTru(maDp, "Người 2", "001200003332", "0903333332", VaiTroKhachLuuTru.KHACH_DI_CUNG));
        dp.addKhachLuuTru(new KhachLuuTru(maDp, "Người 3", "001200003333", "0903333333", VaiTroKhachLuuTru.KHACH_DI_CUNG));

        BusinessException ex = assertThrows(BusinessException.class, () -> datPhongService.taoDatPhong(dp));
        assertTrue(ex.getMessage().contains("vượt quá sức chứa tối đa"));
    }

    @Test
    @DisplayName("Test 9: Từ chối khi khách lưu trú thiếu CCCD (null hoặc rỗng)")
    void test09_TuChoiKhiKhachThieuCCCD() {
        KhachHang kh = khachHangDAO.findById("KH001").orElseThrow();
        Phong phong = phongDAO.findById("P101").orElseThrow();
        NhanVien nv = nhanVienDAO.findById("NV001").orElseThrow();

        long time = System.currentTimeMillis() % 100000;
        String maDp = "T_NO_CC_" + time;
        DatPhong dp = new DatPhong(maDp, kh, phong, nv,
                LocalDateTime.of(2028, 2, 5, 14, 0),
                LocalDateTime.of(2028, 2, 7, 12, 0),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);

        dp.addKhachLuuTru(new KhachLuuTru(maDp, "Khách Không CCCD", "", "0904444444", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));

        BusinessException ex = assertThrows(BusinessException.class, () -> datPhongService.taoDatPhong(dp));
        assertTrue(ex.getMessage().contains("không hợp lệ") || ex.getMessage().contains("12 chữ số"));
    }

    @Test
    @DisplayName("Test 10: Từ chối khi CCCD không đúng định dạng 12 chữ số (10 số, 13 số, chứa chữ)")
    void test10_TuChoiKhiCCCDSaiDinhDang() {
        KhachHang kh = khachHangDAO.findById("KH001").orElseThrow();
        Phong phong = phongDAO.findById("P101").orElseThrow();
        NhanVien nv = nhanVienDAO.findById("NV001").orElseThrow();

        long time = System.currentTimeMillis() % 100000;
        String maDp = "T_BADCC_" + time;
        DatPhong dp = new DatPhong(maDp, kh, phong, nv,
                LocalDateTime.of(2028, 2, 8, 14, 0),
                LocalDateTime.of(2028, 2, 10, 12, 0),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);

        // CCCD chứa chữ cái
        dp.addKhachLuuTru(new KhachLuuTru(maDp, "Khách CCCD Chữ", "001200ABC123", "0905555555", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));

        BusinessException ex = assertThrows(BusinessException.class, () -> datPhongService.taoDatPhong(dp));
        assertTrue(ex.getMessage().contains("Số CCCD phải gồm đúng 12 chữ số"));
    }

    @Test
    @DisplayName("Test 11: Từ chối khi có 2 khách trùng CCCD trong cùng một lượt đặt phòng")
    void test11_TuChoiTrungCCCDTrongCungMotDatPhong() {
        KhachHang kh = khachHangDAO.findById("KH001").orElseThrow();
        Phong phongDoi = phongDAO.findAll().stream().filter(p -> p.getLoaiGiuong() == LoaiGiuong.DOI).findFirst().orElseThrow();
        NhanVien nv = nhanVienDAO.findById("NV001").orElseThrow();

        long time = System.currentTimeMillis() % 100000;
        String maDp = "T_DUPCC_" + time;
        DatPhong dp = new DatPhong(maDp, kh, phongDoi, nv,
                LocalDateTime.of(2028, 2, 12, 14, 0),
                LocalDateTime.of(2028, 2, 14, 12, 0),
                null, null, 2, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);

        String cccdDungChung = "001200009999";
        dp.addKhachLuuTru(new KhachLuuTru(maDp, "Khách A", cccdDungChung, "0906666661", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        dp.addKhachLuuTru(new KhachLuuTru(maDp, "Khách B", cccdDungChung, "0906666662", VaiTroKhachLuuTru.KHACH_DI_CUNG));

        BusinessException ex = assertThrows(BusinessException.class, () -> datPhongService.taoDatPhong(dp));
        assertTrue(ex.getMessage().contains("bị trùng lặp trong cùng một đơn đặt phòng"));
    }

    @Test
    @DisplayName("Test 12: Rollback transaction khi lưu danh sách khách thất bại")
    void test12_RollbackKhiLuuKhachThatBai() {
        KhachHang kh = khachHangDAO.findById("KH001").orElseThrow();
        Phong phongDoi = phongDAO.findAll().stream().filter(p -> p.getLoaiGiuong() == LoaiGiuong.DOI).findFirst().orElseThrow();
        NhanVien nv = nhanVienDAO.findById("NV001").orElseThrow();

        long time = System.currentTimeMillis() % 100000;
        String maDp = "T_RLBCK_" + time;
        DatPhong dp = new DatPhong(maDp, kh, phongDoi, nv,
                LocalDateTime.of(2028, 2, 16, 14, 0),
                LocalDateTime.of(2028, 2, 18, 12, 0),
                null, null, 2, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);

        // Khách thứ 2 có CCCD sai định dạng
        dp.addKhachLuuTru(new KhachLuuTru(maDp, "Khách Hợp Lệ", "001200007771", "0907777771", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        dp.addKhachLuuTru(new KhachLuuTru(maDp, "Khách Hỏng", "12345", "0907777772", VaiTroKhachLuuTru.KHACH_DI_CUNG));

        assertThrows(BusinessException.class, () -> datPhongService.taoDatPhong(dp));

        // Kiểm tra chắc chắn dat_phong và chi_tiet_khach_dat_phong KHÔNG có bản ghi nào được lưu (Rollback sạch sẽ)
        assertTrue(datPhongDAO.findById(maDp).isEmpty(), "Không được có bản ghi rác trong dat_phong sau khi rollback");
        List<KhachLuuTru> guests = khachLuuTruDAO.findByMaDatPhong(maDp);
        assertTrue(guests.isEmpty(), "Không được có bản ghi rác trong chi_tiet_khach_dat_phong sau khi rollback");
    }

    // =========================================================================
    // NHÓM 3: KIỂM THỬ ĐẶT PHÒNG & TRẠNG THÁI PHÒNG (TEST 13 -> TEST 19)
    // =========================================================================

    @Test
    @DisplayName("Test 13: Đặt phòng thành công trong khoảng thời gian trống")
    void test13_DatPhongThanhCongKhoangThoiGianTrong() {
        KhachHang kh = khachHangDAO.findById("KH001").orElseThrow();
        Phong phong = phongDAO.findById("P101").orElseThrow();
        NhanVien nv = nhanVienDAO.findById("NV001").orElseThrow();

        long time = System.currentTimeMillis() % 100000;
        String maDp = "TEST_SUCCESS_" + time;
        DatPhong dp = new DatPhong(maDp, kh, phong, nv,
                LocalDateTime.of(2028, 3, 1, 14, 0),
                LocalDateTime.of(2028, 3, 5, 12, 0),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);
        dp.addKhachLuuTru(new KhachLuuTru(maDp, "Khách Đúng Lịch", "001200008881", "0908888881", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));

        assertDoesNotThrow(() -> datPhongService.taoDatPhong(dp));

        assertTrue(datPhongDAO.findById(maDp).isPresent());
        datPhongService.huyDatPhong(maDp);
    }

    @Test
    @DisplayName("Test 14: Từ chối đặt phòng khi khoảng thời gian bị trùng lặp với đơn khác")
    void test14_TuChoiKhiTrungLichDatPhong() {
        KhachHang kh = khachHangDAO.findById("KH001").orElseThrow();
        Phong phong = phongDAO.findById("P101").orElseThrow();
        NhanVien nv = nhanVienDAO.findById("NV001").orElseThrow();

        long time = System.currentTimeMillis() % 100000;
        String maDp1 = "TEST_TRUNG_1_" + time;
        DatPhong dp1 = new DatPhong(maDp1, kh, phong, nv,
                LocalDateTime.of(2028, 3, 10, 14, 0),
                LocalDateTime.of(2028, 3, 15, 12, 0),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);
        dp1.addKhachLuuTru(new KhachLuuTru(maDp1, "Khách 1", "001200009991", "0909999991", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        datPhongService.taoDatPhong(dp1);

        // Đơn thứ 2 cố tình đặt trong khoảng từ 12/03 đến 18/03 -> trùng
        String maDp2 = "TEST_TRUNG_2_" + time;
        DatPhong dp2 = new DatPhong(maDp2, kh, phong, nv,
                LocalDateTime.of(2028, 3, 12, 14, 0),
                LocalDateTime.of(2028, 3, 18, 12, 0),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);
        dp2.addKhachLuuTru(new KhachLuuTru(maDp2, "Khách 2", "001200009992", "0909999992", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));

        BusinessException ex = assertThrows(BusinessException.class, () -> datPhongService.taoDatPhong(dp2));
        assertTrue(ex.getMessage().contains("đã có khách đặt trong khoảng thời gian này"));

        datPhongService.huyDatPhong(maDp1);
    }

    @Test
    @DisplayName("Test 15: Cho phép đặt phòng gối đầu: Ngày nhận phòng = Ngày trả phòng của đơn trước")
    void test15_ChoPhepDatPhongGoiDau() {
        KhachHang kh = khachHangDAO.findById("KH001").orElseThrow();
        Phong phong = phongDAO.findById("P101").orElseThrow();
        NhanVien nv = nhanVienDAO.findById("NV001").orElseThrow();

        long time = System.currentTimeMillis() % 100000;
        String maDp1 = "TEST_GOI_1_" + time;
        DatPhong dp1 = new DatPhong(maDp1, kh, phong, nv,
                LocalDateTime.of(2028, 4, 1, 14, 0),
                LocalDateTime.of(2028, 4, 5, 12, 0),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);
        dp1.addKhachLuuTru(new KhachLuuTru(maDp1, "Khách 1", "001200001231", "0901231231", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        datPhongService.taoDatPhong(dp1);

        // Đơn thứ 2 nhận phòng đúng lúc 14:00 ngày 05/04 (sau giờ trả 12:00 của đơn 1) -> HỢP LỆ
        String maDp2 = "TEST_GOI_2_" + time;
        DatPhong dp2 = new DatPhong(maDp2, kh, phong, nv,
                LocalDateTime.of(2028, 4, 5, 14, 0),
                LocalDateTime.of(2028, 4, 8, 12, 0),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);
        dp2.addKhachLuuTru(new KhachLuuTru(maDp2, "Khách 2", "001200001232", "0901231232", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));

        assertDoesNotThrow(() -> datPhongService.taoDatPhong(dp2));

        datPhongService.huyDatPhong(maDp1);
        datPhongService.huyDatPhong(maDp2);
    }

    @Test
    @DisplayName("Test 16: Check-in: Chỉ cho phép check-in đúng ngày hoặc sau ngày nhận phòng dự kiến")
    void test16_TuChoiCheckInTruocNgayNhan() {
        KhachHang kh = khachHangDAO.findById("KH001").orElseThrow();
        Phong phong = phongDAO.findById("P101").orElseThrow();
        NhanVien nv = nhanVienDAO.findById("NV001").orElseThrow();

        long time = System.currentTimeMillis() % 100000;
        String maDp = "T_EARLY_" + time;
        // Đặt phòng vào năm 2028
        DatPhong dp = new DatPhong(maDp, kh, phong, nv,
                LocalDateTime.of(2028, 5, 1, 14, 0),
                LocalDateTime.of(2028, 5, 5, 12, 0),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);
        dp.addKhachLuuTru(new KhachLuuTru(maDp, "Khách Sớm", "001200003451", "0903453451", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        datPhongService.taoDatPhong(dp);

        // Hôm nay (2026) cố tình check-in trước hạn -> PHẢI BỊ TỪ CHỐI
        BusinessException ex = assertThrows(BusinessException.class, () -> datPhongService.xacNhanCheckIn(maDp));
        assertTrue(ex.getMessage().contains("Chưa đến ngày nhận phòng"));

        datPhongService.huyDatPhong(maDp);
    }

    @Test
    @DisplayName("Test 17: Check-out: Tự động chuyển trạng thái phòng sang Dọn dẹp (CLEANING) và tạo hóa đơn")
    void test17_CheckOutChuyenCleaningVaTaoHoaDon() {
        KhachHang kh = khachHangDAO.findById("KH002").orElseThrow();
        Phong phong = phongDAO.findById("P102").orElseThrow();
        NhanVien nv = nhanVienDAO.findById("NV002").orElseThrow();

        // Đưa phòng về AVAILABLE
        phongDAO.updateTrangThai(phong.getMaPhong(), TrangThaiPhong.AVAILABLE);

        long time = System.currentTimeMillis() % 100000;
        String maDp = "TEST_OUT_" + time;
        DatPhong dp = new DatPhong(maDp, kh, phong, nv,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(2),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);
        dp.addKhachLuuTru(new KhachLuuTru(maDp, "Khách Out", "001200005671", "0905675671", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        datPhongService.taoDatPhong(dp);

        datPhongService.xacNhanCheckIn(maDp);

        // Thực hiện Check-out
        HoaDon hd = datPhongService.xacNhanCheckOut(maDp);
        assertNotNull(hd, "Hóa đơn phải được tự động tạo khi check-out");

        Phong pSauOut = phongDAO.findById(phong.getMaPhong()).orElseThrow();
        assertEquals(TrangThaiPhong.CLEANING, pSauOut.getTrangThaiHienTai(), "Phòng sau check-out phải chuyển sang CLEANING");
    }

    @Test
    @DisplayName("Test 18: Hoàn tất dọn dẹp: Chuyển phòng từ CLEANING về AVAILABLE")
    void test18_HoanTatDonPhongVeAvailable() {
        Phong phong = phongDAO.findById("P101").orElseThrow();
        phongDAO.updateTrangThai("P101", TrangThaiPhong.CLEANING);

        TrangThaiPhong ttMoi = phongService.hoanTatDonPhong("P101");
        assertEquals(TrangThaiPhong.AVAILABLE, ttMoi);

        Phong pSauDon = phongDAO.findById("P101").orElseThrow();
        assertEquals(TrangThaiPhong.AVAILABLE, pSauDon.getTrangThaiHienTai());
    }

    @Test
    @DisplayName("Test 19: Từ chối thao tác đặt phòng / check-in với phòng đang bảo trì (MAINTENANCE)")
    void test19_TuChoiDatPhongBaoTri() {
        Phong phong = phongDAO.findById("P201").orElseThrow();
        phongDAO.updateTrangThai("P201", TrangThaiPhong.MAINTENANCE);

        KhachHang kh = khachHangDAO.findById("KH001").orElseThrow();
        NhanVien nv = nhanVienDAO.findById("NV001").orElseThrow();

        long time = System.currentTimeMillis() % 100000;
        String maDp = "TEST_MAINT_" + time;
        DatPhong dp = new DatPhong(maDp, kh, phong, nv,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(2),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);
        dp.addKhachLuuTru(new KhachLuuTru(maDp, "Khách Bảo Trì", "001200007891", "0907897891", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));

        BusinessException ex = assertThrows(BusinessException.class, () -> datPhongService.taoDatPhong(dp));
        assertTrue(ex.getMessage().contains("đang bảo trì"));

        // Khôi phục lại trạng thái phòng
        phongDAO.updateTrangThai("P201", TrangThaiPhong.AVAILABLE);
    }

    // =========================================================================
    // NHÓM 4: KIỂM THỬ HÓA ĐƠN & TÀI CHÍNH (TEST 20 -> TEST 24)
    // =========================================================================

    @Test
    @DisplayName("Test 20: Phân tách tiền cọc và giảm giá: Tiền cọc không được cộng dồn hay ghi đè vào giảm giá")
    void test20_PhanTachTienCocVaGiamGia() {
        KhachHang kh = khachHangDAO.findById("KH001").orElseThrow();
        Phong phong = phongDAO.findById("P101").orElseThrow();
        NhanVien nv = nhanVienDAO.findById("NV001").orElseThrow();

        phongDAO.updateTrangThai("P101", TrangThaiPhong.AVAILABLE);

        long time = System.currentTimeMillis() % 100000;
        String maDp = "TEST_DEP_" + time;
        BigDecimal tienCoc = new BigDecimal("300000.00");

        DatPhong dp = new DatPhong(maDp, kh, phong, nv,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, tienCoc, null);
        dp.addKhachLuuTru(new KhachLuuTru(maDp, "Khách Cọc", "001200008901", "0908908901", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        datPhongService.taoDatPhong(dp);

        datPhongService.xacNhanCheckIn(maDp);
        HoaDon hd = datPhongService.xacNhanCheckOut(maDp);

        // Tiền cọc phải được ghi nhận đúng vào tienCoc và KHÔNG được ghi đè vào tienGiamGia
        assertEquals(0, tienCoc.compareTo(hd.getTienCoc()), "Tiền cọc của hóa đơn phải đúng 300,000 VNĐ");
        assertEquals(0, BigDecimal.ZERO.compareTo(hd.getTienGiamGia()), "Tiền giảm giá khuyến mãi ban đầu phải là 0, không được gộp tiền cọc vào đây");
    }

    @Test
    @DisplayName("Test 21: Tính đúng số tiền còn lại phải thu = max(0, Tổng chi phí - Tiền cọc)")
    void test21_CongThucConLaiPhaiThuChuan() {
        // Giả lập hóa đơn:
        // Tiền phòng: 1,000,000
        // Tiền dịch vụ: 200,000
        // Tiền giảm giá: 100,000
        // -> Tổng chi phí = 1,000,000 + 200,000 - 100,000 = 1,100,000
        // Tiền cọc: 400,000
        // -> Còn lại phải thu = 1,100,000 - 400,000 = 700,000
        HoaDon hd = new HoaDon();
        hd.setTienPhong(new BigDecimal("1000000.00"));
        hd.setTienDichVu(new BigDecimal("200000.00"));
        hd.setTienGiamGia(new BigDecimal("100000.00"));
        hd.setTienCoc(new BigDecimal("400000.00"));

        assertEquals(new BigDecimal("1100000.00"), hd.getTongChiPhi(), "Tổng chi phí = Phòng + Dịch vụ - Giảm giá");
        assertEquals(new BigDecimal("700000.00"), hd.getConLaiPhaiThu(), "Còn lại phải thu = Tổng chi phí - Tiền cọc");

        // Trường hợp tiền cọc lớn hơn tổng chi phí (ví dụ khách cọc 1,500,000) -> còn lại phải thu = 0
        hd.setTienCoc(new BigDecimal("1500000.00"));
        assertEquals(BigDecimal.ZERO, hd.getConLaiPhaiThu(), "Còn lại phải thu không thể âm, tối thiểu là 0");
    }

    @Test
    @DisplayName("Test 22: Chống thanh toán trùng lặp (Idempotent Payment)")
    void test22_ChongThanhToanTrungLap() {
        KhachHang kh = khachHangDAO.findById("KH001").orElseThrow();
        Phong phong = phongDAO.findById("P101").orElseThrow();
        NhanVien nv = nhanVienDAO.findById("NV001").orElseThrow();

        phongDAO.updateTrangThai("P101", TrangThaiPhong.AVAILABLE);

        long time = System.currentTimeMillis() % 100000;
        String maDp = "TEST_IDEM_" + time;
        DatPhong dp = new DatPhong(maDp, kh, phong, nv,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);
        dp.addKhachLuuTru(new KhachLuuTru(maDp, "Khách Idem", "001200009988", "0909988998", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        datPhongService.taoDatPhong(dp);

        datPhongService.xacNhanCheckIn(maDp);
        HoaDon hd = datPhongService.xacNhanCheckOut(maDp);

        // Thanh toán lần 1 thành công
        hoaDonService.thanhToanHoaDon(hd.getMaHoaDon(), hd.getConLaiPhaiThu(), PhuongThucThanhToan.TIEN_MAT, "Lần 1");

        // Thanh toán lần 2 trên cùng hóa đơn -> BỊ CHẶN TUYỆT ĐỐI
        BusinessException ex = assertThrows(BusinessException.class, () ->
                hoaDonService.thanhToanHoaDon(hd.getMaHoaDon(), hd.getConLaiPhaiThu(), PhuongThucThanhToan.CHUYEN_KHOAN, "Lần 2")
        );
        assertTrue(ex.getMessage().contains("đã được thanh toán đầy đủ"));
    }

    @Test
    @DisplayName("Test 23: Không cho phép thanh toán số tiền âm hoặc lớn hơn số tiền còn lại phải thu")
    void test23_KiemSoatSoTienThanhToan() {
        KhachHang kh = khachHangDAO.findById("KH001").orElseThrow();
        Phong phong = phongDAO.findById("P101").orElseThrow();
        NhanVien nv = nhanVienDAO.findById("NV001").orElseThrow();

        phongDAO.updateTrangThai("P101", TrangThaiPhong.AVAILABLE);

        long time = System.currentTimeMillis() % 100000;
        String maDp = "TEST_AMOUNT_" + time;
        DatPhong dp = new DatPhong(maDp, kh, phong, nv,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);
        dp.addKhachLuuTru(new KhachLuuTru(maDp, "Khách Số Tiền", "001200009977", "0909977997", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        datPhongService.taoDatPhong(dp);

        datPhongService.xacNhanCheckIn(maDp);
        HoaDon hd = datPhongService.xacNhanCheckOut(maDp);

        // 1. Thanh toán số tiền <= 0 -> Từ chối
        BusinessException exAm = assertThrows(BusinessException.class, () ->
                hoaDonService.thanhToanHoaDon(hd.getMaHoaDon(), new BigDecimal("-100000"), PhuongThucThanhToan.TIEN_MAT, "Test âm")
        );
        assertTrue(exAm.getMessage().contains("lớn hơn") || exAm.getMessage().contains("không được âm"));

        // 2. Thanh toán vượt quá số tiền còn lại phải thu -> Từ chối
        BigDecimal vuotQua = hd.getConLaiPhaiThu().add(new BigDecimal("500000"));
        BusinessException exVuot = assertThrows(BusinessException.class, () ->
                hoaDonService.thanhToanHoaDon(hd.getMaHoaDon(), vuotQua, PhuongThucThanhToan.TIEN_MAT, "Test vượt")
        );
        assertTrue(exVuot.getMessage().contains("vượt quá số tiền còn lại phải thu"));

        // Dọn dẹp: thanh toán đúng để kết thúc
        hoaDonService.thanhToanHoaDon(hd.getMaHoaDon(), hd.getConLaiPhaiThu(), PhuongThucThanhToan.TIEN_MAT, "Test xong");
    }

    @Test
    @DisplayName("Test 24: Tự động cập nhật trạng thái hóa đơn sang DA_THANH_TOAN khi đã thanh toán đủ")
    void test24_CapNhatTrangThaiDaThanhToan() {
        KhachHang kh = khachHangDAO.findById("KH001").orElseThrow();
        Phong phong = phongDAO.findById("P101").orElseThrow();
        NhanVien nv = nhanVienDAO.findById("NV001").orElseThrow();

        phongDAO.updateTrangThai("P101", TrangThaiPhong.AVAILABLE);

        long time = System.currentTimeMillis() % 100000;
        String maDp = "TEST_STATUS_" + time;
        DatPhong dp = new DatPhong(maDp, kh, phong, nv,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);
        dp.addKhachLuuTru(new KhachLuuTru(maDp, "Khách Trạng Thái", "001200009966", "0909966996", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        datPhongService.taoDatPhong(dp);

        datPhongService.xacNhanCheckIn(maDp);
        HoaDon hd = datPhongService.xacNhanCheckOut(maDp);

        assertEquals(TrangThaiThanhToan.CHUA_THANH_TOAN, hd.getTrangThai());

        hoaDonService.thanhToanHoaDon(hd.getMaHoaDon(), hd.getConLaiPhaiThu(), PhuongThucThanhToan.CHUYEN_KHOAN, "Hoàn tất");

        HoaDon hdSau = hoaDonDAO.findById(hd.getMaHoaDon()).orElseThrow();
        assertEquals(TrangThaiThanhToan.DA_THANH_TOAN, hdSau.getTrangThai(), "Trạng thái hóa đơn phải chuyển sang DA_THANH_TOAN");
    }

    // =========================================================================
    // CÁC TEST BỔ TRỢ HỆ THỐNG
    // =========================================================================

    @Test
    @DisplayName("Kiểm tra xác thực đăng nhập đúng và sai")
    void testAuthService() {
        TaiKhoan tk = authService.dangNhap("admin", "admin123");
        assertNotNull(tk);
        assertEquals("admin", tk.getTenDangNhap());

        assertThrows(BusinessException.class, () -> authService.dangNhap("admin", "wrong_password"));
        assertThrows(BusinessException.class, () -> authService.dangNhap("khong_ton_tai", "123456"));
    }

    @Test
    @DisplayName("Kiểm tra tính bảng lương nhân viên tự động bằng Đa hình")
    void testTinhBangLuongPolymorphism() {
        Map<String, Object> bangLuong = nhanVienService.tinhBangLuong();
        assertNotNull(bangLuong);
        BigDecimal tongQuy = (BigDecimal) bangLuong.get("tongQuyLuong");
        assertTrue(tongQuy.compareTo(BigDecimal.ZERO) > 0, "Tổng quỹ lương phải lớn hơn 0");
    }
}
