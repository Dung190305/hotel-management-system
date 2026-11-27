package com.qlks.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Tiện ích định dạng tiền tệ và ngày giờ Việt Nam cho giao diện Swing.
 */
public final class FormatUtil {

    private static final DecimalFormat CURRENCY_FORMAT;
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        CURRENCY_FORMAT = new DecimalFormat("#,##0", symbols);
    }

    private FormatUtil() {
    }

    /**
     * Định dạng số tiền sang chuẩn hiển thị: ví dụ 1.500.000 VNĐ
     */
    public static String formatTien(BigDecimal soTien) {
        if (soTien == null) {
            return "0 VNĐ";
        }
        return CURRENCY_FORMAT.format(soTien) + " VNĐ";
    }

    /**
     * Định dạng ngày: dd/MM/yyyy
     */
    public static String formatNgay(LocalDate ngay) {
        if (ngay == null)
            return "";
        return ngay.format(DATE_FORMATTER);
    }

    /**
     * Định dạng ngày giờ: dd/MM/yyyy HH:mm
     */
    public static String formatNgayGio(LocalDateTime ngayGio) {
        if (ngayGio == null)
            return "";
        return ngayGio.format(DATE_TIME_FORMATTER);
    }

    /**
     * Parse chuỗi số tiền thành BigDecimal an toàn
     */
    public static BigDecimal parseTien(String str) {
        if (str == null || str.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            String s = str.trim();
            // Nếu chuỗi chứa số thập phân lẻ .00 hoặc ,00 (ví dụ từ MySQL toPlainString) thì loại bỏ phần .00 trước
            s = s.replaceAll("[\\.,]0+$", "");
            String clean = s.replaceAll("[^0-9]", "");
            if (clean.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return new BigDecimal(clean);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}
