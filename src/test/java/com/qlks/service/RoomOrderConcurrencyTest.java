package com.qlks.service;

import com.qlks.dao.DatPhongDAO;
import com.qlks.dao.DichVuDAO;
import com.qlks.dao.PhongDAO;
import com.qlks.exception.BusinessException;
import com.qlks.model.DatPhong;
import com.qlks.model.DichVu;
import com.qlks.model.DonDichVuPhong;
import com.qlks.model.DonDichVuPhong.ChiTietMon;
import com.qlks.model.DonDichVuPhong.HinhThucThanhToanOrder;
import com.qlks.model.DonDichVuPhong.TrangThaiOrder;
import com.qlks.model.KhachHang;
import com.qlks.model.Phong;
import com.qlks.model.PhongThuong;
import com.qlks.model.enums.LoaiGiuong;
import com.qlks.model.enums.TrangThaiDatPhong;
import com.qlks.model.enums.TrangThaiPhong;
import com.qlks.server.RoomOrderHttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Nghiệp vụ & Tương tranh Đặt dịch vụ tại phòng qua mã QR (In-Room QR Order)")
class RoomOrderConcurrencyTest {

    private RoomOrderService roomOrderService;
    private DichVuService mockDichVuService;
    private DatPhongDAO mockDatPhongDAO;
    private PhongDAO mockPhongDAO;
    private DichVuDAO mockDichVuDAO;

    private Phong phong101;
    private DatPhong datPhong101;
    private DichVu dvCoca;

    @BeforeEach
    void setUp() {
        RoomOrderService.clearAllForTest();
        RoomOrderService.setKhungGioPhucVu(LocalTime.of(0, 0), LocalTime.of(23, 59)); // 24/7 cho test

        phong101 = new PhongThuong("P101", "101", 1, BigDecimal.valueOf(500000),
                TrangThaiPhong.OCCUPIED, LoaiGiuong.DON, true, true);
        KhachHang kh = new KhachHang("KH001", "Nguyễn Văn A", "0901234567", "a@test.com", "001200000001", LocalDate.now());
        datPhong101 = new DatPhong("DP001", kh, phong101, null,
                LocalDateTime.now().minusHours(2), LocalDateTime.now().plusDays(1),
                LocalDateTime.now().minusHours(2), null, 1, TrangThaiDatPhong.DANG_O, BigDecimal.valueOf(200000), LocalDateTime.now());

        dvCoca = new DichVu("DV001", "Coca-Cola", new BigDecimal("20000"), true);

        mockPhongDAO = new PhongDAO() {
            @Override public Optional<Phong> findById(String id) { return "P101".equals(id) ? Optional.of(phong101) : Optional.empty(); }
            @Override public Optional<Phong> findBySoPhong(String soPhong) { return "101".equals(soPhong) ? Optional.of(phong101) : Optional.empty(); }
            @Override public List<Phong> findAll() { return List.of(phong101); }
            @Override public boolean save(Phong entity) { return true; }
            @Override public boolean update(Phong entity) { return true; }
            @Override public boolean delete(String id) { return true; }
            @Override public List<Phong> findByTrangThai(TrangThaiPhong tt) { return Collections.emptyList(); }
            @Override public boolean updateTrangThai(String maPhong, TrangThaiPhong tt) { phong101.setTrangThaiHienTai(tt); return true; }
            @Override public boolean updateTrangThai(Connection conn, String maPhong, TrangThaiPhong tt) { phong101.setTrangThaiHienTai(tt); return true; }
            @Override public Optional<Phong> findByIdForUpdate(Connection conn, String id) { return findById(id); }
            @Override public List<Phong> findPhongTrong(LocalDateTime in, LocalDateTime out) { return Collections.emptyList(); }
            @Override public void dongBoTrangThaiTheoDon() { }
            @Override public boolean updateTrangThaiCheckIn(Connection conn, String maPhong) { return true; }
        };

        mockDatPhongDAO = new DatPhongDAO() {
            @Override public Optional<DatPhong> findById(String id) { return "DP001".equals(id) ? Optional.of(datPhong101) : Optional.empty(); }
            @Override public Optional<DatPhong> findById(Connection conn, String id) { return findById(id); }
            @Override public List<DatPhong> findAll() { return List.of(datPhong101); }
            @Override public boolean save(DatPhong entity) { return true; }
            @Override public boolean update(DatPhong entity) { return true; }
            @Override public boolean delete(String id) { return true; }
            @Override public List<DatPhong> findByKhachHang(String maKh) { return Collections.emptyList(); }
            @Override public List<DatPhong> findByPhong(String maPhong) { return Collections.emptyList(); }
            @Override public List<DatPhong> findByTrangThai(TrangThaiDatPhong tt) { return tt == datPhong101.getTrangThai() ? List.of(datPhong101) : Collections.emptyList(); }
            @Override public boolean updateTrangThai(String maDatPhong, TrangThaiDatPhong trangThaiMoi) { return true; }
            @Override public boolean updateTrangThai(Connection conn, String maDatPhong, TrangThaiDatPhong trangThaiMoi) { return true; }
            @Override public boolean isPhongBiTrungLich(String maPhong, LocalDateTime in, LocalDateTime out, String ignore) { return false; }
            @Override public boolean isPhongBiTrungLich(Connection conn, String maPhong, LocalDateTime in, LocalDateTime out, String ignore) { return false; }
            @Override public boolean save(Connection conn, DatPhong entity) { return true; }
            @Override public boolean xacNhanCheckIn(String maDatPhong, LocalDateTime thoiGianCheckInThucTe) { return true; }
            @Override public boolean xacNhanCheckIn(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckInThucTe) { return true; }
            @Override public boolean xacNhanCheckIn(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckInThucTe, String maNvCheckIn) { return true; }
            @Override public boolean xacNhanCheckOut(String maDatPhong, LocalDateTime thoiGianCheckOutThucTe) { return true; }
            @Override public boolean xacNhanCheckOut(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckOutThucTe, String maNvCheckOut) { return true; }
            @Override public boolean hasDatPhongXacNhanTrongNgay(String maPhong, LocalDate ngay) { return false; }
            @Override public Optional<DatPhong> findByIdForUpdate(Connection conn, String id) { return findById(id); }
            @Override public boolean isPhongDangCoNguoiO(Connection conn, String maPhong, String exclude) { return datPhong101.getTrangThai() == TrangThaiDatPhong.DANG_O; }
            @Override public Optional<DatPhong> findDangOByPhong(String maPhong) { return datPhong101.getTrangThai() == TrangThaiDatPhong.DANG_O ? Optional.of(datPhong101) : Optional.empty(); }
        };

        mockDichVuDAO = new DichVuDAO() {
            @Override public Optional<DichVu> findById(String id) { return "DV001".equals(id) ? Optional.of(dvCoca) : Optional.empty(); }
            @Override public List<DichVu> findAll() { return List.of(dvCoca); }
            @Override public boolean save(DichVu entity) { return true; }
            @Override public boolean update(DichVu entity) { return true; }
            @Override public boolean delete(String id) { return true; }
            @Override public List<DichVu> findDangKinhDoanh() { return List.of(dvCoca); }
            @Override public List<DichVu> search(String kw) { return List.of(dvCoca); }
        };

        mockDichVuService = new DichVuService(mockDichVuDAO, null, mockDatPhongDAO, null) {
            @Override
            public void ghiNhanDichVu(String maDatPhong, String maDichVu, int soLuong) {
                if (datPhong101.getTrangThai() != TrangThaiDatPhong.DANG_O) {
                    throw new BusinessException("Đơn đặt phòng " + maDatPhong + " đã trả phòng, không thể ghi nhận dịch vụ!");
                }
            }

            @Override
            public void ghiNhanDanhSachDichVu(String maDatPhong, List<DonDichVuPhong.ChiTietMon> danhSachMon) {
                if (datPhong101.getTrangThai() != TrangThaiDatPhong.DANG_O) {
                    throw new BusinessException("Đơn đặt phòng " + maDatPhong + " đã trả phòng, không thể ghi nhận dịch vụ!");
                }
            }
        };

        roomOrderService = new RoomOrderService(mockDatPhongDAO, mockPhongDAO, mockDichVuDAO, mockDichVuService);
    }

    @AfterEach
    void tearDown() {
        RoomOrderService.clearAllForTest();
        RoomOrderHttpServer.stopServer();
    }

    @Test
    @DisplayName("Khách tại phòng đang OCCUPIED đặt dịch vụ thành công, tạo đơn CHO_TIEP_NHAN")
    void testTaoOrderThanhCong_PhongDangO() {
        List<ChiTietMon> items = List.of(new ChiTietMon("DV001", "Coca-Cola", 2, null));
        DonDichVuPhong order = roomOrderService.taoYeuCauOrder("101", "token-1", items, "Cho xin thêm đá", HinhThucThanhToanOrder.GHI_VAO_HOA_DON);

        assertNotNull(order);
        assertTrue(order.getOrderId().startsWith("ORD-"));
        assertEquals("101", order.getSoPhong());
        assertEquals(TrangThaiOrder.CHO_TIEP_NHAN, order.getTrangThai());
        assertEquals(new BigDecimal("40000"), order.tinhTongTien(), "2 lon Coca x 20.000 = 40.000 đ");
        assertTrue(roomOrderService.coDonChoDuyetCuaPhong("P101"));
    }

    @Test
    @DisplayName("Từ chối đặt dịch vụ nếu phòng không ở trạng thái OCCUPIED (ví dụ phòng trống hoặc đang dọn dẹp)")
    void testTuChoiOrder_KhiPhongKhongCoKhach() {
        phong101.setTrangThaiHienTai(TrangThaiPhong.AVAILABLE);

        List<ChiTietMon> items = List.of(new ChiTietMon("DV001", "Coca-Cola", 1, null));
        assertThrows(BusinessException.class, () -> {
            roomOrderService.taoYeuCauOrder("101", "token-2", items, "", HinhThucThanhToanOrder.GHI_VAO_HOA_DON);
        }, "Phòng trống không được phép gọi dịch vụ");
    }

    @Test
    @DisplayName("Chống Double-Submit qua Idempotency: Nhiều luồng gửi cùng clientToken chỉ tạo đúng 1 đơn duy nhất")
    void testChongDoubleSubmit_Idempotency() throws InterruptedException {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threads);

        List<ChiTietMon> items = List.of(new ChiTietMon("DV001", "Coca-Cola", 2, null));
        String idempotencyToken = "IDEMPOTENT-TOKEN-UUID-999";

        Set<String> orderIds = Collections.synchronizedSet(new HashSet<>());

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    DonDichVuPhong o = roomOrderService.taoYeuCauOrder("101", idempotencyToken, items, "Giao nhanh", HinhThucThanhToanOrder.GHI_VAO_HOA_DON);
                    orderIds.add(o.getOrderId());
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(1, orderIds.size(), "Tất cả các luồng bấm lặp phải nhận về cùng 1 mã đơn hàng duy nhất");
        assertEquals(1, roomOrderService.layTatCaDon().size(), "Hệ thống chỉ tạo ra duy nhất 1 bản ghi đơn hàng");
    }

    @Test
    @DisplayName("Lễ tân tiếp nhận đơn hàng chuyển sang DA_TIEP_NHAN và ghi nhận dịch vụ vào Hóa đơn")
    void testTiepNhanVaGiaoHang_ThanhCong() {
        List<ChiTietMon> items = List.of(new ChiTietMon("DV001", "Coca-Cola", 2, null));
        DonDichVuPhong order = roomOrderService.taoYeuCauOrder("101", "token-3", items, "", HinhThucThanhToanOrder.GHI_VAO_HOA_DON);

        roomOrderService.tiepNhanVaGiaoHang(order.getOrderId(), "NV001");

        assertEquals(TrangThaiOrder.DA_TIEP_NHAN, order.getTrangThai());
        assertEquals("NV001", order.getNguoiDuyet());
        assertFalse(roomOrderService.coDonChoDuyetCuaPhong("P101"), "Đã duyệt xong thì không còn đơn chờ tiếp nhận");
    }

    @Test
    @DisplayName("Xung đột tương tranh với Check-out: Khi phòng đã Check-out trước khi duyệt, đơn tự động bị từ chối")
    void testXungDotCheckOutVsOrder() {
        List<ChiTietMon> items = List.of(new ChiTietMon("DV001", "Coca-Cola", 2, null));
        DonDichVuPhong order = roomOrderService.taoYeuCauOrder("101", "token-4", items, "", HinhThucThanhToanOrder.GHI_VAO_HOA_DON);

        // Giả lập lễ tân làm thủ tục Check-out xong trước khi đơn được tiếp nhận
        datPhong101.setTrangThai(TrangThaiDatPhong.DA_TRA_PHONG);

        assertThrows(BusinessException.class, () -> {
            roomOrderService.tiepNhanVaGiaoHang(order.getOrderId(), "NV001");
        });

        assertEquals(TrangThaiOrder.TU_CHOI, order.getTrangThai(), "Đơn phải tự động chuyển thành TU_CHOI");
        assertTrue(order.getLyDoTuChoi().contains("đã trả phòng"), "Ghi nhận rõ lý do từ chối kiểm toán");
    }

    @Test
    @DisplayName("Kiểm thử HTTP Server nhúng: Khởi động server, lấy Menu JSON và đặt món qua REST API")
    void testHttpServerEndpoints() throws Exception {
        int testPort = 8099;
        RoomOrderHttpServer.setDependenciesForTesting(roomOrderService, mockDichVuDAO, mockPhongDAO);
        try {
            boolean started = RoomOrderHttpServer.startServer(testPort);
            if (!started) {
                System.out.println("Bỏ qua test HTTP REST do sandbox chặn bind port");
                return;
            }

            HttpClient client = HttpClient.newHttpClient();

            // 1. Kiểm tra GET /api/menu
            HttpRequest reqMenu = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + testPort + "/api/menu"))
                    .GET()
                    .build();
            HttpResponse<String> resMenu = client.send(reqMenu, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, resMenu.statusCode());
            assertTrue(resMenu.body().contains("Coca-Cola"));

            // 2. Kiểm tra POST /api/order
            String jsonPayload = "{\"room\":\"101\",\"clientToken\":\"REST-1\",\"items\":[{\"ma\":\"DV001\",\"ten\":\"Coca\",\"soLuong\":1}],\"ghiChu\":\"Đá lạnh\",\"paymentMethod\":\"GHI_VAO_HOA_DON\"}";
            HttpRequest reqOrder = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:" + testPort + "/api/order"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> resOrder = client.send(reqOrder, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, resOrder.statusCode());
            assertTrue(resOrder.body().contains("\"success\":true"));

            // 3. Xác minh đơn đã vào hệ thống
            assertEquals(1, roomOrderService.layDanhSachChoDuyet().size());
            assertEquals("101", roomOrderService.layDanhSachChoDuyet().get(0).getSoPhong());
        } finally {
            RoomOrderHttpServer.stopServer();
        }
    }

    @Test
    @DisplayName("Kiểm tra triệt tiêu va chạm ID (Zero-Collision) khi 50 luồng đồng thời tạo mã đơn")
    void testZeroCollisionOrderIdGeneration() throws InterruptedException {
        int soLuongLuong = 50;
        Set<String> danhSachMaDon = Collections.synchronizedSet(new HashSet<>());
        ExecutorService executor = Executors.newFixedThreadPool(soLuongLuong);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(soLuongLuong);

        for (int i = 0; i < soLuongLuong; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    DonDichVuPhong don = roomOrderService.taoYeuCauOrder(
                            "101",
                            "TOKEN-RACE-" + index,
                            List.of(new DonDichVuPhong.ChiTietMon("DV001", "Coca-Cola", 1, new BigDecimal("20000"))),
                            "Test race",
                            DonDichVuPhong.HinhThucThanhToanOrder.GHI_VAO_HOA_DON
                    );
                    danhSachMaDon.add(don.getOrderId());
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        // Khẳng định: Toàn bộ 50 luồng sinh ra 50 mã đơn hoàn toàn khác nhau, tỷ lệ va chạm = 0%!
        assertEquals(soLuongLuong, danhSachMaDon.size(), "Toàn bộ 50 mã đơn phải là duy nhất tuyệt đối, không có va chạm ID");
    }
}
