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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử các quy tắc chuẩn PMS khách sạn thực tế (PmsBusinessRuleTest)")
class PmsBusinessRuleTest {

    private final Map<String, Phong> bangPhong = new HashMap<>();
    private final Map<String, DatPhong> bangDatPhong = new HashMap<>();
    private final Map<String, HoaDon> bangHoaDon = new HashMap<>();

    private PhongService phongService;
    private DatPhongService datPhongService;

    @BeforeEach
    void setUp() {
        AuthService.setTaiKhoanHienTai(new TaiKhoan("admin", "hash", VaiTro.ADMIN, true));
        NhanVien nv = new QuanLy("NV001", "Quản Lý", "0901234567", "ql@test.com", "admin", LocalDate.now(), BigDecimal.valueOf(18000000), true, 1.5, BigDecimal.valueOf(3000000));
        AuthService.setNhanVienHienTai(nv);

        bangPhong.clear();
        bangDatPhong.clear();
        bangHoaDon.clear();

        // Phòng thường đơn giá 500.000 VNĐ
        Phong p = new PhongThuong("P101", "101", 1, BigDecimal.valueOf(500000), TrangThaiPhong.AVAILABLE, LoaiGiuong.DON, true, true);
        bangPhong.put("P101", p);

        // Mock PhongDAO
        PhongDAO mockPhongDAO = new PhongDAO() {
            @Override public Optional<Phong> findById(String id) { return Optional.ofNullable(bangPhong.get(id)); }
            @Override public Optional<Phong> findByIdForUpdate(Connection conn, String id) { return findById(id); }
            @Override public Optional<Phong> findBySoPhong(String soPhong) { return Optional.empty(); }
            @Override public List<Phong> findByTrangThai(TrangThaiPhong trangThai) { return Collections.emptyList(); }
            @Override public List<Phong> findPhongTrong(LocalDateTime checkIn, LocalDateTime checkOut) { return Collections.emptyList(); }
            @Override public boolean updateTrangThai(String maPhong, TrangThaiPhong trangThaiMoi) {
                Phong phong = bangPhong.get(maPhong);
                if (phong != null) {
                    phong.setTrangThaiHienTai(trangThaiMoi);
                    return true;
                }
                return false;
            }
            @Override public boolean updateTrangThai(Connection conn, String maPhong, TrangThaiPhong trangThaiMoi) {
                return updateTrangThai(maPhong, trangThaiMoi);
            }
            @Override public boolean updateKinhDoanh(String maPhong, boolean dangKinhDoanh) { return true; }
            @Override public List<Phong> findAll() { return new ArrayList<>(bangPhong.values()); }
            @Override public boolean save(Phong phong) { bangPhong.put(phong.getMaPhong(), phong); return true; }
            @Override public boolean update(Phong phong) { return save(phong); }
            @Override public boolean delete(String id) { return bangPhong.remove(id) != null; }
        };

        // Mock DatPhongDAO
        DatPhongDAO mockDatPhongDAO = new DatPhongDAO() {
            @Override public Optional<DatPhong> findById(String id) { return Optional.ofNullable(bangDatPhong.get(id)); }
            @Override public Optional<DatPhong> findById(Connection conn, String id) { return findById(id); }
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
            @Override public boolean xacNhanCheckOut(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckOutThucTe, String maNvCheckOut) { return true; }
            @Override public boolean isPhongBiTrungLich(String maPhong, LocalDateTime ngayNhanMoi, LocalDateTime ngayTraMoi, String excludeMaDatPhong) { return false; }
            @Override public boolean isPhongBiTrungLich(Connection conn, String maPhong, LocalDateTime ngayNhanMoi, LocalDateTime ngayTraMoi, String excludeMaDatPhong) { return false; }
            @Override public boolean hasDatPhongXacNhanTrongNgay(String maPhong, LocalDate ngay) {
                // Kiểm tra phòng có đơn đặt hôm nay
                Phong phong = bangPhong.get(maPhong);
                return phong != null && phong.getTrangThaiHienTai() == TrangThaiPhong.RESERVED;
            }
            @Override public boolean save(Connection conn, DatPhong datPhong) { return true; }
            @Override public List<DatPhong> findAll() { return new ArrayList<>(bangDatPhong.values()); }
            @Override public boolean save(DatPhong datPhong) { bangDatPhong.put(datPhong.getMaDatPhong(), datPhong); return true; }
            @Override public boolean update(DatPhong datPhong) { return save(datPhong); }
            @Override public boolean delete(String id) { return bangDatPhong.remove(id) != null; }
        };

        // Mock HoaDonDAO
        HoaDonDAO mockHoaDonDAO = new HoaDonDAO() {
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

        // Mock ChiTietDichVuDAO
        ChiTietDichVuDAO mockChiTietDichVuDAO = new ChiTietDichVuDAO() {
            @Override public List<ChiTietDichVu> findByMaDatPhong(String maDatPhong) { return Collections.emptyList(); }
            @Override public List<ChiTietDichVu> findByMaDatPhong(Connection conn, String maDatPhong) { return Collections.emptyList(); }
            @Override public boolean save(ChiTietDichVu ctdv) { return true; }
            @Override public boolean delete(Long id) { return true; }
            @Override public boolean deleteByMaDatPhong(String maDatPhong) { return true; }
        };

        Connection mockConnection = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("isClosed".equals(name)) return false;
                    if ("setAutoCommit".equals(name)) return null;
                    if ("commit".equals(name)) return null;
                    if ("rollback".equals(name)) return null;
                    if ("close".equals(name)) return null;
                    return null;
                }
        );

        phongService = new PhongService(mockPhongDAO, mockDatPhongDAO);
        datPhongService = new DatPhongService(mockDatPhongDAO, mockPhongDAO, mockHoaDonDAO, null, mockChiTietDichVuDAO) {
            @Override protected Connection layKetNoi() { return mockConnection; }
        };
    }

    @AfterEach
    void tearDown() {
        AuthService.setTaiKhoanHienTai(null);
        AuthService.setNhanVienHienTai(null);
    }

    @Test
    @DisplayName("Chuẩn PMS: Chặn tuyệt đối việc đổi trạng thái phòng sang OCCUPIED thủ công")
    void testChanDoiTrangThaiSangOccupiedThuCong() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                phongService.chuyenTrangThaiPhong("P101", TrangThaiPhong.OCCUPIED)
        );
        assertTrue(ex.getMessage().contains("OCCUPIED") && ex.getMessage().contains("Check-in"),
                "Phải thông báo trạng thái OCCUPIED chỉ sinh ra từ quy trình Check-in");
    }

    @Test
    @DisplayName("Chuẩn PMS: Chặn tuyệt đối việc đổi trạng thái phòng sang RESERVED thủ công")
    void testChanDoiTrangThaiSangReservedThuCong() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                phongService.chuyenTrangThaiPhong("P101", TrangThaiPhong.RESERVED)
        );
        assertTrue(ex.getMessage().contains("RESERVED") && ex.getMessage().contains("Đặt phòng"),
                "Phải thông báo trạng thái RESERVED chỉ sinh ra từ phiếu Đặt phòng");
    }

    @Test
    @DisplayName("Chuẩn PMS: Chặn đưa phòng sang BẢO TRÌ (MAINTENANCE) khi phòng đang có khách đặt trước hôm nay")
    void testChanDuaPhongSangBaoTriKhiCoLichDatHomNay() {
        Phong p = bangPhong.get("P101");
        p.setTrangThaiHienTai(TrangThaiPhong.RESERVED);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                phongService.chuyenTrangThaiPhong("P101", TrangThaiPhong.MAINTENANCE)
        );
        assertTrue(ex.getMessage().contains("RESERVED") || ex.getMessage().contains("đặt trước"),
                "Phải chặn đưa phòng có khách đặt hôm nay vào bảo trì");
    }

    @Test
    @DisplayName("Chuẩn PMS: Cho phép chuyển trạng thái giữa AVAILABLE, CLEANING và MAINTENANCE khi phòng trống")
    void testChuyenTrangThaiHopLeKhiPhongTrong() {
        TrangThaiPhong tt1 = phongService.chuyenTrangThaiPhong("P101", TrangThaiPhong.CLEANING);
        assertEquals(TrangThaiPhong.CLEANING, tt1);

        TrangThaiPhong tt2 = phongService.chuyenTrangThaiPhong("P101", TrangThaiPhong.MAINTENANCE);
        assertEquals(TrangThaiPhong.MAINTENANCE, tt2);

        TrangThaiPhong tt3 = phongService.chuyenTrangThaiPhong("P101", TrangThaiPhong.AVAILABLE);
        assertEquals(TrangThaiPhong.AVAILABLE, tt3);
    }

    @Test
    @DisplayName("Chuẩn PMS: Phụ thu 30% giá phòng khi khách trả phòng trễ sau 12:30 trưa (trước 18:00)")
    void testPhuThuTraPhongMuonSau12h30() {
        KhachHang kh = new KhachHang("KH001", "Trần Khách", "0911223344", "kh@test.com", "001200000001", LocalDate.now());
        Phong p = bangPhong.get("P101"); // 500.000 VNĐ / đêm

        // Khách check in hôm nay lúc 14:00, dự kiến trả ngày mai
        LocalDateTime nhanPhong = LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 0));
        DatPhong dp = new DatPhong("DP_TEST_LATE", kh, p, AuthService.getNhanVienHienTai(),
                nhanPhong, nhanPhong.plusDays(1), nhanPhong, null, 1, TrangThaiDatPhong.DANG_O, BigDecimal.ZERO, LocalDateTime.now());
        bangDatPhong.put("DP_TEST_LATE", dp);

        // Khách check-out hôm sau lúc 15:00 (quá 12:30 và trước 18:00)
        LocalDateTime traPhong15h = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(15, 0));
        HoaDon hd = datPhongService.xacNhanCheckOut("DP_TEST_LATE", traPhong15h);

        assertNotNull(hd);
        // 1 đêm (500.000) + 30% phụ thu trễ (150.000) = 650.000 VNĐ
        BigDecimal expectedTienPhong = BigDecimal.valueOf(650000.0);
        assertEquals(0, expectedTienPhong.compareTo(hd.getTienPhong()),
                "Tiền phòng phải bằng 1 đêm (500k) + 30% phụ thu trễ 15h (150k) = 650k");
    }

    @Test
    @DisplayName("Chuẩn PMS: Phụ thu 100% giá phòng khi khách trả phòng trễ sau 18:00 (tương đương thêm 1 đêm)")
    void testPhuThuTraPhongMuonSau18h() {
        KhachHang kh = new KhachHang("KH001", "Trần Khách", "0911223344", "kh@test.com", "001200000001", LocalDate.now());
        Phong p = bangPhong.get("P101"); // 500.000 VNĐ / đêm

        LocalDateTime nhanPhong = LocalDateTime.of(LocalDate.now(), LocalTime.of(14, 0));
        DatPhong dp = new DatPhong("DP_TEST_LATE_18H", kh, p, AuthService.getNhanVienHienTai(),
                nhanPhong, nhanPhong.plusDays(1), nhanPhong, null, 1, TrangThaiDatPhong.DANG_O, BigDecimal.ZERO, LocalDateTime.now());
        bangDatPhong.put("DP_TEST_LATE_18H", dp);

        // Khách check-out hôm sau lúc 19:30 (sau 18:00)
        LocalDateTime traPhong19h30 = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(19, 30));
        HoaDon hd = datPhongService.xacNhanCheckOut("DP_TEST_LATE_18H", traPhong19h30);

        assertNotNull(hd);
        // 1 đêm (500.000) + 100% phụ thu trễ sau 18h (500.000) = 1.000.000 VNĐ
        BigDecimal expectedTienPhong = BigDecimal.valueOf(1000000.0);
        assertEquals(0, expectedTienPhong.compareTo(hd.getTienPhong()),
                "Tiền phòng phải bằng 1 đêm (500k) + 100% phụ thu sau 18h (500k) = 1.000.000đ");
    }

    @Test
    @DisplayName("Kiểm tra layTatCaPhong tuân thủ CQS, là truy vấn thuần túy không gây Deadlock hay Side-effect")
    void testLayTatCaPhong_KhongSideEffectVaTuanThuCQS() {
        List<Phong> dsPhong = phongService.layTatCaPhong();
        assertNotNull(dsPhong);
        assertEquals(1, dsPhong.size());
        assertEquals("P101", dsPhong.get(0).getMaPhong());
    }
}
