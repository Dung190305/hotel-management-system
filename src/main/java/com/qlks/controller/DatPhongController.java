package com.qlks.controller;

import com.qlks.model.DatPhong;
import com.qlks.model.HoaDon;
import com.qlks.model.KhachHang;
import com.qlks.model.NhanVien;
import com.qlks.model.Phong;
import com.qlks.model.dto.KetQuaHuyDatPhong;
import com.qlks.model.dto.ThongTinPhuThu;
import com.qlks.model.enums.PhuongThucThanhToan;
import com.qlks.service.AuthService;
import com.qlks.service.DatPhongService;
import com.qlks.service.KhachHangService;
import com.qlks.service.PhongService;
import com.qlks.util.FormatUtil;
import com.qlks.util.UIUtil;

import javax.swing.*;
import java.awt.Component;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Controller xử lý các tác vụ Đặt phòng, tìm phòng trống, hủy đặt phòng qua SwingWorker.
 */
public class DatPhongController extends BaseController {

    private final DatPhongService datPhongService;
    private final KhachHangService khachHangService;
    private final PhongService phongService;

    public DatPhongController() {
        this.datPhongService = new DatPhongService();
        this.khachHangService = new KhachHangService();
        this.phongService = new PhongService();
    }

    public DatPhongController(DatPhongService datPhongService, KhachHangService khachHangService, PhongService phongService) {
        this.datPhongService = datPhongService;
        this.khachHangService = khachHangService;
        this.phongService = phongService;
    }

    public List<DatPhong> layTatCaDatPhong() {
        return datPhongService.layTatCaDatPhong();
    }

    public Optional<DatPhong> timTheoMa(String maDatPhong) {
        return datPhongService.timTheoMa(maDatPhong);
    }

    public List<KhachHang> layTatCaKhachHang() {
        return khachHangService.layTatCaKhachHang();
    }

    public List<Phong> timPhongTrong(LocalDateTime checkIn, LocalDateTime checkOut) {
        return phongService.timPhongTrong(checkIn, checkOut);
    }

    public NhanVien getNhanVienHienTai() {
        return AuthService.getNhanVienHienTai();
    }

    public void taoDatPhong(Component parent, DatPhong dp, Runnable onSuccess) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                datPhongService.taoDatPhong(dp);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    UIUtil.showSuccess(parent, "Đặt phòng thành công cho khách hàng " + dp.getKhachHang().getHoTen() + "!");
                    if (onSuccess != null) onSuccess.run();
                } catch (Exception ex) {
                    handleException(parent, ex);
                }
            }
        };
        worker.execute();
    }

    public void taoDatPhong(DatPhong dp) {
        datPhongService.taoDatPhong(dp);
    }

    public void xacNhanCheckIn(Component parent, String maDatPhong, Runnable onSuccess) {
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
                    UIUtil.showSuccess(parent, "Check-in thành công!");
                    if (onSuccess != null) onSuccess.run();
                } catch (Exception ex) {
                    handleException(parent, ex);
                }
            }
        };
        worker.execute();
    }

    public void xacNhanCheckIn(String maDatPhong) {
        datPhongService.xacNhanCheckIn(maDatPhong);
    }

    public void xacNhanCheckIn(String maDatPhong, LocalDateTime thoiGianCheckIn) {
        datPhongService.xacNhanCheckIn(maDatPhong, thoiGianCheckIn);
    }

    public HoaDon xacNhanCheckOut(String maDatPhong) {
        return datPhongService.xacNhanCheckOut(maDatPhong);
    }

    public HoaDon xacNhanCheckOut(String maDatPhong, LocalDateTime thoiGianCheckOut) {
        return datPhongService.xacNhanCheckOut(maDatPhong, thoiGianCheckOut);
    }

    public ThongTinPhuThu tinhPhuThuEarlyCheckIn(Phong phong, LocalDateTime thoiGianCheckIn, LocalDateTime ngayNhanDuKien) {
        return datPhongService.tinhPhuThuEarlyCheckIn(phong, thoiGianCheckIn, ngayNhanDuKien);
    }

    public ThongTinPhuThu tinhPhuThuLateCheckOut(Phong phong, LocalDateTime thoiGianCheckOut) {
        return datPhongService.tinhPhuThuLateCheckOut(phong, thoiGianCheckOut);
    }

    public BigDecimal tinhTienCocToiThieu(Phong phong, LocalDateTime ngayNhan, LocalDateTime ngayTra) {
        return datPhongService.tinhTienCocToiThieu(phong, ngayNhan, ngayTra);
    }

    public void huyDatPhong(Component parent, String maDatPhong, Runnable onSuccess) {
        huyDatPhongAsync(parent, maDatPhong, "Khách yêu cầu hủy phòng", PhuongThucThanhToan.TIEN_MAT, res -> {
            if (onSuccess != null) onSuccess.run();
        });
    }

    public void huyDatPhongAsync(Component parent, String maDatPhong, String lyDoHuy,
                                PhuongThucThanhToan phuongThucHoan, Consumer<KetQuaHuyDatPhong> onSuccess) {
        SwingWorker<KetQuaHuyDatPhong, Void> worker = new SwingWorker<>() {
            @Override
            protected KetQuaHuyDatPhong doInBackground() {
                return datPhongService.huyDatPhong(maDatPhong, lyDoHuy, phuongThucHoan);
            }

            @Override
            protected void done() {
                try {
                    KetQuaHuyDatPhong res = get();
                    String msg;
                    if (res.getTienCocBanDau().compareTo(BigDecimal.ZERO) > 0) {
                        msg = String.format(
                                "Hủy đơn đặt phòng %s thành công!\n" +
                                "══════════════ QUYẾT TOÁN CỌC HỦY PHÒNG ══════════════\n" +
                                "• Tiền cọc ban đầu:        %s VNĐ\n" +
                                "• Thời gian còn lại:       %d giờ trước check-in\n" +
                                "• Tỷ lệ hoàn cọc:          %s%%\n" +
                                "------------------------------------------------------\n" +
                                "• SỐ TIỀN HOÀN TRẢ KHÁCH:  %s VNĐ (%s)\n" +
                                "• PHÍ PHẠT KHÁCH SẠN THU:  %s VNĐ\n" +
                                "• Mã hóa đơn thanh lý:     %s",
                                res.getMaDatPhong(),
                                FormatUtil.formatTien(res.getTienCocBanDau()),
                                res.getSoGioConLai(),
                                res.getTyLeHoan().multiply(BigDecimal.valueOf(100)).intValue(),
                                FormatUtil.formatTien(res.getTienHoanLai()),
                                phuongThucHoan != null ? phuongThucHoan.getTenHienThi() : "Tiền mặt",
                                FormatUtil.formatTien(res.getPhiPhatHuy()),
                                res.getMaHoaDonQuyetToan() != null ? res.getMaHoaDonQuyetToan() : "N/A"
                        );
                    } else {
                        msg = "Đã hủy đơn đặt phòng " + maDatPhong + " thành công (Đơn không có tiền cọc).";
                    }
                    UIUtil.showSuccess(parent, msg);
                    if (onSuccess != null) onSuccess.accept(res);
                } catch (Exception ex) {
                    handleException(parent, ex);
                }
            }
        };
        worker.execute();
    }

    public KetQuaHuyDatPhong huyDatPhong(String maDatPhong) {
        return datPhongService.huyDatPhong(maDatPhong);
    }

    public KetQuaHuyDatPhong huyDatPhong(String maDatPhong, String lyDoHuy, PhuongThucThanhToan phuongThucHoan) {
        return datPhongService.huyDatPhong(maDatPhong, lyDoHuy, phuongThucHoan);
    }
}

