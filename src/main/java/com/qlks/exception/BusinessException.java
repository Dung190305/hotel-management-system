package com.qlks.exception;

/**
 * Ngoại lệ nghiệp vụ (Business Rule Violation Exception).
 * Được ném ra khi vi phạm các quy tắc như:
 * - Trùng lịch đặt phòng
 * - Ngày nhận >= ngày trả
 * - Thanh toán hóa đơn đã thanh toán rồi
 * - Số tiền không hợp lệ
 */
public class BusinessException extends HotelException {
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
