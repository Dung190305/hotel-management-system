package com.qlks.service;

import com.qlks.dao.ChiTietDichVuDAO;
import com.qlks.dao.DichVuDAO;
import com.qlks.dao.HoaDonDAO;
import com.qlks.dao.ThanhToanDAO;
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

@DisplayName("Kiểm thử Nghiệp vụ Thanh toán nhiều đợt & Đa phương thức (ThanhToanNhieuDotTest)")
class ThanhToanNhieuDotTest {

    private final Map<String, HoaDon> bangHoaDon = new HashMap<>();
    private final List<ThanhToan> bangThanhToan = new ArrayList<>();

    private HoaDonService hoaDonService;
    private NhanVien nvHienTai;
    private Connection mockConnection;

    @BeforeEach
    void setUp() {
        nvHienTai = new LeTan("NV001", "Lễ Tân 01", "0901234567", "letan01@test.com", "letan01",
                LocalDate.now(), BigDecimal.valueOf(8000000), true, 26, BigDecimal.valueOf(150000));
        AuthService.setTaiKhoanHienTai(new TaiKhoan("letan01", "hash", VaiTro.LE_TAN, true));
        AuthService.setNhanVienHienTai(nvHienTai);

        bangHoaDon.clear();
        bangThanhToan.clear();

        HoaDonDAO mockHoaDonDAO = new HoaDonDAO() {
            @Override public Optional<HoaDon> findById(String id) { return Optional.ofNullable(bangHoaDon.get(id)); }
            @Override public Optional<HoaDon> findById(Connection conn, String id) { return findById(id); }
            @Override public Optional<HoaDon> findByIdForUpdate(Connection conn, String id) { return findById(id); }
            @Override public Optional<HoaDon> findByMaDatPhong(String maDatPhong) { return Optional.empty(); }
            @Override public Optional<HoaDon> findByMaDatPhong(Connection conn, String maDatPhong) { return Optional.empty(); }
            @Override public List<HoaDon> findAll() { return new ArrayList<>(bangHoaDon.values()); }
            @Override public boolean save(HoaDon hd) { bangHoaDon.put(hd.getMaHoaDon(), hd); return true; }
            @Override public boolean save(Connection conn, HoaDon hd) { return save(hd); }
            @Override public boolean update(HoaDon hd) { bangHoaDon.put(hd.getMaHoaDon(), hd); return true; }
            @Override public boolean update(Connection conn, HoaDon hd) { return update(hd); }
            @Override public boolean delete(String id) { return bangHoaDon.remove(id) != null; }
            @Override public boolean updateTrangThai(String maHoaDon, TrangThaiThanhToan trangThaiMoi) {
                HoaDon hd = bangHoaDon.get(maHoaDon);
                if (hd != null) { hd.setTrangThai(trangThaiMoi); return true; }
                return false;
            }
            @Override public boolean updateTrangThai(Connection conn, String maHoaDon, TrangThaiThanhToan trangThaiMoi) {
                return updateTrangThai(maHoaDon, trangThaiMoi);
            }
            @Override public boolean updateTien(String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) { return true; }
        };

        ThanhToanDAO mockThanhToanDAO = new ThanhToanDAO() {
            @Override
            public List<ThanhToan> findByMaHoaDon(String maHoaDon) {
                return bangThanhToan.stream().filter(t -> maHoaDon.equals(t.getMaHoaDon())).toList();
            }

            @Override
            public boolean save(ThanhToan thanhToan) {
                bangThanhToan.add(thanhToan);
                return true;
            }

            @Override
            public boolean save(Connection conn, ThanhToan thanhToan) {
                return save(thanhToan);
            }

            @Override
            public BigDecimal tinhTongTienDaThanhToan(String maHoaDon) {
                BigDecimal sum = BigDecimal.ZERO;
                for (ThanhToan tt : bangThanhToan) {
                    if (maHoaDon.equals(tt.getMaHoaDon())) {
                        sum = sum.add(tt.getSoTien());
                    }
                }
                return sum;
            }

            @Override
            public BigDecimal tinhTongTienDaThanhToan(Connection conn, String maHoaDon) {
                return tinhTongTienDaThanhToan(maHoaDon);
            }
        };

        ChiTietDichVuDAO mockChiTietDichVuDAO = new ChiTietDichVuDAO() {
            @Override public List<ChiTietDichVu> findByMaDatPhong(String maDatPhong) { return Collections.emptyList(); }
            @Override public List<ChiTietDichVu> findByMaDatPhong(Connection conn, String maDatPhong) { return Collections.emptyList(); }
            @Override public boolean save(ChiTietDichVu ctdv) { return true; }
            @Override public boolean delete(Long id) { return true; }
            @Override public boolean deleteByMaDatPhong(String maDatPhong) { return true; }
        };

        DichVuDAO mockDichVuDAO = new DichVuDAO() {
            @Override public Optional<DichVu> findById(String id) { return Optional.empty(); }
            @Override public List<DichVu> findAll() { return Collections.emptyList(); }
            @Override public List<DichVu> findDangKinhDoanh() { return Collections.emptyList(); }
            @Override public List<DichVu> search(String keyword) { return Collections.emptyList(); }
            @Override public boolean save(DichVu dv) { return true; }
            @Override public boolean update(DichVu dv) { return true; }
            @Override public boolean delete(String id) { return true; }
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

        hoaDonService = new HoaDonService(mockHoaDonDAO, mockChiTietDichVuDAO, mockDichVuDAO, mockThanhToanDAO) {
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

    private HoaDon taoHoaDonMau(String maHd, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienCoc) {
        KhachHang kh = new KhachHang("KH001", "Nguyễn Văn Khách", "0912345678", "khach@test.com", "001200000001", LocalDate.now());
        Phong phong = new PhongThuong("P101", "101", 1, tienPhong, TrangThaiPhong.OCCUPIED, LoaiGiuong.DON, true, true);
        DatPhong dp = new DatPhong("DP_TEST", kh, phong, nvHienTai,
                LocalDateTime.now().minusDays(1), LocalDateTime.now(),
                LocalDateTime.now().minusDays(1), LocalDateTime.now(), 1, TrangThaiDatPhong.DA_TRA_PHONG, tienCoc, LocalDateTime.now().minusDays(1));

        BigDecimal tongChiPhi = tienPhong.add(tienDichVu);
        HoaDon hd = new HoaDon(maHd, dp, nvHienTai, LocalDateTime.now(),
                tienPhong, tienDichVu, BigDecimal.ZERO, tienCoc, tongChiPhi, TrangThaiThanhToan.CHUA_THANH_TOAN);
        bangHoaDon.put(maHd, hd);
        return hd;
    }

    @Test
    @DisplayName("Test 1: Thanh toán 1 lần đủ 100% -> Chuyển trạng thái DA_THANH_TOAN")
    void testThanhToan1Lan_Du100PhanTram_ChuyenDaThanhToan() {
        // Tiền phòng: 1.000.000, Dịch vụ: 0, Cọc: 0 -> Còn lại: 1.000.000đ
        HoaDon hd = taoHoaDonMau("HD_FULL_01", BigDecimal.valueOf(1000000), BigDecimal.ZERO, BigDecimal.ZERO);

        assertEquals(0, BigDecimal.valueOf(1000000).compareTo(hoaDonService.tinhSoTienConLaiCanThu("HD_FULL_01")));

        // Khách trả đủ 1.000.000đ bằng Tiền mặt
        ThanhToan tt = hoaDonService.thanhToanHoaDon("HD_FULL_01", BigDecimal.valueOf(1000000), PhuongThucThanhToan.TIEN_MAT, "Trả đủ 1 lần");

        assertNotNull(tt);
        assertEquals(TrangThaiThanhToan.DA_THANH_TOAN, hd.getTrangThai(), "Hóa đơn phải chuyển sang ĐÃ THANH TOÁN");
        assertEquals(0, BigDecimal.ZERO.compareTo(hoaDonService.tinhSoTienConLaiCanThu("HD_FULL_01")), "Số tiền còn lại phải về 0");
        assertEquals(1, hoaDonService.layLichSuThanhToan("HD_FULL_01").size());
    }

    @Test
    @DisplayName("Test 2: Thanh toán nhiều đợt (Đợt 1: Tiền mặt, Đợt 2: Chuyển khoản) -> Tự động chuyển DA_THANH_TOAN khi đợt cuối trả hết")
    void testThanhToanNhieuDot_Dot1TienMat_Dot2ChuyenKhoan() {
        // Tổng tiền: 2.000.000đ (Phòng 1.8tr + DV 200k), Cọc: 500.000đ -> Còn nợ: 1.500.000đ
        HoaDon hd = taoHoaDonMau("HD_SPLIT_01", BigDecimal.valueOf(1800000), BigDecimal.valueOf(200000), BigDecimal.valueOf(500000));

        assertEquals(0, BigDecimal.valueOf(1500000).compareTo(hoaDonService.tinhSoTienConLaiCanThu("HD_SPLIT_01")),
                "Ban đầu còn phải thu 1.500.000đ");

        // ĐỢT 1: Khách trả trước 600.000đ bằng TIỀN MẶT
        ThanhToan tt1 = hoaDonService.thanhToanHoaDon("HD_SPLIT_01", BigDecimal.valueOf(600000), PhuongThucThanhToan.TIEN_MAT, "Đợt 1: Tiền mặt");
        assertNotNull(tt1);

        // Sau đợt 1: Hóa đơn VẪN PHẢI LÀ CHƯA_THANH_TOAN
        assertEquals(TrangThaiThanhToan.CHUA_THANH_TOAN, hd.getTrangThai(),
                "Sau đợt 1 khách còn nợ nên hóa đơn phải giữ nguyên trạng thái CHƯA_THANH_TOAN");
        // Số tiền còn lại phải giảm còn: 1.500.000 - 600.000 = 900.000đ
        assertEquals(0, BigDecimal.valueOf(900000).compareTo(hoaDonService.tinhSoTienConLaiCanThu("HD_SPLIT_01")),
                "Số tiền còn lại cần thu tiếp phải là 900.000đ");
        assertEquals(0, BigDecimal.valueOf(600000).compareTo(hoaDonService.tinhTongTienDaThanhToan("HD_SPLIT_01")));

        // ĐỢT 2: Khách quẹt thẻ CHUYỂN KHOẢN trả nốt 900.000đ
        ThanhToan tt2 = hoaDonService.thanhToanHoaDon("HD_SPLIT_01", BigDecimal.valueOf(900000), PhuongThucThanhToan.CHUYEN_KHOAN, "Đợt 2: Chuyển khoản hết");
        assertNotNull(tt2);

        // Sau đợt 2: Hóa đơn ĐÃ ĐƯỢC TẤT TOÁN ĐỦ
        assertEquals(TrangThaiThanhToan.DA_THANH_TOAN, hd.getTrangThai(),
                "Sau đợt 2 trả hết nợ, hóa đơn phải tự động chuyển sang ĐÃ THANH TOÁN");
        assertEquals(0, BigDecimal.ZERO.compareTo(hoaDonService.tinhSoTienConLaiCanThu("HD_SPLIT_01")),
                "Số tiền còn lại phải là 0");

        // Kiểm tra lịch sử thanh toán gồm đủ 2 đợt
        List<ThanhToan> lichSu = hoaDonService.layLichSuThanhToan("HD_SPLIT_01");
        assertEquals(2, lichSu.size());
        assertEquals(PhuongThucThanhToan.TIEN_MAT, lichSu.get(0).getPhuongThuc());
        assertEquals(0, BigDecimal.valueOf(600000).compareTo(lichSu.get(0).getSoTien()));
        assertEquals(PhuongThucThanhToan.CHUYEN_KHOAN, lichSu.get(1).getPhuongThuc());
        assertEquals(0, BigDecimal.valueOf(900000).compareTo(lichSu.get(1).getSoTien()));
    }

    @Test
    @DisplayName("Test 3: Từ chối thanh toán khi số tiền trả vượt quá số tiền còn lại phải thu")
    void testThanhToan_VuotQuaSoTienConLai_BiTuChoi() {
        // Còn nợ 500.000đ
        taoHoaDonMau("HD_OVER_01", BigDecimal.valueOf(500000), BigDecimal.ZERO, BigDecimal.ZERO);

        // Khách đòi trả 600.000đ -> PHẢI BỊ TỪ CHỐI
        BusinessException ex = assertThrows(BusinessException.class, () ->
                hoaDonService.thanhToanHoaDon("HD_OVER_01", BigDecimal.valueOf(600000), PhuongThucThanhToan.TIEN_MAT, "Trả dư")
        );
        assertTrue(ex.getMessage().contains("vượt quá số tiền còn lại phải thu"));
    }

    @Test
    @DisplayName("Test 4: Chống thanh toán trùng lặp khi hóa đơn đã đóng tất toán xong")
    void testThanhToan_KhiHoaDonDaTatToan_BiTuChoi() {
        HoaDon hd = taoHoaDonMau("HD_PAID_01", BigDecimal.valueOf(500000), BigDecimal.ZERO, BigDecimal.ZERO);

        // Thanh toán xong đợt 1 hết 500k
        hoaDonService.thanhToanHoaDon("HD_PAID_01", BigDecimal.valueOf(500000), PhuongThucThanhToan.TIEN_MAT, "Xong");
        assertEquals(TrangThaiThanhToan.DA_THANH_TOAN, hd.getTrangThai());

        // Cố tình thu thêm lần nữa trên hóa đơn đã xong -> PHẢI BỊ CHẶN
        BusinessException ex = assertThrows(BusinessException.class, () ->
                hoaDonService.thanhToanHoaDon("HD_PAID_01", BigDecimal.valueOf(100000), PhuongThucThanhToan.TIEN_MAT, "Thu lậu")
        );
        assertTrue(ex.getMessage().contains("đã được thanh toán đầy đủ"));
    }

    @Test
    @DisplayName("Test 5: Từ chối số tiền thanh toán âm")
    void testThanhToan_SoTienAm_BiTuChoi() {
        taoHoaDonMau("HD_NEG_01", BigDecimal.valueOf(500000), BigDecimal.ZERO, BigDecimal.ZERO);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                hoaDonService.thanhToanHoaDon("HD_NEG_01", BigDecimal.valueOf(-50000), PhuongThucThanhToan.TIEN_MAT, "Số âm")
        );
        assertTrue(ex.getMessage().contains("không được âm") || ex.getMessage().contains("lớn hơn hoặc bằng 0"));
    }

    @Test
    @DisplayName("Test 6: Tất toán bằng tiền đặt cọc (Khi tiền cọc đã đủ 100% chi phí)")
    void testTatToan_KhiTienCocDuHoacThua() {
        // Chi phí 500.000đ, Cọc 500.000đ -> Còn nợ 0đ
        HoaDon hd = taoHoaDonMau("HD_COC_DU", BigDecimal.valueOf(500000), BigDecimal.ZERO, BigDecimal.valueOf(500000));

        assertEquals(0, BigDecimal.ZERO.compareTo(hoaDonService.tinhSoTienConLaiCanThu("HD_COC_DU")));

        // Khách tất toán cọc với số tiền 0đ
        ThanhToan tt = hoaDonService.thanhToanHoaDon("HD_COC_DU", BigDecimal.ZERO, PhuongThucThanhToan.TIEN_MAT, "Tất toán cọc");
        assertNotNull(tt);
        assertEquals(TrangThaiThanhToan.DA_THANH_TOAN, hd.getTrangThai());
    }
}

