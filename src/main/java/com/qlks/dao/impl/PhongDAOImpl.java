package com.qlks.dao.impl;

import com.qlks.dao.PhongDAO;
import com.qlks.exception.DataAccessException;
import com.qlks.model.Phong;
import com.qlks.model.PhongThuong;
import com.qlks.model.PhongVIP;
import com.qlks.model.enums.TrangThaiPhong;
import com.qlks.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Triển khai JDBC thuần cho PhongDAO.
 * Minh họa tính Đa hình: Ánh xạ kết quả truy vấn thành PhongThuong hoặc PhongVIP.
 */
public class PhongDAOImpl implements PhongDAO {

    private Phong mapResultSetToPhong(ResultSet rs) throws SQLException {
        String ma = rs.getString("ma_phong");
        String so = rs.getString("so_phong");
        int tang = rs.getInt("tang");
        String loai = rs.getString("loai_phong");
        BigDecimal gia = rs.getBigDecimal("don_gia_ngay");
        TrangThaiPhong trangThai = TrangThaiPhong.fromString(rs.getString("trang_thai_hien_tai"));
        boolean kinhDoanh = rs.getBoolean("dang_kinh_doanh");

        com.qlks.model.enums.LoaiGiuong loaiGiuong = com.qlks.model.enums.LoaiGiuong.DON;
        try {
            String strGiuong = rs.getString("loai_giuong");
            if (strGiuong != null) {
                loaiGiuong = com.qlks.model.enums.LoaiGiuong.fromString(strGiuong);
            } else if (loai != null && loai.toUpperCase().contains("DOI")) {
                loaiGiuong = com.qlks.model.enums.LoaiGiuong.DOI;
            }
        } catch (SQLException ignored) {
            if (loai != null && loai.toUpperCase().contains("DOI")) {
                loaiGiuong = com.qlks.model.enums.LoaiGiuong.DOI;
            }
        }

        if (loai != null && loai.toUpperCase().contains("VIP")) {
            boolean viewBien = rs.getBoolean("co_ban_cong_view_bien");
            BigDecimal phiVip = rs.getBigDecimal("phi_dich_vu_vip");
            return new PhongVIP(ma, so, tang, gia, trangThai, loaiGiuong, kinhDoanh, viewBien, phiVip);
        } else {
            boolean coCuaSo = rs.getBoolean("co_cua_so");
            return new PhongThuong(ma, so, tang, gia, trangThai, loaiGiuong, kinhDoanh, coCuaSo);
        }
    }

    @Override
    public Optional<Phong> findById(String maPhong) {
        String sql = "SELECT * FROM phong WHERE ma_phong = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maPhong);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPhong(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tìm phòng theo mã: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Phong> findBySoPhong(String soPhong) {
        String sql = "SELECT * FROM phong WHERE so_phong = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, soPhong);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPhong(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tìm phòng theo số phòng: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Phong> findByTrangThai(TrangThaiPhong trangThai) {
        List<Phong> list = new ArrayList<>();
        String sql = "SELECT * FROM phong WHERE trang_thai_hien_tai = ? ORDER BY so_phong ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, trangThai.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPhong(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi lọc phòng theo trạng thái: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Phong> findAll() {
        List<Phong> list = new ArrayList<>();
        String sql = "SELECT * FROM phong ORDER BY tang ASC, so_phong ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToPhong(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi lấy danh sách phòng: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Phong> findPhongTrong(LocalDateTime checkIn, LocalDateTime checkOut) {
        List<Phong> list = new ArrayList<>();
        String sql = "SELECT p.* FROM phong p " +
                "WHERE p.dang_kinh_doanh = TRUE " +
                "AND p.trang_thai_hien_tai != 'MAINTENANCE' " +
                "AND NOT EXISTS ( " +
                "    SELECT 1 FROM dat_phong dp " +
                "    WHERE dp.ma_phong = p.ma_phong " +
                "    AND dp.trang_thai NOT IN ('DA_HUY', 'DA_TRA_PHONG') " +
                "    AND ? < dp.ngay_tra_phong AND ? > dp.ngay_nhan_phong " +
                ") " +
                "ORDER BY p.tang ASC, p.so_phong ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(checkIn));
            stmt.setTimestamp(2, Timestamp.valueOf(checkOut));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPhong(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tìm phòng trống theo thời gian: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean save(Phong phong) {
        String sql = "INSERT INTO phong (ma_phong, so_phong, tang, loai_phong, loai_giuong, suc_chua_toi_da, don_gia_ngay, " +
                "trang_thai_hien_tai, co_cua_so, co_ban_cong_view_bien, phi_dich_vu_vip, dang_kinh_doanh) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phong.getMaPhong());
            stmt.setString(2, phong.getSoPhong());
            stmt.setInt(3, phong.getTang());
            stmt.setString(4, phong.getLoaiPhong());
            stmt.setString(5, phong.getLoaiGiuong() != null ? phong.getLoaiGiuong().name() : "DON");
            stmt.setInt(6, phong.getSucChuaToiDa());
            stmt.setBigDecimal(7, phong.getDonGiaNgay());
            stmt.setString(8, phong.getTrangThaiHienTai().name());
            stmt.setBoolean(12, phong.isDangKinhDoanh());

            if (phong instanceof PhongVIP) {
                PhongVIP vip = (PhongVIP) phong;
                stmt.setBoolean(9, true);
                stmt.setBoolean(10, vip.isCoBanCongViewBien());
                stmt.setBigDecimal(11, vip.getPhiDichVuVip());
            } else if (phong instanceof PhongThuong) {
                PhongThuong thuong = (PhongThuong) phong;
                stmt.setBoolean(9, thuong.isCoCuaSo());
                stmt.setBoolean(10, false);
                stmt.setBigDecimal(11, BigDecimal.ZERO);
            }

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi thêm mới phòng: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Phong phong) {
        String sql = "UPDATE phong SET so_phong = ?, tang = ?, loai_phong = ?, loai_giuong = ?, suc_chua_toi_da = ?, " +
                "don_gia_ngay = ?, trang_thai_hien_tai = ?, " +
                "co_cua_so = ?, co_ban_cong_view_bien = ?, phi_dich_vu_vip = ?, dang_kinh_doanh = ? " +
                "WHERE ma_phong = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phong.getSoPhong());
            stmt.setInt(2, phong.getTang());
            stmt.setString(3, phong.getLoaiPhong());
            stmt.setString(4, phong.getLoaiGiuong() != null ? phong.getLoaiGiuong().name() : "DON");
            stmt.setInt(5, phong.getSucChuaToiDa());
            stmt.setBigDecimal(6, phong.getDonGiaNgay());
            stmt.setString(7, phong.getTrangThaiHienTai().name());
            stmt.setBoolean(11, phong.isDangKinhDoanh());
            stmt.setString(12, phong.getMaPhong());

            if (phong instanceof PhongVIP) {
                PhongVIP vip = (PhongVIP) phong;
                stmt.setBoolean(8, true);
                stmt.setBoolean(9, vip.isCoBanCongViewBien());
                stmt.setBigDecimal(10, vip.getPhiDichVuVip());
            } else if (phong instanceof PhongThuong) {
                PhongThuong thuong = (PhongThuong) phong;
                stmt.setBoolean(8, thuong.isCoCuaSo());
                stmt.setBoolean(9, false);
                stmt.setBigDecimal(10, BigDecimal.ZERO);
            }

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi cập nhật thông tin phòng: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateTrangThai(String maPhong, TrangThaiPhong trangThaiMoi) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return updateTrangThai(conn, maPhong, trangThaiMoi);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi cập nhật trạng thái phòng: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateTrangThai(Connection conn, String maPhong, TrangThaiPhong trangThaiMoi) {
        String sql = "UPDATE phong SET trang_thai_hien_tai = ? WHERE ma_phong = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, trangThaiMoi.name());
            stmt.setString(2, maPhong);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi cập nhật trạng thái phòng trong transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateTrangThaiCheckIn(Connection conn, String maPhong) {
        String sql = "UPDATE phong SET trang_thai_hien_tai = 'OCCUPIED' WHERE ma_phong = ? AND trang_thai_hien_tai IN ('AVAILABLE', 'RESERVED')";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maPhong);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi cập nhật trạng thái phòng khi Check-in: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Phong> findByIdForUpdate(Connection conn, String maPhong) {
        String sql = "SELECT * FROM phong WHERE ma_phong = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maPhong);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPhong(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi khóa dòng dữ liệu phòng (FOR UPDATE): " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public boolean updateKinhDoanh(String maPhong, boolean dangKinhDoanh) {
        String sql = "UPDATE phong SET dang_kinh_doanh = ? WHERE ma_phong = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, dangKinhDoanh);
            stmt.setString(2, maPhong);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi cập nhật trạng thái kinh doanh của phòng: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(String maPhong) {
        // Soft delete: chuyển dang_kinh_doanh = false
        return updateKinhDoanh(maPhong, false);
    }

    @Override
    @Deprecated
    public void dongBoTrangThaiTheoDon() {
        // No-op: Đã bị vô hiệu hóa để ngăn chặn Deadlock và Table-level lock khi truy vấn danh sách phòng.
    }
}
