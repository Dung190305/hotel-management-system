package com.qlks.controller;

import com.qlks.model.ChiTietDichVu;
import com.qlks.model.DichVu;
import com.qlks.service.DichVuService;
import com.qlks.util.UIUtil;

import javax.swing.*;
import java.awt.Component;
import java.util.List;
import java.util.Optional;

/**
 * Controller quản lý danh mục dịch vụ và ghi nhận dịch vụ theo phiếu đặt phòng.
 */
public class DichVuController extends BaseController {

    private final DichVuService dichVuService;

    public DichVuController() {
        this.dichVuService = new DichVuService();
    }

    public DichVuController(DichVuService dichVuService) {
        this.dichVuService = dichVuService;
    }

    public List<DichVu> layTatCaDichVu() {
        return dichVuService.layTatCaDichVu();
    }

    public List<DichVu> layDichVuDangKinhDoanh() {
        return dichVuService.layDichVuDangKinhDoanh();
    }

    public Optional<DichVu> timTheoMa(String maDichVu) {
        return dichVuService.timTheoMa(maDichVu);
    }

    public List<ChiTietDichVu> layDichVuTheoDatPhong(String maDatPhong) {
        return dichVuService.layDichVuTheoDatPhong(maDatPhong);
    }

    public void ghiNhanDichVu(String maDatPhong, String maDichVu, int soLuong) {
        dichVuService.ghiNhanDichVu(maDatPhong, maDichVu, soLuong);
    }

    public void themDichVu(DichVu dv) {
        dichVuService.themDichVu(dv);
    }

    public void capNhatDichVu(DichVu dv) {
        dichVuService.capNhatDichVu(dv);
    }

    public void ngungKinhDoanh(String maDichVu) {
        dichVuService.ngungKinhDoanh(maDichVu);
    }

    public void themDichVu(Component parent, DichVu dv, Runnable onSuccess) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                dichVuService.themDichVu(dv);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    UIUtil.showSuccess(parent, "Thêm dịch vụ thành công!");
                    if (onSuccess != null) onSuccess.run();
                } catch (Exception ex) {
                    handleException(parent, ex);
                }
            }
        };
        worker.execute();
    }

    public void capNhatDichVu(Component parent, DichVu dv, Runnable onSuccess) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                dichVuService.capNhatDichVu(dv);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    UIUtil.showSuccess(parent, "Cập nhật dịch vụ thành công!");
                    if (onSuccess != null) onSuccess.run();
                } catch (Exception ex) {
                    handleException(parent, ex);
                }
            }
        };
        worker.execute();
    }

    public void ngungKinhDoanh(Component parent, String maDichVu, Runnable onSuccess) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                dichVuService.ngungKinhDoanh(maDichVu);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    UIUtil.showSuccess(parent, "Đã ngừng kinh doanh dịch vụ!");
                    if (onSuccess != null) onSuccess.run();
                } catch (Exception ex) {
                    handleException(parent, ex);
                }
            }
        };
        worker.execute();
    }
}
