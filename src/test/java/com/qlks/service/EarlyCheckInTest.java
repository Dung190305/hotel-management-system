package com.qlks.service;

import com.qlks.dao.*;
import com.qlks.exception.BusinessException;
import com.qlks.model.*;
import com.qlks.model.dto.ThongTinPhuThu;
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

@DisplayName("Kiểm thử Quy tắc Phụ thu Early Check-in (Nhận phòng sớm) chuẩn PMS")
class EarlyCheckInTest {

    private final Map<String, Phong> bangPhong = new HashMap<>();
    private final Map<String, DatPhong> bangDatPhong = new HashMap<>();
    private final Map<String, HoaDon> bangHoaDon = new HashMap<>();

    private DatPhongService datPhongService;
    private NhanVien nvHienTai;
    private Phong phongMoi;

    @BeforeEach
    void setUp() {
        nvHienTai = new LeTan("NV001", "Lễ Tân 01", "0901234567", "letan01@test.com", "letan01",
                LocalDate.now(), BigDecimal.valueOf(8000000), true, 26, BigDecimal.valueOf(150000));
        AuthService.setTaiKhoanHienTai(new TaiKhoan("letan01", "hash", VaiTro.LE_TAN, true));
        AuthService.setNhanVienHienTai(nvHienTai);

        bangPhong.clear();
        bangDatPhong.clear();
        bangHoaDon.clear();

        // Phòng đơn thường đơn giá 500.000 VNĐ / ngày
        phongMoi = new PhongThuong("P101", "101", 1, BigDecimal.valueOf(500000),
                TrangThaiPhong.AVAILABLE, LoaiGiuong.DON, true, true);
        bangPhong.put("P101", phongMoi);

        PhongDAO mockPhongDAO = new PhongDAO() {
            @Override public Optional<Phong> findById(String id) { return Optional.ofNullable(bangPhong.get(id)); }
            @Override public Optional<Phong> findByIdForUpdate(Connection conn, String id) { return findById(id); }
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
            @Override public boolean save(Phong phong) { bangPhong.put(phong.getMaPhong(), phong); return true; }
            @Override public boolean update(Phong phong) { return save(phong); }
            @Override public boolean delete(String id) { return bangPhong.remove(id) != null; }
        };

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
            @Override public boolean xacNhanCheckIn(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckInThucTe) { return true; }
            @Override public boolean xacNhanCheckIn(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckInThucTe, String maNv) {
                DatPhong dp = bangDatPhong.get(maDatPhong);
                if (dp != null) {
                    dp.setTrangThai(TrangThaiDatPhong.DANG_O);
                    dp.setThoiGianNhanThucTe(thoiGianCheckInThucTe);
                    dp.setMaNvCheckIn(maNv);
                    return true;
                }
                return false;
            }
            @Override public boolean xacNhanCheckOut(String maDatPhong, LocalDateTime thoiGianCheckOutThucTe) { return true; }
            @Override public boolean xacNhanCheckOut(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckOutThucTe) { return true; }
            @Override public boolean xacNhanCheckOut(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckOutThucTe, String maNv) {
                DatPhong dp = bangDatPhong.get(maDatPhong);
                if (dp != null) {
                    dp.setTrangThai(TrangThaiDatPhong.DA_TRA_PHONG);
                    dp.setThoiGianTraThucTe(thoiGianCheckOutThucTe);
                    dp.setMaNvCheckOut(maNv);
                    return true;
                }
                return false;
            }
            @Override public boolean isPhongBiTrungLich(String maPhong, LocalDateTime ngayNhanMoi, LocalDateTime ngayTraMoi, String excludeMaDatPhong) { return false; }
            @Override public boolean isPhongBiTrungLich(Connection conn, String maPhong, LocalDateTime ngayNhanMoi, LocalDateTime ngayTraMoi, String excludeMaDatPhong) { return false; }
            @Override public boolean hasDatPhongXacNhanTrongNgay(String maPhong, LocalDate ngay) { return false; }
            @Override public boolean save(Connection conn, DatPhong datPhong) { bangDatPhong.put(datPhong.getMaDatPhong(), datPhong); return true; }
            @Override public List<DatPhong> findAll() { return new ArrayList<>(bangDatPhong.values()); }
            @Override public boolean save(DatPhong datPhong) { bangDatPhong.put(datPhong.getMaDatPhong(), datPhong); return true; }
            @Override public boolean update(DatPhong datPhong) { return save(datPhong); }
            @Override public boolean delete(String id) { return bangPhong.remove(id) != null; }
        };

        HoaDonDAO mockHoaDonDAO = new HoaDonDAO() {
            @Override public Optional<HoaDon> findById(String id) { return Optional.ofNullable(bangHoaDon.get(id)); }
            @Override public Optional<HoaDon> findById(Connection conn, String id) { return findById(id); }
            @Override public Optional<HoaDon> findByIdForUpdate(Connection conn, String id) { return findById(id); }
            @Override public Optional<HoaDon> findByMaDatPhong(String maDatPhong) { return Optional.ofNullable(bangHoaDon.get(maDatPhong)); }
            @Override public Optional<HoaDon> findByMaDatPhong(Connection conn, String maDatPhong) { return findByMaDatPhong(maDatPhong); }
            @Override public List<HoaDon> findAll() { return new ArrayList<>(bangHoaDon.values()); }
            @Override public boolean save(HoaDon hoaDon) {
                bangHoaDon.put(hoaDon.getDatPhong().getMaDatPhong(), hoaDon);
                bangHoaDon.put(hoaDon.getMaHoaDon(), hoaDon);
                return true;
            }
            @Override public boolean save(Connection conn, HoaDon hoaDon) { return save(hoaDon); }
            @Override public boolean update(HoaDon hoaDon) {
                bangHoaDon.put(hoaDon.getDatPhong().getMaDatPhong(), hoaDon);
                bangHoaDon.put(hoaDon.getMaHoaDon(), hoaDon);
                return true;
            }
            @Override public boolean update(Connection conn, HoaDon hoaDon) { return update(hoaDon); }
            @Override public boolean delete(String id) { return bangHoaDon.remove(id) != null; }
            @Override public boolean updateTrangThai(String maHoaDon, TrangThaiThanhToan trangThaiMoi) { return true; }
            @Override public boolean updateTrangThai(Connection conn, String maHoaDon, TrangThaiThanhToan trangThaiMoi) { return true; }
            @Override public boolean updateTien(String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) { return true; }
            @Override public boolean updateTien(Connection conn, String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) { return true; }
        };

        KhachLuuTruDAO mockKhachLuuTruDAO = new KhachLuuTruDAO() {
            @Override public List<KhachLuuTru> findByMaDatPhong(String maDatPhong) {
                DatPhong dp = bangDatPhong.get(maDatPhong);
                return dp != null ? dp.getDanhSachKhach() : Collections.emptyList();
            }
            @Override public List<KhachLuuTru> findByMaDatPhong(Connection conn, String maDatPhong) { return findByMaDatPhong(maDatPhong); }
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

        ThanhToanDAO mockThanhToanDAO = new ThanhToanDAO() {
            @Override public List<ThanhToan> findByMaHoaDon(String maHoaDon) { return Collections.emptyList(); }
            @Override public boolean save(ThanhToan thanhToan) { return true; }
            @Override public boolean save(Connection conn, ThanhToan thanhToan) { return true; }
            @Override public BigDecimal tinhTongTienDaThanhToan(String maHoaDon) { return BigDecimal.ZERO; }
        };

        datPhongService = new DatPhongService(
                mockDatPhongDAO,
                mockPhongDAO,
                mockHoaDonDAO,
                mockKhachLuuTruDAO,
                mockChiTietDichVuDAO,
                mockThanhToanDAO
        ) {
            @Override
            protected Connection layKetNoi() {
                return (Connection) Proxy.newProxyInstance(
                        Connection.class.getClassLoader(),
                        new Class<?>[]{Connection.class},
                        (proxy, method, args) -> {
                            String name = method.getName();
                            if (name.equals("setAutoCommit") || name.equals("commit") || name.equals("rollback") || name.equals("close")) {
                                return null;
                            }
                            if (name.equals("isClosed")) {
                                return false;
                            }
                            return null;
                        }
                );
            }
        };
    }

    @AfterEach
    void tearDown() {
        AuthService.setTaiKhoanHienTai(null);
        AuthService.setNhanVienHienTai(null);
    }

    private DatPhong taoDatPhongMau(String maDp, LocalDate ngayNhan, LocalDate ngayTra) {
        KhachHang kh = new KhachHang("KH001", "Trần Khách", "0911223344", "kh@test.com", "001200000001", LocalDate.now());
        LocalDateTime checkInDuKien = LocalDateTime.of(ngayNhan, LocalTime.of(14, 0));
        LocalDateTime checkOutDuKien = LocalDateTime.of(ngayTra, LocalTime.of(12, 0));

        DatPhong dp = new DatPhong(maDp, kh, phongMoi, nvHienTai, checkInDuKien, checkOutDuKien,
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, LocalDateTime.now());
        dp.addKhachLuuTru(new KhachLuuTru(maDp, "Trần Khách", "001200000001", "0911223344", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        bangDatPhong.put(maDp, dp);
        return dp;
    }

    @Test
    @DisplayName("Test 1: Check-in trước 06:00 sáng -> Phụ thu 100% giá phòng 1 ngày")
    void testEarlyCheckIn_Truoc06h_PhuThu100PhanTram() {
        LocalDate homNay = LocalDate.now();
        DatPhong dp = taoDatPhongMau("DP_EARLY_05H", homNay, homNay.plusDays(1));

        LocalDateTime checkIn05h30 = LocalDateTime.of(homNay, LocalTime.of(5, 30));
        ThongTinPhuThu pt = datPhongService.tinhPhuThuEarlyCheckIn(phongMoi, checkIn05h30, dp.getNgayNhanPhong());

        assertTrue(pt.coPhuThu());
        assertEquals(100, pt.getPhanTramPhuThu());
        assertEquals(0, BigDecimal.valueOf(500000).compareTo(pt.getSoTienPhuThu()), "Phụ thu 100% của 500k = 500k");

        // Thực hiện Check-in lúc 05:30
        datPhongService.xacNhanCheckIn("DP_EARLY_05H", checkIn05h30);
        assertEquals(TrangThaiDatPhong.DANG_O, dp.getTrangThai());
        assertEquals(checkIn05h30, dp.getThoiGianNhanThucTe());

        // Check-out đúng giờ hôm sau lúc 11:00
        LocalDateTime checkOut11h = LocalDateTime.of(homNay.plusDays(1), LocalTime.of(11, 0));
        HoaDon hd = datPhongService.xacNhanCheckOut("DP_EARLY_05H", checkOut11h);

        // 1 đêm (500k) + Early phụ thu 100% (500k) = 1.000.000 VNĐ
        assertEquals(0, BigDecimal.valueOf(1000000).compareTo(hd.getTienPhong()));
    }

    @Test
    @DisplayName("Test 2: Check-in từ 06:00 đến trước 09:00 sáng -> Phụ thu 50% giá phòng 1 ngày")
    void testEarlyCheckIn_Tu06hDen09h_PhuThu50PhanTram() {
        LocalDate homNay = LocalDate.now();
        DatPhong dp = taoDatPhongMau("DP_EARLY_07H", homNay, homNay.plusDays(1));

        LocalDateTime checkIn07h15 = LocalDateTime.of(homNay, LocalTime.of(7, 15));
        ThongTinPhuThu pt = datPhongService.tinhPhuThuEarlyCheckIn(phongMoi, checkIn07h15, dp.getNgayNhanPhong());

        assertTrue(pt.coPhuThu());
        assertEquals(50, pt.getPhanTramPhuThu());
        assertEquals(0, BigDecimal.valueOf(250000).compareTo(pt.getSoTienPhuThu()), "Phụ thu 50% của 500k = 250k");

        datPhongService.xacNhanCheckIn("DP_EARLY_07H", checkIn07h15);
        LocalDateTime checkOut11h = LocalDateTime.of(homNay.plusDays(1), LocalTime.of(11, 0));
        HoaDon hd = datPhongService.xacNhanCheckOut("DP_EARLY_07H", checkOut11h);

        // 1 đêm (500k) + Early phụ thu 50% (250k) = 750.000 VNĐ
        assertEquals(0, BigDecimal.valueOf(750000).compareTo(hd.getTienPhong()));
    }

    @Test
    @DisplayName("Test 3: Check-in từ 09:00 đến trước 12:00 trưa -> Phụ thu 30% giá phòng 1 ngày")
    void testEarlyCheckIn_Tu09hDen12h_PhuThu30PhanTram() {
        LocalDate homNay = LocalDate.now();
        DatPhong dp = taoDatPhongMau("DP_EARLY_10H", homNay, homNay.plusDays(1));

        LocalDateTime checkIn10h30 = LocalDateTime.of(homNay, LocalTime.of(10, 30));
        ThongTinPhuThu pt = datPhongService.tinhPhuThuEarlyCheckIn(phongMoi, checkIn10h30, dp.getNgayNhanPhong());

        assertTrue(pt.coPhuThu());
        assertEquals(30, pt.getPhanTramPhuThu());
        assertEquals(0, BigDecimal.valueOf(150000).compareTo(pt.getSoTienPhuThu()), "Phụ thu 30% của 500k = 150k");

        datPhongService.xacNhanCheckIn("DP_EARLY_10H", checkIn10h30);
        LocalDateTime checkOut11h = LocalDateTime.of(homNay.plusDays(1), LocalTime.of(11, 0));
        HoaDon hd = datPhongService.xacNhanCheckOut("DP_EARLY_10H", checkOut11h);

        // 1 đêm (500k) + Early phụ thu 30% (150k) = 650.000 VNĐ
        assertEquals(0, BigDecimal.valueOf(650000).compareTo(hd.getTienPhong()));
    }

    @Test
    @DisplayName("Test 4: Check-in từ 12:00 đến 14:00 chiều -> Miễn phí phụ thu (Hỗ trợ khách)")
    void testEarlyCheckIn_Tu12hDen14h_MienPhiPhuThu() {
        LocalDate homNay = LocalDate.now();
        DatPhong dp = taoDatPhongMau("DP_EARLY_13H", homNay, homNay.plusDays(1));

        LocalDateTime checkIn13h00 = LocalDateTime.of(homNay, LocalTime.of(13, 0));
        ThongTinPhuThu pt = datPhongService.tinhPhuThuEarlyCheckIn(phongMoi, checkIn13h00, dp.getNgayNhanPhong());

        assertFalse(pt.coPhuThu());
        assertEquals(0, pt.getPhanTramPhuThu());
        assertEquals(0, BigDecimal.ZERO.compareTo(pt.getSoTienPhuThu()));

        datPhongService.xacNhanCheckIn("DP_EARLY_13H", checkIn13h00);
        LocalDateTime checkOut11h = LocalDateTime.of(homNay.plusDays(1), LocalTime.of(11, 0));
        HoaDon hd = datPhongService.xacNhanCheckOut("DP_EARLY_13H", checkOut11h);

        // 1 đêm (500k) + 0 phụ thu = 500.000 VNĐ
        assertEquals(0, BigDecimal.valueOf(500000).compareTo(hd.getTienPhong()));
    }

    @Test
    @DisplayName("Test 5: Check-in sau 14:00 (Đúng quy chuẩn tiêu chuẩn) -> 0% phụ thu")
    void testCheckIn_Sau14h_DungQuyChuan_KhongPhuThu() {
        LocalDate homNay = LocalDate.now();
        DatPhong dp = taoDatPhongMau("DP_STD_15H", homNay, homNay.plusDays(1));

        LocalDateTime checkIn15h00 = LocalDateTime.of(homNay, LocalTime.of(15, 0));
        ThongTinPhuThu pt = datPhongService.tinhPhuThuEarlyCheckIn(phongMoi, checkIn15h00, dp.getNgayNhanPhong());

        assertFalse(pt.coPhuThu());
        assertEquals(0, BigDecimal.ZERO.compareTo(pt.getSoTienPhuThu()));

        datPhongService.xacNhanCheckIn("DP_STD_15H", checkIn15h00);
        LocalDateTime checkOut11h = LocalDateTime.of(homNay.plusDays(1), LocalTime.of(11, 0));
        HoaDon hd = datPhongService.xacNhanCheckOut("DP_STD_15H", checkOut11h);

        assertEquals(0, BigDecimal.valueOf(500000).compareTo(hd.getTienPhong()));
    }

    @Test
    @DisplayName("Test 6: Kết hợp cả Early Check-in (+50%) và Late Check-out (+30%) trên cùng một đơn")
    void testKetHop_EarlyCheckInVaLateCheckOut() {
        LocalDate homNay = LocalDate.now();
        DatPhong dp = taoDatPhongMau("DP_COMBO", homNay, homNay.plusDays(1));

        // Nhận sớm lúc 08:00 sáng (+50% = 250k)
        LocalDateTime checkIn08h = LocalDateTime.of(homNay, LocalTime.of(8, 0));
        datPhongService.xacNhanCheckIn("DP_COMBO", checkIn08h);

        // Trả trễ hôm sau lúc 15:30 chiều (+30% = 150k)
        LocalDateTime checkOut15h30 = LocalDateTime.of(homNay.plusDays(1), LocalTime.of(15, 30));
        HoaDon hd = datPhongService.xacNhanCheckOut("DP_COMBO", checkOut15h30);

        // 1 đêm (500k) + Early 50% (250k) + Late 30% (150k) = 900.000 VNĐ
        BigDecimal expected = BigDecimal.valueOf(900000);
        assertEquals(0, expected.compareTo(hd.getTienPhong()),
                "Tiền phòng phải bằng 1 đêm (500k) + Nhận sớm 8h (250k) + Trả trễ 15h30 (150k) = 900k");
    }

    @Test
    @DisplayName("Test 7: Từ chối Check-in khi chưa đến ngày nhận phòng")
    void testChanCheckIn_TruocNgayNhanPhong() {
        LocalDate ngayMai = LocalDate.now().plusDays(1);
        DatPhong dp = taoDatPhongMau("DP_FUTURE", ngayMai, ngayMai.plusDays(2));

        // Cố tình check-in vào hôm nay (trước ngày nhận 1 ngày)
        LocalDateTime checkInHomNay = LocalDateTime.now();
        BusinessException ex = assertThrows(BusinessException.class, () ->
                datPhongService.xacNhanCheckIn("DP_FUTURE", checkInHomNay)
        );
        assertTrue(ex.getMessage().contains("Chưa đến ngày nhận phòng"));
    }
}
