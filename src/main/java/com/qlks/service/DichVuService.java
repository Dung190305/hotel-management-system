package com.qlks.service;

import com.qlks.dao.DichVuDAO;
import com.qlks.dao.impl.DichVuDAOImpl;
import com.qlks.exception.BusinessException;
import com.qlks.model.DichVu;
import com.qlks.util.ValidationUtil;

import java.util.List;
import java.util.Optional;

/**
 * Service xử lý danh mục dịch vụ phát sinh của khách sạn.
 */
public class DichVuService {

    private final DichVuDAO dichVuDAO;

    public DichVuService() {
        this.dichVuDAO = new DichVuDAOImpl();
    }

    public DichVuService(DichVuDAO dichVuDAO) {
        this.dichVuDAO = dichVuDAO;
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

    public void themDichVu(DichVu dv) {
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

    public void ngungKinhDoanh(String maDichVu) {
        Optional<DichVu> old = dichVuDAO.findById(maDichVu);
        if (old.isEmpty()) {
            throw new BusinessException("Không tìm thấy dịch vụ.");
        }
        dichVuDAO.delete(maDichVu);
    }
}
