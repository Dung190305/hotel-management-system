package com.qlks.service;

import com.qlks.dao.DatPhongDAO;
import com.qlks.dao.PhongDAO;
import com.qlks.dao.impl.DatPhongDAOImpl;
import com.qlks.dao.impl.PhongDAOImpl;
import com.qlks.exception.BusinessException;
import com.qlks.model.Phong;
import com.qlks.model.enums.Permission;
import com.qlks.model.enums.TrangThaiPhong;
import com.qlks.util.AppEventBus;
import com.qlks.util.ValidationUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service xử lý nghiệp vụ quản lý phòng khách sạn và chuyển trạng thái phòng.
 */
public class PhongService {

    private final PhongDAO phongDAO;
    private final DatPhongDAO datPhongDAO;

    public PhongService() {
        this.phongDAO = new PhongDAOImpl();
        this.datPhongDAO = new DatPhongDAOImpl();
    }

    public PhongService(PhongDAO phongDAO, DatPhongDAO datPhongDAO) {
        this.phongDAO = phongDAO;
        this.datPhongDAO = datPhongDAO;
    }

    public List<Phong> layTatCaPhong() {
        return phongDAO.findAll();
    }

    public Optional<Phong> timTheoMa(String maPhong) {
        return phongDAO.findById(maPhong);
    }

    public Optional<Phong> timTheoSoPhong(String soPhong) {
        return phongDAO.findBySoPhong(soPhong);
    }

    public List<Phong> layPhongTheoTrangThai(TrangThaiPhong trangThai) {
        return phongDAO.findByTrangThai(trangThai);
    }

    public List<Phong> timPhongTrong(LocalDateTime checkIn, LocalDateTime checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new BusinessException("Thời gian nhận và trả phòng không được để trống.");
        }
        if (!checkIn.isBefore(checkOut)) {
            throw new BusinessException("Thời gian trả phòng phải sau thời gian nhận phòng.");
        }
        return phongDAO.findPhongTrong(checkIn, checkOut);
    }

    public void themPhong(Phong phong) {
        AuthorizationService.require(Permission.SUA_DANH_MUC);
        ValidationUtil.validate(phong);

        if (phongDAO.findById(phong.getMaPhong()).isPresent()) {
            throw new BusinessException("Mã phòng '" + phong.getMaPhong() + "' đã tồn tại trên hệ thống.");
        }
        if (phongDAO.findBySoPhong(phong.getSoPhong()).isPresent()) {
            throw new BusinessException("Số phòng '" + phong.getSoPhong() + "' đã được sử dụng. Vui lòng chọn số khác.");
        }

        boolean success = phongDAO.save(phong);
        if (!success) {
            throw new BusinessException("Không thể thêm phòng. Vui lòng thử lại!");
        }
        AppEventBus.fireRoomStatusChanged();
    }

    public void capNhatPhong(Phong phong) {
        AuthorizationService.require(Permission.SUA_DANH_MUC);
        ValidationUtil.validate(phong);

        Optional<Phong> oldOpt = phongDAO.findById(phong.getMaPhong());
        if (oldOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy thông tin phòng cần cập nhật.");
        }

        Optional<Phong> soPhongCheck = phongDAO.findBySoPhong(phong.getSoPhong());
        if (soPhongCheck.isPresent() && !soPhongCheck.get().getMaPhong().equals(phong.getMaPhong())) {
            throw new BusinessException("Số phòng '" + phong.getSoPhong() + "' đã được dùng bởi phòng khác.");
        }

        boolean success = phongDAO.update(phong);
        if (!success) {
            throw new BusinessException("Cập nhật phòng thất bại.");
        }
        AppEventBus.fireRoomStatusChanged();
    }

    /**
     * Quy tắc nghiệp vụ hoàn tất dọn dẹp buồng phòng:
     * - Chỉ áp dụng khi phòng đang dọn dẹp (CLEANING).
     * - Nếu phòng đã có khách đặt trước nhận phòng hôm nay -> tự động chuyển sang RESERVED (Đã đặt).
     * - Nếu không có lịch đặt hôm nay -> chuyển sang AVAILABLE (Còn trống).
     */
    public TrangThaiPhong hoanTatDonPhong(String maPhong) {
        Optional<Phong> phongOpt = phongDAO.findById(maPhong);
        if (phongOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy phòng có mã: " + maPhong);
        }

        Phong p = phongOpt.get();
        if (p.getTrangThaiHienTai() == TrangThaiPhong.OCCUPIED) {
            throw new BusinessException("Phòng đang có khách lưu trú, không thể thực hiện hoàn tất dọn dẹp!");
        }

        // Kiểm tra xem phòng có lượt đặt phòng đã xác nhận trong ngày hôm nay không
        boolean coDatPhongHomNay = datPhongDAO.hasDatPhongXacNhanTrongNgay(maPhong, LocalDate.now());
        TrangThaiPhong ttMoi = coDatPhongHomNay ? TrangThaiPhong.RESERVED : TrangThaiPhong.AVAILABLE;

        boolean success = phongDAO.updateTrangThai(maPhong, ttMoi);
        if (!success) {
            throw new BusinessException("Hoàn tất dọn phòng thất bại.");
        }

        AppEventBus.fireRoomStatusChanged();
        return ttMoi;
    }

    /**
     * Chuyển trạng thái phòng theo nghiệp vụ khách sạn có kiểm soát:
     * - Không cho phép tự ý đổi trạng thái phòng đang có khách (OCCUPIED) mà chưa check-out.
     * - Không cho phép đưa phòng đang có khách vào bảo trì (MAINTENANCE).
     * - Khi đưa phòng trở lại hoạt động từ CLEANING hoặc MAINTENANCE sang AVAILABLE,
     *   nếu có khách đặt hôm nay sẽ tự chuyển sang RESERVED.
     */
    public TrangThaiPhong chuyenTrangThaiPhong(String maPhong, TrangThaiPhong trangThaiMoi) {
        Optional<Phong> phongOpt = phongDAO.findById(maPhong);
        if (phongOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy phòng có mã: " + maPhong);
        }

        Phong p = phongOpt.get();

        // 1. Chặn tuyệt đối việc gán trạng thái OCCUPIED hoặc RESERVED thủ công
        if (trangThaiMoi == TrangThaiPhong.OCCUPIED) {
            throw new BusinessException("Trạng thái 'Đang có khách' (OCCUPIED) chỉ được cập nhật tự động khi Lễ tân thực hiện thủ tục Check-in nhận phòng!");
        }
        if (trangThaiMoi == TrangThaiPhong.RESERVED) {
            throw new BusinessException("Trạng thái 'Đã đặt' (RESERVED) chỉ được cập nhật tự động thông qua phiếu Đặt phòng!");
        }

        // 2. Không cho phép tự ý đổi trạng thái phòng đang có khách lưu trú
        if (p.getTrangThaiHienTai() == TrangThaiPhong.OCCUPIED && trangThaiMoi != TrangThaiPhong.OCCUPIED) {
            throw new BusinessException("Phòng đang có khách lưu trú (OCCUPIED). Vui lòng thực hiện trả phòng trước khi đổi trạng thái!");
        }

        // 3. Không cho phép đưa phòng vào bảo trì nếu đang có khách hoặc đang có lịch đặt hôm nay
        if (trangThaiMoi == TrangThaiPhong.MAINTENANCE) {
            if (p.getTrangThaiHienTai() == TrangThaiPhong.OCCUPIED) {
                throw new BusinessException("Phòng đang có khách lưu trú, không thể đưa vào bảo trì!");
            }
            if (p.getTrangThaiHienTai() == TrangThaiPhong.RESERVED || datPhongDAO.hasDatPhongXacNhanTrongNgay(maPhong, LocalDate.now())) {
                throw new BusinessException("Phòng " + p.getSoPhong() + " đang có khách đặt trước hôm nay (RESERVED). Vui lòng chuyển phòng hoặc hủy đơn trước khi đưa vào bảo trì!");
            }
        }

        // 4. Tự động kiểm tra lịch đặt phòng hôm nay khi muốn mở phòng còn trống
        TrangThaiPhong ttThucTe = trangThaiMoi;
        if ((p.getTrangThaiHienTai() == TrangThaiPhong.CLEANING || p.getTrangThaiHienTai() == TrangThaiPhong.MAINTENANCE)
                && trangThaiMoi == TrangThaiPhong.AVAILABLE) {
            boolean coDatHomNay = datPhongDAO.hasDatPhongXacNhanTrongNgay(maPhong, LocalDate.now());
            if (coDatHomNay) {
                ttThucTe = TrangThaiPhong.RESERVED;
            }
        }

        boolean success = phongDAO.updateTrangThai(maPhong, ttThucTe);
        if (!success) {
            throw new BusinessException("Chuyển trạng thái phòng thất bại.");
        }

        AppEventBus.fireRoomStatusChanged();
        return ttThucTe;
    }

    public void ngungKinhDoanhPhong(String maPhong) {
        AuthorizationService.require(Permission.SUA_DANH_MUC);
        Optional<Phong> phongOpt = phongDAO.findById(maPhong);
        if (phongOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy phòng để ngừng kinh doanh.");
        }

        Phong p = phongOpt.get();
        if (p.getTrangThaiHienTai() == TrangThaiPhong.OCCUPIED || p.getTrangThaiHienTai() == TrangThaiPhong.RESERVED) {
            throw new BusinessException("Không thể ngừng kinh doanh phòng đang có khách hoặc đã có người đặt trước!");
        }

        phongDAO.updateKinhDoanh(maPhong, false);
        AppEventBus.fireRoomStatusChanged();
    }
}
