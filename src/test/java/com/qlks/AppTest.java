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
        String adminHash = org.mindrot.jbcrypt.BCrypt.hashpw("admin123", org.mindrot.jbcrypt.BCrypt.gensalt(10));
        String letanHash = org.mindrot.jbcrypt.BCrypt.hashpw("letan123", org.mindrot.jbcrypt.BCrypt.gensalt(10));
        System.out.println("ADMIN_HASH=" + adminHash);
        System.out.println("LETAN_HASH=" + letanHash);
        assertTrue(org.mindrot.jbcrypt.BCrypt.checkpw("admin123", adminHash));
        assertTrue(org.mindrot.jbcrypt.BCrypt.checkpw("letan123", letanHash));
    }
}
