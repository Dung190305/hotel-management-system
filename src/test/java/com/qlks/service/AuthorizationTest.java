package com.qlks.service;

import com.qlks.exception.BusinessException;
import com.qlks.model.TaiKhoan;
import com.qlks.model.enums.Permission;
import com.qlks.model.enums.VaiTro;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử ma trận phân quyền RBAC tại tầng Service (AuthorizationTest)")
class AuthorizationTest {

    private final NhanVienService nhanVienService = new NhanVienService();
    private final PhongService phongService = new PhongService();
    private final DichVuService dichVuService = new DichVuService();

    @BeforeEach
    @AfterEach
    void resetSession() {
        AuthService.setTaiKhoanHienTai(null);
    }

    @Test
    @DisplayName("LE_TAN gọi themNhanVien phải bị chặn bằng BusinessException")
    void testLeTan_KhongCoQuyenQuanLyNhanVien() {
        TaiKhoan tkLeTan = new TaiKhoan("letan_test", "hash", VaiTro.LE_TAN, true);
        AuthService.setTaiKhoanHienTai(tkLeTan);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                nhanVienService.themNhanVien(null, "user_moi", "pass", VaiTro.LE_TAN)
        );
        assertTrue(ex.getMessage().contains("Bạn không có quyền") || ex.getMessage().contains("QUAN_LY_TAI_KHOAN"));
    }

    @Test
    @DisplayName("LE_TAN gọi themPhong (sửa danh mục) phải bị chặn bằng BusinessException")
    void testLeTan_KhongCoQuyenSuaDanhMucPhong() {
        TaiKhoan tkLeTan = new TaiKhoan("letan_test", "hash", VaiTro.LE_TAN, true);
        AuthService.setTaiKhoanHienTai(tkLeTan);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                phongService.themPhong(null)
        );
        assertTrue(ex.getMessage().contains("Bạn không có quyền") || ex.getMessage().contains("SUA_DANH_MUC"));
    }

    @Test
    @DisplayName("LE_TAN gọi themDichVu (sửa danh mục) phải bị chặn bằng BusinessException")
    void testLeTan_KhongCoQuyenSuaDanhMucDichVu() {
        TaiKhoan tkLeTan = new TaiKhoan("letan_test", "hash", VaiTro.LE_TAN, true);
        AuthService.setTaiKhoanHienTai(tkLeTan);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                dichVuService.themDichVu(null)
        );
        assertTrue(ex.getMessage().contains("Bạn không có quyền") || ex.getMessage().contains("SUA_DANH_MUC"));
    }

    @Test
    @DisplayName("MANAGER gọi themNhanVien phải bị chặn (chỉ ADMIN mới có quyền nhân sự)")
    void testManager_KhongCoQuyenQuanLyNhanVien() {
        TaiKhoan tkManager = new TaiKhoan("manager_test", "hash", VaiTro.MANAGER, true);
        AuthService.setTaiKhoanHienTai(tkManager);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                nhanVienService.themNhanVien(null, "user_moi", "pass", VaiTro.LE_TAN)
        );
        assertTrue(ex.getMessage().contains("Bạn không có quyền") || ex.getMessage().contains("QUAN_LY_TAI_KHOAN"));
    }

    @Test
    @DisplayName("Kiểm tra toàn diện ma trận quyền RBAC theo đề cương")
    void testMaTranPhanQuyenChiTiet() {
        // ADMIN: Có tất cả quyền
        for (Permission p : Permission.values()) {
            assertTrue(AuthorizationService.hasPermission(VaiTro.ADMIN, p), "ADMIN phải có quyền: " + p);
        }

        // MANAGER: Có quyền danh mục, khách hàng, đặt phòng, dịch vụ/thanh toán, báo cáo; KHÔNG có quản lý tài khoản/nhân viên
        assertFalse(AuthorizationService.hasPermission(VaiTro.MANAGER, Permission.QUAN_LY_TAI_KHOAN));
        assertTrue(AuthorizationService.hasPermission(VaiTro.MANAGER, Permission.SUA_DANH_MUC));
        assertTrue(AuthorizationService.hasPermission(VaiTro.MANAGER, Permission.QUAN_LY_KHACH_HANG));
        assertTrue(AuthorizationService.hasPermission(VaiTro.MANAGER, Permission.DAT_PHONG));
        assertTrue(AuthorizationService.hasPermission(VaiTro.MANAGER, Permission.GHI_NHAN_DICH_VU_THANH_TOAN));
        assertTrue(AuthorizationService.hasPermission(VaiTro.MANAGER, Permission.XEM_BAO_CAO));

        // LE_TAN: Chỉ có khách hàng, đặt phòng, dịch vụ/thanh toán; KHÔNG có sửa danh mục, nhân sự, báo cáo
        assertFalse(AuthorizationService.hasPermission(VaiTro.LE_TAN, Permission.QUAN_LY_TAI_KHOAN));
        assertFalse(AuthorizationService.hasPermission(VaiTro.LE_TAN, Permission.SUA_DANH_MUC));
        assertFalse(AuthorizationService.hasPermission(VaiTro.LE_TAN, Permission.XEM_BAO_CAO));
        assertTrue(AuthorizationService.hasPermission(VaiTro.LE_TAN, Permission.QUAN_LY_KHACH_HANG));
        assertTrue(AuthorizationService.hasPermission(VaiTro.LE_TAN, Permission.DAT_PHONG));
        assertTrue(AuthorizationService.hasPermission(VaiTro.LE_TAN, Permission.GHI_NHAN_DICH_VU_THANH_TOAN));
    }

    @Test
    @DisplayName("Chưa đăng nhập mà gọi require phải ném BusinessException")
    void testChuaDangNhap_BiChan() {
        AuthService.setTaiKhoanHienTai(null);
        BusinessException ex = assertThrows(BusinessException.class, () ->
                AuthorizationService.require(Permission.DAT_PHONG)
        );
        assertTrue(ex.getMessage().contains("đăng nhập"));
    }
}

