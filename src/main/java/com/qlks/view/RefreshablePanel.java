package com.qlks.view;

/**
 * Interface cho các Panel giao diện có khả năng làm mới dữ liệu từ CSDL MySQL.
 * Hỗ trợ đồng bộ dữ liệu thời gian thực khi chuyển Tab hoặc khi nhận thông báo sự kiện.
 */
public interface RefreshablePanel {
    /**
     * Tải lại toàn bộ dữ liệu mới nhất từ CSDL và cập nhật hiển thị lên giao diện Swing.
     */
    void loadDuLieu();
}
