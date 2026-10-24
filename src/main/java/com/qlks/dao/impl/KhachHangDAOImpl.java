package com.qlks.dao.impl;

import com.qlks.dao.KhachHangDAO;
import com.qlks.exception.DataAccessException;
import com.qlks.model.KhachHang;
import com.qlks.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Triển khai JDBC thuần cho KhachHangDAO.
 */
public class KhachHangDAOImpl implements KhachHangDAO {

    private KhachHang mapResultSetToKhachHang(ResultSet rs) throws SQLException {
        String ma = rs.getString("ma_khach_hang");
        String ten = rs.getString("ho_ten");
        String sdt = rs.getString("so_dien_thoai");
        String email = rs.getString("email");
        String cccd = rs.getString("so_cccd");
        Date ngayDk = rs.getDate("ngay_dang_ky");
        return new KhachHang(ma, ten, sdt, email, cccd, ngayDk != null ? ngayDk.toLocalDate() : LocalDate.now());
    }

    @Override
    public Optional<KhachHang> findById(String maKhachHang) {
        String sql = "SELECT ma_khach_hang, ho_ten, so_dien_thoai, email, so_cccd, ngay_dang_ky FROM khach_hang WHERE ma_khach_hang = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maKhachHang);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToKhachHang(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tìm khách hàng theo mã: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<KhachHang> findBySoCCCD(String soCCCD) {
        String sql = "SELECT ma_khach_hang, ho_ten, so_dien_thoai, email, so_cccd, ngay_dang_ky FROM khach_hang WHERE so_cccd = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, soCCCD);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToKhachHang(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tìm khách hàng theo CCCD: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<KhachHang> findBySoDienThoai(String soDienThoai) {
        String sql = "SELECT ma_khach_hang, ho_ten, so_dien_thoai, email, so_cccd, ngay_dang_ky FROM khach_hang WHERE so_dien_thoai = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, soDienThoai);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToKhachHang(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tìm khách hàng theo số điện thoại: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<KhachHang> search(String keyword) {
        List<KhachHang> list = new ArrayList<>();
        String sql = "SELECT ma_khach_hang, ho_ten, so_dien_thoai, email, so_cccd, ngay_dang_ky FROM khach_hang " +
                "WHERE ho_ten LIKE ? OR so_dien_thoai LIKE ? OR so_cccd LIKE ? ORDER BY ho_ten ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String kw = "%" + (keyword != null ? keyword.trim() : "") + "%";
            stmt.setString(1, kw);
            stmt.setString(2, kw);
            stmt.setString(3, kw);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToKhachHang(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tìm kiếm khách hàng: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<KhachHang> findAll() {
        List<KhachHang> list = new ArrayList<>();
        String sql = "SELECT ma_khach_hang, ho_ten, so_dien_thoai, email, so_cccd, ngay_dang_ky FROM khach_hang ORDER BY ngay_dang_ky DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToKhachHang(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi lấy danh sách khách hàng: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean save(KhachHang kh) {
        String sql = "INSERT INTO khach_hang (ma_khach_hang, ho_ten, so_dien_thoai, email, so_cccd, ngay_dang_ky) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, kh.getMaKhachHang());
            stmt.setString(2, kh.getHoTen());
            stmt.setString(3, kh.getSoDienThoai());
            stmt.setString(4, kh.getEmail());
            stmt.setString(5, kh.getSoCCCD());
            stmt.setDate(6, Date.valueOf(kh.getNgayDangKy() != null ? kh.getNgayDangKy() : LocalDate.now()));
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi thêm mới khách hàng: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(KhachHang kh) {
        String sql = "UPDATE khach_hang SET ho_ten = ?, so_dien_thoai = ?, email = ?, so_cccd = ? WHERE ma_khach_hang = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, kh.getHoTen());
            stmt.setString(2, kh.getSoDienThoai());
            stmt.setString(3, kh.getEmail());
            stmt.setString(4, kh.getSoCCCD());
            stmt.setString(5, kh.getMaKhachHang());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi cập nhật khách hàng: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(String maKhachHang) {
        String sql = "DELETE FROM khach_hang WHERE ma_khach_hang = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maKhachHang);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Không thể xóa khách hàng đã có lịch sử đặt phòng/lưu trú: " + e.getMessage(), e);
        }
    }
}
