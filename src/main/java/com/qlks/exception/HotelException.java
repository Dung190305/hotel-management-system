package com.qlks.exception;

/**
 * Lớp ngoại lệ cơ sở (Root Exception) cho toàn bộ ứng dụng Quản lý khách sạn.
 * Kế thừa RuntimeException để hỗ trợ Unchecked Exception linh hoạt.
 */
public class HotelException extends RuntimeException {
    public HotelException(String message) {
        super(message);
    }

    public HotelException(String message, Throwable cause) {
        super(message, cause);
    }
}
