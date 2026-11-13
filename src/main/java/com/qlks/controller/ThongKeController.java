package com.qlks.controller;

import com.qlks.service.ThongKeService;

import javax.swing.*;
import java.awt.Component;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Controller xử lý tổng hợp số liệu thống kê doanh thu và báo cáo qua SwingWorker.
 */
public class ThongKeController extends BaseController {

    private final ThongKeService thongKeService;

    public ThongKeController() {
        this.thongKeService = new ThongKeService();
    }

    public ThongKeController(ThongKeService thongKeService) {
        this.thongKeService = thongKeService;
    }

    public Map<String, Object> layTongQuan() {
        return thongKeService.layTongQuan();
    }

    public List<Map<String, Object>> thongKeDoanhThuTheoNgay(LocalDate tuNgay, LocalDate denNgay) {
        return thongKeService.thongKeDoanhThuTheoNgay(tuNgay, denNgay);
    }

    public void layTongQuanAsync(Component parent, Consumer<Map<String, Object>> onSuccess) {
        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() {
                return thongKeService.layTongQuan();
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> data = get();
                    if (onSuccess != null) onSuccess.accept(data);
                } catch (Exception ex) {
                    handleException(parent, ex);
                }
            }
        };
        worker.execute();
    }

    public void thongKeDoanhThuTheoNgayAsync(Component parent, LocalDate tuNgay, LocalDate denNgay, Consumer<List<Map<String, Object>>> onSuccess) {
        SwingWorker<List<Map<String, Object>>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Map<String, Object>> doInBackground() {
                return thongKeService.thongKeDoanhThuTheoNgay(tuNgay, denNgay);
            }

            @Override
            protected void done() {
                try {
                    List<Map<String, Object>> data = get();
                    if (onSuccess != null) onSuccess.accept(data);
                } catch (Exception ex) {
                    handleException(parent, ex);
                }
            }
        };
        worker.execute();
    }
}

