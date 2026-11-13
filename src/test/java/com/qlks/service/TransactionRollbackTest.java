package com.qlks.service;

import com.qlks.dao.ChiTietDichVuDAO;
import com.qlks.dao.DichVuDAO;
import com.qlks.dao.HoaDonDAO;
import com.qlks.dao.ThanhToanDAO;
import com.qlks.exception.BusinessException;
import com.qlks.model.HoaDon;
import com.qlks.model.TaiKhoan;
import com.qlks.model.ThanhToan;
import com.qlks.model.enums.PhuongThucThanhToan;
import com.qlks.model.enums.TrangThaiThanhToan;
import com.qlks.model.enums.VaiTro;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử tính toàn vẹn Transaction & Rollback khi xảy ra lỗi giữa chừng (TransactionRollbackTest)")
class TransactionRollbackTest {

    private boolean buocThuHaiNemLoi = false;
    private boolean daCommit = false;
    private boolean daRollback = false;

    private final List<ThanhToan> danhSachThanhToanDaLuu = new ArrayList<>();
    private final Map<String, HoaDon> bangHoaDon = new HashMap<>();

    private HoaDonService hoaDonService;

    @BeforeEach
    void setUp() {
        AuthService.setTaiKhoanHienTai(new TaiKhoan("admin", "hash", VaiTro.ADMIN, true));
        danhSachThanhToanDaLuu.clear();
        bangHoaDon.clear();
        buocThuHaiNemLoi = false;
        daCommit = false;
        daRollback = false;

        // Tạo hóa đơn mẫu
        HoaDon hd = new HoaDon("HD_TX_001", null, null, LocalDateTime.now(),
                BigDecimal.valueOf(1000000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.valueOf(1000000), TrangThaiThanhToan.CHUA_THANH_TOAN);
        bangHoaDon.put("HD_TX_001", hd);

        // Hóa đơn đã cọc đủ 100% (còn lại phải thu = 0)
        HoaDon hdCocDu = new HoaDon("HD_COC_DU", null, null, LocalDateTime.now(),
                BigDecimal.valueOf(1000000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(1000000),
                BigDecimal.valueOf(1000000), TrangThaiThanhToan.CHUA_THANH_TOAN);
        bangHoaDon.put("HD_COC_DU", hdCocDu);

        // Hóa đơn cọc thừa (cọc 1.2tr, tổng tiền 1tr)
        HoaDon hdCocThua = new HoaDon("HD_COC_THUA", null, null, LocalDateTime.now(),
                BigDecimal.valueOf(1000000), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.valueOf(1200000),
                BigDecimal.valueOf(1000000), TrangThaiThanhToan.CHUA_THANH_TOAN);
        bangHoaDon.put("HD_COC_THUA", hdCocThua);

        // Tạo Connection giả lập qua Dynamic Proxy để kiểm soát Transaction
        Connection mockConnection = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("isClosed".equals(name)) return false;
                    if ("setAutoCommit".equals(name)) return null;
                    if ("commit".equals(name)) {
                        daCommit = true;
                        return null;
                    }
                    if ("rollback".equals(name)) {
                        daRollback = true;
                        return null;
                    }
                    if ("close".equals(name)) return null;
                    return null;
                }
        );

        // Mock HoaDonDAO
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
            @Override public boolean updateTrangThai(String maHoaDon, TrangThaiThanhToan trangThaiMoi) {
                if (buocThuHaiNemLoi) {
                    throw new RuntimeException("Cố tình gây lỗi hệ thống ở bước 2 cập nhật hóa đơn!");
                }
                HoaDon h = bangHoaDon.get(maHoaDon);
                if (h != null) { h.setTrangThai(trangThaiMoi); return true; }
                return false;
            }
            @Override public boolean updateTrangThai(Connection conn, String maHoaDon, TrangThaiThanhToan trangThaiMoi) {
                return updateTrangThai(maHoaDon, trangThaiMoi);
            }
            @Override public boolean updateTien(String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) { return true; }
            @Override public boolean updateTien(Connection conn, String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) { return true; }
        };

        // Mock ThanhToanDAO
        ThanhToanDAO mockThanhToanDAO = new ThanhToanDAO() {
            @Override public List<ThanhToan> findByMaHoaDon(String maHoaDon) { return danhSachThanhToanDaLuu; }
            @Override public boolean save(ThanhToan thanhToan) { danhSachThanhToanDaLuu.add(thanhToan); return true; }
            @Override public boolean save(Connection conn, ThanhToan thanhToan) { return save(thanhToan); }
            @Override public BigDecimal tinhTongTienDaThanhToan(String maHoaDon) { return BigDecimal.ZERO; }
        };

        // Subclass HoaDonService ghi đè layKetNoi() trả về mockConnection
        hoaDonService = new HoaDonService(mockHoaDonDAO, null, null, mockThanhToanDAO) {
            @Override
            protected Connection layKetNoi() {
                return mockConnection;
            }
        };
    }

    @AfterEach
    void tearDown() {
        AuthService.setTaiKhoanHienTai(null);
    }

    @Test
    @DisplayName("Thanh toán bình thường thành công: Giao dịch được COMMIT và hóa đơn DA_THANH_TOAN")
    void testThanhToanThanhCong() {
        ThanhToan tt = hoaDonService.thanhToanHoaDon("HD_TX_001", BigDecimal.valueOf(1000000), PhuongThucThanhToan.TIEN_MAT, "Thanh toán đủ");
        assertNotNull(tt);
        assertEquals(TrangThaiThanhToan.DA_THANH_TOAN, bangHoaDon.get("HD_TX_001").getTrangThai());
        assertTrue(daCommit, "Transaction phải được COMMIT khi mọi bước thành công");
        assertFalse(daRollback, "Transaction không được rollback khi thành công");
    }

    @Test
    @DisplayName("Lỗi ở bước thứ 2 khi thanh toán: Giao dịch phải ROLLBACK, hóa đơn vẫn giữ nguyên CHUA_THANH_TOAN")
    void testRollbackKhiLoiBuocHai() {
        buocThuHaiNemLoi = true;

        BusinessException ex = assertThrows(BusinessException.class, () ->
                hoaDonService.thanhToanHoaDon("HD_TX_001", BigDecimal.valueOf(1000000), PhuongThucThanhToan.CHUYEN_KHOAN, "Giao dịch lỗi")
        );

        assertTrue(ex.getMessage().contains("thất bại") || ex.getMessage().contains("lỗi"));
        assertTrue(daRollback, "Transaction phải thực thi ROLLBACK khi bước 2 gặp lỗi");
        assertFalse(daCommit, "Transaction không được phép commit khi có lỗi");
        // Trạng thái hóa đơn vẫn phải giữ nguyên là CHUA_THANH_TOAN
        assertEquals(TrangThaiThanhToan.CHUA_THANH_TOAN, bangHoaDon.get("HD_TX_001").getTrangThai(),
                "Hóa đơn không được chuyển sang DA_THANH_TOAN khi bước 2 gặp lỗi");
    }

    @Test
    @DisplayName("Nghiệp vụ thực tế: Khách đã đặt cọc đủ 100% -> Tất toán 0 VNĐ thành công và chuyển sang DA_THANH_TOAN")
    void testThanhToanKhiKhachDatCocDu100PhanTram() {
        ThanhToan tt = hoaDonService.thanhToanHoaDon("HD_COC_DU", BigDecimal.ZERO, PhuongThucThanhToan.TIEN_MAT, null);
        assertNotNull(tt);
        assertEquals(TrangThaiThanhToan.DA_THANH_TOAN, bangHoaDon.get("HD_COC_DU").getTrangThai(),
                "Hóa đơn phải chuyển sang DA_THANH_TOAN sau khi tất toán bằng tiền cọc");
        assertTrue(tt.getGhiChu().contains("Tất toán bằng tiền đặt cọc"));
        assertTrue(daCommit, "Transaction tất toán cọc phải được commit");
    }

    @Test
    @DisplayName("Nghiệp vụ thực tế: Khách đặt cọc thừa -> Tất toán 0 VNĐ và ghi nhận hoàn lại cọc thừa cho khách")
    void testThanhToanKhiKhachDatCocThua() {
        ThanhToan tt = hoaDonService.thanhToanHoaDon("HD_COC_THUA", BigDecimal.ZERO, PhuongThucThanhToan.TIEN_MAT, null);
        assertNotNull(tt);
        assertEquals(TrangThaiThanhToan.DA_THANH_TOAN, bangHoaDon.get("HD_COC_THUA").getTrangThai());
        assertTrue(tt.getGhiChu().contains("Hoàn lại tiền cọc thừa"), "Ghi chú thanh toán phải có lưu vết hoàn cọc thừa cho khách");
        assertTrue(daCommit);
    }
}
