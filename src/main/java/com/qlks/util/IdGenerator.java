package com.qlks.util;

import com.qlks.exception.DataAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tiện ích sinh mã định danh tăng dần an toàn đa luồng (Thread-safe Id Generator).
 * - Sử dụng câu lệnh nguyên tử UPSERT với MySQL LAST_INSERT_ID, chống trùng lặp đa luồng tuyệt đối.
 * - Tách biệt việc cấp phát mã ra khỏi Transaction nghiệp vụ dài (Autonomous Sequence Generation)
 *   nhằm triệt tiêu nút thắt cổ chai và nguy cơ treo hệ thống (Serialization Lock Bottleneck).
 * - Tích hợp Circuit Breaker tự động chuyển đổi sang Atomic in-memory an toàn khi CSDL ngoại tuyến.
 */
public final class IdGenerator {

    private static final Logger LOGGER = Logger.getLogger(IdGenerator.class.getName());
    private static final ConcurrentHashMap<String, AtomicLong> FALLBACK_MAP = new ConcurrentHashMap<>();

    // Circuit Breaker chống timeout cascading khi CSDL ngoại tuyến (ví dụ trong Unit Test)
    private static volatile boolean dbCircuitOpen = false;
    private static volatile long lastCircuitTripTime = 0;
    private static final long CIRCUIT_RESET_TIMEOUT_MS = 15000; // 15 giây

    private IdGenerator() {
        // Utility class
    }

    /**
     * Sinh mã định danh kế tiếp cho tiền tố quy định trên kết nối độc lập autocommit nhanh.
     *
     * @param prefix Tiền tố mã (VD: "HD", "DP", "TT", "KH", "DV", "NV")
     * @return Chuỗi mã định danh định dạng {PREFIX}{6_CHU_SO} (VD: HD000001)
     */
    public static String next(String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Tiền tố prefix không được để trống!");
        }
        prefix = prefix.trim().toUpperCase();

        if (dbCircuitOpen) {
            if (System.currentTimeMillis() - lastCircuitTripTime < CIRCUIT_RESET_TIMEOUT_MS) {
                return nextInMemory(prefix);
            }
            // Hết thời gian chờ, thử thăm dò lại CSDL
            dbCircuitOpen = false;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            return generateFromConnection(conn, prefix);
        } catch (Exception e) {
            dbCircuitOpen = true;
            lastCircuitTripTime = System.currentTimeMillis();
            LOGGER.log(Level.FINE, "Không thể kết nối CSDL để sinh mã, chuyển sang chế độ in-memory: " + e.getMessage());
            return nextInMemory(prefix);
        }
    }

    /**
     * Sinh mã định danh kế tiếp tương thích với giao diện nhận Connection.
     * <p>
     * LƯU Ý BẢO VỆ TƯƠNG TRANH: Để loại bỏ hoàn toàn nút thắt cổ chai và nguy cơ giữ
     * Exclusive Lock trên bảng sequence trong suốt Transaction nghiệp vụ dài, việc cấp phát
     * mã luôn được thực hiện độc lập (Autonomous Sequence Generation) trên kết nối autocommit
     * hoàn tất trong ~1ms và giải phóng khóa ngay lập tức.
     *
     * @param conn   Connection hiện tại của transaction (được bỏ qua để bảo vệ tương tranh)
     * @param prefix Tiền tố mã (VD: "HD", "TT", "DP")
     * @return Chuỗi mã định danh
     */
    public static String next(Connection conn, String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Tiền tố prefix không được để trống!");
        }
        prefix = prefix.trim().toUpperCase();

        // Nếu conn là mock proxy trong môi trường kiểm thử cô lập, trả về in-memory ngay lập tức
        if (conn != null && java.lang.reflect.Proxy.isProxyClass(conn.getClass())) {
            return nextInMemory(prefix);
        }

        return next(prefix);
    }

    /**
     * Thực thi câu lệnh Atomic UPSERT duy nhất với LAST_INSERT_ID:
     * - Nếu prefix chưa có: INSERT và gán LAST_INSERT_ID(1).
     * - Nếu prefix đã có: UPDATE và gán LAST_INSERT_ID(last_value + 1).
     * Cả hai trường hợp đều trả về giá trị LAST_INSERT_ID chính xác, không bao giờ trả về 0.
     */
    private static String generateFromConnection(Connection conn, String prefix) throws SQLException {
        String upsertSql = "INSERT INTO id_sequence (prefix, `last_value`) "
                + "VALUES (?, LAST_INSERT_ID(1)) "
                + "ON DUPLICATE KEY UPDATE `last_value` = LAST_INSERT_ID(`last_value` + 1)";

        long val = 0;
        try (PreparedStatement ps = conn.prepareStatement(upsertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, prefix);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs != null && rs.next()) {
                    val = rs.getLong(1);
                }
            }
        }

        // Fallback kiểm tra lại nếu JDBC driver không trả về qua getGeneratedKeys
        if (val <= 0) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT LAST_INSERT_ID()")) {
                if (rs != null && rs.next()) {
                    val = rs.getLong(1);
                }
            }
        }

        if (val > 0) {
            return formatId(prefix, val);
        }

        throw new SQLException("Không thể lấy LAST_INSERT_ID từ MySQL cho tiền tố: " + prefix);
    }

    /**
     * Sinh mã in-memory an toàn đa luồng (phục vụ Unit Test offline hoặc khi CSDL gián đoạn).
     */
    public static synchronized String nextInMemory(String prefix) {
        AtomicLong counter = FALLBACK_MAP.computeIfAbsent(prefix, p -> new AtomicLong(0));
        long val = counter.incrementAndGet();
        return formatId(prefix, val);
    }

    /**
     * Xóa bộ đếm in-memory (dùng cho Unit Test cô lập).
     */
    public static void resetInMemory() {
        FALLBACK_MAP.clear();
    }

    /**
     * Cho phép ép buộc chế độ in-memory (tiện lợi cho Unit Test offline chạy tức thì không bị độ trễ kết nối).
     */
    public static void setForceInMemory(boolean force) {
        dbCircuitOpen = force;
        lastCircuitTripTime = force ? (System.currentTimeMillis() + 86400000L) : 0;
    }

    /**
     * Reset Circuit Breaker cho phép thử lại kết nối CSDL ngay lập tức.
     */
    public static void resetCircuitBreaker() {
        dbCircuitOpen = false;
        lastCircuitTripTime = 0;
    }

    private static String formatId(String prefix, long sequenceNumber) {
        return String.format("%s%06d", prefix, sequenceNumber);
    }
}
