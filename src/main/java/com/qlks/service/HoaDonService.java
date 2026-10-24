package com.qlks.service;

import com.qlks.dao.ChiTietDichVuDAO;
import com.qlks.dao.DichVuDAO;
import com.qlks.dao.HoaDonDAO;
import com.qlks.dao.ThanhToanDAO;
import com.qlks.dao.impl.ChiTietDichVuDAOImpl;
import com.qlks.dao.impl.DichVuDAOImpl;
import com.qlks.dao.impl.HoaDonDAOImpl;
import com.qlks.dao.impl.ThanhToanDAOImpl;
import com.qlks.exception.BusinessException;
import com.qlks.model.ChiTietDichVu;
import com.qlks.model.DichVu;
import com.qlks.model.HoaDon;
import com.qlks.model.ThanhToan;
import com.qlks.model.enums.PhuongThucThanhToan;
import com.qlks.model.enums.TrangThaiThanhToan;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service xử lý tính toán hóa đơn, thêm dịch vụ phát sinh và thanh toán an toàn.
 * Chống thanh toán trùng lặp (Idempotent Payment).
 */
public class HoaDonService {

    private final HoaDonDAO hoaDonDAO;
    private final ChiTietDichVuDAO chiTietDichVuDAO;
    private final DichVuDAO dichVuDAO;
    private final ThanhToanDAO thanhToanDAO;

    public HoaDonService() {
        this.hoaDonDAO = new HoaDonDAOImpl();
        this.chiTietDichVuDAO = new ChiTietDichVuDAOImpl();
        this.dichVuDAO = new DichVuDAOImpl();
        this.thanhToanDAO = new ThanhToanDAOImpl();
    }

    public HoaDonService(HoaDonDAO hoaDonDAO, ChiTietDichVuDAO chiTietDichVuDAO, DichVuDAO dichVuDAO, ThanhToanDAO thanhToanDAO) {
        this.hoaDonDAO = hoaDonDAO;
        this.chiTietDichVuDAO = chiTietDichVuDAO;
        this.dichVuDAO = dichVuDAO;
        this.thanhToanDAO = thanhToanDAO;
    }

    public List<HoaDon> layTatCaHoaDon() {
        return hoaDonDAO.findAll();
    }

    public Optional<HoaDon> timTheoMa(String maHoaDon) {
        return hoaDonDAO.findById(maHoaDon);
    }

    public Optional<HoaDon> timTheoMaDatPhong(String maDatPhong) {
        return hoaDonDAO.findByMaDatPhong(maDatPhong);
    }

    /**
     * Thêm dịch vụ khách đã sử dụng vào hóa đơn.
     * Lưu vết đơn giá dịch vụ tại thời điểm dùng để không bị đổi giá sau này.
     */
    public void themDichVuVaoHoaDon(String maHoaDon, String maDichVu, int soLuong) {
        if (soLuong <= 0) {
            throw new BusinessException("Số lượng dịch vụ phải lớn hơn 0.");
        }

        Optional<HoaDon> hdOpt = hoaDonDAO.findById(maHoaDon);
        if (hdOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy hóa đơn: " + maHoaDon);
        }

        HoaDon hd = hdOpt.get();
        if (hd.getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN) {
            throw new BusinessException("Hóa đơn đã được thanh toán xong, không thể thêm dịch vụ!");
        }

        Optional<DichVu> dvOpt = dichVuDAO.findById(maDichVu);
        if (dvOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy dịch vụ được chọn.");
        }
        DichVu dv = dvOpt.get();

        // Tạo chi tiết dịch vụ với đơn giá tại thời điểm dùng
        ChiTietDichVu ctdv = new ChiTietDichVu(null, maHoaDon, dv, soLuong, dv.getDonGiaHienTai(), LocalDateTime.now());
        chiTietDichVuDAO.save(ctdv);

        // Tính lại tổng tiền dịch vụ của hóa đơn
        List<ChiTietDichVu> dsDichVu = chiTietDichVuDAO.findByMaHoaDon(maHoaDon);
        BigDecimal tongTienDichVu = BigDecimal.ZERO;
        for (ChiTietDichVu ct : dsDichVu) {
            tongTienDichVu = tongTienDichVu.add(ct.tinhThanhTien());
        }

        hd.setTienDichVu(tongTienDichVu);
        BigDecimal tongTienMoi = hd.tinhTongTien();
        hoaDonDAO.updateTien(maHoaDon, hd.getTienPhong(), tongTienDichVu, hd.getTienGiamGia(), tongTienMoi);
    }

    /**
     * Quy tắc nghiệp vụ thanh toán hóa đơn:
     * - Không cho phép thanh toán hóa đơn đã thanh toán rồi (Chống thanh toán trùng lặp).
     * - Kiểm tra số tiền trả đủ.
     * - Ghi nhận giao dịch vào bảng thanh_toan và cập nhật trạng thái hóa đơn -> DA_THANH_TOAN.
     */
    public ThanhToan thanhToanHoaDon(String maHoaDon, BigDecimal soTienThanhToan, PhuongThucThanhToan phuongThuc, String ghiChu) {
        if (soTienThanhToan == null || soTienThanhToan.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Số tiền thanh toán phải lớn hơn 0.");
        }

        Optional<HoaDon> hdOpt = hoaDonDAO.findById(maHoaDon);
        if (hdOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy hóa đơn cần thanh toán: " + maHoaDon);
        }

        HoaDon hd = hdOpt.get();
        // 1. CHỐNG THANH TOÁN TRÙNG LẶP
        if (hd.getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN) {
            throw new BusinessException("Hóa đơn '" + maHoaDon + "' đã được thanh toán đầy đủ trước đó!");
        }

        // 2. Kiểm tra số tiền thanh toán tối thiểu phải bằng số tiền hóa đơn
        if (soTienThanhToan.compareTo(hd.getTongTien()) < 0) {
            throw new BusinessException(String.format("Số tiền thanh toán (%s VNĐ) không đủ để thanh toán tổng tiền hóa đơn (%s VNĐ)!",
                    soTienThanhToan.toPlainString(), hd.getTongTien().toPlainString()));
        }

        // 3. Tạo bản ghi giao dịch thanh toán
        String maThanhToan = "TT" + System.currentTimeMillis() % 1000000;
        ThanhToan tt = new ThanhToan(maThanhToan, maHoaDon, soTienThanhToan, phuongThuc, LocalDateTime.now(), ghiChu);
        boolean saved = thanhToanDAO.save(tt);
        if (!saved) {
            throw new BusinessException("Lưu giao dịch thanh toán thất bại.");
        }

        // 4. Cập nhật trạng thái hóa đơn -> DA_THANH_TOAN
        hoaDonDAO.updateTrangThai(maHoaDon, TrangThaiThanhToan.DA_THANH_TOAN);
        return tt;
    }

    public List<ThanhToan> layLichSuThanhToan(String maHoaDon) {
        return thanhToanDAO.findByMaHoaDon(maHoaDon);
    }
}
