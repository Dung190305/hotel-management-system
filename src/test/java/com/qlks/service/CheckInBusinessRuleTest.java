package com.qlks.service;

import com.qlks.dao.DatPhongDAO;
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

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử quy tắc Check-in BR-04 và Audit Trail (CheckInBusinessRuleTest)")
class CheckInBusinessRuleTest {

    private final Map<String, DatPhong> bangDatPhong = new HashMap<>();
    private final Map<String, Phong> bangPhong = new HashMap<>();
    private DatPhongService datPhongService;
    private NhanVien nvHienTai;

    @BeforeEach
    void setUp() {
        nvHienTai = new LeTan("NV_TEST_01", "Lễ Tân Test", "0909999888", "letan@test.com", "letan_test",
                LocalDate.now(), BigDecimal.valueOf(7000000), true, 26, BigDecimal.valueOf(150000));
        AuthService.setTaiKhoanHienTai(new TaiKhoan("letan_test", "hash", VaiTro.LE_TAN, true));
        AuthService.setNhanVienHienTai(nvHienTai);

        bangDatPhong.clear();
        bangPhong.clear();

        // Tạo phòng mẫu
        Phong p = new PhongThuong("P101", "101", 1, BigDecimal.valueOf(500000),
                TrangThaiPhong.AVAILABLE, LoaiGiuong.DON, true, true);
        bangPhong.put("P101", p);

        // Tạo phiếu đặt phòng trạng thái CHO_XAC_NHAN
        KhachHang kh = new KhachHang("KH001", "Nguyễn Văn Khách", "0912345678", "khach@test.com", "001200000001", LocalDate.now());
        DatPhong dpChoXacNhan = new DatPhong("DP_CHO_001", kh, p, nvHienTai,
                LocalDateTime.now(), LocalDateTime.now().plusDays(2),
                null, null, 1, TrangThaiDatPhong.CHO_XAC_NHAN, BigDecimal.ZERO, LocalDateTime.now());
        dpChoXacNhan.addKhachLuuTru(new KhachLuuTru("DP_CHO_001", "Nguyễn Văn Khách", "001200000001", "0912345678", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        bangDatPhong.put("DP_CHO_001", dpChoXacNhan);

        // Tạo phiếu đặt phòng trạng thái DA_XAC_NHAN
        DatPhong dpDaXacNhan = new DatPhong("DP_XACNHAN_001", kh, p, nvHienTai,
                LocalDateTime.now(), LocalDateTime.now().plusDays(2),
                null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, BigDecimal.ZERO, LocalDateTime.now());
        dpDaXacNhan.addKhachLuuTru(new KhachLuuTru("DP_XACNHAN_001", "Nguyễn Văn Khách", "001200000001", "0912345678", VaiTroKhachLuuTru.NGUOI_DAI_DIEN));
        bangDatPhong.put("DP_XACNHAN_001", dpDaXacNhan);

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
            @Override public boolean xacNhanCheckIn(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckInThucTe, String maNvCheckIn) {
                DatPhong dp = bangDatPhong.get(maDatPhong);
                if (dp != null) {
                    dp.setTrangThai(TrangThaiDatPhong.DANG_O);
                    dp.setThoiGianNhanThucTe(thoiGianCheckInThucTe);
                    dp.setMaNvCheckIn(maNvCheckIn);
                    return true;
                }
                return false;
            }
            @Override public boolean xacNhanCheckOut(String maDatPhong, LocalDateTime thoiGianCheckOutThucTe) { return true; }
            @Override public boolean xacNhanCheckOut(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckOutThucTe) { return true; }
            @Override public boolean xacNhanCheckOut(Connection conn, String maDatPhong, LocalDateTime thoiGianCheckOutThucTe, String maNvCheckOut) {
                DatPhong dp = bangDatPhong.get(maDatPhong);
                if (dp != null) {
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

        PhongDAO mockPhongDAO = new PhongDAO() {
            @Override public Optional<Phong> findById(String id) { return Optional.ofNullable(bangPhong.get(id)); }
            @Override public Optional<Phong> findBySoPhong(String soPhong) { return Optional.empty(); }
            @Override public List<Phong> findByTrangThai(TrangThaiPhong trangThai) { return Collections.emptyList(); }
            @Override public List<Phong> findPhongTrong(LocalDateTime checkIn, LocalDateTime checkOut) { return Collections.emptyList(); }
            @Override public boolean updateTrangThai(String maPhong, TrangThaiPhong trangThaiMoi) { return true; }
            @Override public boolean updateTrangThai(Connection conn, String maPhong, TrangThaiPhong trangThaiMoi) { return true; }
            @Override public boolean updateKinhDoanh(String maPhong, boolean dangKinhDoanh) { return true; }
            @Override public List<Phong> findAll() { return new ArrayList<>(bangPhong.values()); }
            @Override public boolean save(Phong phong) { return true; }
            @Override public boolean update(Phong phong) { return true; }
            @Override public boolean delete(String id) { return true; }
        };

        KhachLuuTruDAO mockKhachLuuTruDAO = new KhachLuuTruDAO() {
            @Override public List<KhachLuuTru> findByMaDatPhong(String maDatPhong) {
                DatPhong dp = bangDatPhong.get(maDatPhong);
                return dp != null ? dp.getDanhSachKhach() : Collections.emptyList();
            }
            @Override public boolean save(KhachLuuTru khachLuuTru) { return true; }
            @Override public boolean save(Connection conn, KhachLuuTru khachLuuTru) { return true; }
            @Override public boolean saveAll(Connection conn, String maDatPhong, List<KhachLuuTru> danhSachKhach) { return true; }
            @Override public boolean deleteByMaDatPhong(Connection conn, String maDatPhong) { return true; }
        };

        Connection mockConnection = (Connection) java.lang.reflect.Proxy.newProxyInstance(
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

        datPhongService = new DatPhongService(mockDatPhongDAO, mockPhongDAO, null, mockKhachLuuTruDAO) {
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
    @DisplayName("Quy tắc BR-04: Từ chối Check-in đối với phiếu đặt phòng đang ở trạng thái CHO_XAC_NHAN")
    void testBR04_TuChoiCheckInKhiChoXacNhan() {
        BusinessException ex = assertThrows(BusinessException.class, () ->
                datPhongService.xacNhanCheckIn("DP_CHO_001")
        );
        assertTrue(ex.getMessage().contains("Chờ xác nhận") || ex.getMessage().contains("xác nhận"));
        // Trạng thái phiếu vẫn là CHO_XAC_NHAN
        assertEquals(TrangThaiDatPhong.CHO_XAC_NHAN, bangDatPhong.get("DP_CHO_001").getTrangThai());
    }

    @Test
    @DisplayName("Quy tắc BR-04: Chấp nhận Check-in khi phiếu DA_XAC_NHAN và ghi nhận ma_nv_checkin")
    void testBR04_ChapNhanCheckInKhiDaXacNhanVaLuuAudit() {
        assertDoesNotThrow(() -> {
            datPhongService.xacNhanCheckIn("DP_XACNHAN_001");
        });
        DatPhong dp = bangDatPhong.get("DP_XACNHAN_001");
        assertEquals(TrangThaiDatPhong.DANG_O, dp.getTrangThai());
        assertEquals("NV_TEST_01", dp.getMaNvCheckIn(), "Audit Trail: Mã nhân viên Check-in phải được lưu vết chính xác");
    }

    @Test
    @DisplayName("Nghiệp vụ thực tế: Từ chối Check-in khi phòng đang được dọn dẹp (CLEANING)")
    void testTuChoiCheckInKhiPhongDangCleaning() {
        Phong pCleaning = bangPhong.get("P101");
        pCleaning.setTrangThaiHienTai(TrangThaiPhong.CLEANING);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                datPhongService.xacNhanCheckIn("DP_XACNHAN_001")
        );
        assertTrue(ex.getMessage().contains("CLEANING") || ex.getMessage().contains("dọn dẹp"),
                "Thông báo phải nêu rõ phòng đang dọn dẹp");
        assertEquals(TrangThaiDatPhong.DA_XAC_NHAN, bangDatPhong.get("DP_XACNHAN_001").getTrangThai());
    }

    @Test
    @DisplayName("Nghiệp vụ thực tế: Từ chối Check-in khi phòng đang có khách lưu trú (OCCUPIED)")
    void testTuChoiCheckInKhiPhongDangOccupied() {
        Phong pOccupied = bangPhong.get("P101");
        pOccupied.setTrangThaiHienTai(TrangThaiPhong.OCCUPIED);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                datPhongService.xacNhanCheckIn("DP_XACNHAN_001")
        );
        assertTrue(ex.getMessage().contains("OCCUPIED") || ex.getMessage().contains("có khách"),
                "Thông báo phải nêu rõ phòng đang có khách");
        assertEquals(TrangThaiDatPhong.DA_XAC_NHAN, bangDatPhong.get("DP_XACNHAN_001").getTrangThai());
    }
}
