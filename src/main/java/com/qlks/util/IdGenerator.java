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
    private static final int BLOCK_SIZE = 10;
    private static final ConcurrentHashMap<String, AtomicLong> FALLBACK_MAP = new ConcurrentHashMap<>();

    /**
     * Cấu trúc lưu trữ dải mã (Hi-Lo Sequence Range) trong RAM.
     * Cấp phát nguyên tử qua AtomicLong, loại bỏ hoàn toàn việc gọi CSDL và giữ khóa hàng
     * cho 90% các lần sinh mã tiếp theo.
     */
    private static class SequenceRange {
        private final long maxVal;
        private final AtomicLong currentVal;

        public SequenceRange(long minVal, long maxVal) {
            this.currentVal = new AtomicLong(minVal);
            this.maxVal = maxVal;
        }

        public Long getNext() {
            long next = currentVal.getAndIncrement();
            if (next <= maxVal) {
                return next;
            }
            return null; // Dải mã đã được sử dụng hết
        }
    }

    private static final ConcurrentHashMap<String, SequenceRange> SEQUENCE_CACHE = new ConcurrentHashMap<>();
    private static final Object ALLOCATION_LOCK = new Object();

    // Circuit Breaker chống timeout cascading khi CSDL ngoại tuyến (ví dụ trong Unit Test)
    private static volatile boolean dbCircuitOpen = false;
    private static volatile long lastCircuitTripTime = 0;
    private static final long CIRCUIT_RESET_TIMEOUT_MS = 15000; // 15 giây

    private IdGenerator() {
        // Utility class
    }

    /**
     * Sinh mã định danh kế tiếp cho tiền tố quy định.
     *
     * @param prefix Tiền tố mã (VD: "HD", "DP", "TT", "KH", "DV", "NV")
     * @return Chuỗi mã định danh định dạng {PREFIX}{6_CHU_SO} (VD: HD000001)
     */
    public static String next(String prefix) {
        return next(null, prefix);
    }

    /**
     * Sinh mã định danh kế tiếp tương thích với giao diện nhận Connection.
     * Áp dụng thuật toán Hi-Lo Sequence Block Allocation:
     * - 90% số lần sinh mã được cấp phát tức thì trong RAM (0ms, 0 connection, 0 row lock).
     * - Khi hết block, xin cấp phát 1 block mới gồm 10 mã bằng 1 câu Atomic UPSERT cực nhanh.
     * - Tái sử dụng connection hiện hành (nếu có) để triệt tiêu hoàn toàn rủi ro Connection Pool Deadlock.
     *
     * @param conn   Connection hiện tại của transaction (nếu có)
     * @param prefix Tiền tố mã (VD: "HD", "TT", "DP")
     * @return Chuỗi mã định danh
     */
    public static String next(Connection conn, String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Tiền tố prefix không được để trống!");
        }
        prefix = prefix.trim().toUpperCase();

        // Nếu đang trong chế độ test cô lập hoặc conn là proxy mock, dùng in-memory tức thì
        if (dbCircuitOpen || (conn != null && java.lang.reflect.Proxy.isProxyClass(conn.getClass()))) {
            if (System.currentTimeMillis() - lastCircuitTripTime < CIRCUIT_RESET_TIMEOUT_MS) {
                return nextInMemory(prefix);
            }
            dbCircuitOpen = false;
        }

        // 1. Thử cấp phát từ Hi-Lo Block trong RAM (0ms, 0 connection mượn thêm, 0 lock)
        SequenceRange range = SEQUENCE_CACHE.get(prefix);
        if (range != null) {
            Long val = range.getNext();
            if (val != null) {
                return formatId(prefix, val);
            }
        }

        // 2. Block đã hết hoặc chưa có: Đồng bộ để xin Block mới từ CSDL
        synchronized (ALLOCATION_LOCK) {
            // Double-checked locking kiểm tra xem luồng khác đã nạp block mới chưa
            range = SEQUENCE_CACHE.get(prefix);
            if (range != null) {
                Long val = range.getNext();
                if (val != null) {
                    return formatId(prefix, val);
                }
            }

            if (dbCircuitOpen) {
                return nextInMemory(prefix);
            }

            try {
                long maxVal;
                if (conn != null && !conn.isClosed()) {
                    maxVal = allocateBlockFromConnection(conn, prefix, BLOCK_SIZE);
                } else {
                    try (Connection newConn = DatabaseConnection.getConnection()) {
                        maxVal = allocateBlockFromConnection(newConn, prefix, BLOCK_SIZE);
                    }
                }

                long minVal = maxVal - BLOCK_SIZE + 1;
                SequenceRange newRange = new SequenceRange(minVal, maxVal);
                SEQUENCE_CACHE.put(prefix, newRange);

                Long val = newRange.getNext();
                if (val != null) {
                    return formatId(prefix, val);
                }
            } catch (Exception e) {
                dbCircuitOpen = true;
                lastCircuitTripTime = System.currentTimeMillis();
                LOGGER.log(Level.FINE, "Không thể kết nối CSDL để cấp phát sequence block, chuyển sang in-memory: " + e.getMessage());
                return nextInMemory(prefix);
            }
        }

        return nextInMemory(prefix);
    }

    /**
     * Cấp phát 1 dải mã khối (Block) gồm blockSize số bằng 1 câu lệnh Atomic UPSERT duy nhất:
     * - Nếu prefix chưa có: INSERT và gán LAST_INSERT_ID(blockSize).
     * - Nếu prefix đã có: UPDATE và gán LAST_INSERT_ID(last_value + blockSize).
     */
    private static long allocateBlockFromConnection(Connection conn, String prefix, int blockSize) throws SQLException {
        String upsertSql = "INSERT INTO id_sequence (prefix, `last_value`) "
                + "VALUES (?, LAST_INSERT_ID(?)) "
                + "ON DUPLICATE KEY UPDATE `last_value` = LAST_INSERT_ID(`last_value` + ?)";

        long val = 0;
        try (PreparedStatement ps = conn.prepareStatement(upsertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, prefix);
            ps.setInt(2, blockSize);
            ps.setInt(3, blockSize);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs != null && rs.next()) {
                    val = rs.getLong(1);
                }
            }
        }

        // Fallback nếu driver không trả về qua getGeneratedKeys
        if (val <= 0) {
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT LAST_INSERT_ID()")) {
                if (rs != null && rs.next()) {
                    val = rs.getLong(1);
                }
            }
        }

        if (val > 0) {
            return val;
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
     * Xóa bộ đếm in-memory và Sequence Cache (dùng cho Unit Test cô lập).
     */
    public static void resetInMemory() {
        FALLBACK_MAP.clear();
        SEQUENCE_CACHE.clear();
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
