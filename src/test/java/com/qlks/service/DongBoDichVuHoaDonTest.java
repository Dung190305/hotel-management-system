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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Hợp nhất Nghiệp vụ Ghi nhận Dịch vụ và Tự động Đồng bộ Hóa đơn (DongBoDichVuHoaDonTest)")
class DongBoDichVuHoaDonTest {

    private final Map<String, DatPhong> bangDatPhong = new HashMap<>();
    private final Map<String, DichVu> bangDichVu = new HashMap<>();
    private final List<ChiTietDichVu> bangChiTietDichVu = new ArrayList<>();
    private final Map<String, HoaDon> bangHoaDon = new HashMap<>();
    private final Map<String, Phong> bangPhong = new HashMap<>();

    private DichVuService dichVuService;
    private HoaDonService hoaDonService;
    private NhanVien nvHienTai;
    private Phong phongMoi;
    private DatPhong dpDangO;
    private Connection mockConnection;

    @BeforeEach
    void setUp() {
        nvHienTai = new LeTan("NV001", "Lễ Tân 01", "0901234567", "letan01@test.com", "letan01",
                LocalDate.now(), BigDecimal.valueOf(8000000), true, 26, BigDecimal.valueOf(150000));
        AuthService.setTaiKhoanHienTai(new TaiKhoan("letan01", "hash", VaiTro.LE_TAN, true));
        AuthService.setNhanVienHienTai(nvHienTai);

        bangDatPhong.clear();
        bangDichVu.clear();
        bangChiTietDichVu.clear();
        bangHoaDon.clear();
        bangPhong.clear();

        // 1. Tạo phòng
        phongMoi = new PhongThuong("P101", "101", 1, BigDecimal.valueOf(500000),
                TrangThaiPhong.OCCUPIED, LoaiGiuong.DON, true, true);
        bangPhong.put("P101", phongMoi);

        // 2. Tạo dịch vụ
        DichVu dvNuoc = new DichVu("DV001", "Nước khoáng", BigDecimal.valueOf(20000), true);
        DichVu dvGiat = new DichVu("DV002", "Giặt là", BigDecimal.valueOf(50000), true);
        DichVu dvNgung = new DichVu("DV003", "Dịch vụ đóng cửa", BigDecimal.valueOf(100000), false);
        bangDichVu.put("DV001", dvNuoc);
        bangDichVu.put("DV002", dvGiat);
        bangDichVu.put("DV003", dvNgung);

        // 3. Tạo phiếu đặt phòng ĐANG_O
        KhachHang kh = new KhachHang("KH001", "Trần Khách", "0911223344", "kh@test.com", "001200000001", LocalDate.now());
        dpDangO = new DatPhong("DP_DANG_O_01", kh, phongMoi, nvHienTai,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                LocalDateTime.now().minusDays(1), null, 1, TrangThaiDatPhong.DANG_O, BigDecimal.valueOf(300000), LocalDateTime.now().minusDays(1));
        dpDangO.addKhachLuuTru(new KhachLuuTru("DP_DANG_O_01", "Trần Khách", "001200000001", "0911223344", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        bangDatPhong.put("DP_DANG_O_01", dpDangO);

        // Mocks
        DatPhongDAO mockDatPhongDAO = new DatPhongDAO() {
            @Override public Optional<DatPhong> findById(String id) { return Optional.ofNullable(bangDatPhong.get(id)); }
            @Override public Optional<DatPhong> findById(Connection conn, String id) { return findById(id); }
            @Override public Optional<DatPhong> findByIdForUpdate(Connection conn, String id) { return findById(id); }
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
            @Override public List<DatPhong> findAll() { return new ArrayList<>(bangDatPhong.values()); }
            @Override public boolean save(DatPhong dp) { bangDatPhong.put(dp.getMaDatPhong(), dp); return true; }
            @Override public boolean update(DatPhong dp) { bangDatPhong.put(dp.getMaDatPhong(), dp); return true; }
            @Override public boolean delete(String id) { return bangDatPhong.remove(id) != null; }
        };

        DichVuDAO mockDichVuDAO = new DichVuDAO() {
            @Override public Optional<DichVu> findById(String id) { return Optional.ofNullable(bangDichVu.get(id)); }
            @Override public List<DichVu> findAll() { return new ArrayList<>(bangDichVu.values()); }
            @Override public List<DichVu> findDangKinhDoanh() { return bangDichVu.values().stream().filter(DichVu::isDangKinhDoanh).toList(); }
            @Override public List<DichVu> search(String keyword) { return findAll(); }
            @Override public boolean save(DichVu dv) { bangDichVu.put(dv.getMaDichVu(), dv); return true; }
            @Override public boolean update(DichVu dv) { bangDichVu.put(dv.getMaDichVu(), dv); return true; }
            @Override public boolean delete(String id) { return bangDichVu.remove(id) != null; }
        };

        ChiTietDichVuDAO mockChiTietDichVuDAO = new ChiTietDichVuDAO() {
            private long idSeq = 1;
            @Override public List<ChiTietDichVu> findByMaDatPhong(String maDatPhong) {
                return bangChiTietDichVu.stream().filter(c -> maDatPhong.equals(c.getMaDatPhong())).toList();
            }
            @Override public List<ChiTietDichVu> findByMaDatPhong(Connection conn, String maDatPhong) {
                return findByMaDatPhong(maDatPhong);
            }
            @Override public boolean save(ChiTietDichVu ctdv) {
                ctdv.setId(idSeq++);
                bangChiTietDichVu.add(ctdv);
                return true;
            }
            @Override public boolean save(Connection conn, ChiTietDichVu ctdv) {
                return save(ctdv);
            }
            @Override public boolean delete(Long id) {
                return bangChiTietDichVu.removeIf(c -> Objects.equals(c.getId(), id));
            }
            @Override public boolean deleteByMaDatPhong(String maDatPhong) {
                return bangChiTietDichVu.removeIf(c -> maDatPhong.equals(c.getMaDatPhong()));
            }
        };

        HoaDonDAO mockHoaDonDAO = new HoaDonDAO() {
            @Override public Optional<HoaDon> findById(String id) { return Optional.ofNullable(bangHoaDon.get(id)); }
            @Override public Optional<HoaDon> findById(Connection conn, String id) { return findById(id); }
            @Override public Optional<HoaDon> findByIdForUpdate(Connection conn, String id) { return findById(id); }
            @Override public Optional<HoaDon> findByMaDatPhong(String maDatPhong) {
                return bangHoaDon.values().stream()
                        .filter(h -> h.getDatPhong() != null && maDatPhong.equals(h.getDatPhong().getMaDatPhong()))
                        .findFirst();
            }
            @Override public Optional<HoaDon> findByMaDatPhong(Connection conn, String maDatPhong) { return findByMaDatPhong(maDatPhong); }
            @Override public List<HoaDon> findAll() { return new ArrayList<>(bangHoaDon.values()); }
            @Override public boolean save(HoaDon hd) { bangHoaDon.put(hd.getMaHoaDon(), hd); return true; }
            @Override public boolean save(Connection conn, HoaDon hd) { return save(hd); }
            @Override public boolean update(HoaDon hd) { bangHoaDon.put(hd.getMaHoaDon(), hd); return true; }
            @Override public boolean update(Connection conn, HoaDon hd) { return update(hd); }
            @Override public boolean delete(String id) { return bangHoaDon.remove(id) != null; }
            @Override public boolean updateTrangThai(String maHoaDon, TrangThaiThanhToan trangThai) { return true; }
            @Override public boolean updateTrangThai(Connection conn, String maHoaDon, TrangThaiThanhToan trangThai) { return true; }
            @Override public boolean updateTien(String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) {
                HoaDon hd = bangHoaDon.get(maHoaDon);
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
            @Override public boolean updateTien(Connection conn, String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) {
                return updateTien(maHoaDon, tienPhong, tienDichVu, tienGiamGia, tienCoc, tongTien);
            }
        };

        ThanhToanDAO mockThanhToanDAO = new ThanhToanDAO() {
            @Override public List<ThanhToan> findByMaHoaDon(String maHoaDon) { return Collections.emptyList(); }
            @Override public boolean save(ThanhToan thanhToan) { return true; }
            @Override public boolean save(Connection conn, ThanhToan thanhToan) { return true; }
            @Override public BigDecimal tinhTongTienDaThanhToan(String maHoaDon) { return BigDecimal.ZERO; }
        };

        mockConnection = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("isClosed".equals(name)) return false;
                    if ("setAutoCommit".equals(name) || "commit".equals(name) || "rollback".equals(name) || "close".equals(name)) {
                        return null;
                    }
                    return null;
                }
        );

        dichVuService = new DichVuService(mockDichVuDAO, mockChiTietDichVuDAO, mockDatPhongDAO, mockHoaDonDAO) {
            @Override
            protected Connection layKetNoi() {
                return mockConnection;
            }
        };

        hoaDonService = new HoaDonService(mockHoaDonDAO, mockChiTietDichVuDAO, mockDichVuDAO, mockThanhToanDAO) {
            @Override
            protected Connection layKetNoi() {
                return mockConnection;
            }
        };
        hoaDonService.setDichVuService(dichVuService);
    }

    @AfterEach
    void tearDown() {
        AuthService.setTaiKhoanHienTai(null);
        AuthService.setNhanVienHienTai(null);
    }

    @Test
    @DisplayName("Test 1: Ghi nhận dịch vụ khi CHƯA có hóa đơn -> Lưu chi tiết dịch vụ thành công")
    void testGhiNhanDichVu_KhiChuaCoHoaDon_ChiLuuChiTietDichVu() {
        // Chưa có hóa đơn trong bangHoaDon
        assertTrue(bangHoaDon.isEmpty());

        // Ghi nhận 2 chai nước khoáng (2 x 20.000 = 40.000đ)
        dichVuService.ghiNhanDichVu("DP_DANG_O_01", "DV001", 2);

        List<ChiTietDichVu> ds = dichVuService.layDichVuTheoDatPhong("DP_DANG_O_01");
        assertEquals(1, ds.size());
        assertEquals(2, ds.get(0).getSoLuong());
        assertEquals(0, BigDecimal.valueOf(40000).compareTo(ds.get(0).tinhThanhTien()));
    }

    @Test
    @DisplayName("Test 2: Ghi nhận dịch vụ khi ĐÃ có hóa đơn -> Tự động tính lại và cập nhật vào bảng hóa đơn")
    void testGhiNhanDichVu_KhiDaCoHoaDon_TuDongDongBoTienDichVuVaTongTien() {
        // Giả sử hóa đơn đã được lập trước đó
        HoaDon hd = new HoaDon("HD_001", dpDangO, nvHienTai, LocalDateTime.now(),
                BigDecimal.valueOf(1000000), // Tiền phòng 1 triệu
                BigDecimal.ZERO,             // Chưa có dịch vụ
                BigDecimal.ZERO,             // Giảm giá 0
                BigDecimal.valueOf(300000),  // Cọc 300k
                BigDecimal.valueOf(700000),  // Còn lại phải thu 700k
                TrangThaiThanhToan.CHUA_THANH_TOAN);
        bangHoaDon.put("HD_001", hd);

        // Khách gọi 3 chai nước khoáng (3 x 20k = 60k) qua DichVuService
        dichVuService.ghiNhanDichVu("DP_DANG_O_01", "DV001", 3);

        // Kiểm tra Hóa đơn: tiền dịch vụ phải tự động cập nhật lên 60.000đ
        assertEquals(0, BigDecimal.valueOf(60000).compareTo(hd.getTienDichVu()),
                "Tiền dịch vụ trong hóa đơn phải được tự động đồng bộ lên 60k");
        // Tổng chi phí hóa đơn: 1.000.000 + 60.000 = 1.060.000đ
        assertEquals(0, BigDecimal.valueOf(1060000).compareTo(hd.getTongTien()),
                "Tổng chi phí trong hóa đơn phải là 1.060.000đ");
        // Còn lại phải thu sau cọc 300k: 1.060.000 - 300.000 = 760.000đ
        assertEquals(0, BigDecimal.valueOf(760000).compareTo(hd.getConLaiPhaiThu()),
                "Tổng tiền còn lại phải thu trong hóa đơn phải là 760k");

        // Khách gọi thêm 1 lượt giặt là (50k)
        dichVuService.ghiNhanDichVu("DP_DANG_O_01", "DV002", 1);

        // Tiền dịch vụ tăng lên 60k + 50k = 110.000đ
        assertEquals(0, BigDecimal.valueOf(110000).compareTo(hd.getTienDichVu()));
        // Tổng chi phí: 1.000.000 + 110.000 = 1.110.000đ
        assertEquals(0, BigDecimal.valueOf(1110000).compareTo(hd.getTongTien()));
        // Còn lại phải thu sau cọc: 1.110.000 - 300.000 = 810.000đ
        assertEquals(0, BigDecimal.valueOf(810000).compareTo(hd.getConLaiPhaiThu()));
    }

    @Test
    @DisplayName("Test 3: Thêm dịch vụ qua HoaDonService -> Ủy quyền trơn tru sang DichVuService (SSOT)")
    void testThemDichVuVaoHoaDon_UyQuyenSangDichVuService() {
        HoaDon hd = new HoaDon("HD_002", dpDangO, nvHienTai, LocalDateTime.now(),
                BigDecimal.valueOf(500000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(500000), TrangThaiThanhToan.CHUA_THANH_TOAN);
        bangHoaDon.put("HD_002", hd);

        // Gọi qua HoaDonService
        hoaDonService.themDichVuVaoHoaDon("HD_002", "DV002", 2); // 2 lượt giặt là = 100k

        assertEquals(0, BigDecimal.valueOf(100000).compareTo(hd.getTienDichVu()));
        assertEquals(0, BigDecimal.valueOf(600000).compareTo(hd.getTongTien()));

        // Kiểm tra bản ghi ChiTietDichVu được lưu đúng
        List<ChiTietDichVu> ds = dichVuService.layDichVuTheoDatPhong("DP_DANG_O_01");
        assertEquals(1, ds.size());
        assertEquals("DV002", ds.get(0).getDichVu().getMaDichVu());
    }

    @Test
    @DisplayName("Test 4: Lấp lỗ hổng: Khách đã Check-out (DA_TRA_PHONG) thì HoaDonService cũng phải từ chối ghi thêm dịch vụ")
    void testThemDichVuVaoHoaDon_KhiKhachDaCheckOut_TuChoiNgayLapTuc() {
        // Đổi trạng thái đặt phòng sang DA_TRA_PHONG
        dpDangO.setTrangThai(TrangThaiDatPhong.DA_TRA_PHONG);

        HoaDon hd = new HoaDon("HD_003", dpDangO, nvHienTai, LocalDateTime.now(),
                BigDecimal.valueOf(500000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(500000), TrangThaiThanhToan.CHUA_THANH_TOAN);
        bangHoaDon.put("HD_003", hd);

        // Cố tình ghi thêm dịch vụ vào hóa đơn sau khi khách đã trả phòng -> PHẢI BỊ CHẶN
        BusinessException ex = assertThrows(BusinessException.class, () ->
                hoaDonService.themDichVuVaoHoaDon("HD_003", "DV001", 1)
        );
        assertTrue(ex.getMessage().contains("DANG_O"), "Phải chặn ghi dịch vụ khi khách không còn ở: " + ex.getMessage());
    }

    @Test
    @DisplayName("Test 5: Từ chối ghi dịch vụ khi Hóa đơn đã thanh toán xong (DA_THANH_TOAN)")
    void testGhiNhanDichVu_KhiHoaDonDaThanhToan_ChanTuyetDoi() {
        HoaDon hd = new HoaDon("HD_PAID", dpDangO, nvHienTai, LocalDateTime.now(),
                BigDecimal.valueOf(500000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(500000), TrangThaiThanhToan.DA_THANH_TOAN);
        bangHoaDon.put("HD_PAID", hd);

        // Ghi nhận dịch vụ khi hóa đơn đã đóng -> PHẢI BỊ CHẶN
        BusinessException ex = assertThrows(BusinessException.class, () ->
                dichVuService.ghiNhanDichVu("DP_DANG_O_01", "DV001", 1)
        );
        assertTrue(ex.getMessage().contains("đã được thanh toán"));
    }
}
