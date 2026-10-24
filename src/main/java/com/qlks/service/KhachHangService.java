package com.qlks.service;

import com.qlks.dao.KhachHangDAO;
import com.qlks.dao.impl.KhachHangDAOImpl;
import com.qlks.exception.BusinessException;
import com.qlks.model.KhachHang;
import com.qlks.util.ValidationUtil;

import java.util.List;
import java.util.Optional;

/**
 * Service xử lý nghiệp vụ quản lý khách hàng.
 */
public class KhachHangService {

    private final KhachHangDAO khachHangDAO;

    public KhachHangService() {
        this.khachHangDAO = new KhachHangDAOImpl();
    }

    public KhachHangService(KhachHangDAO khachHangDAO) {
        this.khachHangDAO = khachHangDAO;
    }

    public List<KhachHang> layTatCaKhachHang() {
        return khachHangDAO.findAll();
    }

    public Optional<KhachHang> timTheoMa(String maKhachHang) {
        return khachHangDAO.findById(maKhachHang);
    }

    public Optional<KhachHang> timTheoCCCD(String soCCCD) {
        return khachHangDAO.findBySoCCCD(soCCCD);
    }

    public Optional<KhachHang> timTheoSoDienThoai(String sdt) {
        return khachHangDAO.findBySoDienThoai(sdt);
    }

    public List<KhachHang> timKiem(String keyword) {
        return khachHangDAO.search(keyword);
    }

    public void themKhachHang(KhachHang kh) {
        ValidationUtil.validate(kh);

        if (khachHangDAO.findById(kh.getMaKhachHang()).isPresent()) {
            throw new BusinessException("Mã khách hàng '" + kh.getMaKhachHang() + "' đã tồn tại.");
        }
        if (khachHangDAO.findBySoCCCD(kh.getSoCCCD()).isPresent()) {
            throw new BusinessException("Số CCCD '" + kh.getSoCCCD() + "' đã được đăng ký cho khách hàng khác.");
        }
        if (khachHangDAO.findBySoDienThoai(kh.getSoDienThoai()).isPresent()) {
            throw new BusinessException("Số điện thoại '" + kh.getSoDienThoai() + "' đã tồn tại trên hệ thống.");
        }

        boolean success = khachHangDAO.save(kh);
        if (!success) {
            throw new BusinessException("Thêm mới khách hàng thất bại.");
        }
    }

    public void capNhatKhachHang(KhachHang kh) {
        ValidationUtil.validate(kh);

        Optional<KhachHang> oldOpt = khachHangDAO.findById(kh.getMaKhachHang());
        if (oldOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy thông tin khách hàng cần cập nhật.");
        }

        Optional<KhachHang> checkCccd = khachHangDAO.findBySoCCCD(kh.getSoCCCD());
        if (checkCccd.isPresent() && !checkCccd.get().getMaKhachHang().equals(kh.getMaKhachHang())) {
            throw new BusinessException("Số CCCD '" + kh.getSoCCCD() + "' đã thuộc về khách hàng khác.");
        }

        Optional<KhachHang> checkSdt = khachHangDAO.findBySoDienThoai(kh.getSoDienThoai());
        if (checkSdt.isPresent() && !checkSdt.get().getMaKhachHang().equals(kh.getMaKhachHang())) {
            throw new BusinessException("Số điện thoại '" + kh.getSoDienThoai() + "' đã thuộc về khách hàng khác.");
        }

        boolean success = khachHangDAO.update(kh);
        if (!success) {
            throw new BusinessException("Cập nhật thông tin khách hàng thất bại.");
        }
    }
}
