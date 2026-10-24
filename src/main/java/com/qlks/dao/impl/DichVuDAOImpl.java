package com.qlks.dao.impl;

import com.qlks.dao.DichVuDAO;
import com.qlks.exception.DataAccessException;
import com.qlks.model.DichVu;
import com.qlks.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Triển khai JDBC thuần cho DichVuDAO.
 */
public class DichVuDAOImpl implements DichVuDAO {

    private DichVu mapResultSetToDichVu(ResultSet rs) throws SQLException {
        String ma = rs.getString("ma_dich_vu");
        String ten = rs.getString("ten_dich_vu");
        BigDecimal gia = rs.getBigDecimal("don_gia_hien_tai");
        boolean kinhDoanh = rs.getBoolean("dang_kinh_doanh");
        return new DichVu(ma, ten, gia, kinhDoanh);
    }

    @Override
    public Optional<DichVu> findById(String maDichVu) {
        String sql = "SELECT * FROM dich_vu WHERE ma_dich_vu = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maDichVu);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToDichVu(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tìm dịch vụ theo mã: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<DichVu> findDangKinhDoanh() {
        List<DichVu> list = new ArrayList<>();
        String sql = "SELECT * FROM dich_vu WHERE dang_kinh_doanh = TRUE ORDER BY ten_dich_vu ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToDichVu(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi lấy danh sách dịch vụ đang kinh doanh: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<DichVu> search(String keyword) {
        List<DichVu> list = new ArrayList<>();
        String sql = "SELECT * FROM dich_vu WHERE ten_dich_vu LIKE ? ORDER BY ten_dich_vu ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, "%" + (keyword != null ? keyword.trim() : "") + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDichVu(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tìm kiếm dịch vụ: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<DichVu> findAll() {
        List<DichVu> list = new ArrayList<>();
        String sql = "SELECT * FROM dich_vu ORDER BY ten_dich_vu ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToDichVu(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi lấy toàn bộ danh mục dịch vụ: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean save(DichVu dv) {
        String sql = "INSERT INTO dich_vu (ma_dich_vu, ten_dich_vu, don_gia_hien_tai, dang_kinh_doanh) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, dv.getMaDichVu());
            stmt.setString(2, dv.getTenDichVu());
            stmt.setBigDecimal(3, dv.getDonGiaHienTai());
            stmt.setBoolean(4, dv.isDangKinhDoanh());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi thêm mới dịch vụ: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(DichVu dv) {
        String sql = "UPDATE dich_vu SET ten_dich_vu = ?, don_gia_hien_tai = ?, dang_kinh_doanh = ? WHERE ma_dich_vu = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, dv.getTenDichVu());
            stmt.setBigDecimal(2, dv.getDonGiaHienTai());
            stmt.setBoolean(3, dv.isDangKinhDoanh());
            stmt.setString(4, dv.getMaDichVu());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi cập nhật dịch vụ: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(String maDichVu) {
        // Soft delete: chuyển dang_kinh_doanh = false
        String sql = "UPDATE dich_vu SET dang_kinh_doanh = FALSE WHERE ma_dich_vu = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maDichVu);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi ngừng kinh doanh dịch vụ: " + e.getMessage(), e);
        }
    }
}
