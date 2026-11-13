package com.qlks;

import com.formdev.flatlaf.FlatLightLaf;
import org.mindrot.jbcrypt.BCrypt;

import com.qlks.view.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lớp khởi chạy ứng dụng Quản lý khách sạn (Main Entry Point).
 */
public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        LOGGER.info("=== KHỞI ĐỘNG HỆ THỐNG QUẢN LÝ KHÁCH SẠN ===");
        
        // 1. Cấu hình giao diện FlatLaf hiện đại cho Java Swing
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
            LOGGER.info("Đã áp dụng giao diện FlatLaf thành công.");
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Không thể thiết lập giao diện FlatLaf, sử dụng giao diện mặc định.", ex);
        }

        // 2. Kiểm tra kết nối Database
        if (com.qlks.util.DatabaseConnection.checkConnection()) {
            LOGGER.info("Kết nối cơ sở dữ liệu MySQL hotel_db: THÀNH CÔNG.");
        } else {
            LOGGER.warning("Không thể kết nối đến MySQL. Vui lòng kiểm tra dịch vụ MySQL80 và database.properties!");
        }

        // 3. Khởi động máy chủ QR Room Order phục vụ khách đặt dịch vụ tại phòng
        Thread serverThread = new Thread(() -> {
            com.qlks.server.RoomOrderHttpServer.startServer(com.qlks.server.RoomOrderHttpServer.DEFAULT_PORT);
        }, "RoomOrderServer-Thread");
        serverThread.setDaemon(true);
        serverThread.start();
        Runtime.getRuntime().addShutdownHook(new Thread(com.qlks.server.RoomOrderHttpServer::stopServer));

        // 4. Khởi chạy màn hình đăng nhập LoginFrame trên Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
