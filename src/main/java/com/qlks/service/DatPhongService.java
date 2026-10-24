package com.qlks.service;

import com.qlks.dao.DatPhongDAO;
import com.qlks.dao.HoaDonDAO;
import com.qlks.dao.PhongDAO;
import com.qlks.dao.impl.DatPhongDAOImpl;
import com.qlks.dao.impl.HoaDonDAOImpl;
import com.qlks.dao.impl.PhongDAOImpl;
import com.qlks.exception.BusinessException;
import com.qlks.model.DatPhong;
import com.qlks.model.HoaDon;
import com.qlks.model.Phong;
import com.qlks.model.enums.TrangThaiDatPhong;
import com.qlks.model.enums.TrangThaiPhong;
import com.qlks.model.enums.TrangThaiThanhToan;
import com.qlks.util.ValidationUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Service xử lý các quy tắc nghiệp vụ cốt lõi: Đặt phòng, Chống trùng lịch, Check-in, Check-out.
 */
public class DatPhongService {

    private final DatPhongDAO datPhongDAO;
    private final PhongDAO phongDAO;
    private final HoaDonDAO hoaDonDAO;

    public DatPhongService() {
        this.datPhongDAO = new DatPhongDAOImpl();
        this.phongDAO = new PhongDAOImpl();
        this.hoaDonDAO = new HoaDonDAOImpl();
    }

    public DatPhongService(DatPhongDAO datPhongDAO, PhongDAO phongDAO, HoaDonDAO hoaDonDAO) {
        this.datPhongDAO = datPhongDAO;
        this.phongDAO = phongDAO;
        this.hoaDonDAO = hoaDonDAO;
    }

    public List<DatPhong> layTatCaDatPhong() {
        return datPhongDAO.findAll();
    }

    public Optional<DatPhong> timTheoMa(String maDatPhong) {
        return datPhongDAO.findById(maDatPhong);
    }

    public List<DatPhong> layDanhSachDangO() {
        return datPhongDAO.findByTrangThai(TrangThaiDatPhong.DANG_O);
    }

    /**
     * Quy tắc nghiệp vụ 1: Tạo phiếu đặt phòng với kiểm tra chống trùng lịch.
     */
    public void taoDatPhong(DatPhong dp) {
        ValidationUtil.validate(dp);

        // 1. Kiểm tra ngày giờ hợp lệ
        if (!dp.getNgayNhanPhong().isBefore(dp.getNgayTraPhong())) {
            throw new BusinessException("Ngày trả phòng dự kiến phải diễn ra sau ngày nhận phòng.");
        }

        // Không cho đặt ngày trong quá khứ quá 1 ngày
        if (dp.getNgayNhanPhong().toLocalDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Ngày nhận phòng không thể nằm trong quá khứ.");
        }

        // 2. Kiểm tra mã đặt phòng duy nhất
        if (datPhongDAO.findById(dp.getMaDatPhong()).isPresent()) {
            throw new BusinessException("Mã đặt phòng '" + dp.getMaDatPhong() + "' đã tồn tại.");
        }

        // 3. Kiểm tra phòng tồn tại và sẵn sàng kinh doanh
        Optional<Phong> phongOpt = phongDAO.findById(dp.getPhong().getMaPhong());
        if (phongOpt.isEmpty()) {
            throw new BusinessException("Phòng được chọn không tồn tại.");
        }
        Phong phong = phongOpt.get();
        if (!phong.isDangKinhDoanh()) {
            throw new BusinessException("Phòng " + phong.getSoPhong() + " hiện đang ngừng kinh doanh.");
        }
        if (phong.getTrangThaiHienTai() == TrangThaiPhong.MAINTENANCE) {
            throw new BusinessException("Phòng " + phong.getSoPhong() + " hiện đang bảo trì thiết bị, không thể đặt.");
        }

        // 4. QUY TẮC VÀNG: Kiểm tra trùng lịch phòng
        boolean biTrungLich = datPhongDAO.isPhongBiTrungLich(
                phong.getMaPhong(),
                dp.getNgayNhanPhong(),
                dp.getNgayTraPhong(),
                null
        );
        if (biTrungLich) {
            throw new BusinessException("Phòng " + phong.getSoPhong() + " đã có khách đặt trong khoảng thời gian này! Vui lòng chọn phòng khác hoặc đổi ngày.");
        }

        // 5. Lưu phiếu đặt phòng
        dp.setTrangThai(TrangThaiDatPhong.DA_XAC_NHAN);
        boolean saved = datPhongDAO.save(dp);
        if (!saved) {
            throw new BusinessException("Lưu phiếu đặt phòng thất bại.");
        }

        // Nếu ngày nhận phòng là hôm nay, cập nhật trạng thái phòng sang RESERVED (Đã đặt)
        if (dp.getNgayNhanPhong().toLocalDate().isEqual(LocalDate.now()) && phong.getTrangThaiHienTai() == TrangThaiPhong.AVAILABLE) {
            phongDAO.updateTrangThai(phong.getMaPhong(), TrangThaiPhong.RESERVED);
        }
    }

    /**
     * Quy tắc nghiệp vụ 2: Xác nhận Check-in cho khách đến nhận phòng.
     */
    public void xacNhanCheckIn(String maDatPhong) {
        Optional<DatPhong> dpOpt = datPhongDAO.findById(maDatPhong);
        if (dpOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy phiếu đặt phòng: " + maDatPhong);
        }

        DatPhong dp = dpOpt.get();
        if (dp.getTrangThai() == TrangThaiDatPhong.DANG_O) {
            throw new BusinessException("Khách đã nhận phòng này trước đó.");
        }
        if (dp.getTrangThai() == TrangThaiDatPhong.DA_TRA_PHONG || dp.getTrangThai() == TrangThaiDatPhong.DA_HUY) {
            throw new BusinessException("Phiếu đặt phòng này đã kết thúc hoặc đã bị hủy.");
        }

        LocalDateTime thoiGianCheckIn = LocalDateTime.now();
        // Cập nhật trạng thái đặt phòng -> DANG_O
        datPhongDAO.xacNhanCheckIn(maDatPhong, thoiGianCheckIn);

        // Cập nhật trạng thái vật lý của phòng -> OCCUPIED (Đang có khách)
        phongDAO.updateTrangThai(dp.getPhong().getMaPhong(), TrangThaiPhong.OCCUPIED);
    }

    /**
     * Quy tắc nghiệp vụ 3: Xác nhận Check-out cho khách trả phòng và tự động lập Hóa đơn.
     *
     * @param maDatPhong Mã đặt phòng cần trả
     * @return HoaDon được tạo tự động để chuyển sang bước thanh toán
     */
    public HoaDon xacNhanCheckOut(String maDatPhong) {
        Optional<DatPhong> dpOpt = datPhongDAO.findById(maDatPhong);
        if (dpOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy phiếu đặt phòng: " + maDatPhong);
        }

        DatPhong dp = dpOpt.get();
        if (dp.getTrangThai() != TrangThaiDatPhong.DANG_O) {
            throw new BusinessException("Chỉ có thể check-out cho phòng đang có khách lưu trú (trạng thái DANG_O).");
        }

        LocalDateTime thoiGianCheckOut = LocalDateTime.now();
        // 1. Cập nhật đặt phòng -> DA_TRA_PHONG
        datPhongDAO.xacNhanCheckOut(maDatPhong, thoiGianCheckOut);

        // 2. Cập nhật trạng thái phòng -> CLEANING (Đang dọn dẹp để buồng phòng vệ sinh trước khi nhận khách mới)
        phongDAO.updateTrangThai(dp.getPhong().getMaPhong(), TrangThaiPhong.CLEANING);

        // 3. Tính toán tiền phòng thực tế dựa trên số đêm thực tế (Đa hình Phong.tinhTienPhong)
        LocalDateTime checkInThucTe = dp.getThoiGianNhanThucTe() != null ? dp.getThoiGianNhanThucTe() : dp.getNgayNhanPhong();
        long soDemThucTe = ChronoUnit.DAYS.between(checkInThucTe.toLocalDate(), thoiGianCheckOut.toLocalDate());
        if (soDemThucTe <= 0) {
            soDemThucTe = 1; // Ở trong ngày tính tối thiểu 1 đêm
        }

        // Đa hình tính tiền phòng:
        BigDecimal tienPhong = dp.getPhong().tinhTienPhong(soDemThucTe);

        // Trừ tiền cọc (nếu có cọc trước)
        BigDecimal tienGiamGia = dp.getTienCoc();

        // 4. Tạo hóa đơn thanh toán nếu chưa có
        Optional<HoaDon> hdCuOpt = hoaDonDAO.findByMaDatPhong(maDatPhong);
        if (hdCuOpt.isPresent()) {
            HoaDon hd = hdCuOpt.get();
            hd.setTienPhong(tienPhong);
            hd.setTienGiamGia(tienGiamGia);
            hd.setTongTien(hd.tinhTongTien());
            hoaDonDAO.update(hd);
            return hd;
        } else {
            String maHoaDon = "HD" + System.currentTimeMillis() % 1000000;
            HoaDon hdMoi = new HoaDon(maHoaDon, dp, dp.getNhanVienLap(), LocalDateTime.now(),
                    tienPhong, BigDecimal.ZERO, tienGiamGia,
                    tienPhong.subtract(tienGiamGia).max(BigDecimal.ZERO),
                    TrangThaiThanhToan.CHUA_THANH_TOAN);
            hoaDonDAO.save(hdMoi);
            return hdMoi;
        }
    }

    /**
     * Quy tắc nghiệp vụ 4: Hủy phiếu đặt phòng.
     */
    public void huyDatPhong(String maDatPhong) {
        Optional<DatPhong> dpOpt = datPhongDAO.findById(maDatPhong);
        if (dpOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy phiếu đặt phòng để hủy.");
        }

        DatPhong dp = dpOpt.get();
        if (dp.getTrangThai() == TrangThaiDatPhong.DANG_O || dp.getTrangThai() == TrangThaiDatPhong.DA_TRA_PHONG) {
            throw new BusinessException("Không thể hủy đặt phòng khi khách đang ở hoặc đã hoàn tất trả phòng.");
        }

        datPhongDAO.updateTrangThai(maDatPhong, TrangThaiDatPhong.DA_HUY);

        // Nếu phòng đang ở trạng thái RESERVED, hoàn trả về AVAILABLE
        Phong phong = dp.getPhong();
        if (phong != null && phong.getTrangThaiHienTai() == TrangThaiPhong.RESERVED) {
            phongDAO.updateTrangThai(phong.getMaPhong(), TrangThaiPhong.AVAILABLE);
        }
    }
}
