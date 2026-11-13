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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Triệt tiêu Deadlock giữa Đặt phòng và Truy vấn Danh sách Phòng (DeadlockPreventionTest)")
class DeadlockPreventionTest {

    private final Map<String, Phong> bangPhong = new ConcurrentHashMap<>();
    private final Map<String, DatPhong> bangDatPhong = new ConcurrentHashMap<>();
    private final Map<String, List<KhachLuuTru>> bangKhach = new ConcurrentHashMap<>();

    private PhongDAO mockPhongDAO;
    private DatPhongDAO mockDatPhongDAO;
    private KhachLuuTruDAO mockKhachLuuTruDAO;
    private HoaDonDAO mockHoaDonDAO;

    private Connection mockConn;
    private NhanVien nv;
    private final ReentrantLock lockPhong = new ReentrantLock(true);

    @BeforeEach
    void setUp() {
        nv = new LeTan("NV001", "Lễ Tân 1", "0901000001", "lt1@qlks.com", "letan1",
                LocalDate.now(), BigDecimal.valueOf(8000000), true, 26, BigDecimal.ZERO);
        AuthService.setTaiKhoanHienTai(new TaiKhoan("letan1", "hash", VaiTro.LE_TAN, true));
        AuthService.setNhanVienHienTai(nv);

        bangPhong.clear();
        bangDatPhong.clear();
        bangKhach.clear();

        // Khởi tạo phòng
        Phong p1 = new PhongThuong("P101", "101", 1, BigDecimal.valueOf(500000), TrangThaiPhong.AVAILABLE, LoaiGiuong.DON, true, true);
        Phong p2 = new PhongThuong("P102", "102", 1, BigDecimal.valueOf(500000), TrangThaiPhong.AVAILABLE, LoaiGiuong.DON, true, true);
        bangPhong.put("P101", p1);
        bangPhong.put("P102", p2);

        mockConn = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("commit".equals(name) || "rollback".equals(name) || "close".equals(name)) {
                        if (lockPhong.isHeldByCurrentThread()) {
                            lockPhong.unlock();
                        }
                        return null;
                    }
                    if ("isClosed".equals(name)) return false;
                    return null;
                }
        );

        mockPhongDAO = new PhongDAO() {
            @Override public Optional<Phong> findById(String id) { return Optional.ofNullable(bangPhong.get(id)); }
            @Override
            public Optional<Phong> findByIdForUpdate(Connection conn, String id) {
                if (!lockPhong.isHeldByCurrentThread()) {
                    lockPhong.lock();
                }
                return findById(id);
            }
            @Override public Optional<Phong> findBySoPhong(String soPhong) { return Optional.empty(); }
            @Override public List<Phong> findByTrangThai(TrangThaiPhong trangThai) { return Collections.emptyList(); }
            @Override public List<Phong> findPhongTrong(LocalDateTime checkIn, LocalDateTime checkOut) { return new ArrayList<>(bangPhong.values()); }
            @Override public boolean updateTrangThai(String maPhong, TrangThaiPhong trangThaiMoi) {
                Phong p = bangPhong.get(maPhong);
                if (p != null) { p.setTrangThaiHienTai(trangThaiMoi); return true; }
                return false;
            }
            @Override public boolean updateTrangThai(Connection conn, String maPhong, TrangThaiPhong trangThaiMoi) {
                return updateTrangThai(maPhong, trangThaiMoi);
            }
            @Override public boolean updateKinhDoanh(String maPhong, boolean dangKinhDoanh) { return true; }

            // Truy vấn thuần túy: Không mở lock, không chờ đợi
            @Override public List<Phong> findAll() { return new ArrayList<>(bangPhong.values()); }
            @Override public boolean save(Phong phong) { return true; }
            @Override public boolean update(Phong phong) { return true; }
            @Override public boolean delete(String id) { return true; }

            @Override
            public void dongBoTrangThaiTheoDon() {
                // No-op: Không thực hiện UPDATE JOIN, triệt tiêu hoàn toàn Deadlock
            }
        };

        mockDatPhongDAO = new DatPhongDAO() {
            @Override public Optional<DatPhong> findById(String id) { return Optional.ofNullable(bangDatPhong.get(id)); }
            @Override public Optional<DatPhong> findById(Connection conn, String id) { return findById(id); }
            @Override public List<DatPhong> findByKhachHang(String maKhachHang) { return Collections.emptyList(); }
            @Override public List<DatPhong> findByPhong(String maPhong) { return Collections.emptyList(); }
            @Override public List<DatPhong> findByTrangThai(TrangThaiDatPhong trangThai) { return Collections.emptyList(); }
            @Override public boolean updateTrangThai(String maDatPhong, TrangThaiDatPhong trangThaiMoi) { return true; }
            @Override public boolean updateTrangThai(Connection conn, String maDatPhong, TrangThaiDatPhong trangThaiMoi) { return true; }
            @Override public boolean xacNhanCheckIn(String maDatPhong, LocalDateTime thoiGianCheckInThucTe) { return true; }
            @Override public boolean xacNhanCheckOut(String maDatPhong, LocalDateTime thoiGianCheckOutThucTe) { return true; }
            @Override public boolean isPhongBiTrungLich(String maPhong, LocalDateTime tu, LocalDateTime den, String exclude) { return false; }
            @Override public boolean isPhongBiTrungLich(Connection conn, String maPhong, LocalDateTime tu, LocalDateTime den, String exclude) { return false; }
            @Override public boolean hasDatPhongXacNhanTrongNgay(String maPhong, LocalDate ngay) { return false; }
            @Override public boolean save(Connection conn, DatPhong datPhong) {
                bangDatPhong.put(datPhong.getMaDatPhong(), datPhong);
                return true;
            }
            @Override public List<DatPhong> findAll() { return new ArrayList<>(bangDatPhong.values()); }
            @Override public boolean save(DatPhong datPhong) { return save(null, datPhong); }
            @Override public boolean update(DatPhong datPhong) { return true; }
            @Override public boolean delete(String id) { return true; }
        };

        mockKhachLuuTruDAO = new KhachLuuTruDAO() {
            @Override public List<KhachLuuTru> findByMaDatPhong(String maDatPhong) {
                return bangKhach.getOrDefault(maDatPhong, Collections.emptyList());
            }
            @Override public boolean save(Connection conn, KhachLuuTru khach) { return true; }
            @Override public boolean saveAll(Connection conn, String maDatPhong, List<KhachLuuTru> danhSachKhach) {
                bangKhach.put(maDatPhong, danhSachKhach);
                return true;
            }
            @Override public boolean deleteByMaDatPhong(Connection conn, String maDatPhong) { return true; }
        };

        mockHoaDonDAO = new HoaDonDAO() {
            @Override public Optional<HoaDon> findById(String id) { return Optional.empty(); }
            @Override public List<HoaDon> findAll() { return Collections.emptyList(); }
            @Override public boolean save(HoaDon entity) { return true; }
            @Override public boolean save(Connection conn, HoaDon entity) { return true; }
            @Override public boolean update(HoaDon entity) { return true; }
            @Override public boolean delete(String id) { return true; }
            @Override public Optional<HoaDon> findByMaDatPhong(String maDatPhong) { return Optional.empty(); }
            @Override public Optional<HoaDon> findByMaDatPhong(Connection conn, String maDatPhong) { return Optional.empty(); }
            @Override public boolean updateTrangThai(String maHoaDon, TrangThaiThanhToan trangThaiMoi) { return true; }
            @Override public boolean updateTrangThai(Connection conn, String maHoaDon, TrangThaiThanhToan trangThaiMoi) { return true; }
            @Override public boolean updateTien(String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) { return true; }
            @Override public boolean updateTien(Connection conn, String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) { return true; }
        };
    }

    @AfterEach
    void tearDown() {
        AuthService.dangXuat();
        if (lockPhong.isHeldByCurrentThread()) {
            lockPhong.unlock();
        }
    }

    private DatPhongService taoDatPhongService() {
        return new DatPhongService(mockDatPhongDAO, mockPhongDAO, mockHoaDonDAO, mockKhachLuuTruDAO) {
            @Override protected Connection layKetNoi() throws SQLException { return mockConn; }
        };
    }

    private PhongService taoPhongService() {
        return new PhongService(mockPhongDAO, mockDatPhongDAO);
    }

    @Test
    @DisplayName("CQS: layTatCaPhong() là truy vấn thuần túy, không có side-effect ghi ngầm dongBoTrangThaiTheoDon")
    void testLayTatCaPhong_PureQueryKhongGhiNgam() {
        PhongService phongService = taoPhongService();
        List<Phong> list = phongService.layTatCaPhong();

        assertNotNull(list);
        assertEquals(2, list.size());
        // Khẳng định không có lock nào bị giữ lại sau khi gọi hàm đọc
        assertFalse(lockPhong.isLocked(), "Hàm đọc layTatCaPhong tuyệt đối không được chiếm giữ lock của bảng phong");
    }

    @Test
    @DisplayName("Triệt tiêu Deadlock: Vừa đặt phòng vừa chạy Polling đọc danh sách phòng đồng thời -> 0% Deadlock")
    void testDongThoiDatPhongVaDocDanhSachPhong_KhongSinhDeadlock() throws Exception {
        DatPhongService datPhongService = taoDatPhongService();
        PhongService phongService = taoPhongService();

        int soLuongLuongDoc = 5;
        int soLuongLuongGhi = 3;
        int tongSoLuong = soLuongLuongDoc + soLuongLuongGhi;

        ExecutorService executor = Executors.newFixedThreadPool(tongSoLuong);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(tongSoLuong);

        AtomicInteger docThanhCong = new AtomicInteger(0);
        AtomicInteger ghiThanhCong = new AtomicInteger(0);
        AtomicBoolean coDeadlock = new AtomicBoolean(false);

        KhachHang kh = new KhachHang("KH001", "Khách Test", "0911000001", "kh@test.com", "001200000001", LocalDate.now());
        Phong p = bangPhong.get("P101");

        // 1. Submit các luồng đọc liên tục (mô phỏng Polling Timer của PhongPanel)
        for (int i = 0; i < soLuongLuongDoc; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 50; j++) {
                        List<Phong> ds = phongService.layTatCaPhong();
                        assertNotNull(ds);
                        docThanhCong.incrementAndGet();
                    }
                } catch (Exception e) {
                    coDeadlock.set(true);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // 2. Submit các luồng tạo đặt phòng đồng thời (khóa phong và chèn dat_phong)
        for (int i = 0; i < soLuongLuongGhi; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    DatPhong dp = new DatPhong("DP_TEST_" + index, kh, p, nv,
                            LocalDateTime.now().plusDays(index + 1), LocalDateTime.now().plusDays(index + 2),
                            null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.valueOf(150000), LocalDateTime.now());
                    dp.addKhachLuuTru(new KhachLuuTru("DP_TEST_" + index, "Khách " + index, "00120000000" + index, "091100000" + index, VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
                    datPhongService.taoDatPhong(dp);
                    ghiThanhCong.incrementAndGet();
                } catch (Exception e) {
                    if (e.getMessage() != null && e.getMessage().toLowerCase().contains("deadlock")) {
                        coDeadlock.set(true);
                    }
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Bắn đồng thời toàn bộ luồng
        boolean finished = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Toàn bộ tác vụ đọc và đặt phòng phải kết thúc trong 5 giây");
        assertFalse(coDeadlock.get(), "Tuyệt đối không xảy ra Deadlock hoặc Circular Wait giữa đọc và ghi");
        assertTrue(docThanhCong.get() >= 250, "Tất cả các lượt đọc danh sách phòng đều thành công mượt mà");
        assertTrue(ghiThanhCong.get() >= 1, "Ít nhất 1 lượt đặt phòng thành công");
    }
}
