package com.qlks.service;

import com.qlks.dao.PhongDAO;
import com.qlks.dao.impl.PhongDAOImpl;
import com.qlks.exception.BusinessException;
import com.qlks.model.Phong;
import com.qlks.model.enums.TrangThaiPhong;
import com.qlks.util.ValidationUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service xử lý nghiệp vụ quản lý phòng khách sạn.
 */
public class PhongService {

    private final PhongDAO phongDAO;

    public PhongService() {
        this.phongDAO = new PhongDAOImpl();
    }

    public PhongService(PhongDAO phongDAO) {
        this.phongDAO = phongDAO;
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
    }

    public void capNhatPhong(Phong phong) {
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
    }

    public void chuyenTrangThaiPhong(String maPhong, TrangThaiPhong trangThaiMoi) {
        Optional<Phong> phongOpt = phongDAO.findById(maPhong);
        if (phongOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy phòng có mã: " + maPhong);
        }

        Phong p = phongOpt.get();
        if (p.getTrangThaiHienTai() == TrangThaiPhong.OCCUPIED && trangThaiMoi == TrangThaiPhong.AVAILABLE) {
            throw new BusinessException("Phòng đang có khách lưu trú. Cần thực hiện trả phòng trước khi chuyển sang còn trống!");
        }

        boolean success = phongDAO.updateTrangThai(maPhong, trangThaiMoi);
        if (!success) {
            throw new BusinessException("Chuyển trạng thái phòng thất bại.");
        }
    }

    public void ngungKinhDoanhPhong(String maPhong) {
        Optional<Phong> phongOpt = phongDAO.findById(maPhong);
        if (phongOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy phòng để ngừng kinh doanh.");
        }

        Phong p = phongOpt.get();
        if (p.getTrangThaiHienTai() == TrangThaiPhong.OCCUPIED || p.getTrangThaiHienTai() == TrangThaiPhong.RESERVED) {
            throw new BusinessException("Không thể ngừng kinh doanh phòng đang có khách hoặc đã có người đặt trước!");
        }

        phongDAO.updateKinhDoanh(maPhong, false);
    }
}
