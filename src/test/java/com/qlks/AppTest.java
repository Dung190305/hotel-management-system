package com.qlks;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử khởi động hệ thống với JUnit 5.
 */
public class AppTest {

    @Test
    @DisplayName("Kiểm tra môi trường JUnit 5 hoạt động chính xác")
    void testAppEnvironment() {
        assertTrue(true, "Môi trường kiểm thử JUnit 5 đã sẵn sàng.");
    }
}
