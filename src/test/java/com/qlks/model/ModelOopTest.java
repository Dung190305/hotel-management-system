package com.qlks.model;

import com.qlks.exception.ValidationException;
import com.qlks.model.enums.CauHinhLoaiPhong;
import com.qlks.model.enums.HangPhong;
import com.qlks.model.enums.LoaiGiuong;
import com.qlks.model.enums.TrangThaiPhong;
import com.qlks.model.enums.VaiTroKhachLuuTru;
import com.qlks.util.ValidationUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * BỘ KIỂM THỬ MODEL & TÍNH CHẤT HƯỚNG ĐỐI TƯỢNG (OOP):
 * Test 1 -> Test 6: Kiểm thử 4 cấu hình phòng chuẩn, kế thừa tiện nghi và ngăn chặn cấu hình thứ 5.
 * Các test bổ sung: Đa hình tính lương, đa hình tính tiền phòng, Bean Validation, bảo mật che CCCD.
 */
public class ModelOopTest {

    @Test
    @DisplayName("Test 1: Kiểm tra cấu hình PHONG_DON_THUONG (1 giường, sức chứa tối đa 2 người)")
    void test01_CauHinhPhongDonThuong() {
        CauHinhLoaiPhong ch = CauHinhLoaiPhong.PHONG_DON_THUONG;
        assertEquals("Phòng đơn thường", ch.getTenHienThi());
        assertEquals(LoaiGiuong.DON, ch.getLoaiGiuong());
        assertEquals(HangPhong.THUONG, ch.getHangPhong());
        assertEquals(1, ch.getSoGiuong());
        assertEquals(2, ch.getSucChuaToiDa());

        Phong phong = new PhongThuong("P101", "101", 1, new BigDecimal("450000"),
                TrangThaiPhong.AVAILABLE, LoaiGiuong.DON, true, true);
        assertEquals(1, phong.getSoGiuong());
        assertEquals(2, phong.getSucChuaToiDa());
        assertEquals(CauHinhLoaiPhong.PHONG_DON_THUONG, phong.getCauHinh());
        assertEquals("Phòng đơn thường", phong.getTenLoaiPhongHienThi());
    }

    @Test
    @DisplayName("Test 2: Kiểm tra cấu hình PHONG_DON_VIP (1 giường, sức chứa tối đa 2 người)")
    void test02_CauHinhPhongDonVip() {
        CauHinhLoaiPhong ch = CauHinhLoaiPhong.PHONG_DON_VIP;
        assertEquals("Phòng đơn VIP", ch.getTenHienThi());
        assertEquals(LoaiGiuong.DON, ch.getLoaiGiuong());
        assertEquals(HangPhong.VIP, ch.getHangPhong());
        assertEquals(1, ch.getSoGiuong());
        assertEquals(2, ch.getSucChuaToiDa());

        Phong phong = new PhongVIP("P201", "201", 2, new BigDecimal("800000"),
                TrangThaiPhong.AVAILABLE, LoaiGiuong.DON, true, true, new BigDecimal("150000"));
        assertEquals(1, phong.getSoGiuong());
        assertEquals(2, phong.getSucChuaToiDa());
        assertEquals(CauHinhLoaiPhong.PHONG_DON_VIP, phong.getCauHinh());
        assertEquals("Phòng đơn VIP", phong.getTenLoaiPhongHienThi());
    }

    @Test
    @DisplayName("Test 3: Kiểm tra cấu hình PHONG_DOI_THUONG (2 giường, sức chứa tối đa 4 người)")
    void test03_CauHinhPhongDoiThuong() {
        CauHinhLoaiPhong ch = CauHinhLoaiPhong.PHONG_DOI_THUONG;
        assertEquals("Phòng đôi thường", ch.getTenHienThi());
        assertEquals(LoaiGiuong.DOI, ch.getLoaiGiuong());
        assertEquals(HangPhong.THUONG, ch.getHangPhong());
        assertEquals(2, ch.getSoGiuong());
        assertEquals(4, ch.getSucChuaToiDa());

        Phong phong = new PhongThuong("P102", "102", 1, new BigDecimal("700000"),
                TrangThaiPhong.AVAILABLE, LoaiGiuong.DOI, true, true);
        assertEquals(2, phong.getSoGiuong());
        assertEquals(4, phong.getSucChuaToiDa());
        assertEquals(CauHinhLoaiPhong.PHONG_DOI_THUONG, phong.getCauHinh());
        assertEquals("Phòng đôi thường", phong.getTenLoaiPhongHienThi());
    }

    @Test
    @DisplayName("Test 4: Kiểm tra cấu hình PHONG_DOI_VIP (2 giường, sức chứa tối đa 4 người)")
    void test04_CauHinhPhongDoiVip() {
        CauHinhLoaiPhong ch = CauHinhLoaiPhong.PHONG_DOI_VIP;
        assertEquals("Phòng đôi VIP", ch.getTenHienThi());
        assertEquals(LoaiGiuong.DOI, ch.getLoaiGiuong());
        assertEquals(HangPhong.VIP, ch.getHangPhong());
        assertEquals(2, ch.getSoGiuong());
        assertEquals(4, ch.getSucChuaToiDa());

        Phong phong = new PhongVIP("P301", "301", 3, new BigDecimal("1200000"),
                TrangThaiPhong.AVAILABLE, LoaiGiuong.DOI, true, true, new BigDecimal("300000"));
        assertEquals(2, phong.getSoGiuong());
        assertEquals(4, phong.getSucChuaToiDa());
        assertEquals(CauHinhLoaiPhong.PHONG_DOI_VIP, phong.getCauHinh());
        assertEquals("Phòng đôi VIP", phong.getTenLoaiPhongHienThi());
    }

    @Test
    @DisplayName("Test 5: Kế thừa danh mục tiện nghi: VIP kế thừa toàn bộ tiện nghi Thường và bổ sung tiện nghi riêng")
    void test05_KeThuaTienNghiPolymorphism() {
        // Danh mục tiện nghi tiêu chuẩn cơ bản của Phòng Thường (5 món)
        List<String> tienNghiCoBan = PhongThuong.TIEN_NGHI_CO_BAN;
        assertEquals(5, tienNghiCoBan.size());
        assertTrue(tienNghiCoBan.contains("Đệm cao su non"));
        assertTrue(tienNghiCoBan.contains("Điều hòa"));
        assertTrue(tienNghiCoBan.contains("Nhà tắm"));
        assertTrue(tienNghiCoBan.contains("Nhà vệ sinh"));
        assertTrue(tienNghiCoBan.contains("Tủ quần áo"));

        // Phòng VIP kế thừa: có đủ 5 món cơ bản VÀ 3 món VIP bổ sung (Tivi, Bồn tắm, Tủ lạnh) -> tổng 8 món
        Phong phongVip = new PhongVIP("P302", "302", 3, new BigDecimal("1000000"),
                TrangThaiPhong.AVAILABLE, LoaiGiuong.DON, true, true, BigDecimal.ZERO);
        List<String> tienNghiVip = phongVip.getDanhSachTienNghi();

        assertEquals(8, tienNghiVip.size());
        assertTrue(tienNghiVip.containsAll(tienNghiCoBan), "Phòng VIP phải kế thừa đầy đủ 100% tiện nghi cơ bản");
        assertTrue(tienNghiVip.contains("Tivi"), "Phòng VIP phải có Tivi");
        assertTrue(tienNghiVip.contains("Bồn tắm"), "Phòng VIP phải có Bồn tắm");
        assertTrue(tienNghiVip.contains("Tủ lạnh"), "Phòng VIP phải có Tủ lạnh");

        // Kiểm tra tính đa hình qua con trỏ lớp cha Phong
        Phong phongThuong = new PhongThuong("P103", "103", 1, new BigDecimal("500000"),
                TrangThaiPhong.AVAILABLE, LoaiGiuong.DON, true, true);
        assertEquals(5, phongThuong.getDanhSachTienNghi().size());
        assertFalse(phongThuong.getDanhSachTienNghi().contains("Tivi"), "Phòng Thường không có tiện nghi cao cấp VIP");
    }

    @Test
    @DisplayName("Test 6: Ngăn chặn cấu hình phòng thứ 5 không hợp lệ")
    void test06_NganChanCauHinhPhongThuNam() {
        // Số lượng cấu hình phòng chuẩn của toàn hệ thống là duy nhất: chính xác 4
        CauHinhLoaiPhong[] tatCaCauHinh = CauHinhLoaiPhong.values();
        assertEquals(4, tatCaCauHinh.length, "Toàn bộ hệ thống chỉ cho phép chính xác 4 cấu hình phòng, không có cấu hình thứ 5!");

        // Khớp nối từ Loại giường và Hạng phòng chỉ trả về 1 trong 4 cấu hình chuẩn
        CauHinhLoaiPhong ch1 = CauHinhLoaiPhong.tuLoaiGiuongVaHang(LoaiGiuong.DON, HangPhong.THUONG);
        CauHinhLoaiPhong ch2 = CauHinhLoaiPhong.tuLoaiGiuongVaHang(LoaiGiuong.DON, HangPhong.VIP);
        CauHinhLoaiPhong ch3 = CauHinhLoaiPhong.tuLoaiGiuongVaHang(LoaiGiuong.DOI, HangPhong.THUONG);
        CauHinhLoaiPhong ch4 = CauHinhLoaiPhong.tuLoaiGiuongVaHang(LoaiGiuong.DOI, HangPhong.VIP);

        assertEquals(CauHinhLoaiPhong.PHONG_DON_THUONG, ch1);
        assertEquals(CauHinhLoaiPhong.PHONG_DON_VIP, ch2);
        assertEquals(CauHinhLoaiPhong.PHONG_DOI_THUONG, ch3);
        assertEquals(CauHinhLoaiPhong.PHONG_DOI_VIP, ch4);

        // Chuỗi không hợp lệ sẽ tự động quy chuẩn về cấu hình hợp lệ, không tạo thêm loại lạ
        CauHinhLoaiPhong chTuChuoi = CauHinhLoaiPhong.fromString("PHONG_TONG_THONG_PRESIDENTIAL");
        assertNotNull(chTuChuoi);
        assertTrue(List.of(tatCaCauHinh).contains(chTuChuoi));
    }

    @Test
    @DisplayName("Kiểm tra tính Đa hình của phương thức tinhLuong() giữa LeTan và QuanLy")
    void testTinhLuongPolymorphism() {
        // 1. Lễ tân: Lương = 7,000,000 + 20 ca * 150,000 = 10,000,000
        NhanVien leTan = new LeTan("NV01", "Trần Lễ Tân", "0912345678", "letan@hotel.com",
                "letan01", LocalDate.now(), new BigDecimal("7000000"), true, 20, new BigDecimal("150000"));
        assertEquals(new BigDecimal("10000000.00"), leTan.tinhLuong().setScale(2));

        // 2. Quản lý: Lương = 15,000,000 * 1.5 + 5,000,000 = 27,500,000
        NhanVien quanLy = new QuanLy("NV02", "Nguyễn Quản Lý", "0901234567", "quanly@hotel.com",
                "admin", LocalDate.now(), new BigDecimal("15000000"), true, 1.5, new BigDecimal("5000000"));
        assertEquals(new BigDecimal("27500000.00"), quanLy.tinhLuong().setScale(2));
    }

    @Test
    @DisplayName("Kiểm tra tính Đa hình của phương thức tinhTienPhong() giữa PhongThuong và PhongVIP")
    void testTinhTienPhongPolymorphism() {
        // 1. Phòng Thường: 500,000/ngày, ở 3 ngày = 1,500,000
        Phong phongThuong = new PhongThuong("P101", "101", 1, new BigDecimal("500000"),
                TrangThaiPhong.AVAILABLE, LoaiGiuong.DON, true, true);
        assertEquals(new BigDecimal("1500000.00"), phongThuong.tinhTienPhong(3));

        // Phòng thường ở 7 ngày: giảm giá 5% = 3,500,000 - 175,000 = 3,325,000
        assertEquals(new BigDecimal("3325000.00"), phongThuong.tinhTienPhong(7));

        // 2. Phòng VIP: (1,000,000 + 200,000 phụ phí) = 1,200,000/ngày, ở 2 ngày = 2,400,000
        Phong phongVip = new PhongVIP("P301", "301", 3, new BigDecimal("1000000"),
                TrangThaiPhong.AVAILABLE, LoaiGiuong.DON, true, true, new BigDecimal("200000"));
        assertEquals(new BigDecimal("2400000.00"), phongVip.tinhTienPhong(2));

        // Phòng VIP ở 5 ngày: giảm giá 10% = 6,000,000 - 600,000 = 5,400,000
        assertEquals(new BigDecimal("5400000.00"), phongVip.tinhTienPhong(5));
    }

    @Test
    @DisplayName("Kiểm tra tính số đêm và tạm tính tiền trong DatPhong")
    void testDatPhongTinhSoDemVaTienTamTinh() {
        KhachHang kh = new KhachHang("KH01", "Nguyễn Văn A", "0988776655", "a@gmail.com", "001200009999", LocalDate.now());
        Phong phong = new PhongThuong("P102", "102", 1, new BigDecimal("600000"), TrangThaiPhong.AVAILABLE, LoaiGiuong.DON, true, true);
        NhanVien nv = new LeTan("NV01", "Trần Lễ Tân", "0912345678", "letan@hotel.com", "letan01", LocalDate.now(), new BigDecimal("7000000"), true, 0, BigDecimal.ZERO);

        LocalDateTime nhan = LocalDateTime.of(2026, 10, 24, 14, 0);
        LocalDateTime tra = LocalDateTime.of(2026, 10, 27, 12, 0);

        DatPhong dp = new DatPhong("DP001", kh, phong, nv, nhan, tra, null, null, 2, null, BigDecimal.ZERO, null);
        assertEquals(3, dp.tinhSoDem());
        assertEquals(new BigDecimal("1800000.00"), dp.tinhTienTamTinh());
    }

    @Test
    @DisplayName("Kiểm tra che số CCCD bảo mật và định dạng 12 chữ số của KhachLuuTru")
    void testKhachLuuTruCheCccdVaValidate() {
        KhachLuuTru k = new KhachLuuTru("DP999", "Nguyễn Văn An", "001200009876", "0987654321", VaiTroKhachLuuTru.NGUOI_DAI_DIEN);
        assertEquals("001******876", k.getSoCCCDChe(), "Số CCCD phải được che ở giữa dạng 001******876");

        assertTrue(KhachLuuTru.kiemTraCCCD("001200009876"));
        assertFalse(KhachLuuTru.kiemTraCCCD("00120000987")); // 11 số -> sai
        assertFalse(KhachLuuTru.kiemTraCCCD("0012000098765")); // 13 số -> sai
        assertFalse(KhachLuuTru.kiemTraCCCD("00120000987A")); // Chứa chữ cái -> sai
        assertFalse(KhachLuuTru.kiemTraCCCD(""));
        assertFalse(KhachLuuTru.kiemTraCCCD(null));
    }

    @Test
    @DisplayName("Kiểm tra Hibernate Bean Validation bắt lỗi dữ liệu đầu vào")
    void testHibernateValidation() {
        // Khách hàng có số điện thoại sai định dạng
        KhachHang khSaiSdt = new KhachHang("KH01", "Nguyễn B", "12345", "b@gmail.com", "001200001111", LocalDate.now());
        ValidationException ex = assertThrows(ValidationException.class, () -> ValidationUtil.validate(khSaiSdt));
        assertTrue(ex.getMessage().contains("Số điện thoại không đúng định dạng"));
    }
}
