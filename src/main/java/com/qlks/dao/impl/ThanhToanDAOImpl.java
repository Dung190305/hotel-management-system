package com.qlks.dao.impl;

import com.qlks.dao.ThanhToanDAO;
import com.qlks.exception.DataAccessException;
import com.qlks.model.ThanhToan;
import com.qlks.model.enums.PhuongThucThanhToan;
import com.qlks.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Triển khai JDBC thuần cho ThanhToanDAO.
 */
public class ThanhToanDAOImpl implements ThanhToanDAO {

    private ThanhToan mapResultSetToThanhToan(ResultSet rs) throws SQLException {
        String maThanhToan = rs.getString("ma_thanh_toan");
        String maHoaDon = rs.getString("ma_hoa_don");
        BigDecimal soTien = rs.getBigDecimal("so_tien");
        PhuongThucThanhToan phuongThuc = PhuongThucThanhToan.fromString(rs.getString("phuong_thuc"));
        Timestamp ts = rs.getTimestamp("thoi_gian_giao_dich");
        String ghiChu = rs.getString("ghi_chu");

        return new ThanhToan(maThanhToan, maHoaDon, soTien, phuongThuc,
                ts != null ? ts.toLocalDateTime() : null, ghiChu);
    }

    @Override
    public List<ThanhToan> findByMaHoaDon(String maHoaDon) {
        List<ThanhToan> list = new ArrayList<>();
        String sql = "SELECT * FROM thanh_toan WHERE ma_hoa_don = ? ORDER BY thoi_gian_giao_dich ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maHoaDon);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToThanhToan(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi lấy lịch sử thanh toán: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean save(ThanhToan tt) {
        String sql = "INSERT INTO thanh_toan (ma_thanh_toan, ma_hoa_don, so_tien, phuong_thuc, thoi_gian_giao_dich, ghi_chu) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tt.getMaThanhToan());
            stmt.setString(2, tt.getMaHoaDon());
            stmt.setBigDecimal(3, tt.getSoTien());
            stmt.setString(4, tt.getPhuongThuc().name());
            stmt.setTimestamp(5, Timestamp.valueOf(tt.getThoiGianGiaoDich() != null ? tt.getThoiGianGiaoDich() : LocalDateTime.now()));
            stmt.setString(6, tt.getGhiChu());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi lưu giao dịch thanh toán: " + e.getMessage(), e);
        }
    }

    @Override
    public BigDecimal tinhTongTienDaThanhToan(String maHoaDon) {
        String sql = "SELECT COALESCE(SUM(so_tien), 0) FROM thanh_toan WHERE ma_hoa_don = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maHoaDon);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal(1);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tính tổng tiền đã thanh toán: " + e.getMessage(), e);
        }
        return BigDecimal.ZERO;
    }
}
