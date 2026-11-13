package com.qlks.util;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Set;

/**
 * Tiện ích nhận diện ngày cuối tuần và các ngày Lễ Việt Nam để xác định chính sách cọc.
 * - Ngày thường: Thứ 2 -> Thứ 6 (không trùng lễ) -> Cọc tối thiểu 30%.
 * - Cuối tuần (Thứ 7, CN) hoặc Ngày Lễ -> Cọc tối thiểu 50%.
 */
public final class HolidayUtil {

    public static final BigDecimal TY_LE_NGAY_THUONG = new BigDecimal("0.30");
    public static final BigDecimal TY_LE_CUOI_TUAN_LE = new BigDecimal("0.50");

    // Các ngày lễ cố định theo Dương lịch tại Việt Nam
    private static final Set<MonthDay> NGAY_LE_DUONG_LICH = Set.of(
            MonthDay.of(1, 1),   // Tết Dương lịch
            MonthDay.of(4, 30),  // Giải phóng miền Nam
            MonthDay.of(5, 1),   // Quốc tế Lao động
            MonthDay.of(9, 2),   // Quốc khánh
            MonthDay.of(9, 3)    // Nghỉ liền kề Quốc khánh
    );

    private HolidayUtil() {
    }

    /**
     * Kiểm tra một ngày cụ thể có phải là ngày cuối tuần (Thứ 7, CN) hoặc Ngày Lễ hay không.
     */
    public static boolean isCuoiTuanHoacLe(LocalDate date) {
        if (date == null) {
            return false;
        }
        DayOfWeek dow = date.getDayOfWeek();
        if (dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY) {
            return true;
        }
        MonthDay md = MonthDay.from(date);
        return NGAY_LE_DUONG_LICH.contains(md);
    }

    /**
     * Kiểm tra xem trong khoảng thời gian lưu trú (từ ngày nhận đến ngày trả)
     * có ngày nào rơi vào Cuối tuần hoặc Ngày Lễ hay không.
     */
    public static boolean coNgayCuoiTuanHoacLe(LocalDate tuNgay, LocalDate denNgay) {
        if (tuNgay == null || denNgay == null || !denNgay.isAfter(tuNgay)) {
            return isCuoiTuanHoacLe(tuNgay);
        }

        LocalDate current = tuNgay;
        while (current.isBefore(denNgay)) {
            if (isCuoiTuanHoacLe(current)) {
                return true;
            }
            current = current.plusDays(1);
        }
        return false;
    }

    /**
     * Lấy tỷ lệ cọc tối thiểu theo quy định: 50% nếu dính cuối tuần/lễ, ngược lại 30%.
     */
    public static BigDecimal tinhTyLeCoc(LocalDate tuNgay, LocalDate denNgay) {
        return coNgayCuoiTuanHoacLe(tuNgay, denNgay) ? TY_LE_CUOI_TUAN_LE : TY_LE_NGAY_THUONG;
    }

    /**
     * Trả về chuỗi mô tả loại ngày để hiển thị trên giao diện người dùng.
     */
    public static String getMoTaLoaiNgay(LocalDate tuNgay, LocalDate denNgay) {
        if (coNgayCuoiTuanHoacLe(tuNgay, denNgay)) {
            return "Cuối tuần / Ngày Lễ (Yêu cầu cọc 50%)";
        }
        return "Ngày thường (Yêu cầu cọc 30%)";
    }
}

