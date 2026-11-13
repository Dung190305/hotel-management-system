package com.qlks.service;

import com.qlks.model.LeTan;
import com.qlks.model.NhanVien;
import com.qlks.model.QuanLy;
import com.qlks.model.TaiKhoan;
import com.qlks.model.enums.VaiTro;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử tính Thread-safe của AuthService Session giữa EDT và Background Workers")
class AuthSessionConcurrencyTest {

    @BeforeEach
    @AfterEach
    void cleanSession() {
        AuthService.dangXuat();
    }

    @Test
    @DisplayName("Kiểm tra 50 luồng đọc và ghi phiên làm việc đồng thời không bị race condition hay đọc dữ liệu rác")
    void testConcurrentSessionReadWrite() throws InterruptedException {
        int soLuongLuong = 50;
        ExecutorService executor = Executors.newFixedThreadPool(soLuongLuong);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(soLuongLuong);

        TaiKhoan tk1 = new TaiKhoan("admin", "hash1", VaiTro.ADMIN, true);
        NhanVien nv1 = new QuanLy("NV001", "Quản lý 1", "0901234567", "admin@qlks.com",
                "admin", LocalDate.now(), BigDecimal.TEN, true, 1.2, BigDecimal.ZERO);

        TaiKhoan tk2 = new TaiKhoan("letan1", "hash2", VaiTro.LE_TAN, true);
        NhanVien nv2 = new LeTan("NV002", "Lễ tân 1", "0901234568", "letan@qlks.com",
                "letan1", LocalDate.now(), BigDecimal.TEN, true, 10, BigDecimal.ZERO);

        AtomicInteger inconsistencyCount = new AtomicInteger(0);

        for (int i = 0; i < soLuongLuong; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 500; j++) {
                        if (index % 5 == 0) {
                            // Luồng ghi phiên
                            if (j % 2 == 0) {
                                AuthService.setSession(tk1, nv1);
                            } else {
                                AuthService.setSession(tk2, nv2);
                            }
                        } else if (index % 7 == 0) {
                            // Luồng đăng xuất
                            AuthService.dangXuat();
                        } else {
                            // Luồng đọc nguyên tử (như SwingWorker đọc Session để lưu vết kiểm toán audit)
                            AuthService.UserSession s = AuthService.getSession();
                            if (s != null && s.taiKhoan() != null && s.nhanVien() != null) {
                                if (!s.taiKhoan().getTenDangNhap().equals(s.nhanVien().getTenDangNhap())) {
                                    inconsistencyCount.incrementAndGet();
                                }
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = doneLatch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Tất cả các luồng phải hoàn thành trong 5 giây");
        assertEquals(0, inconsistencyCount.get(), 
                "Không được xảy ra tình trạng phiên không nhất quán (TaiKhoan thuộc người này nhưng NhanVien thuộc người khác)");
    }

    @Test
    @DisplayName("Kiểm tra setSession và dangXuat phản ánh tức thì trên biến volatile")
    void testSessionVisibility() {
        assertNull(AuthService.getTaiKhoanHienTai());
        assertNull(AuthService.getNhanVienHienTai());
        assertFalse(AuthService.isAdmin());

        TaiKhoan tk = new TaiKhoan("admin", "hash", VaiTro.ADMIN, true);
        NhanVien nv = new QuanLy("NV001", "Admin", "0901111111", "admin@qlks.com",
                "admin", LocalDate.now(), BigDecimal.TEN, true, 1.0, BigDecimal.ZERO);

        AuthService.setSession(tk, nv);
        assertSame(tk, AuthService.getTaiKhoanHienTai());
        assertSame(nv, AuthService.getNhanVienHienTai());
        assertTrue(AuthService.isAdmin());

        AuthService.dangXuat();
        assertNull(AuthService.getTaiKhoanHienTai());
        assertNull(AuthService.getNhanVienHienTai());
        assertFalse(AuthService.isAdmin());
    }
}
