package com.qlks.dao.impl;

import com.qlks.dao.KhachLuuTruDAO;
import com.qlks.exception.DataAccessException;
import com.qlks.model.KhachLuuTru;
import com.qlks.model.enums.VaiTroKhachLuuTru;
import com.qlks.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Triển khai JDBC thuần cho KhachLuuTruDAO.
 */
public class KhachLuuTruDAOImpl implements KhachLuuTruDAO {

    private KhachLuuTru mapResultSet(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String maDatPhong = rs.getString("ma_dat_phong");
        String hoTen = rs.getString("ho_ten");
        String soCCCD = rs.getString("so_cccd");
        String sdt = rs.getString("so_dien_thoai");
        VaiTroKhachLuuTru vaiTro = VaiTroKhachLuuTru.fromString(rs.getString("vai_tro"));

        Timestamp ts = rs.getTimestamp("ngay_tao");
        LocalDateTime ngayTao = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        return new KhachLuuTru(id, maDatPhong, hoTen, soCCCD, sdt, vaiTro, ngayTao);
    }

    @Override
    public List<KhachLuuTru> findByMaDatPhong(String maDatPhong) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findByMaDatPhong(conn, maDatPhong);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi kết nối khi tìm khách lưu trú: " + e.getMessage(), e);
        }
    }

    @Override
    public List<KhachLuuTru> findByMaDatPhong(Connection conn, String maDatPhong) {
        List<KhachLuuTru> list = new ArrayList<>();
        String sql = "SELECT * FROM chi_tiet_khach_dat_phong WHERE ma_dat_phong = ? ORDER BY id ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maDatPhong);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tìm khách lưu trú theo mã đặt phòng: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean save(KhachLuuTru khach) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return save(conn, khach);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi kết nối khi lưu khách lưu trú: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean save(Connection conn, KhachLuuTru khach) {
        String sql = "INSERT INTO chi_tiet_khach_dat_phong (ma_dat_phong, ho_ten, so_cccd, so_dien_thoai, vai_tro) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, khach.getMaDatPhong());
            stmt.setString(2, khach.getHoTen());
            stmt.setString(3, khach.getSoCCCD());
            stmt.setString(4, khach.getSoDienThoai());
            stmt.setString(5, khach.getVaiTro() != null ? khach.getVaiTro().name() : VaiTroKhachLuuTru.KHACH_DI_CUNG.name());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        khach.setId(rs.getLong(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi lưu thông tin khách lưu trú vào CSDL: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean saveAll(Connection conn, String maDatPhong, List<KhachLuuTru> danhSachKhach) {
        if (danhSachKhach == null || danhSachKhach.isEmpty()) {
            return true;
        }
        for (KhachLuuTru khach : danhSachKhach) {
            khach.setMaDatPhong(maDatPhong);
            boolean ok = save(conn, khach);
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean deleteByMaDatPhong(Connection conn, String maDatPhong) {
        String sql = "DELETE FROM chi_tiet_khach_dat_phong WHERE ma_dat_phong = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maDatPhong);
            return stmt.executeUpdate() >= 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi xóa khách lưu trú theo mã đặt phòng: " + e.getMessage(), e);
        }
    }
}
