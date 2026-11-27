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
import com.qlks.model.NhanVien;
import com.qlks.model.ThanhToan;
import com.qlks.model.enums.Permission;
import com.qlks.model.enums.PhuongThucThanhToan;
import com.qlks.model.enums.TrangThaiThanhToan;
import com.qlks.util.AppEventBus;
import com.qlks.util.DatabaseConnection;
import com.qlks.util.IdGenerator;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service xử lý tính toán hóa đơn, thêm dịch vụ phát sinh và thanh toán an toàn.
 * Chống thanh toán trùng lặp (Idempotent Payment) và trừ tiền cọc chính xác.
 */
public class HoaDonService {

    private final HoaDonDAO hoaDonDAO;
    private final ChiTietDichVuDAO chiTietDichVuDAO;
    private final DichVuDAO dichVuDAO;
    private final ThanhToanDAO thanhToanDAO;
    private DichVuService dichVuService;

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

    public void setDichVuService(DichVuService dichVuService) {
        this.dichVuService = dichVuService;
    }

    public DichVuService getDichVuService() {
        if (dichVuService == null) {
            dichVuService = new DichVuService(dichVuDAO, chiTietDichVuDAO, new com.qlks.dao.impl.DatPhongDAOImpl(), hoaDonDAO) {
                @Override
                protected Connection layKetNoi() throws SQLException {
                    return HoaDonService.this.layKetNoi();
                }
            };
        }
        return dichVuService;
    }

    protected Connection layKetNoi() throws SQLException {
        return DatabaseConnection.getConnection();
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
     * Tính số tiền thực tế khách còn lại phải trả cho hóa đơn
     * (sau khi trừ cọc và trừ tất cả các đợt đã thanh toán trước đó).
     */
    public BigDecimal tinhSoTienConLaiCanThu(String maHoaDon) {
        Optional<HoaDon> hdOpt = hoaDonDAO.findById(maHoaDon);
        if (hdOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy hóa đơn: " + maHoaDon);
        }
        HoaDon hd = hdOpt.get();
        if (hd.getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN) {
            return BigDecimal.ZERO;
        }
        BigDecimal daTra = thanhToanDAO.tinhTongTienDaThanhToan(maHoaDon);
        BigDecimal tongChiPhi = hd.getTongChiPhi();
        BigDecimal tienCoc = hd.getTienCoc() != null ? hd.getTienCoc() : BigDecimal.ZERO;
        BigDecimal conLai = tongChiPhi.subtract(tienCoc).subtract(daTra);
        return conLai.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : conLai;
    }

    /**
     * Lấy tổng số tiền đã thanh toán của hóa đơn.
     */
    public BigDecimal tinhTongTienDaThanhToan(String maHoaDon) {
        return thanhToanDAO.tinhTongTienDaThanhToan(maHoaDon);
    }

    /**
     * Lấy toàn bộ lịch sử các giao dịch thanh toán của hóa đơn.
     */
    public List<ThanhToan> layLichSuThanhToan(String maHoaDon) {
        return thanhToanDAO.findByMaHoaDon(maHoaDon);
    }

    /**
     * Thêm dịch vụ khách đã sử dụng vào hóa đơn.
     * Thống nhất ủy quyền hoàn toàn sang DichVuService (Single Source of Truth)
     * để đảm bảo đồng nhất kiểm tra điều kiện lưu trú (DANG_O), chụp đơn giá snapshot
     * và cập nhật tự động bảng hóa đơn trong một Transaction nguyên tử có khóa dòng.
     */
    public void themDichVuVaoHoaDon(String maHoaDon, String maDichVu, int soLuong) {
        AuthorizationService.require(Permission.GHI_NHAN_DICH_VU_THANH_TOAN);
        Optional<HoaDon> hdOpt = hoaDonDAO.findById(maHoaDon);
        if (hdOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy hóa đơn: " + maHoaDon);
        }

        HoaDon hd = hdOpt.get();
        if (hd.getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN) {
            throw new BusinessException("Hóa đơn đã được thanh toán xong, không thể thêm dịch vụ!");
        }

        if (hd.getDatPhong() == null) {
            throw new BusinessException("Hóa đơn không liên kết với phiếu đặt phòng hợp lệ.");
        }

        getDichVuService().ghiNhanDichVu(hd.getDatPhong().getMaDatPhong(), maDichVu, soLuong);
    }

    /**
     * Quy tắc nghiệp vụ thanh toán hóa đơn:
     * - Khóa dòng bằng SELECT ... FOR UPDATE chống thanh toán đồng thời (Race condition).
     * - Không cho phép thanh toán hóa đơn đã thanh toán rồi (Chống thanh toán trùng lặp).
     * - Kiểm tra số tiền trả đủ so với số tiền CÒN LẠI PHẢI THU (sau khi trừ cọc).
     * - Nếu khách đã cọc đủ 100% (còn lại phải thu = 0), cho phép tất toán 0 VNĐ và hoàn cọc thừa nếu có.
     * - Ghi nhận giao dịch vào bảng thanh_toan và cập nhật trạng thái hóa đơn -> DA_THANH_TOAN trong cùng transaction.
     */
    public ThanhToan thanhToanHoaDon(String maHoaDon, BigDecimal soTienThanhToan, PhuongThucThanhToan phuongThuc, String ghiChu) {
        AuthorizationService.require(Permission.GHI_NHAN_DICH_VU_THANH_TOAN);
        if (soTienThanhToan == null || soTienThanhToan.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("Số tiền thanh toán phải lớn hơn hoặc bằng 0 (không được âm).");
        }

        ThanhToan tt;

        try (Connection conn = layKetNoi()) {
            conn.setAutoCommit(false);
            try {
                // 1. Đọc và khóa dòng hóa đơn chống tranh chấp đồng thời
                Optional<HoaDon> hdOpt = hoaDonDAO.findByIdForUpdate(conn, maHoaDon);
                if (hdOpt.isEmpty()) {
                    // Fallback thử đọc thông thường nếu DB không hỗ trợ SELECT FOR UPDATE
                    hdOpt = hoaDonDAO.findById(conn, maHoaDon);
                }
                if (hdOpt.isEmpty()) {
                    throw new BusinessException("Không tìm thấy hóa đơn cần thanh toán: " + maHoaDon);
                }

                HoaDon hd = hdOpt.get();

                // 2. CHỐNG THANH TOÁN TRÙNG LẶP
                if (hd.getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN) {
                    throw new BusinessException("Hóa đơn '" + maHoaDon + "' đã được thanh toán đầy đủ trước đó!");
                }

                // 3. Tính toán số tiền còn lại phải thu chính xác dựa trên tiền cọc và các đợt đã thanh toán
                BigDecimal daThanhToan = thanhToanDAO.tinhTongTienDaThanhToan(conn, maHoaDon);
                BigDecimal tongChiPhi = hd.getTongChiPhi();
                BigDecimal tienCoc = hd.getTienCoc() != null ? hd.getTienCoc() : BigDecimal.ZERO;
                BigDecimal conLaiPhaiThu = tongChiPhi.subtract(tienCoc).subtract(daThanhToan);
                if (conLaiPhaiThu.compareTo(BigDecimal.ZERO) < 0) {
                    conLaiPhaiThu = BigDecimal.ZERO;
                }

                // Nếu hóa đơn đã được cọc đủ 100% (hoặc thừa cọc), số tiền cần thu thêm là 0
                if (conLaiPhaiThu.compareTo(BigDecimal.ZERO) == 0 && soTienThanhToan.compareTo(BigDecimal.ZERO) > 0) {
                    throw new BusinessException(String.format(
                            "Hóa đơn này đã được tất toán đủ bằng tiền đặt cọc/thanh toán trước! Số tiền cần thu thêm là 0 VNĐ (bạn đã nhập %s VNĐ).",
                            soTienThanhToan.toPlainString()));
                }

                // Không cho phép thanh toán vượt quá số tiền còn lại phải thu
                if (soTienThanhToan.compareTo(conLaiPhaiThu) > 0) {
                    throw new BusinessException(String.format(
                            "Số tiền thanh toán (%s) vượt quá số tiền còn lại phải thu (%s)!",
                            com.qlks.util.FormatUtil.formatTien(soTienThanhToan),
                            com.qlks.util.FormatUtil.formatTien(conLaiPhaiThu)));
                }

                // Hỗ trợ thanh toán nhiều đợt (Partial Payment / Split tender):
                // Cho phép thanh toán từng phần (0 <= soTienThanhToan <= conLaiPhaiThu).

                // 4. Sinh mã và ghi nhận thanh toán trong transaction kèm người thu tiền
                String maThanhToan = IdGenerator.next(conn, "TT");
                NhanVien nvThu = AuthService.getNhanVienHienTai();
                String maNvThu = nvThu != null ? nvThu.getMaNhanVien() : null;

                String ghiChuThucTe = ghiChu;
                if (tienCoc.compareTo(tongChiPhi) > 0 && daThanhToan.compareTo(BigDecimal.ZERO) == 0) {
                    BigDecimal tienHoanLai = tienCoc.subtract(tongChiPhi);
                    String msgHoan = "Hoàn lại tiền cọc thừa: " + tienHoanLai.toPlainString() + " VNĐ";
                    ghiChuThucTe = (ghiChuThucTe != null && !ghiChuThucTe.isBlank()) ? ghiChuThucTe + " (" + msgHoan + ")" : msgHoan;
                } else if (conLaiPhaiThu.compareTo(BigDecimal.ZERO) == 0 && (ghiChuThucTe == null || ghiChuThucTe.isBlank())) {
                    ghiChuThucTe = "Tất toán bằng tiền đặt cọc";
                }

                tt = new ThanhToan(maThanhToan, maHoaDon, soTienThanhToan, phuongThuc, LocalDateTime.now(), maNvThu, ghiChuThucTe);

                boolean saved = thanhToanDAO.save(conn, tt);
                if (!saved) {
                    throw new BusinessException("Lưu giao dịch thanh toán thất bại.");
                }

                // 5. Cập nhật trạng thái Hóa đơn: Chỉ chuyển DA_THANH_TOAN khi số tiền còn lại sau đợt này về 0
                BigDecimal conLaiSauGiaoDich = conLaiPhaiThu.subtract(soTienThanhToan);
                if (conLaiSauGiaoDich.compareTo(BigDecimal.ZERO) == 0) {
                    boolean okHd = hoaDonDAO.updateTrangThai(conn, maHoaDon, TrangThaiThanhToan.DA_THANH_TOAN);
                    if (!okHd) {
                        throw new BusinessException("Cập nhật trạng thái hóa đơn thất bại.");
                    }
                    hd.setTrangThai(TrangThaiThanhToan.DA_THANH_TOAN);
                }

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof BusinessException) throw (BusinessException) ex;
                throw new BusinessException("Giao dịch thanh toán thất bại: " + ex.getMessage(), ex);
            }
        } catch (SQLException ex) {
            throw new BusinessException("Lỗi kết nối CSDL khi thanh toán: " + ex.getMessage(), ex);
        }

        // Đồng bộ sự kiện thời gian thực
        AppEventBus.fireInvoiceChanged();
        return tt;
    }
}
