package com.qlks.service;

import com.qlks.dao.KhachHangDAO;
import com.qlks.dao.NhanVienDAO;
import com.qlks.dao.PhongDAO;
import com.qlks.dao.impl.KhachHangDAOImpl;
import com.qlks.dao.impl.NhanVienDAOImpl;
import com.qlks.dao.impl.PhongDAOImpl;
import com.qlks.exception.BusinessException;
import com.qlks.model.*;
import com.qlks.model.enums.PhuongThucThanhToan;
import com.qlks.model.enums.TrangThaiDatPhong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị các quy tắc nghiệp vụ cốt lõi tại tầng Service:
 * 1. Thuật toán chống trùng lịch đặt phòng
 * 2. Quy trình Check-in, Check-out, Lập hóa đơn
 * 3. Chống thanh toán trùng lặp (Idempotent Payment)
 * 4. Tính toán bảng lương đa hình
 * 5. Xác thực đăng nhập
 */
public class ServiceBusinessRuleTest {

    private final AuthService authService = new AuthService();
    private final DatPhongService datPhongService = new DatPhongService();
    private final HoaDonService hoaDonService = new HoaDonService();
    private final NhanVienService nhanVienService = new NhanVienService();

    private final KhachHangDAO khachHangDAO = new KhachHangDAOImpl();
    private final PhongDAO phongDAO = new PhongDAOImpl();
    private final NhanVienDAO nhanVienDAO = new NhanVienDAOImpl();

    @Test
    @DisplayName("Kiểm tra xác thực đăng nhập đúng và sai")
    void testAuthService() {
        // Đăng nhập đúng
        TaiKhoan tk = authService.dangNhap("admin", "admin123");
        assertNotNull(tk);
        assertEquals("admin", tk.getTenDangNhap());

        // Đăng nhập sai mật khẩu
        assertThrows(BusinessException.class, () -> authService.dangNhap("admin", "wrong_password"));

        // Đăng nhập tài khoản không tồn tại
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

    @Test
    @DisplayName("Kiểm tra quy tắc chống trùng lịch phòng và cho phép gối đầu ngày trả/nhận")
    void testChongTrungLichPhong() {
        KhachHang kh = khachHangDAO.findById("KH001").orElseThrow();
        Phong phong = phongDAO.findById("P101").orElseThrow();
        NhanVien nv = nhanVienDAO.findById("NV001").orElseThrow();

        long suffix = System.currentTimeMillis() % 100000;
        String maDp1 = "TEST_DP_" + suffix;
        LocalDateTime nhan1 = LocalDateTime.of(2027, 5, 10, 14, 0);
        LocalDateTime tra1 = LocalDateTime.of(2027, 5, 15, 12, 0);

        DatPhong dp1 = new DatPhong(maDp1, kh, phong, nv, nhan1, tra1, null, null, 2, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);
        datPhongService.taoDatPhong(dp1);

        // Trường hợp 1: Đặt phòng trùng khoảng thời gian (12/05 đến 18/05) -> PHẢI BỊ TỪ CHỐI
        String maDpTrung = "TEST_TRUNG_" + suffix;
        LocalDateTime nhanTrung = LocalDateTime.of(2027, 5, 12, 14, 0);
        LocalDateTime traTrung = LocalDateTime.of(2027, 5, 18, 12, 0);
        DatPhong dpTrung = new DatPhong(maDpTrung, kh, phong, nv, nhanTrung, traTrung, null, null, 2, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);

        BusinessException ex = assertThrows(BusinessException.class, () -> datPhongService.taoDatPhong(dpTrung));
        assertTrue(ex.getMessage().contains("đã có khách đặt trong khoảng thời gian này"));

        // Trường hợp 2: Khách tiếp theo nhận phòng đúng ngày khách trước trả phòng (15/05 đến 20/05) -> HỢP LỆ
        String maDpGoiDau = "TEST_GOI_DAU_" + suffix;
        LocalDateTime nhanGoiDau = LocalDateTime.of(2027, 5, 15, 14, 0);
        LocalDateTime traGoiDau = LocalDateTime.of(2027, 5, 20, 12, 0);
        DatPhong dpGoiDau = new DatPhong(maDpGoiDau, kh, phong, nv, nhanGoiDau, traGoiDau, null, null, 2, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);

        assertDoesNotThrow(() -> {
            datPhongService.taoDatPhong(dpGoiDau);
        });

        // Hủy booking test sau khi test xong
        datPhongService.huyDatPhong(maDp1);
        datPhongService.huyDatPhong(maDpGoiDau);
    }

    @Test
    @DisplayName("Kiểm tra quy trình Check-in, Check-out, Lập hóa đơn và Chống thanh toán trùng lặp")
    void testQuyTrinhCheckInOutVaThanhToanChongTrung() {
        KhachHang kh = khachHangDAO.findById("KH002").orElseThrow();
        Phong phong = phongDAO.findById("P102").orElseThrow();
        NhanVien nv = nhanVienDAO.findById("NV002").orElseThrow();

        String maDp = "TEST_FLOW_" + System.currentTimeMillis() % 100000;
        LocalDateTime nhan = LocalDateTime.now();
        LocalDateTime tra = nhan.plusDays(2);

        // 1. Tạo đặt phòng
        DatPhong dp = new DatPhong(maDp, kh, phong, nv, nhan, tra, null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);
        datPhongService.taoDatPhong(dp);

        // 2. Check-in
        datPhongService.xacNhanCheckIn(maDp);
        DatPhong dpCheckIn = datPhongService.timTheoMa(maDp).orElseThrow();
        assertEquals(TrangThaiDatPhong.DANG_O, dpCheckIn.getTrangThai());

        // 3. Check-out -> Tự động sinh hóa đơn
        HoaDon hd = datPhongService.xacNhanCheckOut(maDp);
        assertNotNull(hd);
        assertTrue(hd.getTongTien().compareTo(BigDecimal.ZERO) > 0);

        // 4. Thanh toán lần 1 thành công
        ThanhToan tt = hoaDonService.thanhToanHoaDon(hd.getMaHoaDon(), hd.getTongTien(), PhuongThucThanhToan.TIEN_MAT, "Thanh toán test");
        assertNotNull(tt);

        // 5. Cố tình thanh toán lần 2 trên hóa đơn đã thanh toán -> PHẢI BỊ CHẶN (Chống thanh toán trùng)
        BusinessException ex = assertThrows(BusinessException.class, () ->
                hoaDonService.thanhToanHoaDon(hd.getMaHoaDon(), hd.getTongTien(), PhuongThucThanhToan.CHUYEN_KHOAN, "Thanh toán trùng")
        );
        assertTrue(ex.getMessage().contains("đã được thanh toán đầy đủ"));
    }
}
