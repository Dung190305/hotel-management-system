package com.qlks.util;

import org.mindrot.jbcrypt.BCrypt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Công cụ tự động xóa sạch cơ sở dữ liệu cũ và khởi tạo lại toàn bộ Schema & Seed data từ đầu.
 * Đảm bảo đồng bộ 100% dữ liệu sạch theo chuẩn PMS.
 */
public class DatabaseResetTool {

    public static void main(String[] args) {
        System.out.println("=====================================================================");
        System.out.println(" BẮT ĐẦU QUÁ TRÌNH RESET TOÀN BỘ CƠ SỞ DỮ LIỆU HOTEL_DB");
        System.out.println("=====================================================================");

        Properties properties = new Properties();
        try (InputStream input = DatabaseResetTool.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (Exception e) {
            System.err.println("Cảnh báo: Không thể nạp database.properties: " + e.getMessage());
        }

        String envUrl = System.getenv("DB_URL");
        String envUser = System.getenv("DB_USER");
        if (envUser == null) {
            envUser = System.getenv("DB_USERNAME");
        }
        String envPass = System.getenv("DB_PASSWORD");

        String dbUrl = (envUrl != null && !envUrl.isBlank()) ? envUrl : properties.getProperty("db.url");
        String dbUser = (envUser != null && !envUser.isBlank()) ? envUser : properties.getProperty("db.username", "root");
        String dbPass = (envPass != null) ? envPass : properties.getProperty("db.password", "");

        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = "jdbc:mysql://localhost:3306/hotel_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh&useSSL=false&allowPublicKeyRetrieval=true";
        }

        // Tạo server URL không trỏ cụ thể vào hotel_db để thực hiện DROP và CREATE DATABASE
        String serverUrl = dbUrl.replaceFirst("/hotel_db(\\?|$)", "/$1");
        System.out.println("-> Kết nối MySQL Server: " + serverUrl.replaceAll("password=[^&]*", "password=***"));

        File sqlFile = new File("database/hotel_management.sql");
        if (!sqlFile.exists()) {
            System.err.println("LỖI: Không tìm thấy file database/hotel_management.sql!");
            System.exit(1);
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("LỖI: Không tìm thấy MySQL JDBC Driver: " + e.getMessage());
            System.exit(1);
        }

        try (Connection conn = DriverManager.getConnection(serverUrl, dbUser, dbPass);
             Statement stmt = conn.createStatement()) {

            System.out.println("-> Kết nối máy chủ MySQL thành công!");

            // Đọc và tách các câu lệnh SQL từ database/hotel_management.sql
            List<String> statements = parseSqlStatements(sqlFile);
            System.out.println("-> Đã phân tích cú pháp " + statements.size() + " câu lệnh SQL.");

            for (int i = 0; i < statements.size(); i++) {
                String sql = statements.get(i);
                try {
                    stmt.execute(sql);
                } catch (Exception ex) {
                    System.err.println("Lỗi tại câu lệnh " + (i + 1) + ": " + sql);
                    throw ex;
                }
            }

            System.out.println("-> Đã thực thi xong toàn bộ cấu trúc bảng và dữ liệu mẫu.");

            // Cập nhật lại mật khẩu BCrypt chuẩn bảo mật cho các tài khoản
            try (Connection hotelConn = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
                updateAccountPassword(hotelConn, "admin", "admin123");
                updateAccountPassword(hotelConn, "manager", "admin123");
                updateAccountPassword(hotelConn, "letan01", "letan123");
                updateAccountPassword(hotelConn, "letan02", "letan123");
                System.out.println("-> Đã đồng bộ mã hóa mật khẩu BCrypt (admin123, letan123).");

                inThongKeDuLieu(hotelConn);
            }

            System.out.println("=====================================================================");
            System.out.println(" RESET CƠ SỞ DỮ LIỆU THÀNH CÔNG 100%! HỆ THỐNG ĐÃ SẴN SÀNG HOẠT ĐỘNG.");
            System.out.println("=====================================================================");

        } catch (Exception ex) {
            System.err.println("LỖI KHI RESET CƠ SỞ DỮ LIỆU: " + ex.getMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private static List<String> parseSqlStatements(File file) throws Exception {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                // Bỏ qua chú thích dòng và dòng trống
                if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("//")) {
                    continue;
                }
                if (trimmed.startsWith("/*") && trimmed.endsWith("*/")) {
                    continue;
                }

                current.append(line).append("\n");

                if (trimmed.endsWith(";")) {
                    String sql = current.toString().trim();
                    if (sql.endsWith(";")) {
                        sql = sql.substring(0, sql.length() - 1).trim();
                    }
                    if (!sql.isEmpty()) {
                        statements.add(sql);
                    }
                    current.setLength(0);
                }
            }
        }
        return statements;
    }

    private static void updateAccountPassword(Connection conn, String username, String rawPassword) throws Exception {
        String hash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(10));
        String sql = "UPDATE tai_khoan SET mat_khau_hash = ? WHERE ten_dang_nhap = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash);
            ps.setString(2, username);
            ps.executeUpdate();
        }
    }

    private static void inThongKeDuLieu(Connection conn) throws Exception {
        System.out.println("\n--- TỔNG HỢP DỮ LIỆU MẪU ĐÃ KHỞI TẠO ---");
        inSoLuongDong(conn, "tai_khoan", "Tài khoản hệ thống");
        inSoLuongDong(conn, "nhan_vien", "Nhân viên");
        inSoLuongDong(conn, "khach_hang", "Khách hàng");
        inSoLuongDong(conn, "phong", "Phòng");
        inSoLuongDong(conn, "dat_phong", "Đơn đặt phòng (Đã đồng bộ trạng thái)");
        inSoLuongDong(conn, "chi_tiet_khach_dat_phong", "Khách lưu trú & CCCD");
        inSoLuongDong(conn, "dich_vu", "Dịch vụ");
        inSoLuongDong(conn, "chi_tiet_dich_vu", "Dịch vụ phòng đã dùng");
        inSoLuongDong(conn, "id_sequence", "Chuỗi số sinh mã tự động");
        System.out.println("----------------------------------------\n");
    }

    private static void inSoLuongDong(Connection conn, String tableName, String label) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                System.out.println(" • " + label + " (" + tableName + "): " + rs.getInt(1) + " bản ghi");
            }
        }
    }
}

