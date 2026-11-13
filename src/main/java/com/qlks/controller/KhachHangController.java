package com.qlks.controller;

import com.qlks.model.DatPhong;
import com.qlks.model.HoaDon;
import com.qlks.model.KhachHang;
import com.qlks.service.KhachHangService;
import com.qlks.util.UIUtil;

import javax.swing.*;
import java.awt.Component;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Controller xử lý các tác vụ quản lý khách hàng và tra cứu lịch sử qua SwingWorker.
 */
public class KhachHangController extends BaseController {

    private final KhachHangService khachHangService;

    public KhachHangController() {
        this.khachHangService = new KhachHangService();
    }

    public KhachHangController(KhachHangService khachHangService) {
        this.khachHangService = khachHangService;
    }

    public List<KhachHang> layTatCaKhachHang() {
        return khachHangService.layTatCaKhachHang();
    }

    public List<KhachHang> timKiem(String keyword) {
        return khachHangService.timKiem(keyword);
    }

    public Optional<KhachHang> timTheoMa(String maKhachHang) {
        return khachHangService.timTheoMa(maKhachHang);
    }

    public Optional<KhachHang> timTheoCCCD(String cccd) {
        return khachHangService.timTheoCCCD(cccd);
    }

    public List<DatPhong> layLichSuDatPhong(String maKhachHang) {
        return khachHangService.layLichSuDatPhong(maKhachHang);
    }

    public List<HoaDon> layLichSuHoaDon(String maKhachHang) {
        return khachHangService.layLichSuHoaDon(maKhachHang);
    }

    public void themKhachHang(KhachHang kh) {
        khachHangService.themKhachHang(kh);
    }

    public void capNhatKhachHang(KhachHang kh) {
        khachHangService.capNhatKhachHang(kh);
    }

    public void themKhachHang(Component parent, KhachHang kh, Runnable onSuccess) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                khachHangService.themKhachHang(kh);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    UIUtil.showSuccess(parent, "Thêm khách hàng thành công!");
                    if (onSuccess != null) onSuccess.run();
                } catch (Exception ex) {
                    handleException(parent, ex);
                }
            }
        };
        worker.execute();
    }

    public void capNhatKhachHang(Component parent, KhachHang kh, Runnable onSuccess) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                khachHangService.capNhatKhachHang(kh);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    UIUtil.showSuccess(parent, "Cập nhật thông tin khách hàng thành công!");
                    if (onSuccess != null) onSuccess.run();
                } catch (Exception ex) {
                    handleException(parent, ex);
                }
            }
        };
        worker.execute();
    }
}
