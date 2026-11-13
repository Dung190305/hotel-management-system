package com.qlks.controller;

import com.qlks.model.ChiTietDichVu;
import com.qlks.model.DatPhong;
import com.qlks.model.DichVu;
import com.qlks.model.HoaDon;
import com.qlks.model.Phong;
import com.qlks.model.dto.ThongTinPhuThu;
import com.qlks.service.DatPhongService;
import com.qlks.service.DichVuService;
import com.qlks.util.UIUtil;

import javax.swing.*;
import java.awt.Component;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Controller chuyên trách xử lý nghiệp vụ Lễ tân: Check-in, Check-out và gọi Dịch vụ phòng qua SwingWorker.
 */
public class CheckInCheckOutController extends BaseController {

    private final DatPhongService datPhongService;
    private final DichVuService dichVuService;

    public CheckInCheckOutController() {
        this.datPhongService = new DatPhongService();
        this.dichVuService = new DichVuService();
    }

    public CheckInCheckOutController(DatPhongService datPhongService, DichVuService dichVuService) {
        this.datPhongService = datPhongService;
        this.dichVuService = dichVuService;
    }

    public List<DatPhong> layTatCaDatPhong() {
        return datPhongService.layTatCaDatPhong();
    }

    public Optional<DatPhong> timTheoMa(String maDatPhong) {
        return datPhongService.timTheoMa(maDatPhong);
    }

    public List<DichVu> layDichVuDangKinhDoanh() {
        return dichVuService.layDichVuDangKinhDoanh();
    }

    public List<ChiTietDichVu> layDichVuTheoDatPhong(String maDatPhong) {
        return dichVuService.layDichVuTheoDatPhong(maDatPhong);
    }

    public void xacNhanCheckIn(String maDatPhong) {
        datPhongService.xacNhanCheckIn(maDatPhong);
    }

    public void xacNhanCheckIn(String maDatPhong, LocalDateTime thoiGianCheckIn) {
        datPhongService.xacNhanCheckIn(maDatPhong, thoiGianCheckIn);
    }

    public ThongTinPhuThu tinhPhuThuEarlyCheckIn(Phong phong, LocalDateTime thoiGianCheckIn, LocalDateTime ngayNhanDuKien) {
        return datPhongService.tinhPhuThuEarlyCheckIn(phong, thoiGianCheckIn, ngayNhanDuKien);
    }

    public ThongTinPhuThu tinhPhuThuLateCheckOut(Phong phong, LocalDateTime thoiGianCheckOut) {
        return datPhongService.tinhPhuThuLateCheckOut(phong, thoiGianCheckOut);
    }

    public HoaDon xacNhanCheckOut(String maDatPhong) {
        return datPhongService.xacNhanCheckOut(maDatPhong);
    }

    public void ghiNhanDichVu(String maDatPhong, String maDichVu, int soLuong) {
        dichVuService.ghiNhanDichVu(maDatPhong, maDichVu, soLuong);
    }

    public void xacNhanCheckInAsync(Component parent, String maDatPhong, Runnable onSuccess) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                datPhongService.xacNhanCheckIn(maDatPhong);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    UIUtil.showSuccess(parent, "Khách đã Check-in thành công!");
                    if (onSuccess != null) onSuccess.run();
                } catch (Exception ex) {
                    handleException(parent, ex);
                }
            }
        };
        worker.execute();
    }

    public void xacNhanCheckOutAsync(Component parent, String maDatPhong, Consumer<HoaDon> onSuccess) {
        SwingWorker<HoaDon, Void> worker = new SwingWorker<>() {
            @Override
            protected HoaDon doInBackground() {
                return datPhongService.xacNhanCheckOut(maDatPhong);
            }

            @Override
            protected void done() {
                try {
                    HoaDon hd = get();
                    if (onSuccess != null) onSuccess.accept(hd);
                } catch (Exception ex) {
                    handleException(parent, ex);
                }
            }
        };
        worker.execute();
    }
}

