package com.qlks.service;

import com.qlks.exception.BusinessException;
import com.qlks.model.TaiKhoan;
import com.qlks.model.enums.Permission;
import com.qlks.model.enums.VaiTro;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Service kiểm soát phân quyền tập trung (Authorization Service).
 * Kiểm tra quyền hạn trực tiếp tại tầng Service trước khi thực thi nghiệp vụ,
 * đảm bảo người dùng không thể can thiệp dữ liệu trái phép dù gọi từ View hay API.
 */
public final class AuthorizationService {

    private static final Map<VaiTro, Set<Permission>> ROLE_PERMISSIONS = Map.of(
            VaiTro.ADMIN, EnumSet.allOf(Permission.class),

            VaiTro.MANAGER, EnumSet.of(
                    Permission.SUA_DANH_MUC,
                    Permission.QUAN_LY_KHACH_HANG,
                    Permission.DAT_PHONG,
                    Permission.GHI_NHAN_DICH_VU_THANH_TOAN,
                    Permission.XEM_BAO_CAO
            ),

            VaiTro.LE_TAN, EnumSet.of(
                    Permission.QUAN_LY_KHACH_HANG,
                    Permission.DAT_PHONG,
                    Permission.GHI_NHAN_DICH_VU_THANH_TOAN
            )
    );

    private AuthorizationService() {
        // Utility / Service
    }

    /**
     * Kiểm tra vai trò có sở hữu quyền chỉ định hay không.
     *
     * @param vaiTro     Vai trò người dùng
     * @param permission Quyền hạn cần kiểm tra
     * @return true nếu có quyền, ngược lại false
     */
    public static boolean hasPermission(VaiTro vaiTro, Permission permission) {
        if (vaiTro == null || permission == null) {
            return false;
        }
        Set<Permission> perms = ROLE_PERMISSIONS.get(vaiTro);
        return perms != null && perms.contains(permission);
    }

    /**
     * Kiểm tra tài khoản đang đăng nhập hiện tại có sở hữu quyền hay không.
     *
     * @param permission Quyền hạn cần kiểm tra
     * @return true nếu có quyền, ngược lại false
     */
    public static boolean hasPermission(Permission permission) {
        TaiKhoan tk = AuthService.getTaiKhoanHienTai();
        if (tk == null || tk.getVaiTro() == null) {
            return false;
        }
        return hasPermission(tk.getVaiTro(), permission);
    }

    /**
     * Yêu cầu bắt buộc phải có quyền tương ứng.
     * Ném BusinessException nếu người dùng chưa đăng nhập hoặc không đủ thẩm quyền.
     *
     * @param permission Quyền hạn yêu cầu
     * @throws BusinessException khi từ chối quyền
     */
    public static void require(Permission permission) {
        TaiKhoan tk = AuthService.getTaiKhoanHienTai();
        if (tk == null) {
            // Cho phép bypass khi chạy môi trường test không có session đăng nhập nếu cần,
            // hoặc ném lỗi nếu nghiêm ngặt. Để đảm bảo test nghiệp vụ độc lập, ném lỗi rõ ràng:
            throw new BusinessException("Yêu cầu đăng nhập để thực hiện thao tác này.");
        }
        if (!hasPermission(tk.getVaiTro(), permission)) {
            throw new BusinessException(String.format(
                    "Bạn không có quyền thực hiện thao tác này! Quyền yêu cầu: %s (%s). Vai trò hiện tại: %s.",
                    permission.name(), permission.getMoTa(), tk.getVaiTro().getTenHienThi()
            ));
        }
    }
}

