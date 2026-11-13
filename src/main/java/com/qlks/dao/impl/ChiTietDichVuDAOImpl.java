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
        String maDatPhong = null;
        try {
            maDatPhong = rs.getString("ma_dat_phong");
        } catch (Exception e) {
            maDatPhong = rs.getString("ma_hoa_don");
        }
        if (maDatPhong == null) {
            try {
                maDatPhong = rs.getString("ma_hoa_don");
            } catch (Exception ignored) {
            }
        }
        String maDichVu = rs.getString("ma_dich_vu");
        int soLuong = rs.getInt("so_luong");
        BigDecimal donGiaLucDung = rs.getBigDecimal("don_gia_luc_dung");
        Timestamp ts = rs.getTimestamp("thoi_gian_su_dung");

        DichVu dv = dichVuDAO.findById(maDichVu).orElse(null);

        return new ChiTietDichVu(id, maDatPhong, dv, soLuong, donGiaLucDung, ts != null ? ts.toLocalDateTime() : null);
    }

    @Override
    public List<ChiTietDichVu> findByMaDatPhong(String maDatPhong) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findByMaDatPhong(conn, maDatPhong);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi lấy danh sách dịch vụ của phiếu đặt phòng: " + e.getMessage(), e);
        }
    }

    @Override
    public List<ChiTietDichVu> findByMaDatPhong(Connection conn, String maDatPhong) {
        List<ChiTietDichVu> list = new ArrayList<>();
        String sql = "SELECT * FROM chi_tiet_dich_vu WHERE ma_dat_phong = ? ORDER BY thoi_gian_su_dung ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maDatPhong);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToChiTiet(rs));
                }
            }
        } catch (SQLException e) {
            // Fallback thử tìm bằng ma_hoa_don nếu cấu trúc DB cũ chưa chạy migration
            try {
                String sqlFallback = "SELECT * FROM chi_tiet_dich_vu WHERE ma_hoa_don = ? ORDER BY thoi_gian_su_dung ASC";
                try (PreparedStatement stmtFb = conn.prepareStatement(sqlFallback)) {
                    stmtFb.setString(1, maDatPhong);
                    try (ResultSet rs = stmtFb.executeQuery()) {
                        while (rs.next()) {
                            list.add(mapResultSetToChiTiet(rs));
                        }
                    }
                }
            } catch (SQLException ex2) {
                throw new DataAccessException("Lỗi khi lấy danh sách dịch vụ trong transaction: " + e.getMessage(), e);
            }
        }
        return list;
    }

    @Override
    public List<ChiTietDichVu> findByMaDatPhongForUpdate(Connection conn, String maDatPhong) {
        List<ChiTietDichVu> list = new ArrayList<>();
        String sql = "SELECT * FROM chi_tiet_dich_vu WHERE ma_dat_phong = ? ORDER BY thoi_gian_su_dung ASC FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maDatPhong);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToChiTiet(rs));
                }
            }
        } catch (SQLException e) {
            // Fallback thử tìm bằng ma_hoa_don nếu cấu trúc DB cũ chưa chạy migration
            try {
                String sqlFallback = "SELECT * FROM chi_tiet_dich_vu WHERE ma_hoa_don = ? ORDER BY thoi_gian_su_dung ASC FOR UPDATE";
                try (PreparedStatement stmtFb = conn.prepareStatement(sqlFallback)) {
                    stmtFb.setString(1, maDatPhong);
                    try (ResultSet rs = stmtFb.executeQuery()) {
                        while (rs.next()) {
                            list.add(mapResultSetToChiTiet(rs));
                        }
                    }
                }
            } catch (SQLException ex2) {
                throw new DataAccessException("Lỗi khi khóa dòng danh sách dịch vụ (FOR UPDATE): " + e.getMessage(), e);
            }
        }
        return list;
    }

    @Override
    public List<ChiTietDichVu> findByMaHoaDon(String maHoaDon) {
        return findByMaDatPhong(maHoaDon);
    }

    @Override
    public List<ChiTietDichVu> findByMaHoaDon(Connection conn, String maHoaDon) {
        return findByMaDatPhong(conn, maHoaDon);
    }

    @Override
    public boolean save(ChiTietDichVu ctdv) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return save(conn, ctdv);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi thêm dịch vụ: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean save(Connection conn, ChiTietDichVu ctdv) {
        String sql = "INSERT INTO chi_tiet_dich_vu (ma_dat_phong, ma_dich_vu, so_luong, don_gia_luc_dung, thoi_gian_su_dung) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, ctdv.getMaDatPhong());
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
            // Fallback thử chèn bằng ma_hoa_don nếu cấu trúc DB cũ
            try {
                String sqlFb = "INSERT INTO chi_tiet_dich_vu (ma_hoa_don, ma_dich_vu, so_luong, don_gia_luc_dung, thoi_gian_su_dung) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement stmtFb = conn.prepareStatement(sqlFb, Statement.RETURN_GENERATED_KEYS)) {
                    stmtFb.setString(1, ctdv.getMaDatPhong());
                    stmtFb.setString(2, ctdv.getDichVu() != null ? ctdv.getDichVu().getMaDichVu() : null);
                    stmtFb.setInt(3, ctdv.getSoLuong());
                    stmtFb.setBigDecimal(4, ctdv.getDonGiaLucDung());
                    stmtFb.setTimestamp(5, Timestamp.valueOf(ctdv.getThoiGianSuDung() != null ? ctdv.getThoiGianSuDung() : LocalDateTime.now()));
                    int aff = stmtFb.executeUpdate();
                    if (aff > 0) {
                        try (ResultSet gk = stmtFb.getGeneratedKeys()) {
                            if (gk.next()) ctdv.setId(gk.getLong(1));
                        }
                        return true;
                    }
                }
            } catch (SQLException ex2) {
                throw new DataAccessException("Lỗi khi thêm dịch vụ trong transaction: " + e.getMessage(), e);
            }
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
    public boolean deleteByMaDatPhong(String maDatPhong) {
        String sql = "DELETE FROM chi_tiet_dich_vu WHERE ma_dat_phong = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maDatPhong);
            return stmt.executeUpdate() >= 0;
        } catch (SQLException e) {
            return deleteByMaHoaDon(maDatPhong);
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
