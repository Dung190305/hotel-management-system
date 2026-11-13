package com.qlks.service;

import com.qlks.dao.*;
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

@DisplayName("Kiểm thử Chống Race Condition giữa Ghi nhận Dịch vụ và Check-out (DichVuVsCheckOutConcurrencyTest)")
class DichVuVsCheckOutConcurrencyTest {

    private final Map<String, DatPhong> bangDatPhong = new ConcurrentHashMap<>();
    private final Map<String, Phong> bangPhong = new ConcurrentHashMap<>();
    private final Map<String, DichVu> bangDichVu = new ConcurrentHashMap<>();
    private final List<ChiTietDichVu> bangChiTietDichVu = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, HoaDon> bangHoaDon = new ConcurrentHashMap<>();

    private DatPhongDAO mockDatPhongDAO;
    private PhongDAO mockPhongDAO;
    private HoaDonDAO mockHoaDonDAO;
    private KhachLuuTruDAO mockKhachLuuTruDAO;
    private ChiTietDichVuDAO mockChiTietDichVuDAO;
    private DichVuDAO mockDichVuDAO;

    private Connection mockConn;
    private NhanVien nv;
    private long ctdvSeq = 1;
    private final java.util.concurrent.locks.ReentrantLock lockDatPhong = new java.util.concurrent.locks.ReentrantLock(true);

    @BeforeEach
    void setUp() {
        if (lockDatPhong.isHeldByCurrentThread()) {
            lockDatPhong.unlock();
        }
        nv = new LeTan("NV001", "Lễ Tân 1", "0901000001", "lt1@qlks.com", "letan1",
                LocalDate.now(), BigDecimal.valueOf(8000000), true, 26, BigDecimal.ZERO);
        AuthService.setTaiKhoanHienTai(new TaiKhoan("letan1", "hash", VaiTro.LE_TAN, true));
        AuthService.setNhanVienHienTai(nv);

        bangDatPhong.clear();
        bangPhong.clear();
        bangDichVu.clear();
        bangChiTietDichVu.clear();
        bangHoaDon.clear();
        ctdvSeq = 1;

        // 1. Khởi tạo phòng
        Phong p101 = new PhongThuong("P101", "101", 1, BigDecimal.valueOf(500000),
                TrangThaiPhong.OCCUPIED, LoaiGiuong.DON, true, true);
        bangPhong.put("P101", p101);

        // 2. Khởi tạo dịch vụ
        DichVu coca = new DichVu("DV_COCA", "Lon Coca", BigDecimal.valueOf(20000), true);
        DichVu snack = new DichVu("DV_SNACK", "Gói Snack", BigDecimal.valueOf(15000), true);
        bangDichVu.put("DV_COCA", coca);
        bangDichVu.put("DV_SNACK", snack);

        // 3. Khởi tạo phiếu đặt phòng đang ở (DANG_O)
        KhachHang kh = new KhachHang("KH001", "Khách Test", "0911000001", "kh@test.com", "001200000001", LocalDate.now());
        DatPhong dp = new DatPhong("DP_DANG_O", kh, p101, nv,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                LocalDateTime.now().minusDays(1), null, 1, TrangThaiDatPhong.DANG_O, BigDecimal.ZERO, LocalDateTime.now().minusDays(1));
        dp.addKhachLuuTru(new KhachLuuTru("DP_DANG_O", "Khách Test", "001200000001", "0911000001", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        bangDatPhong.put("DP_DANG_O", dp);

        mockConn = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("commit".equals(name) || "rollback".equals(name) || "close".equals(name)) {
                        if (lockDatPhong.isHeldByCurrentThread()) {
                            lockDatPhong.unlock();
                        }
                        return null;
                    }
                    if ("isClosed".equals(name)) return false;
                    return null;
                }
        );

        mockDatPhongDAO = new DatPhongDAO() {
            @Override public Optional<DatPhong> findById(String id) { return Optional.ofNullable(bangDatPhong.get(id)); }
            @Override public Optional<DatPhong> findById(Connection conn, String id) { return findById(id); }
            @Override
            public Optional<DatPhong> findByIdForUpdate(Connection conn, String id) {
                if (!lockDatPhong.isHeldByCurrentThread()) {
                    lockDatPhong.lock();
                }
                return findById(id);
            }
            @Override public List<DatPhong> findByKhachHang(String maKhachHang) { return Collections.emptyList(); }
            @Override public List<DatPhong> findByPhong(String maPhong) { return Collections.emptyList(); }
            @Override public List<DatPhong> findByTrangThai(TrangThaiDatPhong trangThai) { return Collections.emptyList(); }
            @Override public boolean updateTrangThai(String maDatPhong, TrangThaiDatPhong trangThaiMoi) { return true; }
            @Override public boolean updateTrangThai(Connection conn, String maDatPhong, TrangThaiDatPhong trangThaiMoi) { return true; }
            @Override public boolean xacNhanCheckIn(String maDatPhong, LocalDateTime thoiGianCheckInThucTe) { return true; }
            @Override public boolean xacNhanCheckIn(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckInThucTe) { return true; }
            @Override public boolean xacNhanCheckIn(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckInThucTe, String maNvCheckIn) { return true; }
            @Override public boolean xacNhanCheckOut(String maDatPhong, LocalDateTime thoiGianCheckOutThucTe) { return true; }
            @Override public boolean xacNhanCheckOut(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckOutThucTe) { return true; }

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
            @Override public boolean updateKinhDoanh(String maPhong, boolean dangKinhDoanh) { return true; }
            @Override public List<Phong> findAll() { return new ArrayList<>(bangPhong.values()); }
            @Override public boolean save(Phong phong) { return true; }
            @Override public boolean update(Phong phong) { return true; }
            @Override public boolean delete(String id) { return true; }
        };

        mockChiTietDichVuDAO = new ChiTietDichVuDAO() {
            @Override
            public synchronized List<ChiTietDichVu> findByMaDatPhong(String maDatPhong) {
                return bangChiTietDichVu.stream().filter(c -> maDatPhong.equals(c.getMaDatPhong())).toList();
            }

            @Override
            public synchronized List<ChiTietDichVu> findByMaDatPhong(Connection conn, String maDatPhong) {
                return findByMaDatPhong(maDatPhong);
            }

            @Override
            public synchronized List<ChiTietDichVu> findByMaDatPhongForUpdate(Connection conn, String maDatPhong) {
                return findByMaDatPhong(maDatPhong);
            }

            @Override
            public synchronized boolean save(ChiTietDichVu ctdv) {
                ctdv.setId(ctdvSeq++);
                bangChiTietDichVu.add(ctdv);
                return true;
            }

            @Override
            public synchronized boolean save(Connection conn, ChiTietDichVu ctdv) {
                return save(ctdv);
            }

            @Override public boolean delete(Long id) { return true; }
            @Override public boolean deleteByMaDatPhong(String maDatPhong) { return true; }
        };

        mockDichVuDAO = new DichVuDAO() {
            @Override public Optional<DichVu> findById(String id) { return Optional.ofNullable(bangDichVu.get(id)); }
            @Override public List<DichVu> findAll() { return new ArrayList<>(bangDichVu.values()); }
            @Override public List<DichVu> findDangKinhDoanh() { return findAll(); }
            @Override public List<DichVu> search(String keyword) { return findAll(); }
            @Override public boolean save(DichVu entity) { return true; }
            @Override public boolean update(DichVu entity) { return true; }
            @Override public boolean delete(String id) { return true; }
        };

        mockHoaDonDAO = new HoaDonDAO() {
            @Override public Optional<HoaDon> findById(String id) { return Optional.ofNullable(bangHoaDon.get(id)); }
            @Override public Optional<HoaDon> findById(Connection conn, String id) { return findById(id); }
            @Override public synchronized Optional<HoaDon> findByIdForUpdate(Connection conn, String id) { return findById(id); }
            @Override public Optional<HoaDon> findByMaDatPhong(String maDatPhong) { return Optional.ofNullable(bangHoaDon.get(maDatPhong)); }
            @Override public Optional<HoaDon> findByMaDatPhong(Connection conn, String maDatPhong) { return findByMaDatPhong(maDatPhong); }
            @Override public List<HoaDon> findAll() { return new ArrayList<>(bangHoaDon.values()); }
            @Override public synchronized boolean save(HoaDon hoaDon) { bangHoaDon.put(hoaDon.getDatPhong().getMaDatPhong(), hoaDon); return true; }
            @Override public synchronized boolean save(Connection conn, HoaDon hoaDon) { return save(hoaDon); }
            @Override public synchronized boolean update(HoaDon hoaDon) { return save(hoaDon); }
            @Override public synchronized boolean update(Connection conn, HoaDon hoaDon) { return save(hoaDon); }
            @Override public boolean delete(String id) { return bangHoaDon.remove(id) != null; }
            @Override public boolean updateTrangThai(String maHoaDon, TrangThaiThanhToan trangThaiMoi) { return true; }
            @Override public boolean updateTrangThai(Connection conn, String maHoaDon, TrangThaiThanhToan trangThaiMoi) { return true; }

            @Override
            public synchronized boolean updateTien(String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) {
                HoaDon hd = bangHoaDon.values().stream().filter(h -> maHoaDon.equals(h.getMaHoaDon())).findFirst().orElse(null);
                if (hd != null) {
                    hd.setTienPhong(tienPhong);
                    hd.setTienDichVu(tienDichVu);
                    hd.setTienGiamGia(tienGiamGia);
                    hd.setTienCoc(tienCoc);
                    hd.setTongTien(tongTien);
                    return true;
                }
                return false;
            }

            @Override
            public synchronized boolean updateTien(Connection conn, String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) {
                return updateTien(maHoaDon, tienPhong, tienDichVu, tienGiamGia, tienCoc, tongTien);
            }
        };

        mockKhachLuuTruDAO = new KhachLuuTruDAO() {
            @Override public List<KhachLuuTru> findByMaDatPhong(String maDatPhong) {
                DatPhong dp = bangDatPhong.get(maDatPhong);
                return dp != null ? dp.getDanhSachKhach() : Collections.emptyList();
            }
            @Override public boolean save(Connection conn, KhachLuuTru khach) { return true; }
            @Override public boolean saveAll(Connection conn, String maDatPhong, List<KhachLuuTru> danhSachKhach) { return true; }
            @Override public boolean deleteByMaDatPhong(Connection conn, String maDatPhong) { return true; }
        };
    }

    @AfterEach
    void tearDown() {
        AuthService.dangXuat();
    }

    private DatPhongService taoDatPhongService() {
        return new DatPhongService(mockDatPhongDAO, mockPhongDAO, mockHoaDonDAO, mockKhachLuuTruDAO, mockChiTietDichVuDAO) {
            @Override protected Connection layKetNoi() throws SQLException { return mockConn; }
        };
    }

    private DichVuService taoDichVuService() {
        return new DichVuService(mockDichVuDAO, mockChiTietDichVuDAO, mockDatPhongDAO, mockHoaDonDAO) {
            @Override protected Connection layKetNoi() throws SQLException { return mockConn; }
        };
    }

    @Test
    @DisplayName("Ghi nhận dịch vụ trước khi Check-out: Tính đầy đủ 100% vào hóa đơn Check-out, không thất thoát")
    void testGhiNhanDichVuTruocKhiCheckOut_TinhDayDuVaoHoaDon() {
        DichVuService dvService = taoDichVuService();
        DatPhongService dpService = taoDatPhongService();

        // 1. Minibar ghi nhận 2 lon coca (2 * 20k = 40k)
        dvService.ghiNhanDichVu("DP_DANG_O", "DV_COCA", 2);

        // 2. Lễ tân Check-out
        HoaDon hd = dpService.xacNhanCheckOut("DP_DANG_O", LocalDateTime.now());

        assertNotNull(hd);
        assertEquals(0, BigDecimal.valueOf(40000).compareTo(hd.getTienDichVu()),
                "Tiền dịch vụ trên hóa đơn phải đúng 40.000 VNĐ");
        assertEquals(TrangThaiDatPhong.DA_TRA_PHONG, bangDatPhong.get("DP_DANG_O").getTrangThai());
        assertEquals(TrangThaiPhong.CLEANING, bangPhong.get("P101").getTrangThaiHienTai());
    }

    @Test
    @DisplayName("Ghi nhận dịch vụ SAU khi Check-out đã hoàn tất: Bị chặn đứng ngay lập tức, không lọt vào CSDL")
    void testGhiNhanDichVuSauKhiCheckOut_BiChanTuChoiNgayLapTuc() {
        DichVuService dvService = taoDichVuService();
        DatPhongService dpService = taoDatPhongService();

        // 1. Khách trả phòng xong
        HoaDon hd = dpService.xacNhanCheckOut("DP_DANG_O", LocalDateTime.now());
        assertNotNull(hd);
        assertEquals(0, BigDecimal.ZERO.compareTo(hd.getTienDichVu()), "Chưa dùng dịch vụ thì tiền dv = 0");

        // 2. Nhân viên minibar cố tình ghi nhận dịch vụ sau khi khách đã check-out
        BusinessException ex = assertThrows(BusinessException.class, () ->
                dvService.ghiNhanDichVu("DP_DANG_O", "DV_COCA", 1)
        );

        assertTrue(ex.getMessage().contains("Check-out") || ex.getMessage().contains("DANG_O") || ex.getMessage().contains("trả phòng"),
                "Phải báo lỗi rõ ràng khách đã trả phòng: " + ex.getMessage());

        // Kiểm tra chắc chắn: Không có bản ghi nào bị ghi lén vào CSDL
        List<ChiTietDichVu> dsSauCheckOut = mockChiTietDichVuDAO.findByMaDatPhong("DP_DANG_O");
        assertEquals(0, dsSauCheckOut.size(), "Bảng chi tiết dịch vụ tuyệt đối không có bản ghi mồ côi!");
    }

    @Test
    @DisplayName("Kiểm thử Đa luồng: Vừa ghi nhận dịch vụ vừa Check-out đồng thời -> Không thất thoát doanh thu")
    void testDaLuong_CheckOutVaGhiNhanDichVuDongThoi_BaoToanTaiChinh() throws Exception {
        DichVuService dvService = taoDichVuService();
        DatPhongService dpService = taoDatPhongService();

        int soLuongLuongDichVu = 5;
        ExecutorService executor = Executors.newFixedThreadPool(soLuongLuongDichVu + 1);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(soLuongLuongDichVu + 1);

        AtomicInteger dvThanhCong = new AtomicInteger(0);
        AtomicInteger dvThatBai = new AtomicInteger(0);

        // Submit các luồng ghi nhận dịch vụ
        for (int i = 0; i < soLuongLuongDichVu; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    dvService.ghiNhanDichVu("DP_DANG_O", "DV_COCA", 1); // 20.000 VNĐ mỗi lần
                    dvThanhCong.incrementAndGet();
                } catch (BusinessException e) {
                    dvThatBai.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Submit luồng Check-out
        executor.submit(() -> {
            try {
                startLatch.await();
                dpService.xacNhanCheckOut("DP_DANG_O", LocalDateTime.now());
            } catch (Exception ignored) {
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown(); // Bắn đồng thời các luồng
        boolean finished = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Toàn bộ luồng phải kết thúc trong 5 giây");

        // Kiểm tra tính toàn vẹn tài chính:
        // Số tiền dịch vụ trên hóa đơn PHẢI BẰNG CHÍNH XÁC số lượng dịch vụ đã ghi nhận thành công * 20.000 VNĐ
        HoaDon hd = mockHoaDonDAO.findByMaDatPhong("DP_DANG_O").orElse(null);
        assertNotNull(hd, "Hóa đơn phải được tạo");

        BigDecimal expectedTienDichVu = BigDecimal.valueOf(dvThanhCong.get() * 20000L);
        assertEquals(0, expectedTienDichVu.compareTo(hd.getTienDichVu()),
                String.format("Doanh thu phải bảo toàn tuyệt đối: %d dịch vụ thành công tương ứng %s VNĐ, trên hóa đơn: %s VNĐ",
                        dvThanhCong.get(), expectedTienDichVu, hd.getTienDichVu()));

        // Tổng số dòng trong bảng chi tiết dịch vụ phải bằng đúng số lần ghi nhận thành công
        List<ChiTietDichVu> dsDv = mockChiTietDichVuDAO.findByMaDatPhong("DP_DANG_O");
        assertEquals(dvThanhCong.get(), dsDv.size(), "Số dòng dịch vụ trong CSDL phải khớp số lần thành công");
    }
}
