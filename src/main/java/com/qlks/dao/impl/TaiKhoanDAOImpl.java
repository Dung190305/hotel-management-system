package com.qlks.dao.impl;

import com.qlks.dao.TaiKhoanDAO;
import com.qlks.exception.DataAccessException;
import com.qlks.model.TaiKhoan;
import com.qlks.model.enums.VaiTro;
import com.qlks.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Triển khai JDBC thuần cho TaiKhoanDAO.
 */
public class TaiKhoanDAOImpl implements TaiKhoanDAO {

    private TaiKhoan mapResultSetToTaiKhoan(ResultSet rs) throws SQLException {
        String tenDangNhap = rs.getString("ten_dang_nhap");
        String matKhauHash = rs.getString("mat_khau_hash");
        VaiTro vaiTro = VaiTro.fromString(rs.getString("vai_tro"));
        boolean trangThai = rs.getBoolean("trang_thai");
        Timestamp ts = rs.getTimestamp("ngay_tao");
        return new TaiKhoan(tenDangNhap, matKhauHash, vaiTro, trangThai, ts != null ? ts.toLocalDateTime() : null);
    }

    @Override
    public Optional<TaiKhoan> findById(String tenDangNhap) {
        return findByTenDangNhap(tenDangNhap);
    }

    @Override
    public Optional<TaiKhoan> findByTenDangNhap(String tenDangNhap) {
        String sql = "SELECT ten_dang_nhap, mat_khau_hash, vai_tro, trang_thai, ngay_tao FROM tai_khoan WHERE ten_dang_nhap = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tenDangNhap);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTaiKhoan(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tìm tài khoản theo tên đăng nhập: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<TaiKhoan> findAll() {
        List<TaiKhoan> list = new ArrayList<>();
        String sql = "SELECT ten_dang_nhap, mat_khau_hash, vai_tro, trang_thai, ngay_tao FROM tai_khoan ORDER BY ngay_tao DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToTaiKhoan(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi lấy danh sách tài khoản: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean save(TaiKhoan tk) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return save(conn, tk);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tạo mới tài khoản: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean save(Connection conn, TaiKhoan tk) {
        String sql = "INSERT INTO tai_khoan (ten_dang_nhap, mat_khau_hash, vai_tro, trang_thai, ngay_tao) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tk.getTenDangNhap());
            stmt.setString(2, tk.getMatKhauHash());
            stmt.setString(3, tk.getVaiTro().name());
            stmt.setBoolean(4, tk.isTrangThai());
            stmt.setTimestamp(5, Timestamp.valueOf(tk.getNgayTao() != null ? tk.getNgayTao() : java.time.LocalDateTime.now()));
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tạo mới tài khoản: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(TaiKhoan tk) {
        String sql = "UPDATE tai_khoan SET mat_khau_hash = ?, vai_tro = ?, trang_thai = ? WHERE ten_dang_nhap = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tk.getMatKhauHash());
            stmt.setString(2, tk.getVaiTro().name());
            stmt.setBoolean(3, tk.isTrangThai());
            stmt.setString(4, tk.getTenDangNhap());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi cập nhật tài khoản: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(String tenDangNhap) {
        String sql = "DELETE FROM tai_khoan WHERE ten_dang_nhap = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tenDangNhap);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi xóa tài khoản: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateMatKhau(String tenDangNhap, String matKhauHashMoi) {
        String sql = "UPDATE tai_khoan SET mat_khau_hash = ? WHERE ten_dang_nhap = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, matKhauHashMoi);
            stmt.setString(2, tenDangNhap);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi cập nhật mật khẩu: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateTrangThai(String tenDangNhap, boolean trangThai) {
        String sql = "UPDATE tai_khoan SET trang_thai = ? WHERE ten_dang_nhap = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, trangThai);
            stmt.setString(2, tenDangNhap);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi cập nhật trạng thái tài khoản: " + e.getMessage(), e);
        }
    }
}
