package com.qlks.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Tiện ích sinh mã QR Code thanh toán chuyển khoản ngân hàng chuẩn VietQR.
 * Hoạt động offline 100% qua thư viện Google ZXing.
 */
public final class QRCodeUtil {

    // Cấu hình tài khoản thụ hưởng mặc định của Khách sạn
    public static final String DEFAULT_BANK_CODE = "MB"; // Ngân hàng Quân Đội (MB Bank)
    public static final String DEFAULT_BANK_NAME = "MB Bank (Ngân hàng TMCP Quân Đội)";
    public static final String DEFAULT_ACCOUNT_NO = "0345678999";
    public static final String DEFAULT_ACCOUNT_NAME = "KHACH SAN GRAND LUXURY HOTEL";

    private QRCodeUtil() {
    }

    /**
     * Sinh hình ảnh QR code thanh toán từ thông tin giao dịch cọc phòng.
     *
     * @param maDatPhong Mã đặt phòng (VD: DP000001)
     * @param hoTenKhach Tên khách hàng đặt phòng
     * @param soTien     Số tiền cọc cần chuyển khoản
     * @param width      Chiều rộng ảnh (pixel)
     * @param height     Chiều cao ảnh (pixel)
     * @return BufferedImage ảnh mã QR
     */
    public static BufferedImage taoMaQRTienCoc(String maDatPhong, String hoTenKhach, BigDecimal soTien, int width, int height) {
        String noiDungCk = taoNoiDungChuyenKhoan(maDatPhong, hoTenKhach);
        String qrPayload = taoVietQRPayload(DEFAULT_BANK_CODE, DEFAULT_ACCOUNT_NO, DEFAULT_ACCOUNT_NAME, soTien, noiDungCk);
        return taoAnhQRCode(qrPayload, width, height);
    }

    /**
     * Tạo chuỗi nội dung chuyển khoản ngắn gọn, chuẩn không dấu để ngân hàng xử lý mượt mà.
     */
    public static String taoNoiDungChuyenKhoan(String maDatPhong, String hoTenKhach) {
        String tenKhachKhongDau = hoTenKhach != null ? boDauTiengViet(hoTenKhach).toUpperCase().replaceAll("[^A-Z0-9 ]", "").trim() : "";
        if (tenKhachKhongDau.length() > 15) {
            tenKhachKhongDau = tenKhachKhongDau.substring(0, 15).trim();
        }
        return "COC " + (maDatPhong != null ? maDatPhong.trim() : "") + " " + tenKhachKhongDau;
    }

    /**
     * Tạo payload chuẩn liên kết VietQR QuickLink.
     */
    public static String taoVietQRPayload(String bankCode, String accountNo, String accountName, BigDecimal soTien, String noiDung) {
        try {
            long amount = soTien != null ? soTien.longValue() : 0L;
            String encodedAddInfo = URLEncoder.encode(noiDung != null ? noiDung : "", StandardCharsets.UTF_8);
            String encodedName = URLEncoder.encode(accountName != null ? accountName : "", StandardCharsets.UTF_8);
            return String.format("https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                    bankCode, accountNo, amount, encodedAddInfo, encodedName);
        } catch (Exception e) {
            return String.format("STK: %s | NH: %s | SOTIEN: %s | NOIDUNG: %s",
                    accountNo, bankCode, soTien != null ? soTien.toPlainString() : "0", noiDung);
        }
    }

    /**
     * Sinh BufferedImage mã QR từ chuỗi nội dung bất kỳ bằng Google ZXing.
     */
    public static BufferedImage taoAnhQRCode(String content, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (Exception e) {
            // Fallback tạo ảnh trắng nếu lỗi
            BufferedImage fallback = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = fallback.createGraphics();
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setColor(java.awt.Color.BLACK);
            g.drawString("QR Error: " + e.getMessage(), 10, height / 2);
            g.dispose();
            return fallback;
        }
    }

    private static String boDauTiengViet(String str) {
        if (str == null) return "";
        String nfdNormalizedString = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD);
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("").replaceAll("Đ", "D").replaceAll("đ", "d");
    }
}

