package com.qlks.service;

import com.qlks.dao.NhanVienDAO;
import com.qlks.dao.TaiKhoanDAO;
import com.qlks.dao.impl.NhanVienDAOImpl;
import com.qlks.dao.impl.TaiKhoanDAOImpl;
import com.qlks.exception.BusinessException;
import com.qlks.model.NhanVien;
import com.qlks.model.TaiKhoan;
import com.qlks.model.enums.Permission;
import com.qlks.model.enums.VaiTro;
import com.qlks.util.DatabaseConnection;
import com.qlks.util.ValidationUtil;
import org.mindrot.jbcrypt.BCrypt;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
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

    protected Connection layKetNoi() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    /**
     * Thêm mới một nhân viên kèm tài khoản đăng nhập hệ thống trong một Transaction nguyên tử (ACID).
     * Chống lỗi Orphan Record: Nếu thêm nhân viên thất bại (trùng SĐT, Email), tài khoản tạo kèm sẽ được rollback.
     */
    public void themNhanVien(NhanVien nv, String tenDangNhap, String matKhau, VaiTro vaiTro) {
        AuthorizationService.require(Permission.QUAN_LY_TAI_KHOAN);
        ValidationUtil.validate(nv);

        if (nhanVienDAO.findById(nv.getMaNhanVien()).isPresent()) {
            throw new BusinessException("Mã nhân viên '" + nv.getMaNhanVien() + "' đã tồn tại.");
        }

        boolean coTaiKhoan = tenDangNhap != null && !tenDangNhap.trim().isEmpty();
        TaiKhoan tk = null;
        if (coTaiKhoan) {
            String cleanUser = tenDangNhap.trim();
            if (taiKhoanDAO.findByTenDangNhap(cleanUser).isPresent()) {
                throw new BusinessException("Tên đăng nhập '" + cleanUser + "' đã tồn tại.");
            }
            String matKhauHash = BCrypt.hashpw(matKhau != null ? matKhau : "123456", BCrypt.gensalt(10));
            tk = new TaiKhoan(cleanUser, matKhauHash, vaiTro != null ? vaiTro : VaiTro.LE_TAN, true);
            nv.setTenDangNhap(cleanUser);
        }

        try (Connection conn = layKetNoi()) {
            conn.setAutoCommit(false);
            try {
                if (coTaiKhoan && tk != null) {
                    taiKhoanDAO.save(conn, tk);
                }

                boolean success = nhanVienDAO.save(conn, nv);
                if (!success) {
                    throw new BusinessException("Thêm nhân viên thất bại.");
                }

                conn.commit();
            } catch (Exception e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    // Bỏ qua lỗi rollback nếu connection đã bị hủy
                }
                if (e instanceof BusinessException) {
                    throw (BusinessException) e;
                }
                String msg = e.getMessage();
                if (msg != null && (msg.toLowerCase().contains("duplicate") || msg.toLowerCase().contains("trùng"))) {
                    if (msg.toLowerCase().contains("so_dien_thoai") || msg.toLowerCase().contains("sđt")) {
                        throw new BusinessException("Số điện thoại '" + nv.getSoDienThoai() + "' đã được sử dụng bởi nhân viên khác.");
                    } else if (msg.toLowerCase().contains("email")) {
                        throw new BusinessException("Email '" + nv.getEmail() + "' đã được sử dụng bởi nhân viên khác.");
                    } else if (msg.toLowerCase().contains("ten_dang_nhap")) {
                        throw new BusinessException("Tên đăng nhập '" + nv.getTenDangNhap() + "' đã tồn tại.");
                    }
                    throw new BusinessException("Thông tin nhân viên bị trùng lặp (Số điện thoại hoặc Email đã tồn tại).");
                }
                throw new BusinessException("Lỗi khi thêm nhân viên: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new BusinessException("Lỗi kết nối cơ sở dữ liệu khi thêm nhân viên: " + e.getMessage(), e);
        }
    }

    public void capNhatNhanVien(NhanVien nv) {
        AuthorizationService.require(Permission.QUAN_LY_TAI_KHOAN);
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
        AuthorizationService.require(Permission.QUAN_LY_TAI_KHOAN);
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
