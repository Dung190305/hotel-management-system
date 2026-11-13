package com.qlks.service;

import com.qlks.dao.*;
import com.qlks.exception.BusinessException;
import com.qlks.model.*;
import com.qlks.model.dto.KetQuaHuyDatPhong;
import com.qlks.model.enums.*;
import com.qlks.util.HolidayUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Nghiệp vụ Tiền cọc (30%/50%), Mã QR và Chính sách Hủy phòng 3 Mốc (HuyDatPhongVaTienCocTest)")
class HuyDatPhongVaTienCocTest {

    private final Map<String, DatPhong> bangDatPhong = new HashMap<>();
    private final Map<String, Phong> bangPhong = new HashMap<>();
    private final Map<String, HoaDon> bangHoaDon = new HashMap<>();
    private final List<ThanhToan> danhSachThanhToan = new ArrayList<>();

    private DatPhongService datPhongService;
    private NhanVien nvLap;
    private KhachHang khach;
    private Phong phongDon;

    private boolean daCommit = false;
    private boolean daRollback = false;
    private boolean buocLoi = false;

    @BeforeEach
    void setUp() {
        daCommit = false;
        daRollback = false;
        buocLoi = false;
        bangDatPhong.clear();
        bangPhong.clear();
        bangHoaDon.clear();
        danhSachThanhToan.clear();

        nvLap = new LeTan("NV001", "Lễ Tân 1", "0901234567", "lt@test.com", "letan01",
                LocalDate.now(), BigDecimal.valueOf(7000000), true, 26, BigDecimal.valueOf(150000));
        AuthService.setTaiKhoanHienTai(new TaiKhoan("letan01", "hash", VaiTro.LE_TAN, true));
        AuthService.setNhanVienHienTai(nvLap);

        khach = new KhachHang("KH001", "Trần Văn Khách", "0912345678", "khach@test.com", "001200000001", LocalDate.now());
        phongDon = new PhongThuong("P101", "101", 1, BigDecimal.valueOf(1000000),
                TrangThaiPhong.AVAILABLE, LoaiGiuong.DON, true, true);
        bangPhong.put("P101", phongDon);

        // Connection Proxy giả lập giao dịch ACID
        Connection mockConnection = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("isClosed".equals(name)) return false;
                    if ("setAutoCommit".equals(name)) return null;
                    if ("commit".equals(name)) { daCommit = true; return null; }
                    if ("rollback".equals(name)) { daRollback = true; return null; }
                    if ("close".equals(name)) return null;
                    return null;
                }
        );

        DatPhongDAO mockDatPhongDAO = new DatPhongDAO() {
            @Override public Optional<DatPhong> findById(String id) { return Optional.ofNullable(bangDatPhong.get(id)); }
            @Override public Optional<DatPhong> findById(Connection conn, String id) { return findById(id); }
            @Override public Optional<DatPhong> findByIdForUpdate(Connection conn, String id) { return findById(id); }
            @Override public List<DatPhong> findByKhachHang(String maKhachHang) { return Collections.emptyList(); }
            @Override public List<DatPhong> findByPhong(String maPhong) { return Collections.emptyList(); }
            @Override public List<DatPhong> findByTrangThai(TrangThaiDatPhong trangThai) { return Collections.emptyList(); }
            @Override public boolean updateTrangThai(String maDatPhong, TrangThaiDatPhong trangThaiMoi) { return updateTrangThai(null, maDatPhong, trangThaiMoi); }
            @Override public boolean updateTrangThai(Connection conn, String maDatPhong, TrangThaiDatPhong trangThaiMoi) {
                if (buocLoi) throw new RuntimeException("Cố tình gây lỗi DB khi update trạng thái!");
                DatPhong dp = bangDatPhong.get(maDatPhong);
                if (dp != null) { dp.setTrangThai(trangThaiMoi); return true; }
                return false;
            }
            @Override public boolean xacNhanCheckIn(String maDatPhong, LocalDateTime thoiGianCheckInThucTe) { return true; }
            @Override public boolean xacNhanCheckIn(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckInThucTe) { return true; }
            @Override public boolean xacNhanCheckOut(String maDatPhong, LocalDateTime thoiGianCheckOutThucTe) { return true; }
            @Override public boolean xacNhanCheckOut(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckOutThucTe) { return true; }
            @Override public boolean isPhongBiTrungLich(String maPhong, LocalDateTime ngayNhanMoi, LocalDateTime ngayTraMoi, String excludeMaDatPhong) { return false; }
            @Override public boolean isPhongBiTrungLich(Connection conn, String maPhong, LocalDateTime ngayNhanMoi, LocalDateTime ngayTraMoi, String excludeMaDatPhong) { return false; }
            @Override public boolean hasDatPhongXacNhanTrongNgay(String maPhong, LocalDate ngay) { return false; }
            @Override public List<DatPhong> findAll() { return new ArrayList<>(bangDatPhong.values()); }
            @Override public boolean save(DatPhong datPhong) { bangDatPhong.put(datPhong.getMaDatPhong(), datPhong); return true; }
            @Override public boolean save(Connection conn, DatPhong datPhong) { return save(datPhong); }
            @Override public boolean update(DatPhong datPhong) { bangDatPhong.put(datPhong.getMaDatPhong(), datPhong); return true; }
            @Override public boolean delete(String id) { return bangDatPhong.remove(id) != null; }
        };

        PhongDAO mockPhongDAO = new PhongDAO() {
            @Override public Optional<Phong> findById(String id) { return Optional.ofNullable(bangPhong.get(id)); }
            @Override public Optional<Phong> findByIdForUpdate(Connection conn, String id) { return findById(id); }
            @Override public Optional<Phong> findBySoPhong(String soPhong) { return Optional.empty(); }
            @Override public List<Phong> findByTrangThai(TrangThaiPhong trangThai) { return Collections.emptyList(); }
            @Override public List<Phong> findPhongTrong(LocalDateTime checkIn, LocalDateTime checkOut) { return Collections.emptyList(); }
            @Override public boolean updateTrangThai(String maPhong, TrangThaiPhong trangThaiMoi) { return updateTrangThai(null, maPhong, trangThaiMoi); }
            @Override public boolean updateTrangThai(Connection conn, String maPhong, TrangThaiPhong trangThaiMoi) {
                Phong p = bangPhong.get(maPhong);
                if (p != null) { p.setTrangThaiHienTai(trangThaiMoi); return true; }
                return false;
            }
            @Override public boolean updateKinhDoanh(String maPhong, boolean dangKinhDoanh) { return true; }
            @Override public List<Phong> findAll() { return new ArrayList<>(bangPhong.values()); }
            @Override public boolean save(Phong phong) { bangPhong.put(phong.getMaPhong(), phong); return true; }
            @Override public boolean update(Phong phong) { return save(phong); }
            @Override public boolean delete(String id) { return bangPhong.remove(id) != null; }
        };

        HoaDonDAO mockHoaDonDAO = new HoaDonDAO() {
            @Override public Optional<HoaDon> findById(String id) { return Optional.ofNullable(bangHoaDon.get(id)); }
            @Override public Optional<HoaDon> findById(Connection conn, String id) { return findById(id); }
            @Override public Optional<HoaDon> findByIdForUpdate(Connection conn, String id) { return findById(id); }
            @Override public Optional<HoaDon> findByMaDatPhong(String maDatPhong) { return Optional.empty(); }
            @Override public Optional<HoaDon> findByMaDatPhong(Connection conn, String maDatPhong) { return Optional.empty(); }
            @Override public List<HoaDon> findAll() { return new ArrayList<>(bangHoaDon.values()); }
            @Override public boolean save(HoaDon hoaDon) { bangHoaDon.put(hoaDon.getMaHoaDon(), hoaDon); return true; }
            @Override public boolean save(Connection conn, HoaDon hoaDon) { return save(hoaDon); }
            @Override public boolean update(HoaDon hoaDon) { bangHoaDon.put(hoaDon.getMaHoaDon(), hoaDon); return true; }
            @Override public boolean update(Connection conn, HoaDon hoaDon) { return update(hoaDon); }
            @Override public boolean delete(String id) { return bangHoaDon.remove(id) != null; }
            @Override public boolean updateTrangThai(String maHoaDon, TrangThaiThanhToan trangThaiMoi) { return true; }
            @Override public boolean updateTrangThai(Connection conn, String maHoaDon, TrangThaiThanhToan trangThaiMoi) { return true; }
            @Override public boolean updateTien(String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) { return true; }
            @Override public boolean updateTien(Connection conn, String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) { return true; }
        };

        ThanhToanDAO mockThanhToanDAO = new ThanhToanDAO() {
            @Override public List<ThanhToan> findByMaHoaDon(String maHoaDon) { return danhSachThanhToan; }
            @Override public boolean save(ThanhToan thanhToan) { danhSachThanhToan.add(thanhToan); return true; }
            @Override public boolean save(Connection conn, ThanhToan thanhToan) { return save(thanhToan); }
            @Override public BigDecimal tinhTongTienDaThanhToan(String maHoaDon) { return BigDecimal.ZERO; }
        };

        KhachLuuTruDAO mockKhachLuuTruDAO = new KhachLuuTruDAO() {
            @Override public List<KhachLuuTru> findByMaDatPhong(String maDatPhong) { return Collections.emptyList(); }
            @Override public List<KhachLuuTru> findByMaDatPhong(Connection conn, String maDatPhong) { return Collections.emptyList(); }
            @Override public boolean save(KhachLuuTru khach) { return true; }
            @Override public boolean save(Connection conn, KhachLuuTru khach) { return true; }
            @Override public boolean saveAll(Connection conn, String maDatPhong, List<KhachLuuTru> danhSachKhach) { return true; }
            @Override public boolean deleteByMaDatPhong(Connection conn, String maDatPhong) { return true; }
        };

        ChiTietDichVuDAO mockChiTietDichVuDAO = new ChiTietDichVuDAO() {
            @Override public List<ChiTietDichVu> findByMaDatPhong(String maDatPhong) { return Collections.emptyList(); }
            @Override public List<ChiTietDichVu> findByMaDatPhong(Connection conn, String maDatPhong) { return Collections.emptyList(); }
            @Override public List<ChiTietDichVu> findByMaHoaDon(String maHoaDon) { return Collections.emptyList(); }
            @Override public List<ChiTietDichVu> findByMaHoaDon(Connection conn, String maHoaDon) { return Collections.emptyList(); }
            @Override public boolean save(ChiTietDichVu ctdv) { return true; }
            @Override public boolean save(Connection conn, ChiTietDichVu ctdv) { return true; }
            @Override public boolean delete(Long id) { return true; }
            @Override public boolean deleteByMaDatPhong(String maDatPhong) { return true; }
            @Override public boolean deleteByMaHoaDon(String maHoaDon) { return true; }
        };

        datPhongService = new DatPhongService(mockDatPhongDAO, mockPhongDAO, mockHoaDonDAO, mockKhachLuuTruDAO, mockChiTietDichVuDAO, mockThanhToanDAO) {
            @Override
            protected Connection layKetNoi() {
                return mockConnection;
            }
        };
    }

    @AfterEach
    void tearDown() {
        AuthService.setTaiKhoanHienTai(null);
        AuthService.setNhanVienHienTai(null);
    }

    @Test
    @DisplayName("Quy tắc 1: Đặt phòng ngày thường yêu cầu cọc tối thiểu 30%")
    void testTienCocNgayThuong30PhanTram() {
        // Thứ 3 -> Thứ 5 (2 đêm, ngày thường) -> Tiền phòng: 2 x 1.000.000 = 2.000.000 VNĐ
        LocalDateTime nhan = LocalDateTime.of(2027, 3, 2, 14, 0); // Thứ 3
        LocalDateTime tra = LocalDateTime.of(2027, 3, 4, 12, 0);  // Thứ 5
        BigDecimal cocToiThieu = datPhongService.tinhTienCocToiThieu(phongDon, nhan, tra);
        // 30% của 2tr = 600.000 VNĐ
        assertEquals(new BigDecimal("600000"), cocToiThieu);
    }

    @Test
    @DisplayName("Quy tắc 2: Đặt phòng dính Cuối tuần (Thứ 7, CN) yêu cầu cọc tối thiểu 50%")
    void testTienCocCuoiTuan50PhanTram() {
        // Thứ 6 -> Chủ Nhật (2 đêm, dính Thứ 7) -> Tiền phòng: 2.000.000 VNĐ
        LocalDateTime nhan = LocalDateTime.of(2027, 3, 5, 14, 0); // Thứ 6
        LocalDateTime tra = LocalDateTime.of(2027, 3, 7, 12, 0);  // Chủ Nhật
        BigDecimal cocToiThieu = datPhongService.tinhTienCocToiThieu(phongDon, nhan, tra);
        // 50% của 2tr = 1.000.000 VNĐ
        assertEquals(new BigDecimal("1000000"), cocToiThieu);
    }

    @Test
    @DisplayName("Quy tắc 3: Đặt phòng dính Ngày Lễ Việt Nam (30/04 - 01/05) yêu cầu cọc tối thiểu 50%")
    void testTienCocNgayLe50PhanTram() {
        LocalDateTime nhan = LocalDateTime.of(2027, 4, 29, 14, 0);
        LocalDateTime tra = LocalDateTime.of(2027, 5, 2, 12, 0); // Dính 30/04 và 01/05
        BigDecimal cocToiThieu = datPhongService.tinhTienCocToiThieu(phongDon, nhan, tra);
        // 3 đêm = 3.000.000 VNĐ -> 50% = 1.500.000 VNĐ
        assertEquals(new BigDecimal("1500000"), cocToiThieu);
    }

    @Test
    @DisplayName("Quy tắc 4: Từ chối tạo đặt phòng nếu tiền cọc nhập vào nhỏ hơn mức 30%/50% quy định")
    void testTuChoiKhiTienCocNhoHonToiThieu() {
        // Ngày thường: cọc tối thiểu 30% của 1.000.000 VNĐ = 300.000 VNĐ
        LocalDateTime nhan = LocalDateTime.of(2027, 3, 2, 14, 0);
        LocalDateTime tra = LocalDateTime.of(2027, 3, 3, 12, 0);

        DatPhong dp = new DatPhong("DP_COC_THIEU", khach, phongDon, nvLap, nhan, tra,
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.valueOf(200000), LocalDateTime.now());
        dp.addKhachLuuTru(new KhachLuuTru("DP_COC_THIEU", "Trần Văn Khách", "001200000001", "0912345678", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));

        BusinessException ex = assertThrows(BusinessException.class, () -> datPhongService.taoDatPhong(dp));
        assertTrue(ex.getMessage().contains("Tiền đặt cọc không đạt mức tối thiểu"));
    }

    @Test
    @DisplayName("Chính sách hủy Mốc 1: Hủy trước giờ nhận phòng >= 24h -> Hoàn 100% tiền cọc, phạt 0 VNĐ")
    void testHuyPhongTruoc24hHoan100PhanTram() {
        LocalDateTime checkIn = LocalDateTime.of(2027, 5, 10, 14, 0);
        DatPhong dp = new DatPhong("DP_HUY_24H", khach, phongDon, nvLap, checkIn, checkIn.plusDays(2),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.valueOf(1000000), LocalDateTime.now());
        bangDatPhong.put("DP_HUY_24H", dp);

        // Khách hủy trước 36 tiếng (lúc 02:00 ngày 09/05/2027)
        LocalDateTime thoiDiemHuy = LocalDateTime.of(2027, 5, 9, 2, 0);

        KetQuaHuyDatPhong kq = datPhongService.huyDatPhong("DP_HUY_24H", "Thay đổi kế hoạch", PhuongThucThanhToan.CHUYEN_KHOAN, thoiDiemHuy);

        assertNotNull(kq);
        assertEquals(TrangThaiDatPhong.DA_HUY, bangDatPhong.get("DP_HUY_24H").getTrangThai());
        assertEquals(new BigDecimal("1.0"), kq.getTyLeHoan());
        assertEquals(new BigDecimal("1000000"), kq.getTienHoanLai(), "Phải hoàn lại 100% tiền cọc");
        assertEquals(BigDecimal.ZERO, kq.getPhiPhatHuy(), "Phí phạt phải là 0 VNĐ");
        assertTrue(daCommit, "Transaction phải được commit");
        assertEquals(1, danhSachThanhToan.size(), "Phải ghi nhận 1 bản ghi thanh toán chi hoàn tiền");
        assertEquals(new BigDecimal("1000000"), danhSachThanhToan.get(0).getSoTien());
    }

    @Test
    @DisplayName("Chính sách hủy Mốc 2: Hủy sau 24h và trước 2h (còn 5h) -> Hoàn 50% cọc, phạt 50% cọc vào doanh thu")
    void testHuyPhongSau24hTruoc2hHoan50PhanTram() {
        LocalDateTime checkIn = LocalDateTime.of(2027, 5, 10, 14, 0);
        DatPhong dp = new DatPhong("DP_HUY_5H", khach, phongDon, nvLap, checkIn, checkIn.plusDays(2),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.valueOf(1000000), LocalDateTime.now());
        bangDatPhong.put("DP_HUY_5H", dp);

        // Khách hủy trước 5 tiếng (lúc 09:00 ngày 10/05/2027)
        LocalDateTime thoiDiemHuy = LocalDateTime.of(2027, 5, 10, 9, 0);

        KetQuaHuyDatPhong kq = datPhongService.huyDatPhong("DP_HUY_5H", "Bận việc đột xuất", PhuongThucThanhToan.TIEN_MAT, thoiDiemHuy);

        assertNotNull(kq);
        assertEquals(TrangThaiDatPhong.DA_HUY, bangDatPhong.get("DP_HUY_5H").getTrangThai());
        assertEquals(new BigDecimal("0.5"), kq.getTyLeHoan());
        assertEquals(new BigDecimal("500000"), kq.getTienHoanLai(), "Hoàn lại 50% tiền cọc = 500.000 VNĐ");
        assertEquals(new BigDecimal("500000"), kq.getPhiPhatHuy(), "Phạt 50% tiền cọc = 500.000 VNĐ tính vào doanh thu");
        assertTrue(daCommit);
        // Hóa đơn thanh lý cọc ghi nhận doanh thu 500.000 VNĐ
        assertNotNull(kq.getMaHoaDonQuyetToan());
        HoaDon hd = bangHoaDon.get(kq.getMaHoaDonQuyetToan());
        assertNotNull(hd);
        assertEquals(new BigDecimal("500000"), hd.getTongTien());
    }

    @Test
    @DisplayName("Chính sách hủy Mốc 3: Hủy sát giờ dưới 2 tiếng (còn 1h) -> CẤM HỦY và ném ngoại lệ BusinessException")
    void testTuChoiHuyPhongKhiDuoi2Tieng() {
        LocalDateTime checkIn = LocalDateTime.of(2027, 5, 10, 14, 0);
        DatPhong dp = new DatPhong("DP_HUY_1H", khach, phongDon, nvLap, checkIn, checkIn.plusDays(1),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.valueOf(500000), LocalDateTime.now());
        bangDatPhong.put("DP_HUY_1H", dp);

        // Khách gọi hủy lúc 13:00 (chỉ còn 1 tiếng trước Check-in)
        LocalDateTime thoiDiemHuy = LocalDateTime.of(2027, 5, 10, 13, 0);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                datPhongService.huyDatPhong("DP_HUY_1H", "Hủy sát giờ", PhuongThucThanhToan.TIEN_MAT, thoiDiemHuy)
        );

        assertTrue(ex.getMessage().contains("tối thiểu 2 tiếng"));
        // Trạng thái phiếu vẫn là DA_XAC_NHAN, không bị hủy
        assertEquals(TrangThaiDatPhong.DA_XAC_NHAN, bangDatPhong.get("DP_HUY_1H").getTrangThai());
        assertFalse(daCommit);
    }

    @Test
    @DisplayName("Chính sách hủy Mốc 3: Hủy sau giờ Check-in (quá hạn) -> CẤM HỦY")
    void testTuChoiHuyPhongKhiQuaGioCheckIn() {
        LocalDateTime checkIn = LocalDateTime.of(2027, 5, 10, 14, 0);
        DatPhong dp = new DatPhong("DP_QUA_GIO", khach, phongDon, nvLap, checkIn, checkIn.plusDays(1),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.valueOf(500000), LocalDateTime.now());
        bangDatPhong.put("DP_QUA_GIO", dp);

        // Khách gọi hủy lúc 15:30 (đã quá giờ check-in)
        LocalDateTime thoiDiemHuy = LocalDateTime.of(2027, 5, 10, 15, 30);

        assertThrows(BusinessException.class, () ->
                datPhongService.huyDatPhong("DP_QUA_GIO", "Đến trễ không muốn ở", PhuongThucThanhToan.TIEN_MAT, thoiDiemHuy)
        );
        assertEquals(TrangThaiDatPhong.DA_XAC_NHAN, bangDatPhong.get("DP_QUA_GIO").getTrangThai());
    }

    @Test
    @DisplayName("Đơn đặt phòng không có tiền cọc (0 VNĐ) hủy trước hạn hợp lệ -> Hủy thành công, không tạo hóa đơn/phiếu chi")
    void testHuyPhongKhongCocThanhCong() {
        LocalDateTime checkIn = LocalDateTime.of(2027, 5, 10, 14, 0);
        DatPhong dp = new DatPhong("DP_KHONG_COC", khach, phongDon, nvLap, checkIn, checkIn.plusDays(1),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, LocalDateTime.now());
        bangDatPhong.put("DP_KHONG_COC", dp);

        KetQuaHuyDatPhong kq = datPhongService.huyDatPhong("DP_KHONG_COC", "Hủy đơn", PhuongThucThanhToan.TIEN_MAT, checkIn.minusDays(2));
        assertNotNull(kq);
        assertEquals(TrangThaiDatPhong.DA_HUY, bangDatPhong.get("DP_KHONG_COC").getTrangThai());
        assertNull(kq.getMaHoaDonQuyetToan(), "Đơn không cọc không cần tạo hóa đơn thanh lý");
        assertEquals(0, danhSachThanhToan.size(), "Không phát sinh phiếu hoàn tiền");
    }

    @Test
    @DisplayName("Toàn vẹn Transaction: Khi gặp lỗi giữa chừng trong giao dịch hủy phòng -> Tự động ROLLBACK")
    void testRollbackKhiLoiGiaoDichHuyPhong() {
        buocLoi = true; // Kích hoạt lỗi cố tình

        LocalDateTime checkIn = LocalDateTime.of(2027, 5, 10, 14, 0);
        DatPhong dp = new DatPhong("DP_LOI_ROLLBACK", khach, phongDon, nvLap, checkIn, checkIn.plusDays(1),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.valueOf(500000), LocalDateTime.now());
        bangDatPhong.put("DP_LOI_ROLLBACK", dp);

        assertThrows(BusinessException.class, () ->
                datPhongService.huyDatPhong("DP_LOI_ROLLBACK", "Lý do", PhuongThucThanhToan.TIEN_MAT, checkIn.minusDays(2))
        );

        assertTrue(daRollback, "Transaction phải rollback khi xảy ra lỗi");
        assertFalse(daCommit);
        // Trạng thái vẫn là DA_XAC_NHAN
        assertEquals(TrangThaiDatPhong.DA_XAC_NHAN, bangDatPhong.get("DP_LOI_ROLLBACK").getTrangThai());
    }
}

