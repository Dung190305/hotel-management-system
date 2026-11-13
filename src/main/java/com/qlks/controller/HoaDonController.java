package com.qlks.controller;

import com.qlks.model.HoaDon;
import com.qlks.model.ThanhToan;
import com.qlks.model.enums.PhuongThucThanhToan;
import com.qlks.service.HoaDonService;
import com.qlks.util.UIUtil;

import javax.swing.*;
import java.awt.Component;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Controller xử lý Quản lý Hóa đơn và Thanh toán an toàn chống thanh toán trùng lặp qua SwingWorker.
 */
public class HoaDonController extends BaseController {

    private final HoaDonService hoaDonService;

    public HoaDonController() {
        this.hoaDonService = new HoaDonService();
    }

    public HoaDonController(HoaDonService hoaDonService) {
        this.hoaDonService = hoaDonService;
    }

    public List<HoaDon> layTatCaHoaDon() {
        return hoaDonService.layTatCaHoaDon();
    }

    public Optional<HoaDon> timTheoMa(String maHoaDon) {
        return hoaDonService.timTheoMa(maHoaDon);
    }

    public BigDecimal tinhSoTienConLaiCanThu(String maHoaDon) {
        return hoaDonService.tinhSoTienConLaiCanThu(maHoaDon);
    }

    public BigDecimal tinhTongTienDaThanhToan(String maHoaDon) {
        return hoaDonService.tinhTongTienDaThanhToan(maHoaDon);
    }

    public List<ThanhToan> layLichSuThanhToan(String maHoaDon) {
        return hoaDonService.layLichSuThanhToan(maHoaDon);
    }

    public void thanhToanHoaDon(String maHoaDon, BigDecimal soTienThanhToan, PhuongThucThanhToan phuongThuc, String ghiChu) {
        hoaDonService.thanhToanHoaDon(maHoaDon, soTienThanhToan, phuongThuc, ghiChu);
    }

    public void thanhToanHoaDonAsync(Component parent, String maHoaDon, BigDecimal soTienThanhToan,
                                    PhuongThucThanhToan phuongThuc, String ghiChu, Runnable onSuccess) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                hoaDonService.thanhToanHoaDon(maHoaDon, soTienThanhToan, phuongThuc, ghiChu);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    UIUtil.showSuccess(parent, "Thanh toán thành công hóa đơn " + maHoaDon + "!");
                    if (onSuccess != null) onSuccess.run();
                } catch (Exception ex) {
                    handleException(parent, ex);
                }
            }
        };
        worker.execute();
    }
}

