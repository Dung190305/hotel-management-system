package com.qlks.model;

import com.qlks.exception.ValidationException;
import com.qlks.model.enums.TrangThaiPhong;
import com.qlks.util.ValidationUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử đơn vị các tính chất OOP và quy tắc nghiệp vụ trong tầng Model.
 */
public class ModelOopTest {

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
                TrangThaiPhong.AVAILABLE, true, true);
        assertEquals(new BigDecimal("1500000.00"), phongThuong.tinhTienPhong(3));

        // Phòng thường ở 7 ngày: giảm giá 5% = 3,500,000 - 175,000 = 3,325,000
        assertEquals(new BigDecimal("3325000.00"), phongThuong.tinhTienPhong(7));

        // 2. Phòng VIP: (1,000,000 + 200,000 phụ phí) = 1,200,000/ngày, ở 2 ngày = 2,400,000
        Phong phongVip = new PhongVIP("P301", "301", 3, new BigDecimal("1000000"),
                TrangThaiPhong.AVAILABLE, true, true, new BigDecimal("200000"));
        assertEquals(new BigDecimal("2400000.00"), phongVip.tinhTienPhong(2));

        // Phòng VIP ở 5 ngày: giảm giá 10% = 6,000,000 - 600,000 = 5,400,000
        assertEquals(new BigDecimal("5400000.00"), phongVip.tinhTienPhong(5));
    }

    @Test
    @DisplayName("Kiểm tra tính số đêm và tạm tính tiền trong DatPhong")
    void testDatPhongTinhSoDemVaTienTamTinh() {
        KhachHang kh = new KhachHang("KH01", "Nguyễn Văn A", "0988776655", "a@gmail.com", "001200009999", LocalDate.now());
        Phong phong = new PhongThuong("P102", "102", 1, new BigDecimal("600000"), TrangThaiPhong.AVAILABLE, true, true);
        NhanVien nv = new LeTan("NV01", "Trần Lễ Tân", "0912345678", "letan@hotel.com", "letan01", LocalDate.now(), new BigDecimal("7000000"), true, 0, BigDecimal.ZERO);

        LocalDateTime nhan = LocalDateTime.of(2026, 10, 24, 14, 0);
        LocalDateTime tra = LocalDateTime.of(2026, 10, 27, 12, 0);

        DatPhong dp = new DatPhong("DP001", kh, phong, nv, nhan, tra, null, null, 2, null, BigDecimal.ZERO, null);
        assertEquals(3, dp.tinhSoDem());
        assertEquals(new BigDecimal("1800000.00"), dp.tinhTienTamTinh());
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
