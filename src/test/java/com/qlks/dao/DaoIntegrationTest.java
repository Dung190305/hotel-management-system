package com.qlks.dao;

import com.qlks.dao.impl.KhachHangDAOImpl;
import com.qlks.dao.impl.NhanVienDAOImpl;
import com.qlks.dao.impl.PhongDAOImpl;
import com.qlks.dao.impl.TaiKhoanDAOImpl;
import com.qlks.model.*;
import com.qlks.model.enums.TrangThaiPhong;
import com.qlks.util.DatabaseConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BỘ KIỂM THỬ TÍCH HỢP JDBC & CSDL MYSQL (TEST 25 -> TEST 27):
 * Test 25: Lưu và đọc lại chuẩn tiếng Việt có dấu UTF-8 / utf8mb4.
 * Test 26: Kiểm tra toàn bộ dữ liệu tiếng Việt trong CSDL không bị lỗi dấu hỏi ?
 * Test 27: Kiểm tra tính nguyên tử (Atomicity) và khả năng Rollback khi gặp ngoại lệ giữa chừng.
 */
public class DaoIntegrationTest {

    private final TaiKhoanDAO taiKhoanDAO = new TaiKhoanDAOImpl();
    private final PhongDAO phongDAO = new PhongDAOImpl();
    private final NhanVienDAO nhanVienDAO = new NhanVienDAOImpl();
    private final KhachHangDAO khachHangDAO = new KhachHangDAOImpl();

    @org.junit.jupiter.api.BeforeEach
    void kiemTraKetNoiDatabase() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
                DatabaseConnection.checkConnection(),
                "MySQL chưa được khởi động hoặc không thể kết nối. Tự động bỏ qua Integration Test."
        );
    }

    @Test
    @DisplayName("Kiểm tra kết nối trực tiếp đến MySQL hotel_db")
    void testDatabaseConnection() {
        boolean connected = DatabaseConnection.checkConnection();
        assertTrue(connected, "Kết nối MySQL hotel_db phải thành công.");
    }

    @Test
    @DisplayName("Kiểm tra truy vấn tài khoản và xác thực mật khẩu BCrypt từ database thật")
    void testTaiKhoanDAO() {
        Optional<TaiKhoan> adminOpt = taiKhoanDAO.findByTenDangNhap("admin");
        assertTrue(adminOpt.isPresent(), "Phải tìm thấy tài khoản admin trong database");
        TaiKhoan admin = adminOpt.get();
        assertEquals("admin", admin.getTenDangNhap());
        assertTrue(admin.kiemTraMatKhau("admin123"), "Mật khẩu băm phải khớp với 'admin123'");
    }

    @Test
    @DisplayName("Kiểm tra truy vấn phòng và tính đa hình khi ánh xạ sang PhongThuong / PhongVIP")
    void testPhongDAO() {
        List<Phong> danhSachPhong = phongDAO.findAll();
        assertFalse(danhSachPhong.isEmpty(), "Danh sách phòng không được rỗng");
        assertTrue(danhSachPhong.size() >= 8, "Dữ liệu mẫu phải có ít nhất 8 phòng");

        boolean hasPhongThuong = false;
        boolean hasPhongVip = false;
        for (Phong p : danhSachPhong) {
            if (p instanceof PhongThuong) hasPhongThuong = true;
            if (p instanceof PhongVIP) hasPhongVip = true;
        }

        assertTrue(hasPhongThuong, "Phải có đối tượng phòng thường kế thừa Phong");
        assertTrue(hasPhongVip, "Phải có đối tượng phòng VIP kế thừa Phong");
    }

    @Test
    @DisplayName("Kiểm tra truy vấn nhân viên và tính đa hình giữa QuanLy và LeTan")
    void testNhanVienDAO() {
        List<NhanVien> danhSachNv = nhanVienDAO.findAll();
        assertFalse(danhSachNv.isEmpty(), "Danh sách nhân viên không được rỗng");

        Optional<NhanVien> qlOpt = danhSachNv.stream().filter(nv -> nv instanceof QuanLy).findFirst();
        Optional<NhanVien> ltOpt = danhSachNv.stream().filter(nv -> nv instanceof LeTan).findFirst();

        assertTrue(qlOpt.isPresent(), "Phải ánh xạ thành công đối tượng QuanLy");
        assertTrue(ltOpt.isPresent(), "Phải ánh xạ thành công đối tượng LeTan");

        assertTrue(qlOpt.get().tinhLuong().doubleValue() > 0);
        assertTrue(ltOpt.get().tinhLuong().doubleValue() > 0);
    }

    // =========================================================================
    // NHÓM 5: KIỂM THỬ ENCODING TIẾNG VIỆT & PHỤC HỒI LỖI ACID (TEST 25 -> TEST 27)
    // =========================================================================

    @Test
    @DisplayName("Test 25: Lưu và đọc lại chuẩn tiếng Việt có dấu (Nguyễn Văn Quân, Phạm Minh Tuấn)")
    void test25_GhiVaDocTiengVietUTF8() {
        String maTest = "KH_VN_" + (System.currentTimeMillis() % 100000);
        String tenGoc = "Nguyễn Văn Quân";
        String sdt = "098" + (System.currentTimeMillis() % 10000000);
        String cccd = "001" + (System.currentTimeMillis() % 1000000000);

        KhachHang khMoi = new KhachHang(maTest, tenGoc, sdt, "testvn@hotel.com", cccd, java.time.LocalDate.now());
        boolean saved = khachHangDAO.save(khMoi);
        assertTrue(saved, "Lưu khách hàng tiếng Việt mới phải thành công");

        Optional<KhachHang> docLaiOpt = khachHangDAO.findById(maTest);
        assertTrue(docLaiOpt.isPresent(), "Phải đọc lại được khách hàng vừa lưu");
        assertEquals(tenGoc, docLaiOpt.get().getHoTen(), "Tên tiếng Việt đọc lại phải nguyên vẹn 'Nguyễn Văn Quân', không bị chuyển thành dấu hỏi ?");

        // Dọn dẹp dữ liệu test
        khachHangDAO.delete(maTest);
    }

    @Test
    @DisplayName("Test 26: Không bị lỗi font/dấu hỏi khi load dữ liệu tiếng Việt lên Model và View")
    void test26_KiemTraEncodingKhongBiLoiFontHoiCham() {
        // Kiểm tra tất cả khách hàng trong hệ thống không chứa ký tự dấu hỏi (?) biểu thị hỏng encoding UTF-8
        List<KhachHang> danhSachKhach = khachHangDAO.findAll();
        assertFalse(danhSachKhach.isEmpty(), "Danh sách khách hàng không được rỗng");

        for (KhachHang kh : danhSachKhach) {
            String hoTen = kh.getHoTen();
            assertNotNull(hoTen);
            assertFalse(hoTen.contains("???"), "Họ tên khách hàng '" + hoTen + "' không được chứa chuỗi lỗi font ???");
        }

        // Kiểm tra tất cả nhân viên trong hệ thống
        List<NhanVien> danhSachNv = nhanVienDAO.findAll();
        assertFalse(danhSachNv.isEmpty(), "Danh sách nhân viên không được rỗng");
        for (NhanVien nv : danhSachNv) {
            String hoTen = nv.getHoTen();
            assertNotNull(hoTen);
            assertFalse(hoTen.contains("???"), "Họ tên nhân viên '" + hoTen + "' không được chứa chuỗi lỗi font ???");
        }
    }

    @Test
    @DisplayName("Test 27: Rollback toàn vẹn dữ liệu khi gặp sự cố ngoại lệ giữa chừng (Tính nguyên tử ACID)")
    void test27_RollbackToanVenKhiGapSuCo() throws SQLException {
        // Lấy trạng thái hiện tại của phòng P101
        Phong pBanDau = phongDAO.findById("P101").orElseThrow();
        TrangThaiPhong ttBanDau = pBanDau.getTrangThaiHienTai();

        // Mở một connection giao dịch thủ công
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Bước 1: Thay đổi trạng thái phòng thành OCCUPIED
                phongDAO.updateTrangThai(conn, "P101", TrangThaiPhong.OCCUPIED);

                // Bước 2: Giả lập phát sinh ngoại lệ đột ngột giữa chừng (ví dụ mất điện, crash mạng, lỗi logic)
                boolean coSuCoXayRa = true;
                if (coSuCoXayRa) {
                    throw new RuntimeException("Giả lập sự cố hệ thống đột ngột giữa chừng!");
                }

                conn.commit();
            } catch (Exception ex) {
                // Kích hoạt rollback để hủy toàn bộ thay đổi dở dang
                conn.rollback();
            }
        }

        // Kiểm tra trên connection độc lập khác: Trạng thái phòng P101 phải được hoàn nguyên về ttBanDau ban đầu
        Phong pSauSuCo = phongDAO.findById("P101").orElseThrow();
        assertEquals(ttBanDau, pSauSuCo.getTrangThaiHienTai(),
                "Trạng thái phòng phải được rollback hoàn nguyên nguyên vẹn về trạng thái ban đầu khi có ngoại lệ giữa chừng.");
    }
}
