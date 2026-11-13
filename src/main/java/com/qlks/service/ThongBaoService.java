package com.qlks.service;

import com.qlks.dao.DatPhongDAO;
import com.qlks.dao.HoaDonDAO;
import com.qlks.dao.impl.DatPhongDAOImpl;
import com.qlks.dao.impl.HoaDonDAOImpl;
import com.qlks.model.DatPhong;
import com.qlks.model.HoaDon;
import com.qlks.model.ThongBaoDon;
import com.qlks.model.ThongBaoDon.LoaiThongBao;
import com.qlks.model.ThongBaoDon.MucDo;
import com.qlks.model.enums.TrangThaiDatPhong;
import com.qlks.model.enums.TrangThaiThanhToan;
import com.qlks.util.FormatUtil;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service xử lý logic phát hiện và tổng hợp các thông báo đơn tới hạn:
 * 1. Đơn đặt phòng tới hạn nhận phòng hôm nay hoặc quá hạn nhận phòng (chưa Check-in).
 * 2. Đơn đang ở tới hạn trả phòng hôm nay hoặc quá hạn trả phòng (chưa Check-out).
 * 3. Hóa đơn chưa thanh toán.
 */
public class ThongBaoService {

    public enum TrangThaiChuong {
        KHONG_CO_THONG_BAO, // Không có thông báo (không có dấu chấm than)
        DA_DOC_HET,         // Tất cả thông báo đã đọc (dấu chấm than màu xanh)
        CO_CHUA_DOC         // Có thông báo chưa đọc (dấu chấm than màu đỏ)
    }

    private final DatPhongDAO datPhongDAO;
    private final HoaDonDAO hoaDonDAO;
    private final Set<String> danhSachIdDaDoc = Collections.synchronizedSet(new HashSet<>());

    public ThongBaoService() {
        this.datPhongDAO = new DatPhongDAOImpl();
        this.hoaDonDAO = new HoaDonDAOImpl();
    }

    public ThongBaoService(DatPhongDAO datPhongDAO, HoaDonDAO hoaDonDAO) {
        this.datPhongDAO = datPhongDAO;
        this.hoaDonDAO = hoaDonDAO;
    }

    /**
     * Quét và lấy toàn bộ danh sách thông báo đơn tới hạn tại thời điểm hiện tại.
     */
    public List<ThongBaoDon> layDanhSachThongBaoToiHan() {
        return layDanhSachThongBaoToiHan(LocalDateTime.now());
    }

    /**
     * Quét thông báo với mốc thời gian tùy chọn (hỗ trợ kiểm thử).
     */
    public List<ThongBaoDon> layDanhSachThongBaoToiHan(LocalDateTime thoiDiemKiemTra) {
        List<ThongBaoDon> dsThongBao = new ArrayList<>();
        LocalDate ngayKiemTra = thoiDiemKiemTra.toLocalDate();

        // 1. Quét danh sách Đặt phòng
        List<DatPhong> dsDatPhong = datPhongDAO.findAll();
        for (DatPhong dp : dsDatPhong) {
            if (dp.getTrangThai() == TrangThaiDatPhong.DA_XAC_NHAN || dp.getTrangThai() == TrangThaiDatPhong.CHO_XAC_NHAN) {
                // Kiểm tra tới hạn / quá hạn nhận phòng (Check-in)
                LocalDateTime ngayNhan = dp.getNgayNhanPhong();
                if (ngayNhan != null) {
                    if (ngayNhan.isBefore(thoiDiemKiemTra)) {
                        // Quá hạn nhận phòng
                        String treMoTa = tinhMoTaTre(ngayNhan, thoiDiemKiemTra);
                        dsThongBao.add(new ThongBaoDon(
                                dp.getMaDatPhong(),
                                LoaiThongBao.QUA_HAN_CHECK_IN,
                                MucDo.NGUY_CAP,
                                dp.getKhachHang() != null ? dp.getKhachHang().getHoTen() : "N/A",
                                dp.getKhachHang() != null ? dp.getKhachHang().getSoDienThoai() : "",
                                dp.getPhong() != null ? dp.getPhong().getSoPhong() : "N/A",
                                ngayNhan,
                                "Quá hạn nhận phòng: " + treMoTa + " (chưa check-in)"
                        ));
                    } else if (ngayNhan.toLocalDate().isEqual(ngayKiemTra)) {
                        // Tới hạn nhận phòng hôm nay
                        dsThongBao.add(new ThongBaoDon(
                                dp.getMaDatPhong(),
                                LoaiThongBao.CHECK_IN_HOM_NAY,
                                MucDo.CANH_BAO,
                                dp.getKhachHang() != null ? dp.getKhachHang().getHoTen() : "N/A",
                                dp.getKhachHang() != null ? dp.getKhachHang().getSoDienThoai() : "",
                                dp.getPhong() != null ? dp.getPhong().getSoPhong() : "N/A",
                                ngayNhan,
                                "Đến hạn nhận phòng hôm nay lúc " + FormatUtil.formatNgayGio(ngayNhan)
                        ));
                    }
                }
            } else if (dp.getTrangThai() == TrangThaiDatPhong.DANG_O) {
                // Kiểm tra tới hạn / quá hạn trả phòng (Check-out)
                LocalDateTime ngayTra = dp.getNgayTraPhong();
                if (ngayTra != null) {
                    if (ngayTra.isBefore(thoiDiemKiemTra)) {
                        // Quá hạn trả phòng
                        String treMoTa = tinhMoTaTre(ngayTra, thoiDiemKiemTra);
                        dsThongBao.add(new ThongBaoDon(
                                dp.getMaDatPhong(),
                                LoaiThongBao.QUA_HAN_CHECK_OUT,
                                MucDo.NGUY_CAP,
                                dp.getKhachHang() != null ? dp.getKhachHang().getHoTen() : "N/A",
                                dp.getKhachHang() != null ? dp.getKhachHang().getSoDienThoai() : "",
                                dp.getPhong() != null ? dp.getPhong().getSoPhong() : "N/A",
                                ngayTra,
                                "Quá hạn trả phòng: " + treMoTa + " (chưa check-out)"
                        ));
                    } else if (ngayTra.toLocalDate().isEqual(ngayKiemTra)) {
                        // Tới hạn trả phòng hôm nay
                        dsThongBao.add(new ThongBaoDon(
                                dp.getMaDatPhong(),
                                LoaiThongBao.CHECK_OUT_HOM_NAY,
                                MucDo.CANH_BAO,
                                dp.getKhachHang() != null ? dp.getKhachHang().getHoTen() : "N/A",
                                dp.getKhachHang() != null ? dp.getKhachHang().getSoDienThoai() : "",
                                dp.getPhong() != null ? dp.getPhong().getSoPhong() : "N/A",
                                ngayTra,
                                "Đến hạn trả phòng hôm nay lúc " + FormatUtil.formatNgayGio(ngayTra)
                        ));
                    }
                }
            }
        }

        // 2. Quét danh sách Hóa đơn chưa thanh toán
        List<HoaDon> dsHoaDon = hoaDonDAO.findAll();
        for (HoaDon hd : dsHoaDon) {
            if (hd.getTrangThai() == TrangThaiThanhToan.CHUA_THANH_TOAN) {
                String tenKh = (hd.getDatPhong() != null && hd.getDatPhong().getKhachHang() != null)
                        ? hd.getDatPhong().getKhachHang().getHoTen() : "N/A";
                String sdt = (hd.getDatPhong() != null && hd.getDatPhong().getKhachHang() != null)
                        ? hd.getDatPhong().getKhachHang().getSoDienThoai() : "";
                String soPhong = (hd.getDatPhong() != null && hd.getDatPhong().getPhong() != null)
                        ? hd.getDatPhong().getPhong().getSoPhong() : "N/A";

                dsThongBao.add(new ThongBaoDon(
                        hd.getMaHoaDon(),
                        LoaiThongBao.HOA_DON_CHUA_THANH_TOAN,
                        MucDo.THONG_TIN,
                        tenKh,
                        sdt,
                        soPhong,
                        hd.getNgayLap(),
                        "Còn lại phải thu: " + FormatUtil.formatTien(hd.getConLaiPhaiThu())
                ));
            }
        }

        // 3. Cập nhật trạng thái đã đọc / chưa đọc
        for (ThongBaoDon tb : dsThongBao) {
            tb.setDaDoc(danhSachIdDaDoc.contains(tb.getId()));
        }

        // 4. Sắp xếp ưu tiên: Chưa đọc lên trước, sau đó NGUY_CAP -> CANH_BAO -> THONG_TIN, sau đó theo thời hạn tăng dần
        dsThongBao.sort(Comparator
                .comparing((ThongBaoDon tb) -> tb.isDaDoc() ? 1 : 0)
                .thenComparingInt((ThongBaoDon tb) -> layThuTuUuTienMucDo(tb.getMucDo()))
                .thenComparing(ThongBaoDon::getThoiHan, Comparator.nullsLast(Comparator.naturalOrder())));

        return dsThongBao;
    }

    public void danhDauDaDoc(String id) {
        if (id != null) {
            danhSachIdDaDoc.add(id);
        }
    }

    public void danhDauTatCaDaDoc() {
        List<ThongBaoDon> list = layDanhSachThongBaoToiHan();
        for (ThongBaoDon tb : list) {
            danhSachIdDaDoc.add(tb.getId());
        }
    }

    public void datLaiTrangThaiChuaDoc() {
        danhSachIdDaDoc.clear();
    }

    public int demSoLuongChuaDoc() {
        return (int) layDanhSachThongBaoToiHan().stream()
                .filter(tb -> !tb.isDaDoc())
                .count();
    }

    public TrangThaiChuong getTrangThaiChuong() {
        List<ThongBaoDon> list = layDanhSachThongBaoToiHan();
        if (list.isEmpty()) {
            return TrangThaiChuong.KHONG_CO_THONG_BAO;
        }
        boolean coChuaDoc = list.stream().anyMatch(tb -> !tb.isDaDoc());
        return coChuaDoc ? TrangThaiChuong.CO_CHUA_DOC : TrangThaiChuong.DA_DOC_HET;
    }

    public int demTongSoLuongToiHan() {
        return layDanhSachThongBaoToiHan().size();
    }

    public int demSoLuongCheckInToiHan() {
        return (int) layDanhSachThongBaoToiHan().stream()
                .filter(tb -> tb.getLoaiThongBao() == LoaiThongBao.CHECK_IN_HOM_NAY
                        || tb.getLoaiThongBao() == LoaiThongBao.QUA_HAN_CHECK_IN)
                .count();
    }

    public int demSoLuongCheckOutToiHan() {
        return (int) layDanhSachThongBaoToiHan().stream()
                .filter(tb -> tb.getLoaiThongBao() == LoaiThongBao.CHECK_OUT_HOM_NAY
                        || tb.getLoaiThongBao() == LoaiThongBao.QUA_HAN_CHECK_OUT)
                .count();
    }

    public int demSoLuongHoaDonChuaThanhToan() {
        return (int) layDanhSachThongBaoToiHan().stream()
                .filter(tb -> tb.getLoaiThongBao() == LoaiThongBao.HOA_DON_CHUA_THANH_TOAN)
                .count();
    }

    private int layThuTuUuTienMucDo(MucDo mucDo) {
        if (mucDo == MucDo.NGUY_CAP) return 1;
        if (mucDo == MucDo.CANH_BAO) return 2;
        return 3;
    }

    private String tinhMoTaTre(LocalDateTime mocThoiGian, LocalDateTime thoiDiemHienTai) {
        Duration duration = Duration.between(mocThoiGian, thoiDiemHienTai);
        long tongPhut = duration.toMinutes();
        if (tongPhut < 60) {
            return "Trễ " + Math.max(1, tongPhut) + " phút";
        }
        long tongGio = duration.toHours();
        if (tongGio < 24) {
            return "Trễ " + tongGio + " giờ";
        }
        long tongNgay = duration.toDays();
        return "Trễ " + tongNgay + " ngày";
    }
}
