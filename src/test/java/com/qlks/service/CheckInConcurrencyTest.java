package com.qlks.service;

import com.qlks.dao.DatPhongDAO;
import com.qlks.dao.HoaDonDAO;
import com.qlks.dao.KhachLuuTruDAO;
import com.qlks.dao.PhongDAO;
import com.qlks.dao.ChiTietDichVuDAO;
import com.qlks.exception.BusinessException;
import com.qlks.model.*;
import com.qlks.model.enums.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Chống Race Condition & Double Check-in (CheckInConcurrencyTest)")
class CheckInConcurrencyTest {

    private final Map<String, DatPhong> bangDatPhong = new ConcurrentHashMap<>();
    private final Map<String, Phong> bangPhong = new ConcurrentHashMap<>();
    private final Map<String, HoaDon> bangHoaDon = new ConcurrentHashMap<>();
    private final Map<String, List<KhachLuuTru>> bangKhachLuuTru = new ConcurrentHashMap<>();

    private DatPhongDAO mockDatPhongDAO;
    private PhongDAO mockPhongDAO;
    private HoaDonDAO mockHoaDonDAO;
    private KhachLuuTruDAO mockKhachLuuTruDAO;
    private ChiTietDichVuDAO mockChiTietDichVuDAO;

    private Connection mockConn;
    private NhanVien nv1;
    private NhanVien nv2;

    @BeforeEach
    void setUp() {
        nv1 = new LeTan("NV001", "Lễ Tân 1", "0901000001", "lt1@qlks.com", "letan1",
                LocalDate.now(), BigDecimal.valueOf(8000000), true, 26, BigDecimal.ZERO);
        nv2 = new LeTan("NV002", "Lễ Tân 2", "0901000002", "lt2@qlks.com", "letan2",
                LocalDate.now(), BigDecimal.valueOf(8000000), true, 26, BigDecimal.ZERO);

        AuthService.setTaiKhoanHienTai(new TaiKhoan("letan1", "hash", VaiTro.LE_TAN, true));
        AuthService.setNhanVienHienTai(nv1);

        bangDatPhong.clear();
        bangPhong.clear();
        bangHoaDon.clear();
        bangKhachLuuTru.clear();

        // Phòng 101 chuẩn
        Phong p101 = new PhongThuong("P101", "101", 1, BigDecimal.valueOf(500000),
                TrangThaiPhong.AVAILABLE, LoaiGiuong.DON, true, true);
        bangPhong.put("P101", p101);

        // Khách hàng
        KhachHang kh1 = new KhachHang("KH001", "Khách A", "0911000001", "kha@test.com", "001200000001", LocalDate.now());
        KhachHang kh2 = new KhachHang("KH002", "Khách B", "0911000002", "khb@test.com", "001200000002", LocalDate.now());

        // Đơn 1
        DatPhong dp1 = new DatPhong("DP001", kh1, p101, nv1,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, LocalDateTime.now());
        bangDatPhong.put("DP001", dp1);
        List<KhachLuuTru> kList1 = new ArrayList<>();
        kList1.add(new KhachLuuTru("DP001", "Khách A", "001200000001", "0911000001", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        bangKhachLuuTru.put("DP001", kList1);

        // Đơn 2 (cùng phòng 101 để test va chạm trùng phòng)
        DatPhong dp2 = new DatPhong("DP002", kh2, p101, nv1,
                LocalDateTime.now(), LocalDateTime.now().plusDays(1),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, LocalDateTime.now());
        bangDatPhong.put("DP002", dp2);
        List<KhachLuuTru> kList2 = new ArrayList<>();
        kList2.add(new KhachLuuTru("DP002", "Khách B", "001200000002", "0911000002", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        bangKhachLuuTru.put("DP002", kList2);

        mockConn = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("isClosed".equals(name)) return false;
                    return null;
                }
        );

        mockDatPhongDAO = new DatPhongDAO() {
            @Override public Optional<DatPhong> findById(String id) { return Optional.ofNullable(bangDatPhong.get(id)); }
            @Override public Optional<DatPhong> findById(Connection conn, String id) { return findById(id); }
            @Override public synchronized Optional<DatPhong> findByIdForUpdate(Connection conn, String id) { return findById(id); }
            @Override public List<DatPhong> findByKhachHang(String maKhachHang) { return Collections.emptyList(); }
            @Override public List<DatPhong> findByPhong(String maPhong) { return Collections.emptyList(); }
            @Override public List<DatPhong> findByTrangThai(TrangThaiDatPhong trangThai) { return Collections.emptyList(); }
            @Override public boolean updateTrangThai(String maDatPhong, TrangThaiDatPhong trangThaiMoi) { return true; }
            @Override public boolean updateTrangThai(Connection conn, String maDatPhong, TrangThaiDatPhong trangThaiMoi) { return true; }
            @Override public boolean xacNhanCheckIn(String maDatPhong, LocalDateTime thoiGianCheckInThucTe) { return true; }
            @Override public boolean xacNhanCheckIn(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckInThucTe) { return true; }

            // Mô phỏng chuẩn Atomic CAS UPDATE dat_phong ... WHERE ma_dat_phong = ? AND trang_thai = 'DA_XAC_NHAN'
            @Override
            public synchronized boolean xacNhanCheckIn(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckInThucTe, String maNvCheckIn) {
                DatPhong dp = bangDatPhong.get(maDatPhong);
                if (dp != null && dp.getTrangThai() == TrangThaiDatPhong.DA_XAC_NHAN) {
                    dp.setTrangThai(TrangThaiDatPhong.DANG_O);
                    dp.setThoiGianNhanThucTe(thoiGianCheckInThucTe);
                    dp.setMaNvCheckIn(maNvCheckIn);
                    return true;
                }
                return false;
            }

            @Override public boolean xacNhanCheckOut(String maDatPhong, LocalDateTime thoiGianCheckOutThucTe) { return true; }
            @Override public boolean xacNhanCheckOut(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckOutThucTe) { return true; }

            // Mô phỏng chuẩn Atomic CAS UPDATE dat_phong ... WHERE ma_dat_phong = ? AND trang_thai = 'DANG_O'
            @Override
            public synchronized boolean xacNhanCheckOut(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckOutThucTe, String maNvCheckOut) {
                DatPhong dp = bangDatPhong.get(maDatPhong);
                if (dp != null && dp.getTrangThai() == TrangThaiDatPhong.DANG_O) {
                    dp.setTrangThai(TrangThaiDatPhong.DA_TRA_PHONG);
                    dp.setThoiGianTraThucTe(thoiGianCheckOutThucTe);
                    dp.setMaNvCheckOut(maNvCheckOut);
                    return true;
                }
                return false;
            }

            @Override
            public synchronized boolean isPhongDangCoNguoiO(Connection conn, String maPhong, String excludeMaDatPhong) {
                return bangDatPhong.values().stream().anyMatch(d ->
                        d.getPhong() != null
                                && maPhong.equals(d.getPhong().getMaPhong())
                                && d.getTrangThai() == TrangThaiDatPhong.DANG_O
                                && (excludeMaDatPhong == null || !excludeMaDatPhong.equals(d.getMaDatPhong()))
                );
            }

            @Override public boolean isPhongBiTrungLich(String maPhong, LocalDateTime ngayNhanMoi, LocalDateTime ngayTraMoi, String excludeMaDatPhong) { return false; }
            @Override public boolean isPhongBiTrungLich(Connection conn, String maPhong, LocalDateTime ngayNhanMoi, LocalDateTime ngayTraMoi, String excludeMaDatPhong) { return false; }
            @Override public boolean hasDatPhongXacNhanTrongNgay(String maPhong, LocalDate ngay) { return false; }
            @Override public boolean save(Connection conn, DatPhong datPhong) { return true; }
            @Override public List<DatPhong> findAll() { return new ArrayList<>(bangDatPhong.values()); }
            @Override public boolean save(DatPhong datPhong) { return true; }
            @Override public boolean update(DatPhong datPhong) { return true; }
            @Override public boolean delete(String id) { return true; }
        };

        mockPhongDAO = new PhongDAO() {
            @Override public Optional<Phong> findById(String id) { return Optional.ofNullable(bangPhong.get(id)); }
            @Override public synchronized Optional<Phong> findByIdForUpdate(Connection conn, String id) { return findById(id); }
            @Override public Optional<Phong> findBySoPhong(String soPhong) { return Optional.empty(); }
            @Override public List<Phong> findByTrangThai(TrangThaiPhong trangThai) { return Collections.emptyList(); }
            @Override public List<Phong> findPhongTrong(LocalDateTime checkIn, LocalDateTime checkOut) { return Collections.emptyList(); }
            @Override public boolean updateTrangThai(String maPhong, TrangThaiPhong trangThaiMoi) {
                Phong p = bangPhong.get(maPhong);
                if (p != null) { p.setTrangThaiHienTai(trangThaiMoi); return true; }
                return false;
            }
            @Override public boolean updateTrangThai(Connection conn, String maPhong, TrangThaiPhong trangThaiMoi) {
                return updateTrangThai(maPhong, trangThaiMoi);
            }

            // Mô phỏng chuẩn Atomic CAS UPDATE phong SET trang_thai_hien_tai = 'OCCUPIED' WHERE ma_phong = ? AND trang_thai_hien_tai IN ('AVAILABLE', 'RESERVED')
            @Override
            public synchronized boolean updateTrangThaiCheckIn(Connection conn, String maPhong) {
                Phong p = bangPhong.get(maPhong);
                if (p != null && (p.getTrangThaiHienTai() == TrangThaiPhong.AVAILABLE || p.getTrangThaiHienTai() == TrangThaiPhong.RESERVED)) {
                    p.setTrangThaiHienTai(TrangThaiPhong.OCCUPIED);
                    return true;
                }
                return false;
            }

            @Override public boolean updateKinhDoanh(String maPhong, boolean dangKinhDoanh) { return true; }
            @Override public List<Phong> findAll() { return new ArrayList<>(bangPhong.values()); }
            @Override public boolean save(Phong phong) { return true; }
            @Override public boolean update(Phong phong) { return true; }
            @Override public boolean delete(String id) { return true; }
        };

        mockKhachLuuTruDAO = new KhachLuuTruDAO() {
            @Override public List<KhachLuuTru> findByMaDatPhong(String maDatPhong) {
                return bangKhachLuuTru.getOrDefault(maDatPhong, Collections.emptyList());
            }
            @Override public boolean save(Connection conn, KhachLuuTru khach) { return true; }
            @Override public boolean saveAll(Connection conn, String maDatPhong, List<KhachLuuTru> danhSachKhach) { return true; }
            @Override public boolean deleteByMaDatPhong(Connection conn, String maDatPhong) { return true; }
        };

        mockHoaDonDAO = new HoaDonDAO() {
            @Override public Optional<HoaDon> findById(String id) { return Optional.ofNullable(bangHoaDon.get(id)); }
            @Override public Optional<HoaDon> findById(Connection conn, String id) { return findById(id); }
            @Override public Optional<HoaDon> findByIdForUpdate(Connection conn, String id) { return findById(id); }
            @Override public Optional<HoaDon> findByMaDatPhong(String maDatPhong) { return Optional.ofNullable(bangHoaDon.get(maDatPhong)); }
            @Override public Optional<HoaDon> findByMaDatPhong(Connection conn, String maDatPhong) { return findByMaDatPhong(maDatPhong); }
            @Override public List<HoaDon> findAll() { return new ArrayList<>(bangHoaDon.values()); }
            @Override public boolean save(HoaDon hoaDon) { bangHoaDon.put(hoaDon.getDatPhong().getMaDatPhong(), hoaDon); return true; }
            @Override public boolean save(Connection conn, HoaDon hoaDon) { return save(hoaDon); }
            @Override public boolean update(HoaDon hoaDon) { return save(hoaDon); }
            @Override public boolean update(Connection conn, HoaDon hoaDon) { return save(hoaDon); }
            @Override public boolean delete(String id) { return bangHoaDon.remove(id) != null; }
            @Override public boolean updateTrangThai(String maHoaDon, TrangThaiThanhToan trangThaiMoi) { return true; }
            @Override public boolean updateTrangThai(Connection conn, String maHoaDon, TrangThaiThanhToan trangThaiMoi) { return true; }
            @Override public boolean updateTien(String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) { return true; }
            @Override public boolean updateTien(Connection conn, String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) { return true; }
        };

        mockChiTietDichVuDAO = new ChiTietDichVuDAO() {
            @Override public List<ChiTietDichVu> findByMaDatPhong(String maDatPhong) { return Collections.emptyList(); }
            @Override public List<ChiTietDichVu> findByMaDatPhong(Connection conn, String maDatPhong) { return Collections.emptyList(); }
            @Override public boolean save(ChiTietDichVu chiTietDichVu) { return true; }
            @Override public boolean save(Connection conn, ChiTietDichVu chiTietDichVu) { return true; }
            @Override public boolean delete(Long id) { return true; }
            @Override public boolean deleteByMaDatPhong(String maDatPhong) { return true; }
        };
    }

    @AfterEach
    void tearDown() {
        AuthService.dangXuat();
    }

    private DatPhongService taoService() {
        return new DatPhongService(mockDatPhongDAO, mockPhongDAO, mockHoaDonDAO, mockKhachLuuTruDAO, mockChiTietDichVuDAO) {
            @Override
            protected Connection layKetNoi() throws SQLException {
                return mockConn;
            }
        };
    }

    @Test
    @DisplayName("Chống Double Check-in cùng đơn: Lần đầu thành công, lần 2 bị từ chối rõ ràng và không bị ghi đè Audit Trail")
    void testDoubleCheckIn_CungMotDon_ChiMotLuongThanhCong() {
        DatPhongService service = taoService();

        // 1. Lễ tân 1 Check-in thành công
        LocalDateTime thoiGian1 = LocalDateTime.now().minusMinutes(5);
        service.xacNhanCheckIn("DP001", thoiGian1);

        DatPhong dpSauLuot1 = bangDatPhong.get("DP001");
        assertEquals(TrangThaiDatPhong.DANG_O, dpSauLuot1.getTrangThai());
        assertEquals("NV001", dpSauLuot1.getMaNvCheckIn(), "Mã NV check-in phải là NV001");
        assertEquals(TrangThaiPhong.OCCUPIED, bangPhong.get("P101").getTrangThaiHienTai());

        // 2. Lễ tân 2 cố gắng check-in lại cùng đơn DP001
        AuthService.setNhanVienHienTai(nv2);
        LocalDateTime thoiGian2 = LocalDateTime.now();

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.xacNhanCheckIn("DP001", thoiGian2)
        );

        assertTrue(ex.getMessage().contains("đã được Check-in") || ex.getMessage().contains("Đang ở") || ex.getMessage().contains("DANG_O") || ex.getMessage().contains("nhận phòng này trước đó"),
                "Thông báo lỗi phải chỉ rõ đơn đã được check-in: " + ex.getMessage());

        // Kiểm tra bảo toàn dữ liệu kiểm toán (Audit Trail KHÔNG bị ghi đè bởi Lễ tân 2)
        DatPhong dpSauLuot2 = bangDatPhong.get("DP001");
        assertEquals("NV001", dpSauLuot2.getMaNvCheckIn(), "Audit Trail mã NV check-in tuyệt đối không bị ghi đè!");
        assertEquals(thoiGian1, dpSauLuot2.getThoiGianNhanThucTe(), "Thời gian check-in thực tế ban đầu phải được giữ nguyên!");
    }

    @Test
    @DisplayName("Chống Cross-booking Collision: Hai đơn khác nhau cùng phòng -> Đơn 1 nhận phòng, Đơn 2 bị từ chối")
    void testCrossBookingCollision_HaiDonCungPhong_DonThuHaiBiTuChoi() {
        DatPhongService service = taoService();

        // 1. Đơn DP001 check in vào phòng 101 thành công
        service.xacNhanCheckIn("DP001", LocalDateTime.now());
        assertEquals(TrangThaiDatPhong.DANG_O, bangDatPhong.get("DP001").getTrangThai());
        assertEquals(TrangThaiPhong.OCCUPIED, bangPhong.get("P101").getTrangThaiHienTai());

        // 2. Đơn DP002 cũng được gán phòng 101 cố gắng check in
        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.xacNhanCheckIn("DP002", LocalDateTime.now())
        );

        assertTrue(ex.getMessage().contains("OCCUPIED") || ex.getMessage().contains("đang có khách") || ex.getMessage().contains("lưu trú"),
                "Phải chặn đơn thứ hai với lý do phòng đã có khách lưu trú: " + ex.getMessage());

        // Đơn DP002 vẫn giữ nguyên trạng thái DA_XAC_NHAN, không bị đổi sang DANG_O
        assertEquals(TrangThaiDatPhong.DA_XAC_NHAN, bangDatPhong.get("DP002").getTrangThai());
    }

    @Test
    @DisplayName("Chống Check-in khi phòng đang dọn dẹp (CLEANING) hoặc bảo trì (MAINTENANCE)")
    void testCheckIn_PhongChuaSanSang_BiTuChoi() {
        DatPhongService service = taoService();

        // Phòng đang CLEANING
        bangPhong.get("P101").setTrangThaiHienTai(TrangThaiPhong.CLEANING);
        BusinessException exClean = assertThrows(BusinessException.class, () ->
                service.xacNhanCheckIn("DP001", LocalDateTime.now())
        );
        assertTrue(exClean.getMessage().contains("CLEANING") || exClean.getMessage().contains("dọn dẹp"));

        // Phòng đang MAINTENANCE
        bangPhong.get("P101").setTrangThaiHienTai(TrangThaiPhong.MAINTENANCE);
        BusinessException exMaint = assertThrows(BusinessException.class, () ->
                service.xacNhanCheckIn("DP001", LocalDateTime.now())
        );
        assertTrue(exMaint.getMessage().contains("bảo trì"));
    }

    @Test
    @DisplayName("Kiểm thử Đa luồng (Multi-threading): 10 luồng cùng check-in 1 đơn đồng thời -> Đúng 1 luồng thành công")
    void testCheckInConcurrent_10LuongDongThoi_Chi1ThanhCong() throws Exception {
        DatPhongService service = taoService();
        int soLuongLuong = 10;
        ExecutorService executor = Executors.newFixedThreadPool(soLuongLuong);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(soLuongLuong);

        AtomicInteger thanhCong = new AtomicInteger(0);
        AtomicInteger thatBai = new AtomicInteger(0);

        for (int i = 0; i < soLuongLuong; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Đợi cả 10 luồng cùng sẵn sàng để bắn đồng thời
                    service.xacNhanCheckIn("DP001", LocalDateTime.now());
                    thanhCong.incrementAndGet();
                } catch (BusinessException e) {
                    thatBai.incrementAndGet();
                } catch (Exception e) {
                    // unexpected
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Bắn đồng thời
        boolean finished = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Toàn bộ luồng phải hoàn thành trong 5 giây");
        assertEquals(1, thanhCong.get(), "Chính xác DUY NHẤT 1 luồng check-in thành công");
        assertEquals(soLuongLuong - 1, thatBai.get(), "Toàn bộ 9 luồng còn lại phải bị từ chối sạch sẽ");
        assertEquals(TrangThaiDatPhong.DANG_O, bangDatPhong.get("DP001").getTrangThai());
        assertEquals(TrangThaiPhong.OCCUPIED, bangPhong.get("P101").getTrangThaiHienTai());
    }

    @Test
    @DisplayName("Chống Double Check-out: Check-out lần 2 trên cùng đơn bị từ chối")
    void testDoubleCheckOut_CungDon_ChiMotLanThanhCong() {
        DatPhongService service = taoService();

        // 1. Cho khách check in hôm nay
        service.xacNhanCheckIn("DP001", LocalDateTime.now());
        assertEquals(TrangThaiDatPhong.DANG_O, bangDatPhong.get("DP001").getTrangThai());

        // 2. Lễ tân Check-out hôm sau -> Thành công
        HoaDon hd1 = service.xacNhanCheckOut("DP001", LocalDateTime.now().plusDays(1));
        assertNotNull(hd1);
        assertEquals(TrangThaiDatPhong.DA_TRA_PHONG, bangDatPhong.get("DP001").getTrangThai());
        assertEquals(TrangThaiPhong.CLEANING, bangPhong.get("P101").getTrangThaiHienTai());

        // 3. Lễ tân cố tình Check-out lần 2 trên đơn đã trả phòng
        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.xacNhanCheckOut("DP001", LocalDateTime.now())
        );
        assertTrue(ex.getMessage().contains("Check-out trước đó") || ex.getMessage().contains("DANG_O"),
                "Phải chặn check out lần 2: " + ex.getMessage());
    }
}
