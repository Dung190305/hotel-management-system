package com.qlks.service;

import com.qlks.dao.NhanVienDAO;
import com.qlks.dao.TaiKhoanDAO;
import com.qlks.exception.BusinessException;
import com.qlks.exception.DataAccessException;
import com.qlks.model.LeTan;
import com.qlks.model.NhanVien;
import com.qlks.model.QuanLy;
import com.qlks.model.TaiKhoan;
import com.qlks.model.enums.VaiTro;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Transaction nguyên tử khi Thêm Nhân viên kèm Tài khoản (NhanVienTransactionTest)")
class NhanVienTransactionTest {

    private final Map<String, TaiKhoan> bangTaiKhoan = new HashMap<>();
    private final Map<String, NhanVien> bangNhanVien = new HashMap<>();

    private TaiKhoanDAO mockTaiKhoanDAO;
    private NhanVienDAO mockNhanVienDAO;

    private AtomicBoolean commitCalled;
    private AtomicBoolean rollbackCalled;
    private Connection mockConn;

    // Bộ đệm lưu trữ tạm thời trong transaction trước khi commit
    private final Map<String, TaiKhoan> bufferTaiKhoan = new HashMap<>();
    private final Map<String, NhanVien> bufferNhanVien = new HashMap<>();

    @BeforeEach
    void setUp() {
        AuthService.setTaiKhoanHienTai(new TaiKhoan("admin", "hash", VaiTro.ADMIN, true));
        AuthService.setNhanVienHienTai(new QuanLy("NV_ADMIN", "Admin Quản Lý", "0900000001", "admin@qlks.com",
                "admin", LocalDate.now(), BigDecimal.valueOf(20000000), true, 1.5, BigDecimal.ZERO));

        bangTaiKhoan.clear();
        bangNhanVien.clear();
        bufferTaiKhoan.clear();
        bufferNhanVien.clear();

        commitCalled = new AtomicBoolean(false);
        rollbackCalled = new AtomicBoolean(false);

        mockConn = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("setAutoCommit".equals(name)) {
                        return null;
                    }
                    if ("commit".equals(name)) {
                        commitCalled.set(true);
                        bangTaiKhoan.putAll(bufferTaiKhoan);
                        bangNhanVien.putAll(bufferNhanVien);
                        bufferTaiKhoan.clear();
                        bufferNhanVien.clear();
                        return null;
                    }
                    if ("rollback".equals(name)) {
                        rollbackCalled.set(true);
                        bufferTaiKhoan.clear();
                        bufferNhanVien.clear();
                        return null;
                    }
                    if ("close".equals(name) || "isClosed".equals(name)) {
                        return false;
                    }
                    return null;
                }
        );

        mockTaiKhoanDAO = new TaiKhoanDAO() {
            @Override
            public Optional<TaiKhoan> findById(String id) {
                return findByTenDangNhap(id);
            }

            @Override
            public Optional<TaiKhoan> findByTenDangNhap(String tenDangNhap) {
                return Optional.ofNullable(bangTaiKhoan.get(tenDangNhap));
            }

            @Override
            public boolean updateMatKhau(String tenDangNhap, String matKhauHashMoi) {
                return true;
            }

            @Override
            public boolean updateTrangThai(String tenDangNhap, boolean trangThai) {
                return true;
            }

            @Override
            public List<TaiKhoan> findAll() {
                return new ArrayList<>(bangTaiKhoan.values());
            }

            @Override
            public boolean save(TaiKhoan entity) {
                bangTaiKhoan.put(entity.getTenDangNhap(), entity);
                return true;
            }

            @Override
            public boolean save(Connection conn, TaiKhoan entity) {
                bufferTaiKhoan.put(entity.getTenDangNhap(), entity);
                return true;
            }

            @Override
            public boolean update(TaiKhoan entity) {
                bangTaiKhoan.put(entity.getTenDangNhap(), entity);
                return true;
            }

            @Override
            public boolean delete(String id) {
                return bangTaiKhoan.remove(id) != null;
            }
        };

        mockNhanVienDAO = new NhanVienDAO() {
            @Override
            public Optional<NhanVien> findById(String id) {
                return Optional.ofNullable(bangNhanVien.get(id));
            }

            @Override
            public Optional<NhanVien> findByTenDangNhap(String tenDangNhap) {
                return bangNhanVien.values().stream()
                        .filter(nv -> tenDangNhap.equals(nv.getTenDangNhap()))
                        .findFirst();
            }

            @Override
            public List<NhanVien> search(String keyword) {
                return new ArrayList<>(bangNhanVien.values());
            }

            @Override
            public boolean updateTrangThaiHoatDong(String maNhanVien, boolean dangHoatDong) {
                NhanVien nv = bangNhanVien.get(maNhanVien);
                if (nv != null) {
                    nv.setDangHoatDong(dangHoatDong);
                    return true;
                }
                return false;
            }

            @Override
            public List<NhanVien> findAll() {
                return new ArrayList<>(bangNhanVien.values());
            }

            @Override
            public boolean save(NhanVien entity) {
                bangNhanVien.put(entity.getMaNhanVien(), entity);
                return true;
            }

            @Override
            public boolean save(Connection conn, NhanVien entity) {
                bufferNhanVien.put(entity.getMaNhanVien(), entity);
                return true;
            }

            @Override
            public boolean update(NhanVien entity) {
                bangNhanVien.put(entity.getMaNhanVien(), entity);
                return true;
            }

            @Override
            public boolean delete(String id) {
                return bangNhanVien.remove(id) != null;
            }
        };
    }

    @AfterEach
    void tearDown() {
        AuthService.dangXuat();
    }

    private NhanVienService taoService() {
        return new NhanVienService(mockNhanVienDAO, mockTaiKhoanDAO) {
            @Override
            protected Connection layKetNoi() throws SQLException {
                return mockConn;
            }
        };
    }

    @Test
    @DisplayName("Thêm nhân viên kèm tài khoản thành công: Commit cả 2 bảng và lưu dữ liệu toàn vẹn")
    void testThemNhanVienKemTaiKhoan_ThanhCongCommitDu() {
        NhanVienService service = taoService();
        NhanVien nv = new LeTan("NV002", "Lê Văn Tân", "0912345678", "letan@qlks.com", null,
                LocalDate.now(), BigDecimal.valueOf(8000000), true, 26, BigDecimal.valueOf(150000));

        service.themNhanVien(nv, "letan02", "pass123", VaiTro.LE_TAN);

        assertTrue(commitCalled.get(), "Giao dịch phải được commit thành công");
        assertFalse(rollbackCalled.get(), "Không được rollback");

        // Kiểm tra dữ liệu trong bảng sau commit
        assertTrue(bangTaiKhoan.containsKey("letan02"), "Tài khoản phải được lưu vào bảng");
        assertTrue(bangNhanVien.containsKey("NV002"), "Nhân viên phải được lưu vào bảng");
        assertEquals("letan02", bangNhanVien.get("NV002").getTenDangNhap());
        assertEquals(VaiTro.LE_TAN, bangTaiKhoan.get("letan02").getVaiTro());
    }

    @Test
    @DisplayName("Chống Orphan Record: Thêm nhân viên lỗi -> Rollback hủy hoàn toàn tài khoản tạo kèm")
    void testThemNhanVienLoi_RollbackHuyTaiKhoanChongOrphanRecord() {
        // Giả lập mockNhanVienDAO ném lỗi DataAccessException (ví dụ vi phạm UNIQUE cột email)
        mockNhanVienDAO = new NhanVienDAO() {
            @Override public Optional<NhanVien> findById(String id) { return Optional.empty(); }
            @Override public Optional<NhanVien> findByTenDangNhap(String tenDangNhap) { return Optional.empty(); }
            @Override public List<NhanVien> search(String keyword) { return Collections.emptyList(); }
            @Override public boolean updateTrangThaiHoatDong(String maNhanVien, boolean dangHoatDong) { return true; }
            @Override public List<NhanVien> findAll() { return Collections.emptyList(); }
            @Override public boolean save(NhanVien entity) { return false; }
            @Override public boolean save(Connection conn, NhanVien entity) {
                throw new DataAccessException("Duplicate entry 'letan@qlks.com' for key 'email'");
            }
            @Override public boolean update(NhanVien entity) { return true; }
            @Override public boolean delete(String id) { return true; }
        };

        NhanVienService service = taoService();
        NhanVien nv = new LeTan("NV003", "Trần Trùng Lặp", "0987654321", "letan@qlks.com", null,
                LocalDate.now(), BigDecimal.valueOf(8000000), true, 26, BigDecimal.valueOf(150000));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.themNhanVien(nv, "user_orphan", "pass123", VaiTro.LE_TAN)
        );

        assertTrue(rollbackCalled.get(), "Giao dịch phải được rollback khi lưu nhân viên bị lỗi");
        assertFalse(commitCalled.get(), "Không được commit khi có lỗi");
        assertTrue(ex.getMessage().contains("Email") || ex.getMessage().contains("trùng lặp"));

        // Quan trọng nhất: Bảng tài khoản KHÔNG được có tài khoản mồ côi 'user_orphan'
        assertFalse(bangTaiKhoan.containsKey("user_orphan"),
                "Tài khoản 'user_orphan' phải bị rollback sạch sẽ, không tạo bản ghi mồ côi (Orphan Record)!");
    }

    @Test
    @DisplayName("Thêm nhân viên không kèm tài khoản (tài khoản null/rỗng): Vẫn commit bình thường")
    void testThemNhanVienKhongKemTaiKhoan_HoatDongBinhThuong() {
        NhanVienService service = taoService();
        NhanVien nv = new LeTan("NV004", "Nguyễn Không Acc", "0933333333", "noacc@qlks.com", null,
                LocalDate.now(), BigDecimal.valueOf(7500000), true, 20, BigDecimal.ZERO);

        service.themNhanVien(nv, null, null, null);

        assertTrue(commitCalled.get(), "Giao dịch vẫn phải được commit");
        assertTrue(bangNhanVien.containsKey("NV004"));
        assertNull(bangNhanVien.get("NV004").getTenDangNhap());
        assertEquals(0, bangTaiKhoan.size(), "Không có tài khoản nào được tạo thêm");
    }

    @Test
    @DisplayName("Kiểm tra thông báo tiếng Việt khi trùng số điện thoại")
    void testThemNhanVien_TrungSoDienThoai_BaoLoiThienDung() {
        mockNhanVienDAO = new NhanVienDAO() {
            @Override public Optional<NhanVien> findById(String id) { return Optional.empty(); }
            @Override public Optional<NhanVien> findByTenDangNhap(String tenDangNhap) { return Optional.empty(); }
            @Override public List<NhanVien> search(String keyword) { return Collections.emptyList(); }
            @Override public boolean updateTrangThaiHoatDong(String maNhanVien, boolean dangHoatDong) { return true; }
            @Override public List<NhanVien> findAll() { return Collections.emptyList(); }
            @Override public boolean save(NhanVien entity) { return false; }
            @Override public boolean save(Connection conn, NhanVien entity) {
                throw new DataAccessException("Duplicate entry '0912345678' for key 'so_dien_thoai'");
            }
            @Override public boolean update(NhanVien entity) { return true; }
            @Override public boolean delete(String id) { return true; }
        };

        NhanVienService service = taoService();
        NhanVien nv = new LeTan("NV005", "Trùng Số ĐT", "0912345678", "unique@qlks.com", null,
                LocalDate.now(), BigDecimal.valueOf(8000000), true, 26, BigDecimal.valueOf(150000));

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.themNhanVien(nv, "user_sdt", "pass123", VaiTro.LE_TAN)
        );

        assertTrue(ex.getMessage().contains("Số điện thoại"));
        assertFalse(bangTaiKhoan.containsKey("user_sdt"), "Tài khoản phải được rollback sạch");
    }

    @Test
    @DisplayName("Kiểm tra phân quyền: Nhân viên không có QUAN_LY_TAI_KHOAN bị chặn gọi themNhanVien")
    void testThemNhanVien_PhanQuyenChinhXac() {
        // Đặt người dùng hiện tại là Lễ tân (chỉ có quyền CHECK_IN, CHECK_OUT, ...)
        AuthService.setTaiKhoanHienTai(new TaiKhoan("letan", "hash", VaiTro.LE_TAN, true));
        AuthService.setNhanVienHienTai(new LeTan("NV_LT", "Lễ Tân", "0911111111", "lt@qlks.com",
                "letan", LocalDate.now(), BigDecimal.valueOf(8000000), true, 26, BigDecimal.ZERO));

        NhanVienService service = taoService();
        NhanVien nv = new LeTan("NV006", "Người Mới", "0922222222", "moi@qlks.com", null,
                LocalDate.now(), BigDecimal.valueOf(8000000), true, 26, BigDecimal.ZERO);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.themNhanVien(nv, "user_unauth", "pass123", VaiTro.LE_TAN)
        );

        assertTrue(ex.getMessage().contains("quyền") || ex.getMessage().contains("từ chối"));
    }
}

