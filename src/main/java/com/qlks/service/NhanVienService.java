package com.qlks.service;

import com.qlks.dao.NhanVienDAO;
import com.qlks.dao.TaiKhoanDAO;
import com.qlks.dao.impl.NhanVienDAOImpl;
import com.qlks.dao.impl.TaiKhoanDAOImpl;
import com.qlks.exception.BusinessException;
import com.qlks.model.NhanVien;
import com.qlks.model.TaiKhoan;
import com.qlks.model.enums.VaiTro;
import com.qlks.util.ValidationUtil;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service xử lý nghiệp vụ quản lý nhân viên và tính toán bảng lương.
 * Thể hiện tính Đa hình OOP qua phương thức tinhTongQuyLuong().
 */
public class NhanVienService {

    private final NhanVienDAO nhanVienDAO;
    private final TaiKhoanDAO taiKhoanDAO;

    public NhanVienService() {
        this.nhanVienDAO = new NhanVienDAOImpl();
        this.taiKhoanDAO = new TaiKhoanDAOImpl();
    }

    public NhanVienService(NhanVienDAO nhanVienDAO, TaiKhoanDAO taiKhoanDAO) {
        this.nhanVienDAO = nhanVienDAO;
        this.taiKhoanDAO = taiKhoanDAO;
    }

    public List<NhanVien> layTatCaNhanVien() {
        return nhanVienDAO.findAll();
    }

    public Optional<NhanVien> timTheoMa(String maNv) {
        return nhanVienDAO.findById(maNv);
    }

    public List<NhanVien> timKiem(String keyword) {
        return nhanVienDAO.search(keyword);
    }

    /**
     * Thêm mới một nhân viên kèm tài khoản đăng nhập hệ thống.
     */
    public void themNhanVien(NhanVien nv, String tenDangNhap, String matKhau, VaiTro vaiTro) {
        ValidationUtil.validate(nv);

        if (nhanVienDAO.findById(nv.getMaNhanVien()).isPresent()) {
            throw new BusinessException("Mã nhân viên '" + nv.getMaNhanVien() + "' đã tồn tại.");
        }

        // Tạo tài khoản trước
        if (tenDangNhap != null && !tenDangNhap.trim().isEmpty()) {
            if (taiKhoanDAO.findByTenDangNhap(tenDangNhap).isPresent()) {
                throw new BusinessException("Tên đăng nhập '" + tenDangNhap + "' đã tồn tại.");
            }
            String matKhauHash = BCrypt.hashpw(matKhau != null ? matKhau : "123456", BCrypt.gensalt(10));
            TaiKhoan tk = new TaiKhoan(tenDangNhap, matKhauHash, vaiTro != null ? vaiTro : VaiTro.LE_TAN, true);
            taiKhoanDAO.save(tk);
            nv.setTenDangNhap(tenDangNhap);
        }

        boolean success = nhanVienDAO.save(nv);
        if (!success) {
            throw new BusinessException("Thêm nhân viên thất bại.");
        }
    }

    public void capNhatNhanVien(NhanVien nv) {
        ValidationUtil.validate(nv);
        Optional<NhanVien> old = nhanVienDAO.findById(nv.getMaNhanVien());
        if (old.isEmpty()) {
            throw new BusinessException("Không tìm thấy nhân viên cần cập nhật.");
        }

        boolean success = nhanVienDAO.update(nv);
        if (!success) {
            throw new BusinessException("Cập nhật thông tin nhân viên thất bại.");
        }
    }

    public void ngungHoatDong(String maNv) {
        Optional<NhanVien> old = nhanVienDAO.findById(maNv);
        if (old.isEmpty()) {
            throw new BusinessException("Không tìm thấy nhân viên.");
        }
        nhanVienDAO.updateTrangThaiHoatDong(maNv, false);
    }

    /**
     * Tính tổng quỹ lương chi trả cho toàn bộ nhân viên khách sạn.
     * Thể hiện ĐA HÌNH OOP: Duyệt danh sách List<NhanVien>, từng đối tượng LeTan và QuanLy
     * tự động gọi hàm tinhLuong() tương ứng của nó mà không cần viết lệnh if-else kiểm tra loại.
     *
     * @return Map chứa tổng quỹ lương và danh sách chi tiết lương
     */
    public Map<String, Object> tinhBangLuong() {
        List<NhanVien> danhSach = nhanVienDAO.findAll();
        BigDecimal tongQuyLuong = BigDecimal.ZERO;

        for (NhanVien nv : danhSach) {
            if (nv.isDangHoatDong()) {
                // ĐA HÌNH Ở ĐÂY: nv.tinhLuong() tự động phân giải tại runtime
                tongQuyLuong = tongQuyLuong.add(nv.tinhLuong());
            }
        }

        Map<String, Object> ketQua = new HashMap<>();
        ketQua.put("danhSachNhanVien", danhSach);
        ketQua.put("tongQuyLuong", tongQuyLuong);
        return ketQua;
    }
}
