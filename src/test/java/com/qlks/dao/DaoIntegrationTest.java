package com.qlks.dao;

import com.qlks.dao.impl.NhanVienDAOImpl;
import com.qlks.dao.impl.PhongDAOImpl;
import com.qlks.dao.impl.TaiKhoanDAOImpl;
import com.qlks.model.*;
import com.qlks.util.DatabaseConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử tích hợp kết nối JDBC thực tế giữa ứng dụng và cơ sở dữ liệu MySQL (hotel_db).
 */
public class DaoIntegrationTest {

    private final TaiKhoanDAO taiKhoanDAO = new TaiKhoanDAOImpl();
    private final PhongDAO phongDAO = new PhongDAOImpl();
    private final NhanVienDAO nhanVienDAO = new NhanVienDAOImpl();

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
        assertEquals(8, danhSachPhong.size(), "Dữ liệu mẫu phải có đúng 8 phòng");

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

        // Kiểm tra gọi đa hình tinhLuong() trực tiếp từ dữ liệu MySQL
        assertTrue(qlOpt.get().tinhLuong().doubleValue() > 0);
        assertTrue(ltOpt.get().tinhLuong().doubleValue() > 0);
    }
}
