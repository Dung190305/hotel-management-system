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

    /**
     * Bản ghi đại diện cho phiên người dùng bất biến (Immutable User Session),
     * đảm bảo đọc/ghi nguyên tử 100% không bao giờ bị lệch trạng thái giữa TaiKhoan và NhanVien.
     */
    public record UserSession(TaiKhoan taiKhoan, NhanVien nhanVien) {}

    // Con trỏ phiên làm việc nguyên tử (Atomic Reference qua volatile)
    private static volatile UserSession currentSession;

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

        // Đăng nhập thành công, lưu thông tin vào phiên làm việc nguyên tử
        NhanVien nv = nhanVienDAO.findByTenDangNhap(tk.getTenDangNhap()).orElse(null);
        setSession(tk, nv);

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
     * Thiết lập đồng thời phiên làm việc nguyên tử (Atomic Session Update).
     */
    public static synchronized void setSession(TaiKhoan tk, NhanVien nv) {
        currentSession = (tk == null && nv == null) ? null : new UserSession(tk, nv);
    }

    /**
     * Đăng xuất khỏi hệ thống an toàn đa luồng.
     */
    public static synchronized void dangXuat() {
        currentSession = null;
    }

    /**
     * Lấy snapshot phiên làm việc hiện tại một cách nguyên tử.
     */
    public static UserSession getSession() {
        return currentSession;
    }

    public static TaiKhoan getTaiKhoanHienTai() {
        UserSession s = currentSession;
        return s != null ? s.taiKhoan() : null;
    }

    public static synchronized void setTaiKhoanHienTai(TaiKhoan tk) {
        UserSession s = currentSession;
        NhanVien nv = (s != null) ? s.nhanVien() : null;
        currentSession = (tk == null && nv == null) ? null : new UserSession(tk, nv);
    }

    public static NhanVien getNhanVienHienTai() {
        UserSession s = currentSession;
        return s != null ? s.nhanVien() : null;
    }

    public static synchronized void setNhanVienHienTai(NhanVien nv) {
        UserSession s = currentSession;
        TaiKhoan tk = (s != null) ? s.taiKhoan() : null;
        currentSession = (tk == null && nv == null) ? null : new UserSession(tk, nv);
    }

    public static boolean isAdmin() {
        UserSession s = currentSession;
        return s != null && s.taiKhoan() != null && s.taiKhoan().getVaiTro() == com.qlks.model.enums.VaiTro.ADMIN;
    }
}
