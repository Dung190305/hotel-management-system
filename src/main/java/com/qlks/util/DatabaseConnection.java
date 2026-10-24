package com.qlks.util;

import com.qlks.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quản lý kết nối cơ sở dữ liệu MySQL thông qua JDBC.
 * Đọc cấu hình từ file database.properties trong classpath (bảo mật, không hardcode).
 */
public final class DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static final Properties PROPERTIES = new Properties();

    private static String url;
    private static String username;
    private static String password;

    static {
        try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (input == null) {
                throw new DataAccessException("Không tìm thấy file cấu hình database.properties trong resources!");
            }
            PROPERTIES.load(input);

            String driver = PROPERTIES.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
            url = PROPERTIES.getProperty("db.url");
            username = PROPERTIES.getProperty("db.username");
            password = PROPERTIES.getProperty("db.password");

            // Nạp JDBC Driver
            Class.forName(driver);
            LOGGER.info("Đã nạp MySQL JDBC Driver thành công.");
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Lỗi khởi tạo cấu hình kết nối Database", e);
            throw new DataAccessException("Không thể khởi tạo cấu hình cơ sở dữ liệu: " + e.getMessage(), e);
        }
    }

    private DatabaseConnection() {
        // Utility class
    }

    /**
     * Mở một kết nối mới tới cơ sở dữ liệu MySQL.
     * Người gọi phải có trách nhiệm đóng kết nối bằng try-with-resources.
     *
     * @return Connection đối tượng kết nối JDBC
     * @throws DataAccessException nếu kết nối thất bại
     */
    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi kết nối MySQL: " + e.getMessage(), e);
            throw new DataAccessException("Lỗi kết nối đến cơ sở dữ liệu MySQL: " + e.getMessage(), e);
        }
    }

    /**
     * Phương thức kiểm tra kết nối nhanh tới Database.
     *
     * @return true nếu kết nối thành công, false nếu thất bại
     */
    public static boolean checkConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Kiểm tra kết nối thất bại: " + e.getMessage());
            return false;
        }
    }
}
