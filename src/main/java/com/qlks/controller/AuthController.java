package com.qlks.controller;

import com.qlks.model.TaiKhoan;
import com.qlks.service.AuthService;

/**
 * Controller xử lý đăng nhập, phiên người dùng và đăng xuất.
 */
public class AuthController extends BaseController {

    private final AuthService authService;

    public AuthController() {
        this.authService = new AuthService();
    }

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public TaiKhoan dangNhap(String tenDangNhap, String matKhau) {
        return authService.dangNhap(tenDangNhap, matKhau);
    }

    public void doiMatKhau(String tenDangNhap, String matKhauCu, String matKhauMoi) {
        authService.doiMatKhau(tenDangNhap, matKhauCu, matKhauMoi, matKhauMoi);
    }

    public void doiMatKhau(String tenDangNhap, String matKhauCu, String matKhauMoi, String xacNhanMatKhauMoi) {
        authService.doiMatKhau(tenDangNhap, matKhauCu, matKhauMoi, xacNhanMatKhauMoi);
    }

    public void dangXuat() {
        authService.dangXuat();
    }

    public AuthService getAuthService() {
        return authService;
    }
}
