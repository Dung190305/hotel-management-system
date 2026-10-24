package com.qlks.dao.impl;

import com.qlks.dao.ChiTietDichVuDAO;
import com.qlks.dao.DichVuDAO;
import com.qlks.exception.DataAccessException;
import com.qlks.model.ChiTietDichVu;
import com.qlks.model.DichVu;
import com.qlks.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Triển khai JDBC thuần cho ChiTietDichVuDAO.
 */
public class ChiTietDichVuDAOImpl implements ChiTietDichVuDAO {

    private final DichVuDAO dichVuDAO = new DichVuDAOImpl();

    private ChiTietDichVu mapResultSetToChiTiet(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String maHoaDon = rs.getString("ma_hoa_don");
        String maDichVu = rs.getString("ma_dich_vu");
        int soLuong = rs.getInt("so_luong");
        BigDecimal donGiaLucDung = rs.getBigDecimal("don_gia_luc_dung");
        Timestamp ts = rs.getTimestamp("thoi_gian_su_dung");

        DichVu dv = dichVuDAO.findById(maDichVu).orElse(null);

        return new ChiTietDichVu(id, maHoaDon, dv, soLuong, donGiaLucDung, ts != null ? ts.toLocalDateTime() : null);
    }

    @Override
    public List<ChiTietDichVu> findByMaHoaDon(String maHoaDon) {
        List<ChiTietDichVu> list = new ArrayList<>();
        String sql = "SELECT * FROM chi_tiet_dich_vu WHERE ma_hoa_don = ? ORDER BY thoi_gian_su_dung ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maHoaDon);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToChiTiet(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi lấy danh sách dịch vụ của hóa đơn: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean save(ChiTietDichVu ctdv) {
        String sql = "INSERT INTO chi_tiet_dich_vu (ma_hoa_don, ma_dich_vu, so_luong, don_gia_luc_dung, thoi_gian_su_dung) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, ctdv.getMaHoaDon());
            stmt.setString(2, ctdv.getDichVu() != null ? ctdv.getDichVu().getMaDichVu() : null);
            stmt.setInt(3, ctdv.getSoLuong());
            stmt.setBigDecimal(4, ctdv.getDonGiaLucDung());
            stmt.setTimestamp(5, Timestamp.valueOf(ctdv.getThoiGianSuDung() != null ? ctdv.getThoiGianSuDung() : LocalDateTime.now()));

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        ctdv.setId(generatedKeys.getLong(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi thêm dịch vụ vào hóa đơn: " + e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM chi_tiet_dich_vu WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi xóa chi tiết dịch vụ: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteByMaHoaDon(String maHoaDon) {
        String sql = "DELETE FROM chi_tiet_dich_vu WHERE ma_hoa_don = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maHoaDon);
            return stmt.executeUpdate() >= 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi xóa dịch vụ theo hóa đơn: " + e.getMessage(), e);
        }
    }
}
