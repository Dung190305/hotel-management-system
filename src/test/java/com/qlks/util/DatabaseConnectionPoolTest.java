package com.qlks.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử HikariCP Connection Pool - Ngăn ngừa cạn kiệt Connection")
class DatabaseConnectionPoolTest {

    @AfterEach
    void tearDown() {
        // Reset connection pool sau mỗi test
        DatabaseConnection.closeDataSource();
    }

    @Test
    @DisplayName("Kiểm tra cấu hình mặc định của HikariCP Connection Pool")
    void testHikariConfigDefaults() {
        HikariDataSource ds = DatabaseConnection.getDataSource();
        assertNotNull(ds, "HikariDataSource phải được khởi tạo");
        assertEquals("QLKS-HikariCP-Pool", ds.getPoolName());
        assertEquals(10, ds.getMaximumPoolSize(), "Mặc định maxPoolSize phải là 10");
        assertTrue(ds.getMinimumIdle() <= 10, "minimumIdle không được vượt quá maxPoolSize");
        assertEquals(10000L, ds.getConnectionTimeout(), "connectionTimeout mặc định là 10000ms");
        assertEquals(300000L, ds.getIdleTimeout(), "idleTimeout mặc định là 300000ms (5 phút)");
        assertEquals(1800000L, ds.getMaxLifetime(), "maxLifetime mặc định là 1800000ms (30 phút)");
    }

    @Test
    @DisplayName("Kiểm tra singleton và hàm closeDataSource giải phóng tài nguyên an toàn")
    void testDataSourceSingletonAndClose() {
        HikariDataSource ds1 = DatabaseConnection.getDataSource();
        HikariDataSource ds2 = DatabaseConnection.getDataSource();
        assertSame(ds1, ds2, "DatabaseConnection.getDataSource() phải trả về cùng một singleton instance");

        assertFalse(ds1.isClosed(), "Pool phải đang hoạt động trước khi đóng");
        DatabaseConnection.closeDataSource();
        assertTrue(ds1.isClosed(), "Pool phải được đóng sau khi gọi closeDataSource()");

        // Khi gọi lại getDataSource(), một pool mới được tạo ra nếu cần
        HikariDataSource ds3 = DatabaseConnection.getDataSource();
        assertNotNull(ds3);
        assertNotSame(ds1, ds3, "Sau khi đóng, một pool mới được khởi tạo lại khi được yêu cầu");
    }

    @Test
    @DisplayName("Kiểm tra khả năng thiết lập DataSource tùy chỉnh (setDataSource)")
    void testCustomDataSource() {
        HikariConfig customConfig = new HikariConfig();
        customConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        customConfig.setJdbcUrl("jdbc:mysql://localhost:3306/hotel_db");
        customConfig.setUsername("root");
        customConfig.setPassword("test");
        customConfig.setMaximumPoolSize(5);
        customConfig.setMinimumIdle(1);
        customConfig.setInitializationFailTimeout(-1);
        customConfig.setPoolName("Custom-Test-Pool");

        HikariDataSource customDs = new HikariDataSource(customConfig);
        try {
            DatabaseConnection.setDataSource(customDs);
            assertSame(customDs, DatabaseConnection.getDataSource());
            assertEquals(5, DatabaseConnection.getDataSource().getMaximumPoolSize());
            assertEquals("Custom-Test-Pool", DatabaseConnection.getDataSource().getPoolName());
        } finally {
            DatabaseConnection.closeDataSource();
        }
    }
}

