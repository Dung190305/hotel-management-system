package com.qlks.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử tiện ích sinh mã định danh tăng dần an toàn đa luồng (IdGenerator)")
class IdGeneratorTest {

    @BeforeEach
    void setUp() {
        IdGenerator.resetInMemory();
        IdGenerator.setForceInMemory(true);
    }

    @Test
    @DisplayName("Kiểm tra 50 luồng đồng thời gọi next('HD') sinh ra 50 mã duy nhất, không trùng lặp")
    void testSinhMaDaLuongKhongTrung() throws InterruptedException {
        int soLuongLuong = 50;
        Set<String> danhSachMa = Collections.synchronizedSet(new HashSet<>());
        ExecutorService executor = Executors.newFixedThreadPool(soLuongLuong);
        CountDownLatch readyLatch = new CountDownLatch(soLuongLuong);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(soLuongLuong);

        for (int i = 0; i < soLuongLuong; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Đợi tất cả 50 luồng sẵn sàng rồi xuất phát đồng thời
                    String ma = IdGenerator.next("HD");
                    danhSachMa.add(ma);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Bắn pháo hiệu chạy đồng thời
        boolean xong = doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(xong, "Tất cả các luồng phải hoàn tất trong thời gian quy định");
        assertEquals(soLuongLuong, danhSachMa.size(), 
                "Tất cả 50 mã sinh ra phải hoàn toàn khác nhau (không bị trùng khóa chính)");
        
        // Kiểm tra định dạng mã
        for (String ma : danhSachMa) {
            assertTrue(ma.startsWith("HD"), "Mã phải bắt đầu bằng HD: " + ma);
            assertEquals(8, ma.length(), "Định dạng mã chuẩn {PREFIX}{6 số}: " + ma);
        }
    }

    @Test
    @DisplayName("Kiểm tra tiền tố phân biệt rõ giữa các loại thực thể")
    void testTienToDocLap() {
        String maDp = IdGenerator.next("DP");
        String maHd = IdGenerator.next("HD");
        String maTt = IdGenerator.next("TT");

        assertTrue(maDp.startsWith("DP"));
        assertTrue(maHd.startsWith("HD"));
        assertTrue(maTt.startsWith("TT"));
    }

    @Test
    @DisplayName("Kiểm tra tiền tố mới luôn bắt đầu bằng 000001, không bao giờ bằng 0")
    void testNewPrefixStartsAtOne() {
        String maMoi = IdGenerator.next("NEWPREFIX");
        assertEquals("NEWPREFIX000001", maMoi, "Mã mới phải bắt đầu bằng 1 (000001)");
    }

    @Test
    @DisplayName("Kiểm tra next(Connection, prefix) trong Transaction không gây lỗi và sinh mã hợp lệ")
    void testNextWithConnectionInTransaction() throws SQLException {
        // Tạo một mock connection đang trong transaction (autoCommit = false)
        Connection mockConn = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("isClosed".equals(method.getName())) return false;
                    if ("getAutoCommit".equals(method.getName())) return false; // Đang trong transaction!
                    return null;
                }
        );

        String maHd1 = IdGenerator.next(mockConn, "HD");
        String maHd2 = IdGenerator.next(mockConn, "HD");

        assertNotNull(maHd1);
        assertNotNull(maHd2);
        assertNotEquals(maHd1, maHd2, "Hai mã sinh ra liên tiếp phải khác nhau");
        assertTrue(maHd1.startsWith("HD"));
        assertTrue(maHd2.startsWith("HD"));
    }
}
