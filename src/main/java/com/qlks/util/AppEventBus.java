package com.qlks.util;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Event Bus trung gian xử lý thông báo sự kiện đồng bộ dữ liệu thời gian thực giữa các Panel trong Swing.
 * Áp dụng mẫu thiết kế Observer (Publish/Subscribe) chuẩn OOP.
 */
public final class AppEventBus {

    public enum EventType {
        ROOM_STATUS_CHANGED, // Khi trạng thái phòng thay đổi (đặt phòng, check-in, check-out, dọn phòng, bảo trì)
        BOOKING_CHANGED,     // Khi danh sách đặt phòng thay đổi (tạo mới, hủy, check-in, check-out)
        INVOICE_CHANGED,     // Khi hóa đơn thay đổi (thêm dịch vụ, thanh toán)
        CUSTOMER_CHANGED,    // Khi khách hàng được thêm hoặc sửa
        ROOM_ORDER_RECEIVED, // Khi khách trong phòng quét QR gửi đơn gọi dịch vụ mới (Pending Order)
        ROOM_ORDER_UPDATED   // Khi đơn dịch vụ phòng được duyệt, đang giao hoặc từ chối
    }

    public interface AppEventListener {
        void onEvent(EventType eventType);
    }

    private static final List<AppEventListener> LISTENERS = new CopyOnWriteArrayList<>();

    private AppEventBus() {
    }

    /**
     * Đăng ký lắng nghe sự kiện thay đổi dữ liệu.
     */
    public static void register(AppEventListener listener) {
        if (listener != null && !LISTENERS.contains(listener)) {
            LISTENERS.add(listener);
        }
    }

    /**
     * Hủy đăng ký lắng nghe sự kiện.
     */
    public static void unregister(AppEventListener listener) {
        if (listener != null) {
            LISTENERS.remove(listener);
        }
    }

    /**
     * Phát sự kiện tới tất cả các Listener trên Event Dispatch Thread (EDT).
     */
    public static void publish(EventType eventType) {
        SwingUtilities.invokeLater(() -> {
            for (AppEventListener listener : LISTENERS) {
                try {
                    listener.onEvent(eventType);
                } catch (Exception e) {
                    // Tránh lỗi từ 1 listener làm ảnh hưởng đến các listener khác
                    e.printStackTrace();
                }
            }
        });
    }

    public static void fireRoomStatusChanged() {
        publish(EventType.ROOM_STATUS_CHANGED);
    }

    public static void fireBookingChanged() {
        publish(EventType.BOOKING_CHANGED);
    }

    public static void fireInvoiceChanged() {
        publish(EventType.INVOICE_CHANGED);
    }

    public static void fireCustomerChanged() {
        publish(EventType.CUSTOMER_CHANGED);
    }

    public static void fireRoomOrderReceived() {
        publish(EventType.ROOM_ORDER_RECEIVED);
    }

    public static void fireRoomOrderUpdated() {
        publish(EventType.ROOM_ORDER_UPDATED);
    }
}
