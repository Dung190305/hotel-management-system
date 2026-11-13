package com.qlks.controller;

import com.qlks.model.Phong;
import com.qlks.model.enums.TrangThaiPhong;
import com.qlks.service.PhongService;
import com.qlks.util.UIUtil;

import javax.swing.*;
import java.awt.Component;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Controller điều khiển màn hình Quản lý Phòng và Sơ đồ phòng.
 * Sử dụng SwingWorker để thực thi tác vụ nền chống đóng băng giao diện (UI Freeze).
 */
public class PhongController extends BaseController {

    private final PhongService phongService;

    public PhongController() {
        this.phongService = new PhongService();
    }

    public PhongController(PhongService phongService) {
        this.phongService = phongService;
    }

    public void taiDanhSachPhongAsync(Consumer<List<Phong>> onSuccess, Consumer<Throwable> onError) {
        SwingWorker<List<Phong>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Phong> doInBackground() {
                return phongService.layTatCaPhong();
            }

            @Override
            protected void done() {
                try {
                    List<Phong> list = get();
                    if (onSuccess != null) onSuccess.accept(list);
                } catch (Exception ex) {
                    if (onError != null) onError.accept(ex);
                }
            }
        };
        worker.execute();
    }

    public List<Phong> layTatCaPhong() {
        return phongService.layTatCaPhong();
    }

    public Optional<Phong> timTheoMa(String maPhong) {
        return phongService.timTheoMa(maPhong);
    }

    public void themPhong(Component parent, Phong phong, Runnable onSuccess) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                phongService.themPhong(phong);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    UIUtil.showSuccess(parent, "Thêm phòng mới thành công!");
                    if (onSuccess != null) onSuccess.run();
                } catch (Exception ex) {
                    handleException(parent, ex);
                }
            }
        };
        worker.execute();
    }

    public void capNhatPhong(Component parent, Phong phong, Runnable onSuccess) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                phongService.capNhatPhong(phong);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    UIUtil.showSuccess(parent, "Cập nhật phòng thành công!");
                    if (onSuccess != null) onSuccess.run();
                } catch (Exception ex) {
                    handleException(parent, ex);
                }
            }
        };
        worker.execute();
    }

    public TrangThaiPhong hoanTatDonPhong(String maPhong) {
        return phongService.hoanTatDonPhong(maPhong);
    }

    public TrangThaiPhong chuyenTrangThaiPhong(String maPhong, TrangThaiPhong trangThaiMoi) {
        return phongService.chuyenTrangThaiPhong(maPhong, trangThaiMoi);
    }

    public void themPhong(Phong phong) {
        phongService.themPhong(phong);
    }

    public void capNhatPhong(Phong phong) {
        phongService.capNhatPhong(phong);
    }

    public void doiTrangThai(Component parent, String maPhong, TrangThaiPhong trangThaiMoi, Runnable onSuccess) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                phongService.chuyenTrangThaiPhong(maPhong, trangThaiMoi);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    UIUtil.showSuccess(parent, "Đã cập nhật trạng thái phòng!");
                    if (onSuccess != null) onSuccess.run();
                } catch (Exception ex) {
                    handleException(parent, ex);
                }
            }
        };
        worker.execute();
    }

    public void batTatKinhDoanh(Component parent, String maPhong, boolean dangKinhDoanh, Runnable onSuccess) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                phongService.ngungKinhDoanhPhong(maPhong);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    UIUtil.showSuccess(parent, (dangKinhDoanh ? "Kích hoạt" : "Ngừng") + " kinh doanh phòng thành công!");
                    if (onSuccess != null) onSuccess.run();
                } catch (Exception ex) {
                    handleException(parent, ex);
                }
            }
        };
        worker.execute();
    }
}
