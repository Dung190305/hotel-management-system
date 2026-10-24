package com.qlks.service;

import com.qlks.dao.NhanVienDAO;
import com.qlks.dao.TaiKhoanDAO;
import com.qlks.dao.impl.NhanVienDAOImpl;
import com.qlks.dao.impl.TaiKhoanDAOImpl;
import com.qlks.exception.BusinessException;
import com.qlks.model.NhanVien;
import com.qlks.model.TaiKhoan;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

/**
 * Service xử lý xác thực, đăng nhập, đổi mật khẩu và quản lý phiên người dùng (Session).
 */
public class AuthService {

    private final TaiKhoanDAO taiKhoanDAO;
    private final NhanVienDAO nhanVienDAO;

    // Quản lý phiên làm việc người dùng hiện tại (Current User Session)
    private static TaiKhoan taiKhoanHienTai;
    private static NhanVien nhanVienHienTai;

    public AuthService() {
        this.taiKhoanDAO = new TaiKhoanDAOImpl();
        this.nhanVienDAO = new NhanVienDAOImpl();
    }

    public AuthService(TaiKhoanDAO taiKhoanDAO, NhanVienDAO nhanVienDAO) {
        this.taiKhoanDAO = taiKhoanDAO;
        this.nhanVienDAO = nhanVienDAO;
    }

    /**
     * Xác thực thông tin đăng nhập người dùng.
     *
     * @param tenDangNhap Tên đăng nhập
     * @param matKhau     Mật khẩu dạng chuỗi thường
     * @return TaiKhoan đối tượng tài khoản đăng nhập thành công
     * @throws BusinessException nếu sai thông tin hoặc tài khoản bị khóa
     */
    public TaiKhoan dangNhap(String tenDangNhap, String matKhau) {
        if (tenDangNhap == null || tenDangNhap.trim().isEmpty() || matKhau == null || matKhau.trim().isEmpty()) {
            throw new BusinessException("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
        }

        Optional<TaiKhoan> tkOpt = taiKhoanDAO.findByTenDangNhap(tenDangNhap.trim());
        if (tkOpt.isEmpty()) {
            throw new BusinessException("Tên đăng nhập hoặc mật khẩu không chính xác.");
        }

        TaiKhoan tk = tkOpt.get();
        if (!tk.isTrangThai()) {
            throw new BusinessException("Tài khoản này đã bị khóa. Vui lòng liên hệ Quản lý!");
        }

        if (!tk.kiemTraMatKhau(matKhau)) {
            throw new BusinessException("Tên đăng nhập hoặc mật khẩu không chính xác.");
        }

        // Đăng nhập thành công, lưu thông tin vào phiên làm việc
        taiKhoanHienTai = tk;
        nhanVienHienTai = nhanVienDAO.findByTenDangNhap(tk.getTenDangNhap()).orElse(null);

        return tk;
    }

    /**
     * Đổi mật khẩu tài khoản người dùng.
     */
    public void doiMatKhau(String tenDangNhap, String matKhauCu, String matKhauMoi, String xacNhanMatKhauMoi) {
        if (matKhauMoi == null || matKhauMoi.length() < 6) {
            throw new BusinessException("Mật khẩu mới phải có tối thiểu 6 ký tự.");
        }
        if (!matKhauMoi.equals(xacNhanMatKhauMoi)) {
            throw new BusinessException("Mật khẩu mới và xác nhận mật khẩu không trùng khớp.");
        }

        Optional<TaiKhoan> tkOpt = taiKhoanDAO.findByTenDangNhap(tenDangNhap);
        if (tkOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy tài khoản để đổi mật khẩu.");
        }

        TaiKhoan tk = tkOpt.get();
        if (!tk.kiemTraMatKhau(matKhauCu)) {
            throw new BusinessException("Mật khẩu hiện tại không chính xác.");
        }

        // Băm mật khẩu mới bằng BCrypt và lưu vào DB
        String matKhauHashMoi = BCrypt.hashpw(matKhauMoi, BCrypt.gensalt(10));
        boolean success = taiKhoanDAO.updateMatKhau(tenDangNhap, matKhauHashMoi);
        if (!success) {
            throw new BusinessException("Cập nhật mật khẩu thất bại, vui lòng thử lại.");
        }

        tk.setMatKhauHash(matKhauHashMoi);
    }

    /**
     * Đăng xuất khỏi hệ thống.
     */
    public void dangXuat() {
        taiKhoanHienTai = null;
        nhanVienHienTai = null;
    }

    public static TaiKhoan getTaiKhoanHienTai() {
        return taiKhoanHienTai;
    }

    public static NhanVien getNhanVienHienTai() {
        return nhanVienHienTai;
    }

    public static boolean isAdmin() {
        return taiKhoanHienTai != null && taiKhoanHienTai.getVaiTro() == com.qlks.model.enums.VaiTro.ADMIN;
    }
}
