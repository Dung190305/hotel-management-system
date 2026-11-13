package com.qlks.service;

import com.qlks.dao.ChiTietDichVuDAO;
import com.qlks.dao.DatPhongDAO;
import com.qlks.dao.DichVuDAO;
import com.qlks.dao.HoaDonDAO;
import com.qlks.dao.impl.ChiTietDichVuDAOImpl;
import com.qlks.dao.impl.DatPhongDAOImpl;
import com.qlks.dao.impl.DichVuDAOImpl;
import com.qlks.dao.impl.HoaDonDAOImpl;
import com.qlks.exception.BusinessException;
import com.qlks.model.ChiTietDichVu;
import com.qlks.model.DatPhong;
import com.qlks.model.DichVu;
import com.qlks.model.HoaDon;
import com.qlks.model.enums.Permission;
import com.qlks.model.enums.TrangThaiDatPhong;
import com.qlks.model.enums.TrangThaiThanhToan;
import com.qlks.util.AppEventBus;
import com.qlks.util.DatabaseConnection;
import com.qlks.util.ValidationUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service xử lý danh mục dịch vụ phát sinh và ghi nhận dịch vụ theo phiếu đặt phòng.
 */
public class DichVuService {

    private final DichVuDAO dichVuDAO;
    private final ChiTietDichVuDAO chiTietDichVuDAO;
    private final DatPhongDAO datPhongDAO;
    private final HoaDonDAO hoaDonDAO;

    public DichVuService() {
        this.dichVuDAO = new DichVuDAOImpl();
        this.chiTietDichVuDAO = new ChiTietDichVuDAOImpl();
        this.datPhongDAO = new DatPhongDAOImpl();
        this.hoaDonDAO = new HoaDonDAOImpl();
    }

    public DichVuService(DichVuDAO dichVuDAO) {
        this.dichVuDAO = dichVuDAO;
        this.chiTietDichVuDAO = new ChiTietDichVuDAOImpl();
        this.datPhongDAO = new DatPhongDAOImpl();
        this.hoaDonDAO = new HoaDonDAOImpl();
    }

    public DichVuService(DichVuDAO dichVuDAO, ChiTietDichVuDAO chiTietDichVuDAO, DatPhongDAO datPhongDAO) {
        this(dichVuDAO, chiTietDichVuDAO, datPhongDAO, new HoaDonDAOImpl());
    }

    public DichVuService(DichVuDAO dichVuDAO, ChiTietDichVuDAO chiTietDichVuDAO, DatPhongDAO datPhongDAO, HoaDonDAO hoaDonDAO) {
        this.dichVuDAO = dichVuDAO;
        this.chiTietDichVuDAO = chiTietDichVuDAO;
        this.datPhongDAO = datPhongDAO;
        this.hoaDonDAO = hoaDonDAO != null ? hoaDonDAO : new HoaDonDAOImpl();
    }

    protected Connection layKetNoi() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    public List<DichVu> layTatCaDichVu() {
        return dichVuDAO.findAll();
    }

    public List<DichVu> layDichVuDangKinhDoanh() {
        return dichVuDAO.findDangKinhDoanh();
    }

    public Optional<DichVu> timTheoMa(String maDichVu) {
        return dichVuDAO.findById(maDichVu);
    }

    public List<DichVu> timKiem(String keyword) {
        return dichVuDAO.search(keyword);
    }

    public List<ChiTietDichVu> layDichVuTheoDatPhong(String maDatPhong) {
        return chiTietDichVuDAO.findByMaDatPhong(maDatPhong);
    }

    /**
     * Ghi nhận dịch vụ khách sử dụng theo phiếu đặt phòng (Booking Service Binding).
     * Quy tắc nghiệp vụ và kiểm soát tương tranh chuẩn PMS:
     * - Phiếu đặt phòng bắt buộc phải đang ở (DANG_O).
     * - Dịch vụ phải đang kinh doanh.
     * - Số lượng phải > 0.
     * - Đơn giá được chụp tại thời điểm sử dụng (Snapshot Price).
     * - KHÓA DÒNG BI QUAN (SELECT FOR UPDATE) trên dat_phong và hoa_don để chống Race Condition với Check-out.
     * - TỰ ĐỘNG ĐỒNG BỘ HÓA ĐƠN: Nếu hóa đơn của đơn đặt phòng đã tồn tại trong CSDL, tính lại tổng tiền dịch vụ
     *   và cập nhật ngay vào bảng hoa_don trong cùng một Transaction nguyên tử (ACID).
     */
    public void ghiNhanDichVu(String maDatPhong, String maDichVu, int soLuong) {
        AuthorizationService.require(Permission.GHI_NHAN_DICH_VU_THANH_TOAN);
        if (soLuong <= 0) {
            throw new BusinessException("Số lượng dịch vụ phải lớn hơn 0.");
        }

        // 1. Kiểm tra dịch vụ và trạng thái kinh doanh
        Optional<DichVu> dvOpt = dichVuDAO.findById(maDichVu);
        if (dvOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy dịch vụ được chọn: " + maDichVu);
        }
        DichVu dv = dvOpt.get();
        if (!dv.isDangKinhDoanh()) {
            throw new BusinessException("Dịch vụ '" + dv.getTenDichVu() + "' hiện đang ngừng kinh doanh.");
        }

        // 2. Thực thi trong Transaction nguyên tử và khóa dòng bi quan chống tương tranh
        try (Connection conn = layKetNoi()) {
            conn.setAutoCommit(false);
            try {
                // Khóa dòng phiếu đặt phòng để kiểm tra trạng thái và chống xung đột với Check-out
                DatPhong dpLocked = datPhongDAO.findByIdForUpdate(conn, maDatPhong)
                        .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu đặt phòng: " + maDatPhong));

                if (dpLocked.getTrangThai() != TrangThaiDatPhong.DANG_O) {
                    if (dpLocked.getTrangThai() == TrangThaiDatPhong.DA_TRA_PHONG) {
                        throw new BusinessException("Đơn đặt phòng " + maDatPhong + " đã hoàn tất thủ tục trả phòng (Check-out). Chỉ có thể ghi nhận dịch vụ khi khách đang lưu trú (trạng thái DANG_O)!");
                    }
                    throw new BusinessException(String.format(
                            "Chỉ có thể ghi nhận dịch vụ khi khách đang lưu trú (trạng thái DANG_O). Trạng thái hiện tại: %s.",
                            dpLocked.getTrangThai().getTenHienThi()));
                }

                // Kiểm tra xem đơn đặt phòng này đã được tạo Hóa đơn trước đó hay chưa
                Optional<HoaDon> hdOpt = hoaDonDAO.findByMaDatPhong(conn, maDatPhong);
                if (hdOpt.isPresent()) {
                    HoaDon hd = hdOpt.get();
                    if (hd.getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN) {
                        throw new BusinessException("Hóa đơn của đơn đặt phòng này đã được thanh toán, không thể ghi thêm dịch vụ.");
                    }
                    // Khóa dòng hóa đơn nếu đã có
                    hoaDonDAO.findByIdForUpdate(conn, hd.getMaHoaDon());
                }

                // 3. Ghi nhận dịch vụ với đơn giá snapshot tại thời điểm dùng trong Transaction
                ChiTietDichVu ctdv = new ChiTietDichVu(null, maDatPhong, dv, soLuong, dv.getDonGiaHienTai(), LocalDateTime.now());
                boolean success = chiTietDichVuDAO.save(conn, ctdv);
                if (!success) {
                    throw new BusinessException("Lưu chi tiết dịch vụ thất bại.");
                }

                // 4. Đồng bộ tự động hóa đơn: Nếu hóa đơn đã tạo, tính lại tổng tiền dịch vụ và cập nhật
                if (hdOpt.isPresent()) {
                    HoaDon hd = hdOpt.get();
                    List<ChiTietDichVu> dsDichVu = chiTietDichVuDAO.findByMaDatPhongForUpdate(conn, maDatPhong);
                    BigDecimal tongTienDichVu = BigDecimal.ZERO;
                    if (dsDichVu != null) {
                        for (ChiTietDichVu ct : dsDichVu) {
                            tongTienDichVu = tongTienDichVu.add(ct.tinhThanhTien());
                        }
                    }
                    hd.setTienDichVu(tongTienDichVu);
                    BigDecimal tongTienMoi = hd.tinhTongTien();
                    hoaDonDAO.updateTien(conn, hd.getMaHoaDon(), hd.getTienPhong(), tongTienDichVu, hd.getTienGiamGia(), hd.getTienCoc(), tongTienMoi);
                }

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof BusinessException) throw (BusinessException) ex;
                throw new BusinessException("Lỗi giao dịch khi ghi nhận dịch vụ: " + ex.getMessage(), ex);
            }
        } catch (SQLException ex) {
            throw new BusinessException("Lỗi kết nối CSDL khi ghi nhận dịch vụ: " + ex.getMessage(), ex);
        }

        AppEventBus.fireInvoiceChanged();
    }

    public void themDichVu(DichVu dv) {
        AuthorizationService.require(Permission.SUA_DANH_MUC);
        ValidationUtil.validate(dv);
        if (dichVuDAO.findById(dv.getMaDichVu()).isPresent()) {
            throw new BusinessException("Mã dịch vụ '" + dv.getMaDichVu() + "' đã tồn tại.");
        }
        boolean success = dichVuDAO.save(dv);
        if (!success) {
            throw new BusinessException("Thêm mới dịch vụ thất bại.");
        }
    }

    public void capNhatDichVu(DichVu dv) {
        AuthorizationService.require(Permission.SUA_DANH_MUC);
        ValidationUtil.validate(dv);
        Optional<DichVu> old = dichVuDAO.findById(dv.getMaDichVu());
        if (old.isEmpty()) {
            throw new BusinessException("Không tìm thấy dịch vụ để cập nhật.");
        }
        boolean success = dichVuDAO.update(dv);
        if (!success) {
            throw new BusinessException("Cập nhật dịch vụ thất bại.");
        }
    }

    /**
     * Ngừng kinh doanh dịch vụ (Soft delete để bảo toàn toàn vẹn lịch sử giao dịch).
     */
    public void ngungKinhDoanh(String maDichVu) {
        AuthorizationService.require(Permission.SUA_DANH_MUC);
        Optional<DichVu> old = dichVuDAO.findById(maDichVu);
        if (old.isEmpty()) {
            throw new BusinessException("Không tìm thấy dịch vụ.");
        }
        DichVu dv = old.get();
        dv.setDangKinhDoanh(false);
        dichVuDAO.update(dv);
    }
}
