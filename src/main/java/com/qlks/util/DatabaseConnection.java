package com.qlks.util;

import com.qlks.exception.DataAccessException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Quản lý kết nối cơ sở dữ liệu MySQL thông qua HikariCP Connection Pool.
 * Đọc cấu hình từ biến môi trường hoặc file database.properties trong classpath.
 * Hỗ trợ tự động quản lý vòng đời kết nối, tái sử dụng kết nối (connection reuse),
 * ngăn ngừa cạn kiệt Connection (Connection Starvation) và hiệu ứng tuyết lở (Cascading Failure).
 */
public final class DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static final Properties PROPERTIES = new Properties();

    private static String url;
    private static String username;
    private static String password;
    private static String driver;

    private static volatile HikariDataSource dataSource;

    static {
        try {
            // Nạp cấu hình từ database.properties nếu có
            try (InputStream input = DatabaseConnection.class.getClassLoader()
                    .getResourceAsStream("database.properties")) {
                if (input != null) {
                    PROPERTIES.load(input);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Không thể đọc file database.properties: " + e.getMessage());
            }

            // Ưu tiên 1: Đọc từ biến môi trường (Environment Variables) hoặc System Properties
            String envUrl = getEnvOrProperty("DB_URL");
            String envUser = getEnvOrProperty("DB_USER");
            if (envUser == null) {
                envUser = getEnvOrProperty("DB_USERNAME");
            }
            String envPass = getEnvOrProperty("DB_PASSWORD");
            String envDriver = getEnvOrProperty("DB_DRIVER");

            // Ưu tiên 2: Fallback về database.properties
            url = (envUrl != null && !envUrl.isBlank()) ? envUrl : PROPERTIES.getProperty("db.url");
            username = (envUser != null && !envUser.isBlank()) ? envUser : PROPERTIES.getProperty("db.username");
            password = (envPass != null) ? envPass : PROPERTIES.getProperty("db.password");
            driver = (envDriver != null && !envDriver.isBlank()) ? envDriver
                    : PROPERTIES.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");

            if (url == null || url.isBlank()) {
                throw new DataAccessException(
                        "Chưa cấu hình đường dẫn kết nối CSDL (DB_URL hoặc db.url trong database.properties)!");
            }

            // Nạp JDBC Driver
            Class.forName(driver);
            LOGGER.info("Đã nạp MySQL JDBC Driver thành công: " + driver);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Lỗi nạp Driver MySQL JDBC", e);
            throw new DataAccessException("Không tìm thấy Driver MySQL: " + e.getMessage(), e);
        }
    }

    private static String getEnvOrProperty(String key) {
        String val = System.getenv(key);
        if (val == null || val.isBlank()) {
            val = System.getProperty(key);
        }
        return (val != null && !val.isBlank()) ? val.trim() : null;
    }

    private static int getIntConfig(String envKey, String propKey, int defaultValue) {
        String val = getEnvOrProperty(envKey);
        if (val == null || val.isBlank()) {
            val = PROPERTIES.getProperty(propKey);
        }
        if (val != null && !val.isBlank()) {
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException e) {
                LOGGER.warning("Giá trị cấu hình " + envKey + "/" + propKey + " không hợp lệ: " + val + ", sử dụng mặc định: " + defaultValue);
            }
        }
        return defaultValue;
    }

    private static long getLongConfig(String envKey, String propKey, long defaultValue) {
        String val = getEnvOrProperty(envKey);
        if (val == null || val.isBlank()) {
            val = PROPERTIES.getProperty(propKey);
        }
        if (val != null && !val.isBlank()) {
            try {
                return Long.parseLong(val.trim());
            } catch (NumberFormatException e) {
                LOGGER.warning("Giá trị cấu hình " + envKey + "/" + propKey + " không hợp lệ: " + val + ", sử dụng mặc định: " + defaultValue);
            }
        }
        return defaultValue;
    }

    private DatabaseConnection() {
        // Utility class
    }

    /**
     * Khởi tạo HikariDataSource (Lazy Initialization thread-safe).
     */
    public static HikariDataSource getDataSource() {
        if (dataSource == null) {
            synchronized (DatabaseConnection.class) {
                if (dataSource == null) {
                    dataSource = createDataSource();
                }
            }
        }
        return dataSource;
    }

    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driver);
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);

        int maxPoolSize = getIntConfig("DB_POOL_MAX_SIZE", "db.pool.max_size", 10);
        int minIdle = getIntConfig("DB_POOL_MIN_IDLE", "db.pool.min_idle", 2);
        long connectionTimeout = getLongConfig("DB_POOL_TIMEOUT", "db.pool.timeout", 10000L);
        long idleTimeout = getLongConfig("DB_POOL_IDLE_TIMEOUT", "db.pool.idle_timeout", 300000L);
        long maxLifetime = getLongConfig("DB_POOL_MAX_LIFETIME", "db.pool.max_lifetime", 1800000L);

        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(Math.min(minIdle, maxPoolSize));
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        // initializationFailTimeout = -1 ngăn pool ném ngoại lệ khi ứng dụng khởi động nếu DB tạm thời chưa sẵn sàng
        config.setInitializationFailTimeout(-1);
        config.setPoolName("QLKS-HikariCP-Pool");

        // Các thiết lập tối ưu hiệu năng cho MySQL JDBC
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        LOGGER.info(String.format("Khởi tạo HikariCP Pool [name=%s, maxPoolSize=%d, minIdle=%d, timeout=%dms]",
                config.getPoolName(), maxPoolSize, minIdle, connectionTimeout));

        HikariDataSource ds = new HikariDataSource(config);

        // Đăng ký JVM shutdown hook giải phóng kết nối an toàn khi tắt app
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(DatabaseConnection::closeDataSource, "HikariCP-Shutdown-Hook"));
        } catch (IllegalStateException e) {
            // JVM is already shutting down
        }

        return ds;
    }

    /**
     * Mượn một kết nối từ HikariCP Connection Pool.
     * Người gọi phải đóng kết nối bằng try-with-resources (khi đóng, kết nối sẽ được
     * trả về Pool thay vì đóng kết nối vật lý TCP).
     *
     * @return Connection đối tượng kết nối JDBC được quản lý bởi HikariCP
     * @throws DataAccessException nếu mượn kết nối thất bại
     */
    public static Connection getConnection() {
        try {
            return getDataSource().getConnection();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi lấy kết nối từ HikariCP Pool: " + e.getMessage(), e);
            throw new DataAccessException("Lỗi kết nối đến cơ sở dữ liệu MySQL: " + e.getMessage(), e);
        }
    }

    /**
     * Đóng Connection Pool giải phóng tài nguyên.
     */
    public static synchronized void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            LOGGER.info("Đang đóng HikariDataSource Connection Pool...");
            dataSource.close();
            dataSource = null;
        }
    }

    /**
     * Cho phép gán DataSource tùy chỉnh (chủ yếu phục vụ kiểm thử hoặc thay đổi runtime).
     */
    public static synchronized void setDataSource(HikariDataSource customDataSource) {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        dataSource = customDataSource;
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
