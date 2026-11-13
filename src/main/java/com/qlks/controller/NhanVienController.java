package com.qlks.controller;

import com.qlks.model.NhanVien;
import com.qlks.model.enums.VaiTro;
import com.qlks.service.NhanVienService;
import com.qlks.util.UIUtil;

import javax.swing.*;
import java.awt.Component;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller quản lý nhân viên và tính bảng lương.
 */
public class NhanVienController extends BaseController {

    private final NhanVienService nhanVienService;

    public NhanVienController() {
        this.nhanVienService = new NhanVienService();
    }

    public NhanVienController(NhanVienService nhanVienService) {
        this.nhanVienService = nhanVienService;
    }

    public List<NhanVien> layTatCaNhanVien() {
        return nhanVienService.layTatCaNhanVien();
    }

    public Optional<NhanVien> timTheoMa(String maNhanVien) {
        return nhanVienService.timTheoMa(maNhanVien);
    }

    public Map<String, Object> tinhBangLuong() {
        return nhanVienService.tinhBangLuong();
    }

    public void themNhanVien(NhanVien nv, String tenDangNhap, String matKhau, VaiTro vaiTro) {
        nhanVienService.themNhanVien(nv, tenDangNhap, matKhau, vaiTro);
    }

    public void themNhanVien(Component parent, NhanVien nv, String tenDangNhap, String matKhau, VaiTro vaiTro, Runnable onSuccess) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                nhanVienService.themNhanVien(nv, tenDangNhap, matKhau, vaiTro);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    UIUtil.showSuccess(parent, "Thêm nhân viên và tạo tài khoản thành công!");
                    if (onSuccess != null) onSuccess.run();
                } catch (Exception ex) {
                    handleException(parent, ex);
                }
            }
        };
        worker.execute();
    }
}
