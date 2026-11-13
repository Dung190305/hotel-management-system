package com.qlks.service;

import com.qlks.dao.DatPhongDAO;
import com.qlks.dao.HoaDonDAO;
import com.qlks.model.*;
import com.qlks.model.ThongBaoDon.LoaiThongBao;
import com.qlks.model.ThongBaoDon.MucDo;
import com.qlks.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử Logic Thông báo đơn tới hạn (ThongBaoService)")
class ThongBaoServiceTest {

    private DatPhongDAO mockDatPhongDAO;
    private HoaDonDAO mockHoaDonDAO;
    private ThongBaoService thongBaoService;

    private List<DatPhong> danhSachDatPhongMau;
    private List<HoaDon> danhSachHoaDonMau;

    @BeforeEach
    void setUp() {
        danhSachDatPhongMau = new ArrayList<>();
        danhSachHoaDonMau = new ArrayList<>();

        mockDatPhongDAO = new DatPhongDAO() {
            @Override
            public List<DatPhong> findAll() {
                return danhSachDatPhongMau;
            }

            @Override public Optional<DatPhong> findById(String id) { return Optional.empty(); }
            @Override public boolean save(DatPhong entity) { return true; }
            @Override public boolean update(DatPhong entity) { return true; }
            @Override public boolean delete(String id) { return true; }
            @Override public List<DatPhong> findByKhachHang(String maKhachHang) { return List.of(); }
            @Override public List<DatPhong> findByPhong(String maPhong) { return List.of(); }
            @Override public List<DatPhong> findByTrangThai(TrangThaiDatPhong trangThai) { return List.of(); }
            @Override public boolean updateTrangThai(String maDatPhong, TrangThaiDatPhong trangThaiMoi) { return true; }
            @Override public boolean xacNhanCheckIn(String maDatPhong, LocalDateTime thoiGianCheckInThucTe) { return true; }
            @Override public boolean xacNhanCheckOut(String maDatPhong, LocalDateTime thoiGianCheckOutThucTe) { return true; }
            @Override public boolean isPhongBiTrungLich(String maPhong, LocalDateTime ngayNhanMoi, LocalDateTime ngayTraMoi, String excludeMaDatPhong) { return false; }
            @Override public boolean hasDatPhongXacNhanTrongNgay(String maPhong, LocalDate ngay) { return false; }
            @Override public Optional<DatPhong> findById(java.sql.Connection conn, String maDatPhong) { return Optional.empty(); }
            @Override public boolean save(java.sql.Connection conn, DatPhong datPhong) { return true; }
            @Override public boolean updateTrangThai(java.sql.Connection conn, String maDatPhong, TrangThaiDatPhong trangThaiMoi) { return true; }
            @Override public boolean xacNhanCheckIn(java.sql.Connection conn, String maDatPhong, LocalDateTime thoiGianCheckInThucTe) { return true; }
            @Override public boolean xacNhanCheckOut(java.sql.Connection conn, String maDatPhong, LocalDateTime thoiGianCheckOutThucTe) { return true; }
            @Override public boolean isPhongBiTrungLich(java.sql.Connection conn, String maPhong, LocalDateTime ngayNhanMoi, LocalDateTime ngayTraMoi, String excludeMaDatPhong) { return false; }
        };

        mockHoaDonDAO = new HoaDonDAO() {
            @Override
            public List<HoaDon> findAll() {
                return danhSachHoaDonMau;
            }

            @Override public Optional<HoaDon> findById(String id) { return Optional.empty(); }
            @Override public boolean save(HoaDon entity) { return true; }
            @Override public boolean update(HoaDon entity) { return true; }
            @Override public boolean delete(String id) { return true; }
            @Override public Optional<HoaDon> findByMaDatPhong(String maDatPhong) { return Optional.empty(); }
            @Override public Optional<HoaDon> findByMaDatPhong(java.sql.Connection conn, String maDatPhong) { return Optional.empty(); }
            @Override public boolean save(java.sql.Connection conn, HoaDon hoaDon) { return true; }
            @Override public boolean updateTrangThai(String maHoaDon, TrangThaiThanhToan trangThaiMoi) { return true; }
            @Override public boolean updateTrangThai(java.sql.Connection conn, String maHoaDon, TrangThaiThanhToan trangThaiMoi) { return true; }
            @Override public boolean updateTien(String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) { return true; }
            @Override public boolean updateTien(java.sql.Connection conn, String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) { return true; }
        };

        thongBaoService = new ThongBaoService(mockDatPhongDAO, mockHoaDonDAO);
    }

    private DatPhong taoDatPhongMau(String maDP, String soPhong, LocalDateTime checkIn, LocalDateTime checkOut, TrangThaiDatPhong trangThai) {
        KhachHang kh = new KhachHang("KH001", "Nguyễn Văn Test", "0901234567", "test@gmail.com", "001200001234", LocalDate.of(2024, 1, 1));
        Phong phong = new PhongThuong("P101", soPhong, 1, new BigDecimal("500000"), TrangThaiPhong.AVAILABLE, true, true);
        NhanVien nv = new LeTan("NV001", "Lễ Tân", "0912345678", "letan@hotel.com", "letan01", LocalDate.now(), new BigDecimal("8000000"), true, 26, new BigDecimal("150000"));

        return new DatPhong(maDP, kh, phong, nv, checkIn, checkOut, null, null, 2, trangThai, BigDecimal.ZERO, LocalDateTime.now());
    }

    @Test
    @DisplayName("1. Phát hiện đơn tới hạn nhận phòng hôm nay (CHECK_IN_HOM_NAY)")
    void testCheckInHomNay() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 2, 10, 0);
        LocalDateTime checkInHomNay = LocalDateTime.of(2026, 11, 2, 14, 0); // Chiều nay nhận
        LocalDateTime checkOut = LocalDateTime.of(2026, 11, 4, 12, 0);

        danhSachDatPhongMau.add(taoDatPhongMau("DP001", "101", checkInHomNay, checkOut, TrangThaiDatPhong.DA_XAC_NHAN));

        List<ThongBaoDon> ketQua = thongBaoService.layDanhSachThongBaoToiHan(now);

        assertEquals(1, ketQua.size());
        ThongBaoDon tb = ketQua.get(0);
        assertEquals("DP001", tb.getMaDon());
        assertEquals(LoaiThongBao.CHECK_IN_HOM_NAY, tb.getLoaiThongBao());
        assertEquals(MucDo.CANH_BAO, tb.getMucDo());
    }

    @Test
    @DisplayName("2. Phát hiện đơn quá hạn nhận phòng (QUA_HAN_CHECK_IN)")
    void testQuaHanCheckIn() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 2, 18, 0);
        LocalDateTime checkInQuaHan = LocalDateTime.of(2026, 11, 2, 14, 0); // Đã trễ 4 tiếng
        LocalDateTime checkOut = LocalDateTime.of(2026, 11, 4, 12, 0);

        danhSachDatPhongMau.add(taoDatPhongMau("DP002", "102", checkInQuaHan, checkOut, TrangThaiDatPhong.DA_XAC_NHAN));

        List<ThongBaoDon> ketQua = thongBaoService.layDanhSachThongBaoToiHan(now);

        assertEquals(1, ketQua.size());
        ThongBaoDon tb = ketQua.get(0);
        assertEquals("DP002", tb.getMaDon());
        assertEquals(LoaiThongBao.QUA_HAN_CHECK_IN, tb.getLoaiThongBao());
        assertEquals(MucDo.NGUY_CAP, tb.getMucDo());
        assertTrue(tb.getTinhTrangChiTiet().contains("Trễ 4 giờ"));
    }

    @Test
    @DisplayName("3. Phát hiện đơn tới hạn trả phòng hôm nay (CHECK_OUT_HOM_NAY)")
    void testCheckOutHomNay() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 2, 9, 0);
        LocalDateTime checkIn = LocalDateTime.of(2026, 10, 31, 14, 0);
        LocalDateTime checkOutHomNay = LocalDateTime.of(2026, 11, 2, 12, 0); // Trưa nay trả phòng

        danhSachDatPhongMau.add(taoDatPhongMau("DP003", "201", checkIn, checkOutHomNay, TrangThaiDatPhong.DANG_O));

        List<ThongBaoDon> ketQua = thongBaoService.layDanhSachThongBaoToiHan(now);

        assertEquals(1, ketQua.size());
        ThongBaoDon tb = ketQua.get(0);
        assertEquals("DP003", tb.getMaDon());
        assertEquals(LoaiThongBao.CHECK_OUT_HOM_NAY, tb.getLoaiThongBao());
        assertEquals(MucDo.CANH_BAO, tb.getMucDo());
    }

    @Test
    @DisplayName("4. Phát hiện đơn quá hạn trả phòng (QUA_HAN_CHECK_OUT)")
    void testQuaHanCheckOut() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 2, 15, 0);
        LocalDateTime checkIn = LocalDateTime.of(2026, 10, 31, 14, 0);
        LocalDateTime checkOutQuaHan = LocalDateTime.of(2026, 11, 2, 12, 0); // Đã trễ 3 tiếng chưa trả

        danhSachDatPhongMau.add(taoDatPhongMau("DP004", "202", checkIn, checkOutQuaHan, TrangThaiDatPhong.DANG_O));

        List<ThongBaoDon> ketQua = thongBaoService.layDanhSachThongBaoToiHan(now);

        assertEquals(1, ketQua.size());
        ThongBaoDon tb = ketQua.get(0);
        assertEquals("DP004", tb.getMaDon());
        assertEquals(LoaiThongBao.QUA_HAN_CHECK_OUT, tb.getLoaiThongBao());
        assertEquals(MucDo.NGUY_CAP, tb.getMucDo());
        assertTrue(tb.getTinhTrangChiTiet().contains("Trễ 3 giờ"));
    }

    @Test
    @DisplayName("5. Bỏ qua các đơn đã trả phòng hoặc đã hủy")
    void testBoQuaDonDaTraVaDaHuy() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 2, 15, 0);
        LocalDateTime past = LocalDateTime.of(2026, 11, 1, 12, 0);

        danhSachDatPhongMau.add(taoDatPhongMau("DP005", "101", past, past.plusDays(1), TrangThaiDatPhong.DA_TRA_PHONG));
        danhSachDatPhongMau.add(taoDatPhongMau("DP006", "102", past, past.plusDays(1), TrangThaiDatPhong.DA_HUY));

        List<ThongBaoDon> ketQua = thongBaoService.layDanhSachThongBaoToiHan(now);
        assertEquals(0, ketQua.size(), "Các đơn đã trả phòng hoặc đã hủy không được coi là tới hạn");
    }

    @Test
    @DisplayName("6. Sắp xếp ưu tiên: Quá hạn (NGUY_CAP) luôn đứng trước Cảnh báo (CANH_BAO)")
    void testThuTuUuTienSapXep() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 2, 16, 0);

        // Đơn 1: Check-in hôm nay lúc 20:00 (chưa quá hạn -> CANH_BAO)
        danhSachDatPhongMau.add(taoDatPhongMau("DP_CANH_BAO", "101",
                LocalDateTime.of(2026, 11, 2, 20, 0),
                LocalDateTime.of(2026, 11, 4, 12, 0),
                TrangThaiDatPhong.DA_XAC_NHAN));

        // Đơn 2: Check-out đã trễ lúc 12:00 (quá hạn -> NGUY_CAP)
        danhSachDatPhongMau.add(taoDatPhongMau("DP_NGUY_CAP", "201",
                LocalDateTime.of(2026, 10, 31, 14, 0),
                LocalDateTime.of(2026, 11, 2, 12, 0),
                TrangThaiDatPhong.DANG_O));

        List<ThongBaoDon> ketQua = thongBaoService.layDanhSachThongBaoToiHan(now);

        assertEquals(2, ketQua.size());
        assertEquals("DP_NGUY_CAP", ketQua.get(0).getMaDon(), "Đơn khẩn cấp/quá hạn phải được ưu tiên lên đầu");
        assertEquals("DP_CANH_BAO", ketQua.get(1).getMaDon());
    }

    @Test
    @DisplayName("7. Trạng thái chuông: Không có thông báo -> KHONG_CO_THONG_BAO (Không có dấu !)")
    void testTrangThaiChuongKhongCoThongBao() {
        assertEquals(0, thongBaoService.demTongSoLuongToiHan());
        assertEquals(ThongBaoService.TrangThaiChuong.KHONG_CO_THONG_BAO, thongBaoService.getTrangThaiChuong());
    }

    @Test
    @DisplayName("8. Trạng thái chuông: Có thông báo chưa đọc -> CO_CHUA_DOC (Dấu ! màu ĐỎ)")
    void testTrangThaiChuongCoChuaDoc() {
        LocalDateTime checkIn = LocalDateTime.now().minusHours(2);
        danhSachDatPhongMau.add(taoDatPhongMau("DP_RED", "101", checkIn, checkIn.plusDays(2), TrangThaiDatPhong.DA_XAC_NHAN));

        assertTrue(thongBaoService.demTongSoLuongToiHan() > 0);
        assertTrue(thongBaoService.demSoLuongChuaDoc() > 0);
        assertEquals(ThongBaoService.TrangThaiChuong.CO_CHUA_DOC, thongBaoService.getTrangThaiChuong(),
                "Khi có thông báo chưa đọc thì chuông phải hiển thị dấu chấm than màu ĐỎ");
    }

    @Test
    @DisplayName("9. Trạng thái chuông: Tất cả thông báo đã đọc -> DA_DOC_HET (Dấu ! màu XANH)")
    void testTrangThaiChuongDaDocHet() {
        LocalDateTime checkIn = LocalDateTime.now().minusHours(2);
        danhSachDatPhongMau.add(taoDatPhongMau("DP_BLUE", "102", checkIn, checkIn.plusDays(2), TrangThaiDatPhong.DA_XAC_NHAN));

        // Đánh dấu tất cả đã đọc
        thongBaoService.danhDauTatCaDaDoc();

        assertTrue(thongBaoService.demTongSoLuongToiHan() > 0);
        assertEquals(0, thongBaoService.demSoLuongChuaDoc());
        assertEquals(ThongBaoService.TrangThaiChuong.DA_DOC_HET, thongBaoService.getTrangThaiChuong(),
                "Khi tất cả thông báo đã đọc thì chuông phải hiển thị dấu chấm than màu XANH");
    }
}
