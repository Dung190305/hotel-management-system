package com.qlks;

import com.formdev.flatlaf.FlatLightLaf;
import org.mindrot.jbcrypt.BCrypt;

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

        // 2. Kiểm tra nhanh thư viện jBCrypt mã hóa mật khẩu
        String samplePassword = "admin";
        String hashed = BCrypt.hashpw(samplePassword, BCrypt.gensalt(10));
        boolean check = BCrypt.checkpw(samplePassword, hashed);
        LOGGER.info("Kiểm tra jBCrypt: Mật khẩu test '" + samplePassword + "' -> Băm: " + (check ? "OK" : "FAILED"));

        // 3. Khởi chạy giao diện chính trên Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            System.out.println("Hệ thống đã sẵn sàng cho các giai đoạn tiếp theo!");
        });
    }
}
