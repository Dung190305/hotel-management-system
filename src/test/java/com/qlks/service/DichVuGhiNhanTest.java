package com.qlks.service;

import com.qlks.dao.ChiTietDichVuDAO;
import com.qlks.dao.DatPhongDAO;
import com.qlks.dao.DichVuDAO;
import com.qlks.dao.HoaDonDAO;
import com.qlks.dao.KhachLuuTruDAO;
import com.qlks.dao.PhongDAO;
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

@DisplayName("Kiểm thử nghiệp vụ gắn Dịch vụ theo Phiếu đặt phòng (DichVuGhiNhanTest)")
class DichVuGhiNhanTest {

    private final Map<String, DatPhong> bangDatPhong = new HashMap<>();
    private final Map<String, DichVu> bangDichVu = new HashMap<>();
    private final List<ChiTietDichVu> bangChiTietDichVu = new ArrayList<>();
    private final Map<String, HoaDon> bangHoaDon = new HashMap<>();
    private final Map<String, Phong> bangPhong = new HashMap<>();

    private DichVuService dichVuService;
    private DatPhongService datPhongService;
    private NhanVien nvHienTai;

    @BeforeEach
    void setUp() {
        nvHienTai = new LeTan("NV_TEST", "Nhân Viên Test", "0900000000", "test@test.com", "nv_test",
                LocalDate.now(), BigDecimal.valueOf(6000000), true, 26, BigDecimal.valueOf(150000));
        AuthService.setTaiKhoanHienTai(new TaiKhoan("nv_test", "hash", VaiTro.LE_TAN, true));
        AuthService.setNhanVienHienTai(nvHienTai);

        bangDatPhong.clear();
        bangDichVu.clear();
        bangChiTietDichVu.clear();
        bangHoaDon.clear();
        bangPhong.clear();

        // 1. Tạo phòng
        Phong phong = new PhongThuong("P101", "101", 1, BigDecimal.valueOf(500000),
                TrangThaiPhong.OCCUPIED, LoaiGiuong.DON, true, true);
        bangPhong.put("P101", phong);

        // 2. Tạo dịch vụ
        DichVu dvNuoc = new DichVu("DV001", "Nước suối", BigDecimal.valueOf(20000), true);
        DichVu dvGiat = new DichVu("DV002", "Giặt là", BigDecimal.valueOf(50000), true);
        DichVu dvNgung = new DichVu("DV003", "Dịch vụ cũ", BigDecimal.valueOf(100000), false);
        bangDichVu.put(dvNuoc.getMaDichVu(), dvNuoc);
        bangDichVu.put(dvGiat.getMaDichVu(), dvGiat);
        bangDichVu.put(dvNgung.getMaDichVu(), dvNgung);

        // 3. Tạo các phiếu đặt phòng
        KhachHang kh = new KhachHang("KH01", "Khách Test", "0911222333", "khach@test.com", "001200000001", LocalDate.now());

        // Phiếu chưa check-in (DA_XAC_NHAN)
        DatPhong dpChuaCheckIn = new DatPhong("DP_CHUA_CI", kh, phong, nvHienTai,
                LocalDateTime.now(), LocalDateTime.now().plusDays(2),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, LocalDateTime.now());
        bangDatPhong.put(dpChuaCheckIn.getMaDatPhong(), dpChuaCheckIn);

        // Phiếu đang ở (DANG_O)
        DatPhong dpDangO = new DatPhong("DP_DANG_O", kh, phong, nvHienTai,
                LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1),
                LocalDateTime.now().minusDays(1), null, 1, TrangThaiDatPhong.DANG_O, BigDecimal.valueOf(100000), LocalDateTime.now().minusDays(1));
        bangDatPhong.put(dpDangO.getMaDatPhong(), dpDangO);

        // Phiếu đã check-out (DA_TRA_PHONG)
        DatPhong dpDaTra = new DatPhong("DP_DA_TRA", kh, phong, nvHienTai,
                LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusDays(3), LocalDateTime.now().minusDays(1), 1, TrangThaiDatPhong.DA_TRA_PHONG, BigDecimal.ZERO, LocalDateTime.now().minusDays(3));
        bangDatPhong.put(dpDaTra.getMaDatPhong(), dpDaTra);

        // DAO Mocks
        DatPhongDAO datPhongDAO = new DatPhongDAO() {
            @Override public Optional<DatPhong> findById(String id) { return Optional.ofNullable(bangDatPhong.get(id)); }
            @Override public Optional<DatPhong> findById(Connection conn, String id) { return findById(id); }
            @Override public List<DatPhong> findAll() { return new ArrayList<>(bangDatPhong.values()); }
            @Override public boolean save(DatPhong dp) { bangDatPhong.put(dp.getMaDatPhong(), dp); return true; }
            @Override public boolean update(DatPhong dp) { bangDatPhong.put(dp.getMaDatPhong(), dp); return true; }
            @Override public boolean delete(String id) { bangDatPhong.remove(id); return true; }
            @Override public boolean isPhongBiTrungLich(String maPhong, LocalDateTime tu, LocalDateTime den, String exclude) { return false; }
            @Override public boolean isPhongBiTrungLich(Connection conn, String maPhong, LocalDateTime tu, LocalDateTime den, String exclude) { return false; }
            @Override public boolean hasDatPhongXacNhanTrongNgay(String maPhong, LocalDate ngay) { return false; }
            @Override public List<DatPhong> findByKhachHang(String maKhachHang) { return Collections.emptyList(); }
            @Override public List<DatPhong> findByPhong(String maPhong) { return Collections.emptyList(); }
            @Override public List<DatPhong> findByTrangThai(TrangThaiDatPhong trangThai) { return Collections.emptyList(); }
            @Override public boolean updateTrangThai(String maDatPhong, TrangThaiDatPhong trangThaiMoi) { return true; }
            @Override public boolean xacNhanCheckIn(String maDatPhong, LocalDateTime thoiGianCheckInThucTe) { return true; }
            @Override public boolean xacNhanCheckOut(String maDatPhong, LocalDateTime thoiGianCheckOutThucTe) {
                DatPhong dp = bangDatPhong.get(maDatPhong);
                if (dp != null) {
                    dp.setTrangThai(TrangThaiDatPhong.DA_TRA_PHONG);
                    dp.setThoiGianTraThucTe(thoiGianCheckOutThucTe);
                }
                return true;
            }
        };

        DichVuDAO dichVuDAO = new DichVuDAO() {
            @Override public Optional<DichVu> findById(String id) { return Optional.ofNullable(bangDichVu.get(id)); }
            @Override public List<DichVu> findAll() { return new ArrayList<>(bangDichVu.values()); }
            @Override public List<DichVu> findDangKinhDoanh() { return bangDichVu.values().stream().filter(DichVu::isDangKinhDoanh).toList(); }
            @Override public List<DichVu> search(String keyword) { return findAll(); }
            @Override public boolean save(DichVu dv) { bangDichVu.put(dv.getMaDichVu(), dv); return true; }
            @Override public boolean update(DichVu dv) { bangDichVu.put(dv.getMaDichVu(), dv); return true; }
            @Override public boolean delete(String id) { bangDichVu.remove(id); return true; }
        };

        ChiTietDichVuDAO chiTietDichVuDAO = new ChiTietDichVuDAO() {
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
            @Override public boolean delete(Long id) {
                return bangChiTietDichVu.removeIf(c -> Objects.equals(c.getId(), id));
            }
            @Override public boolean deleteByMaDatPhong(String maDatPhong) {
                return bangChiTietDichVu.removeIf(c -> maDatPhong.equals(c.getMaDatPhong()));
            }
        };

        HoaDonDAO hoaDonDAO = new HoaDonDAO() {
            @Override public Optional<HoaDon> findById(String id) { return Optional.ofNullable(bangHoaDon.get(id)); }
            @Override public Optional<HoaDon> findById(Connection conn, String id) { return findById(id); }
            @Override public Optional<HoaDon> findByMaDatPhong(String maDatPhong) {
                return bangHoaDon.values().stream().filter(h -> h.getDatPhong() != null && maDatPhong.equals(h.getDatPhong().getMaDatPhong())).findFirst();
            }
            @Override public Optional<HoaDon> findByMaDatPhong(Connection conn, String maDatPhong) { return findByMaDatPhong(maDatPhong); }
            @Override public List<HoaDon> findAll() { return new ArrayList<>(bangHoaDon.values()); }
            @Override public boolean save(HoaDon hd) { bangHoaDon.put(hd.getMaHoaDon(), hd); return true; }
            @Override public boolean save(Connection conn, HoaDon hd) { return save(hd); }
            @Override public boolean update(HoaDon hd) { bangHoaDon.put(hd.getMaHoaDon(), hd); return true; }
            @Override public boolean update(Connection conn, HoaDon hd) { return update(hd); }
            @Override public boolean delete(String id) { bangHoaDon.remove(id); return true; }
            @Override public boolean updateTrangThai(String maHoaDon, TrangThaiThanhToan trangThai) { return true; }
            @Override public boolean updateTrangThai(Connection conn, String maHoaDon, TrangThaiThanhToan trangThai) { return true; }
            @Override public boolean updateTien(String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) { return true; }
        };

        PhongDAO phongDAO = new PhongDAO() {
            @Override public Optional<Phong> findById(String id) { return Optional.ofNullable(bangPhong.get(id)); }
            @Override public Optional<Phong> findBySoPhong(String soPhong) { return Optional.empty(); }
            @Override public Optional<Phong> findByIdForUpdate(Connection conn, String id) { return findById(id); }
            @Override public List<Phong> findAll() { return new ArrayList<>(bangPhong.values()); }
            @Override public List<Phong> findByTrangThai(TrangThaiPhong trangThai) { return Collections.emptyList(); }
            @Override public List<Phong> findPhongTrong(LocalDateTime checkIn, LocalDateTime checkOut) { return Collections.emptyList(); }
            @Override public boolean save(Phong p) { bangPhong.put(p.getMaPhong(), p); return true; }
            @Override public boolean update(Phong p) { bangPhong.put(p.getMaPhong(), p); return true; }
            @Override public boolean delete(String id) { bangPhong.remove(id); return true; }
            @Override public boolean updateTrangThai(String maPhong, TrangThaiPhong trangThai) {
                Phong p = bangPhong.get(maPhong);
                if (p != null) p.setTrangThaiHienTai(trangThai);
                return true;
            }
            @Override public boolean updateTrangThai(Connection conn, String maPhong, TrangThaiPhong trangThai) {
                return updateTrangThai(maPhong, trangThai);
            }
        };

        KhachLuuTruDAO khachLuuTruDAO = new KhachLuuTruDAO() {
            @Override public List<KhachLuuTru> findByMaDatPhong(String maDatPhong) { return Collections.emptyList(); }
            @Override public boolean save(KhachLuuTru k) { return true; }
            @Override public boolean save(Connection conn, KhachLuuTru k) { return true; }
            @Override public boolean saveAll(Connection conn, String maDatPhong, List<KhachLuuTru> list) { return true; }
            @Override public boolean deleteByMaDatPhong(Connection conn, String maDatPhong) { return true; }
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

        dichVuService = new DichVuService(dichVuDAO, chiTietDichVuDAO, datPhongDAO, hoaDonDAO) {
            @Override
            protected Connection layKetNoi() {
                return mockConnection;
            }
        };
        datPhongService = new DatPhongService(datPhongDAO, phongDAO, hoaDonDAO, khachLuuTruDAO, chiTietDichVuDAO) {
            @Override
            protected Connection layKetNoi() {
                return mockConnection;
            }
        };
    }

    @AfterEach
    void tearDown() {
        AuthService.dangXuat();
    }

    @Test
    @DisplayName("Ghi nhận dịch vụ khi phiếu chưa check-in (DA_XAC_NHAN) phải bị từ chối")
    void testGhiNhanDichVuKhiChuaCheckInBiTuChoi() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                dichVuService.ghiNhanDichVu("DP_CHUA_CI", "DV001", 2)
        );
        assertTrue(ex.getMessage().contains("DANG_O"), "Thông báo phải chỉ rõ trạng thái hợp lệ là DANG_O: " + ex.getMessage());
    }

    @Test
    @DisplayName("Sau khi check-out (DA_TRA_PHONG) không thể ghi thêm dịch vụ")
    void testGhiNhanDichVuSauKhiCheckOutBiTuChoi() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                dichVuService.ghiNhanDichVu("DP_DA_TRA", "DV001", 1)
        );
        assertTrue(ex.getMessage().contains("DANG_O"), "Sau check-out không được ghi thêm dịch vụ: " + ex.getMessage());
    }

    @Test
    @DisplayName("Ghi dịch vụ ngừng kinh doanh hoặc số lượng <= 0 phải bị từ chối")
    void testGhiDichVuKhongHopLe() {
        // Dịch vụ ngừng kinh doanh
        assertThrows(BusinessException.class, () ->
                dichVuService.ghiNhanDichVu("DP_DANG_O", "DV003", 1)
        );

        // Số lượng <= 0
        assertThrows(BusinessException.class, () ->
                dichVuService.ghiNhanDichVu("DP_DANG_O", "DV001", 0)
        );
    }

    @Test
    @DisplayName("Đơn giá chụp tại thời điểm ghi dịch vụ: Đổi giá danh mục sau khi ghi thì chi tiết cũ không đổi")
    void testDoiDonGiaDanhMucKhongAnhHuongChiTietCu() {
        // Ghi dịch vụ lúc giá 20,000đ
        dichVuService.ghiNhanDichVu("DP_DANG_O", "DV001", 3);

        List<ChiTietDichVu> dsTruoc = dichVuService.layDichVuTheoDatPhong("DP_DANG_O");
        assertEquals(1, dsTruoc.size());
        assertEquals(0, BigDecimal.valueOf(20000).compareTo(dsTruoc.get(0).getDonGiaLucDung()));
        assertEquals(0, BigDecimal.valueOf(60000).compareTo(dsTruoc.get(0).tinhThanhTien()));

        // Quản lý tăng giá danh mục nước suối lên 35,000đ
        DichVu dvCapNhat = new DichVu("DV001", "Nước suối", BigDecimal.valueOf(35000), true);
        bangDichVu.put("DV001", dvCapNhat);

        // Chi tiết dịch vụ đã ghi trước đó vẫn phải giữ nguyên đơn giá cũ 20,000đ
        List<ChiTietDichVu> dsSau = dichVuService.layDichVuTheoDatPhong("DP_DANG_O");
        assertEquals(1, dsSau.size());
        assertEquals(0, BigDecimal.valueOf(20000).compareTo(dsSau.get(0).getDonGiaLucDung()),
                "Đơn giá lúc dùng trong chi tiết phải không bị thay đổi theo danh mục hiện tại");
        assertEquals(0, BigDecimal.valueOf(60000).compareTo(dsSau.get(0).tinhThanhTien()));
    }

    @Test
    @DisplayName("Check-out tính đúng tổng tiền dịch vụ đã dùng vào hóa đơn")
    void testCheckOutTinhDungTienDichVuVaoHoaDon() {
        // Ghi 2 chai nước suối (2 x 20,000đ = 40,000đ)
        dichVuService.ghiNhanDichVu("DP_DANG_O", "DV001", 2);
        // và 1 lượt giặt là (1 x 50,000đ = 50,000đ)
        dichVuService.ghiNhanDichVu("DP_DANG_O", "DV002", 1);

        // Xác nhận Check-out
        HoaDon hd = datPhongService.xacNhanCheckOut("DP_DANG_O");
        assertNotNull(hd, "Hóa đơn check-out không được null");
        assertEquals(0, BigDecimal.valueOf(90000).compareTo(hd.getTienDichVu()),
                "Tiền dịch vụ trong hóa đơn phải là 90,000đ");
        assertTrue(hd.getTongChiPhi().compareTo(BigDecimal.valueOf(90000)) > 0,
                "Tổng chi phí phải bao gồm cả tiền phòng và tiền dịch vụ");
    }
}
