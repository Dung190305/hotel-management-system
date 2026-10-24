package com.qlks.exception;

/**
 * Ngoại lệ truy xuất dữ liệu (Data Access Exception).
 * Bao bọc các lỗi SQLException hoặc lỗi kết nối JDBC, ngăn không để lộ chi tiết SQL lên tầng giao diện người dùng.
 */
public class DataAccessException extends HotelException {
    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
