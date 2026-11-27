package com.qlks.view;

import com.qlks.controller.*;
import com.qlks.model.*;
import com.qlks.model.enums.*;
import com.qlks.service.AuthService;
import com.qlks.service.ThongBaoService;
import com.qlks.util.AppEventBus;
import com.qlks.util.DatabaseConnection;
import com.qlks.view.dialogs.*;
import com.qlks.view.panels.*;
import org.junit.jupiter.api.*;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UiAndFunctionalFullTest {

    private static boolean isDbConnected() {
        return DatabaseConnection.checkConnection();
    }

    @BeforeAll
    static void setUpAll() {
        // FlatLaf or System Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {}
    }

    @BeforeEach
    void checkDb() {
        Assumptions.assumeTrue(isDbConnected(), "MySQL không khả dụng, bỏ qua test giao diện.");
    }

    @AfterEach
    void tearDown() {
        AuthService.dangXuat();
    }

    // -------------------------------------------------------------
    // 1. TEST ĐĂNG NHẬP, XÁC THỰC & PHÂN QUYỀN
    // -------------------------------------------------------------
    @Test
    @Order(1)
    @DisplayName("UI/Func 1: Kiểm thử Đăng nhập & Xác thực với các vai trò")
    void test1_DangNhapVaXacThuc() throws Exception {
        AuthController authController = new AuthController();

        // 1.1 Đăng nhập sai mật khẩu -> ném Exception
        assertThrows(Exception.class, () -> authController.dangNhap("admin", "sai_mat_khau"));

        // 1.2 Đăng nhập Admin thành công
        TaiKhoan admin = authController.dangNhap("admin", "admin123");
        assertNotNull(admin);
        assertEquals(VaiTro.ADMIN, admin.getVaiTro());
        assertEquals("admin", AuthService.getTaiKhoanHienTai().getTenDangNhap());

        // 1.3 Đăng nhập Lễ tân thành công
        TaiKhoan letan = authController.dangNhap("letan01", "letan123");
        assertNotNull(letan);
        assertEquals(VaiTro.LE_TAN, letan.getVaiTro());

        // 1.4 Đăng nhập Quản lý thành công
        TaiKhoan manager = authController.dangNhap("manager", "admin123");
        assertNotNull(manager);
        assertEquals(VaiTro.MANAGER, manager.getVaiTro());
    }

    // -------------------------------------------------------------
    // 2. TEST GIAO DIỆN CHÍNH (MAINFRAME) VÀ PHÂN QUYỀN GIAO DIỆN
    // -------------------------------------------------------------
    @Test
    @Order(2)
    @DisplayName("UI/Func 2: Kiểm thử MainFrame và chuyển đổi tất cả các Tab theo quyền")
    void test2_MainFrameVaCacTab() throws Exception {
        AuthController authController = new AuthController();
        TaiKhoan admin = authController.dangNhap("admin", "admin123");

        SwingUtilities.invokeAndWait(() -> {
            MainFrame mainFrame = new MainFrame(authController, admin);
            assertNotNull(mainFrame);
            assertEquals("Hệ thống Quản lý Khách sạn - Quản trị viên", mainFrame.getTitle());

            // Chuyển qua từng Tab trong MainFrame đảm bảo không văng Exception
            String[] tabs = {"PHONG", "DAT_PHONG", "CHECK_IN_OUT", "THONG_BAO", "HOA_DON", "KHACH_HANG", "DICH_VU", "THONG_KE", "NHAN_VIEN"};
            for (String tab : tabs) {
                mainFrame.chuyenTab(tab);
            }

            // Gửi sự kiện EventBus kiểm tra lắng nghe tự động
            AppEventBus.publish(AppEventBus.EventType.ROOM_STATUS_CHANGED);
            AppEventBus.publish(AppEventBus.EventType.BOOKING_CHANGED);
            AppEventBus.publish(AppEventBus.EventType.INVOICE_CHANGED);

            mainFrame.dispose();
        });

        // Kiểm tra với quyền LỄ TÂN (Không có tab Nhân viên & Thống kê)
        TaiKhoan letan = authController.dangNhap("letan01", "letan123");
        SwingUtilities.invokeAndWait(() -> {
            MainFrame mainFrameLeTan = new MainFrame(authController, letan);
            assertNotNull(mainFrameLeTan);
            mainFrameLeTan.chuyenTab("PHONG");
            mainFrameLeTan.chuyenTab("CHECK_IN_OUT");
            mainFrameLeTan.dispose();
        });
    }

    // -------------------------------------------------------------
    // 3. TEST QUẢN LÝ PHÒNG (PHONGPANEL & PHONGCONTROLLER)
    // -------------------------------------------------------------
    @Test
    @Order(3)
    @DisplayName("UI/Func 3: Kiểm thử Màn hình Quản lý Phòng & Controller")
    void test3_PhongPanelVaController() throws Exception {
        new AuthController().dangNhap("admin", "admin123");
        PhongController controller = new PhongController();

        // 3.1 Tải danh sách phòng
        List<Phong> danhSach = controller.layTatCaPhong();
        assertNotNull(danhSach);
        assertTrue(danhSach.size() >= 8, "Khách sạn mẫu có ít nhất 8 phòng.");

        // 3.2 Khởi tạo giao diện PhongPanel trên EDT
        SwingUtilities.invokeAndWait(() -> {
            PhongPanel pnl = new PhongPanel();
            assertNotNull(pnl);
            pnl.loadDuLieu();
            pnl.stopAutoRefresh();
        });

        // 3.3 Thao tác cập nhật trạng thái phòng (CLEANING -> AVAILABLE)
        Optional<Phong> p103 = controller.timTheoMa("P103");
        assertTrue(p103.isPresent());
        controller.chuyenTrangThaiPhong("P103", TrangThaiPhong.AVAILABLE);
        assertEquals(TrangThaiPhong.AVAILABLE, controller.timTheoMa("P103").get().getTrangThaiHienTai());

        // Khôi phục lại trạng thái CLEANING cho P103
        controller.chuyenTrangThaiPhong("P103", TrangThaiPhong.CLEANING);
    }

    // -------------------------------------------------------------
    // 4. TEST QUẢN LÝ ĐẶT PHÒNG (DATPHONGPANEL & CONTROLLER)
    // -------------------------------------------------------------
    @Test
    @Order(4)
    @DisplayName("UI/Func 4: Kiểm thử Màn hình Đặt phòng, Tìm phòng trống & Tạo đơn")
    void test4_DatPhongPanelVaController() throws Exception {
        new AuthController().dangNhap("admin", "admin123");
        DatPhongController controller = new DatPhongController();

        // 4.1 Tìm phòng trống cho 5 ngày sau
        LocalDateTime checkIn = LocalDateTime.now().plusDays(5).withHour(14).withMinute(0);
        LocalDateTime checkOut = LocalDateTime.now().plusDays(7).withHour(12).withMinute(0);
        List<Phong> phongTrong = controller.timPhongTrong(checkIn, checkOut);
        assertNotNull(phongTrong);
        assertFalse(phongTrong.isEmpty(), "Phải có phòng trống trong tương lai.");

        // 4.2 Khởi tạo giao diện DatPhongPanel trên EDT
        SwingUtilities.invokeAndWait(() -> {
            DatPhongPanel pnl = new DatPhongPanel();
            assertNotNull(pnl);
            pnl.loadDuLieu();
        });

        // 4.3 Tạo một đơn đặt phòng thử nghiệm
        KhachHang kh = controller.layTatCaKhachHang().get(0);
        Phong phongChon = phongTrong.get(0);
        NhanVien nv = controller.getNhanVienHienTai();

        String maDpTest = "TEST_UI_" + (System.currentTimeMillis() % 100000);
        BigDecimal tienCoc = controller.tinhTienCocToiThieu(phongChon, checkIn, checkOut);
        DatPhong dp = new DatPhong(maDpTest, kh, phongChon, nv, checkIn, checkOut,
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, tienCoc, null);
        dp.addKhachLuuTru(new KhachLuuTru(maDpTest, kh.getHoTen(), kh.getSoCCCD(), kh.getSoDienThoai(), VaiTroKhachLuuTru.NGUOI_DAI_DIEN));

        controller.taoDatPhong(dp);

        // Kiểm tra đã lưu vào DB
        Optional<DatPhong> dpSaved = controller.timTheoMa(maDpTest);
        assertTrue(dpSaved.isPresent());
        assertEquals(TrangThaiDatPhong.DA_XAC_NHAN, dpSaved.get().getTrangThai());

        // 4.4 Hủy đặt phòng và kiểm tra hoàn cọc
        com.qlks.model.dto.KetQuaHuyDatPhong kq = controller.huyDatPhong(maDpTest, "Khách đổi lịch", PhuongThucThanhToan.TIEN_MAT);
        assertNotNull(kq);
        assertEquals(TrangThaiDatPhong.DA_HUY, controller.timTheoMa(maDpTest).get().getTrangThai());
    }

    // -------------------------------------------------------------
    // 5. TEST TIẾP TÂN: CHECK-IN & CHECK-OUT (CHECKINCHECKOUTPANEL & CONTROLLER)
    // -------------------------------------------------------------
    @Test
    @Order(5)
    @DisplayName("UI/Func 5: Kiểm thử Tiếp tân Check-in, gọi dịch vụ phòng và Check-out")
    void test5_CheckInCheckOutVaDichVu() throws Exception {
        new AuthController().dangNhap("letan01", "letan123");
        CheckInCheckOutController controller = new CheckInCheckOutController();

        // 5.1 Khởi tạo giao diện CheckInCheckOutPanel trên EDT
        SwingUtilities.invokeAndWait(() -> {
            CheckInCheckOutPanel pnl = new CheckInCheckOutPanel();
            assertNotNull(pnl);
            pnl.loadDuLieu();
        });

        // Tạo một đơn đặt phòng ngay hôm nay tại P102 (đang AVAILABLE)
        DatPhongController dpController = new DatPhongController();
        KhachHang kh = dpController.layTatCaKhachHang().get(0);
        Phong p102 = new PhongController().timTheoMa("P102").orElseThrow();
        NhanVien nv = dpController.getNhanVienHienTai();

        String maDp = "TEST_CIO_" + (System.currentTimeMillis() % 100000);
        LocalDateTime now = LocalDateTime.now();
        DatPhong dp = new DatPhong(maDp, kh, p102, nv, now, now.plusDays(1),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, null);
        dp.addKhachLuuTru(new KhachLuuTru(maDp, kh.getHoTen(), kh.getSoCCCD(), kh.getSoDienThoai(), VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        dpController.taoDatPhong(dp);

        // 5.2 Thực hiện Check-in
        controller.xacNhanCheckIn(maDp);
        DatPhong dpAfterCi = controller.timTheoMa(maDp).orElseThrow();
        assertEquals(TrangThaiDatPhong.DANG_O, dpAfterCi.getTrangThai());
        assertEquals(TrangThaiPhong.OCCUPIED, new PhongController().timTheoMa("P102").get().getTrangThaiHienTai());

        // 5.3 Ghi nhận sử dụng dịch vụ tại phòng: 2 chai nước suối (DV001)
        controller.ghiNhanDichVu(maDp, "DV001", 2);
        List<ChiTietDichVu> dvList = controller.layDichVuTheoDatPhong(maDp);
        assertFalse(dvList.isEmpty());
        assertEquals(2, dvList.get(0).getSoLuong());

        // 5.4 Thực hiện Check-out
        HoaDon hd = controller.xacNhanCheckOut(maDp);
        assertNotNull(hd);
        assertNotNull(hd.getMaHoaDon());
        assertTrue(hd.getTongChiPhi().compareTo(BigDecimal.ZERO) > 0);
        assertEquals(TrangThaiPhong.CLEANING, new PhongController().timTheoMa("P102").get().getTrangThaiHienTai());

        // Dọn dẹp phòng P102 về lại AVAILABLE
        new PhongController().chuyenTrangThaiPhong("P102", TrangThaiPhong.AVAILABLE);
    }

    // -------------------------------------------------------------
    // 6. TEST HÓA ĐƠN & THANH TOÁN (HOADONPANEL & CONTROLLER)
    // -------------------------------------------------------------
    @Test
    @Order(6)
    @DisplayName("UI/Func 6: Kiểm thử Quản lý Hóa đơn, Thanh toán nhiều đợt & Chống trùng lặp")
    void test6_HoaDonVaThanhToan() throws Exception {
        new AuthController().dangNhap("letan01", "letan123");
        HoaDonController hdController = new HoaDonController();

        // 6.1 Khởi tạo giao diện HoaDonPanel trên EDT
        SwingUtilities.invokeAndWait(() -> {
            HoaDonPanel pnl = new HoaDonPanel();
            assertNotNull(pnl);
            pnl.loadDuLieu();
        });

        // Lấy hóa đơn chưa thanh toán
        List<HoaDon> listHd = hdController.layTatCaHoaDon();
        HoaDon hdChuaTt = listHd.stream()
                .filter(h -> h.getTrangThai() == TrangThaiThanhToan.CHUA_THANH_TOAN)
                .findFirst()
                .orElse(null);

        if (hdChuaTt != null) {
            BigDecimal conLai = hdController.tinhSoTienConLaiCanThu(hdChuaTt.getMaHoaDon());
            assertTrue(conLai.compareTo(BigDecimal.ZERO) > 0);

            // Thanh toán đợt 1: một nửa số tiền
            BigDecimal nuaTien = conLai.divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP);
            hdController.thanhToanHoaDon(hdChuaTt.getMaHoaDon(), nuaTien, PhuongThucThanhToan.TIEN_MAT, "Thanh toán đợt 1");

            BigDecimal conLaiDot1 = hdController.tinhSoTienConLaiCanThu(hdChuaTt.getMaHoaDon());
            assertTrue(conLaiDot1.compareTo(conLai) < 0);

            // Thanh toán đợt 2: phần còn lại
            hdController.thanhToanHoaDon(hdChuaTt.getMaHoaDon(), conLaiDot1, PhuongThucThanhToan.CHUYEN_KHOAN, "Thanh toán dứt điểm đợt 2");

            HoaDon hdHoanTat = hdController.timTheoMa(hdChuaTt.getMaHoaDon()).orElseThrow();
            assertEquals(TrangThaiThanhToan.DA_THANH_TOAN, hdHoanTat.getTrangThai());

            // Chống thanh toán trùng lặp
            assertThrows(Exception.class, () ->
                    hdController.thanhToanHoaDon(hdChuaTt.getMaHoaDon(), new BigDecimal("10000"), PhuongThucThanhToan.TIEN_MAT, "Cố tình thanh toán thêm")
            );
        }
    }

    // -------------------------------------------------------------
    // 7. TEST QUẢN LÝ DỊCH VỤ (DICHVUPANEL & CONTROLLER)
    // -------------------------------------------------------------
    @Test
    @Order(7)
    @DisplayName("UI/Func 7: Kiểm thử Quản lý Dịch vụ (Thêm, Sửa, Ngừng KD)")
    void test7_DichVuPanelVaController() throws Exception {
        new AuthController().dangNhap("manager", "admin123");
        DichVuController controller = new DichVuController();

        // 7.1 Khởi tạo giao diện DichVuPanel trên EDT
        SwingUtilities.invokeAndWait(() -> {
            DichVuPanel pnl = new DichVuPanel();
            assertNotNull(pnl);
            pnl.loadDuLieu();
        });

        // 7.2 Thêm dịch vụ mới
        String maDvTest = "DV" + (System.currentTimeMillis() % 100000);
        DichVu dvMoi = new DichVu(maDvTest, "Nước Ép Cam Tươi Test", new BigDecimal("45000.00"), true);
        controller.themDichVu(dvMoi);

        Optional<DichVu> dvSaved = controller.timTheoMa(maDvTest);
        assertTrue(dvSaved.isPresent());
        assertEquals("Nước Ép Cam Tươi Test", dvSaved.get().getTenDichVu());

        // 7.3 Sửa giá dịch vụ
        dvMoi.setDonGiaHienTai(new BigDecimal("50000.00"));
        controller.capNhatDichVu(dvMoi);
        assertEquals(0, new BigDecimal("50000.00").compareTo(controller.timTheoMa(maDvTest).get().getDonGiaHienTai()));

        // 7.4 Ngừng kinh doanh
        controller.ngungKinhDoanh(maDvTest);
        assertFalse(controller.timTheoMa(maDvTest).get().isDangKinhDoanh());
    }

    // -------------------------------------------------------------
    // 8. TEST QUẢN LÝ KHÁCH HÀNG (KHACHHANGPANEL & CONTROLLER)
    // -------------------------------------------------------------
    @Test
    @Order(8)
    @DisplayName("UI/Func 8: Kiểm thử Quản lý Khách hàng & Tra cứu lịch sử")
    void test8_KhachHangPanelVaController() throws Exception {
        new AuthController().dangNhap("letan01", "letan123");
        KhachHangController controller = new KhachHangController();

        // 8.1 Khởi tạo giao diện KhachHangPanel trên EDT
        SwingUtilities.invokeAndWait(() -> {
            KhachHangPanel pnl = new KhachHangPanel();
            assertNotNull(pnl);
            pnl.loadDuLieu();
        });

        // 8.2 Tìm kiếm khách hàng theo tên hoặc số điện thoại
        List<KhachHang> kq = controller.timKiem("Tuấn");
        assertNotNull(kq);
        assertFalse(kq.isEmpty());

        // 8.3 Tra cứu lịch sử đặt phòng & hóa đơn
        String maKh = kq.get(0).getMaKhachHang();
        List<DatPhong> lsDatPhong = controller.layLichSuDatPhong(maKh);
        assertNotNull(lsDatPhong);
        List<HoaDon> lsHoaDon = controller.layLichSuHoaDon(maKh);
        assertNotNull(lsHoaDon);

        // 8.4 Thêm khách hàng mới hợp lệ (12 chữ số CCCD)
        long suffix = System.currentTimeMillis() % 100000000L;
        String cccdTest = String.format("0012%08d", suffix);
        String sdtTest = String.format("09%08d", suffix);
        String maKhMoi = "KH" + (System.currentTimeMillis() % 100000);
        KhachHang khMoi = new KhachHang(maKhMoi, "Trần Văn Test", sdtTest, "test@gmail.com", cccdTest, LocalDate.now());
        controller.themKhachHang(khMoi);

        Optional<KhachHang> khCheck = controller.timTheoMa(maKhMoi);
        assertTrue(khCheck.isPresent());
        assertEquals("Trần Văn Test", khCheck.get().getHoTen());
    }

    // -------------------------------------------------------------
    // 9. TEST QUẢN LÝ NHÂN VIÊN & BẢNG LƯƠNG (NHANVIENPANEL & CONTROLLER)
    // -------------------------------------------------------------
    @Test
    @Order(9)
    @DisplayName("UI/Func 9: Kiểm thử Quản lý Nhân sự & Bảng Lương Đa hình (Polymorphism)")
    void test9_NhanVienPanelVaBangLuong() throws Exception {
        new AuthController().dangNhap("admin", "admin123");
        NhanVienController controller = new NhanVienController();

        // 9.1 Khởi tạo giao diện NhanVienPanel trên EDT
        SwingUtilities.invokeAndWait(() -> {
            NhanVienPanel pnl = new NhanVienPanel();
            assertNotNull(pnl);
            pnl.loadDuLieu();
        });

        // 9.2 Danh sách nhân viên
        List<NhanVien> ds = controller.layTatCaNhanVien();
        assertNotNull(ds);
        assertTrue(ds.size() >= 4);

        // 9.3 Tính bảng lương đa hình
        Map<String, Object> bangLuong = controller.tinhBangLuong();
        assertNotNull(bangLuong);
        assertTrue(bangLuong.containsKey("danhSachNhanVien"));
        assertTrue(bangLuong.containsKey("tongQuyLuong"));
        BigDecimal tongLuong = (BigDecimal) bangLuong.get("tongQuyLuong");
        assertTrue(tongLuong.compareTo(BigDecimal.ZERO) > 0);

        // Kiểm tra đa hình: Quản lý tính theo hệ số trách nhiệm, Lễ tân tính theo số ca trực
        @SuppressWarnings("unchecked")
        List<NhanVien> nvList = (List<NhanVien>) bangLuong.get("danhSachNhanVien");
        for (NhanVien nv : nvList) {
            BigDecimal luong = nv.tinhLuong();
            assertNotNull(luong);
            assertTrue(luong.compareTo(BigDecimal.ZERO) > 0);
        }
    }

    // -------------------------------------------------------------
    // 10. TEST BÁO CÁO THỐNG KÊ (THONGKEPANEL & CONTROLLER)
    // -------------------------------------------------------------
    @Test
    @Order(10)
    @DisplayName("UI/Func 10: Kiểm thử Báo cáo Thống kê & Doanh thu")
    void test10_ThongKePanelVaController() throws Exception {
        new AuthController().dangNhap("manager", "admin123");
        ThongKeController controller = new ThongKeController();

        // 10.1 Khởi tạo giao diện ThongKePanel trên EDT
        SwingUtilities.invokeAndWait(() -> {
            ThongKePanel pnl = new ThongKePanel();
            assertNotNull(pnl);
            pnl.loadDuLieu();
        });

        // 10.2 Tổng quan dữ liệu thống kê
        Map<String, Object> tongQuan = controller.layTongQuan();
        assertNotNull(tongQuan);
        assertTrue(tongQuan.containsKey("tongDoanhThu"));
        assertTrue(tongQuan.containsKey("soPhongDangO"));
        assertTrue(tongQuan.containsKey("tongSoPhong"));
        assertTrue(tongQuan.containsKey("tongDatPhong"));

        // 10.3 Báo cáo doanh thu theo dải ngày
        LocalDate tuNgay = LocalDate.now().minusDays(30);
        LocalDate denNgay = LocalDate.now().plusDays(1);
        List<Map<String, Object>> doanhThuTheoNgay = controller.thongKeDoanhThuTheoNgay(tuNgay, denNgay);
        assertNotNull(doanhThuTheoNgay);
    }

    // -------------------------------------------------------------
    // 11. TEST THÔNG BÁO TỚI HẠN & CHUÔNG THÔNG MINH (THONGBAOPANEL & CONTROLLER)
    // -------------------------------------------------------------
    @Test
    @Order(11)
    @DisplayName("UI/Func 11: Kiểm thử Trung tâm Thông báo & Chuông trạng thái")
    void test11_ThongBaoPanelVaController() throws Exception {
        new AuthController().dangNhap("letan01", "letan123");
        ThongBaoController controller = new ThongBaoController();

        // 11.1 Khởi tạo giao diện ThongBaoPanel trên EDT
        SwingUtilities.invokeAndWait(() -> {
            ThongBaoPanel pnl = new ThongBaoPanel();
            assertNotNull(pnl);
            pnl.loadDuLieu();
        });

        // 11.2 Danh sách thông báo tới hạn
        List<ThongBaoDon> list = controller.layDanhSachThongBaoToiHan();
        assertNotNull(list);

        // 11.3 Kiểm tra trạng thái chuông
        ThongBaoService.TrangThaiChuong trangThaiChuong = controller.getTrangThaiChuong();
        assertNotNull(trangThaiChuong);

        // 11.4 Đánh dấu tất cả đã đọc
        controller.danhDauTatCaDaDoc();
        assertEquals(0, controller.demSoLuongChuaDoc());
    }

    // -------------------------------------------------------------
    // 12. TEST CÁC HỘP THOẠI DIALOG (QR CODE, ORDER, ĐỔI MK)
    // -------------------------------------------------------------
    @Test
    @Order(12)
    @DisplayName("UI/Func 12: Kiểm thử khởi tạo an toàn các Dialog chuyên biệt")
    void test12_CacDialogChuyenBiet() throws Exception {
        new AuthController().dangNhap("admin", "admin123");

        SwingUtilities.invokeAndWait(() -> {
            JFrame dummyFrame = new JFrame();

            // 12.1 Dialog Đổi mật khẩu
            DoiMatKhauDialog dlgDoiMk = new DoiMatKhauDialog(dummyFrame, new AuthController(), "admin");
            assertNotNull(dlgDoiMk);
            dlgDoiMk.dispose();

            // 12.2 Dialog Quét mã VietQR thanh toán tiền cọc
            QRPaymentDialog dlgQr = new QRPaymentDialog(dummyFrame, "DP001", "Nguyễn Văn Test", new BigDecimal("500000.00"));
            assertNotNull(dlgQr);
            dlgQr.dispose();

            // 12.3 Dialog In thẻ QR gọi món tại phòng
            Phong p101 = new PhongController().timTheoMa("P101").orElse(null);
            InRoomQRCodeDialog dlgInRoom = new InRoomQRCodeDialog(dummyFrame, p101);
            assertNotNull(dlgInRoom);
            dlgInRoom.dispose();

            // 12.4 Dialog Quản lý Order phòng từ khách
            RoomOrderManagementDialog dlgOrder = new RoomOrderManagementDialog(dummyFrame);
            assertNotNull(dlgOrder);
            dlgOrder.dispose();

            // 12.5 Dialog Xem nhanh Thông báo tới hạn
            ThongBaoDialog dlgTb = new ThongBaoDialog(dummyFrame, new ThongBaoController(), tab -> {});
            assertNotNull(dlgTb);
            dlgTb.dispose();

            dummyFrame.dispose();
        });
    }
}
