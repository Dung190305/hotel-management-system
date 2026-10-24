package com.qlks.dao.impl;

import com.qlks.dao.DatPhongDAO;
import com.qlks.dao.KhachHangDAO;
import com.qlks.dao.NhanVienDAO;
import com.qlks.dao.PhongDAO;
import com.qlks.exception.DataAccessException;
import com.qlks.model.DatPhong;
import com.qlks.model.KhachHang;
import com.qlks.model.NhanVien;
import com.qlks.model.Phong;
import com.qlks.model.enums.TrangThaiDatPhong;
import com.qlks.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Triển khai JDBC thuần cho DatPhongDAO.
 * Xử lý thuật toán chống trùng lịch phòng và giao dịch đặt phòng.
 */
public class DatPhongDAOImpl implements DatPhongDAO {

    private final KhachHangDAO khachHangDAO = new KhachHangDAOImpl();
    private final PhongDAO phongDAO = new PhongDAOImpl();
    private final NhanVienDAO nhanVienDAO = new NhanVienDAOImpl();

    private DatPhong mapResultSetToDatPhong(ResultSet rs) throws SQLException {
        String maDatPhong = rs.getString("ma_dat_phong");
        String maKhachHang = rs.getString("ma_khach_hang");
        String maPhong = rs.getString("ma_phong");
        String maNhanVien = rs.getString("ma_nhan_vien");

        Timestamp tsNhan = rs.getTimestamp("ngay_nhan_phong");
        Timestamp tsTra = rs.getTimestamp("ngay_tra_phong");
        Timestamp tsNhanThucTe = rs.getTimestamp("thoi_gian_nhan_thuc_te");
        Timestamp tsTraThucTe = rs.getTimestamp("thoi_gian_tra_thuc_te");

        int soKhach = rs.getInt("so_luong_khach");
        TrangThaiDatPhong trangThai = TrangThaiDatPhong.fromString(rs.getString("trang_thai"));
        BigDecimal tienCoc = rs.getBigDecimal("tien_coc");
        Timestamp tsTao = rs.getTimestamp("ngay_tao");

        KhachHang kh = khachHangDAO.findById(maKhachHang).orElse(null);
        Phong phong = phongDAO.findById(maPhong).orElse(null);
        NhanVien nv = nhanVienDAO.findById(maNhanVien).orElse(null);

        return new DatPhong(maDatPhong, kh, phong, nv,
                tsNhan != null ? tsNhan.toLocalDateTime() : null,
                tsTra != null ? tsTra.toLocalDateTime() : null,
                tsNhanThucTe != null ? tsNhanThucTe.toLocalDateTime() : null,
                tsTraThucTe != null ? tsTraThucTe.toLocalDateTime() : null,
                soKhach, trangThai, tienCoc,
                tsTao != null ? tsTao.toLocalDateTime() : null);
    }

    @Override
    public Optional<DatPhong> findById(String maDatPhong) {
        String sql = "SELECT * FROM dat_phong WHERE ma_dat_phong = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maDatPhong);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToDatPhong(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tìm phiếu đặt phòng theo mã: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<DatPhong> findByKhachHang(String maKhachHang) {
        List<DatPhong> list = new ArrayList<>();
        String sql = "SELECT * FROM dat_phong WHERE ma_khach_hang = ? ORDER BY ngay_nhan_phong DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maKhachHang);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDatPhong(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi lấy lịch sử đặt phòng của khách: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<DatPhong> findByPhong(String maPhong) {
        List<DatPhong> list = new ArrayList<>();
        String sql = "SELECT * FROM dat_phong WHERE ma_phong = ? ORDER BY ngay_nhan_phong DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maPhong);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDatPhong(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi lấy danh sách đặt phòng của phòng: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<DatPhong> findByTrangThai(TrangThaiDatPhong trangThai) {
        List<DatPhong> list = new ArrayList<>();
        String sql = "SELECT * FROM dat_phong WHERE trang_thai = ? ORDER BY ngay_nhan_phong ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, trangThai.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToDatPhong(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi lọc danh sách đặt phòng theo trạng thái: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<DatPhong> findAll() {
        List<DatPhong> list = new ArrayList<>();
        String sql = "SELECT * FROM dat_phong ORDER BY ngay_tao DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToDatPhong(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi lấy toàn bộ danh sách đặt phòng: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean save(DatPhong dp) {
        String sql = "INSERT INTO dat_phong (ma_dat_phong, ma_khach_hang, ma_phong, ma_nhan_vien, " +
                "ngay_nhan_phong, ngay_tra_phong, thoi_gian_nhan_thuc_te, thoi_gian_tra_thuc_te, " +
                "so_luong_khach, trang_thai, tien_coc, ngay_tao) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, dp.getMaDatPhong());
            stmt.setString(2, dp.getKhachHang() != null ? dp.getKhachHang().getMaKhachHang() : null);
            stmt.setString(3, dp.getPhong() != null ? dp.getPhong().getMaPhong() : null);
            stmt.setString(4, dp.getNhanVienLap() != null ? dp.getNhanVienLap().getMaNhanVien() : null);
            stmt.setTimestamp(5, Timestamp.valueOf(dp.getNgayNhanPhong()));
            stmt.setTimestamp(6, Timestamp.valueOf(dp.getNgayTraPhong()));
            stmt.setTimestamp(7, dp.getThoiGianNhanThucTe() != null ? Timestamp.valueOf(dp.getThoiGianNhanThucTe()) : null);
            stmt.setTimestamp(8, dp.getThoiGianTraThucTe() != null ? Timestamp.valueOf(dp.getThoiGianTraThucTe()) : null);
            stmt.setInt(9, dp.getSoLuongKhach());
            stmt.setString(10, dp.getTrangThai().name());
            stmt.setBigDecimal(11, dp.getTienCoc());
            stmt.setTimestamp(12, Timestamp.valueOf(dp.getNgayTao() != null ? dp.getNgayTao() : LocalDateTime.now()));
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi lưu phiếu đặt phòng: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(DatPhong dp) {
        String sql = "UPDATE dat_phong SET ma_khach_hang = ?, ma_phong = ?, ma_nhan_vien = ?, " +
                "ngay_nhan_phong = ?, ngay_tra_phong = ?, thoi_gian_nhan_thuc_te = ?, thoi_gian_tra_thuc_te = ?, " +
                "so_luong_khach = ?, trang_thai = ?, tien_coc = ? " +
                "WHERE ma_dat_phong = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, dp.getKhachHang() != null ? dp.getKhachHang().getMaKhachHang() : null);
            stmt.setString(2, dp.getPhong() != null ? dp.getPhong().getMaPhong() : null);
            stmt.setString(3, dp.getNhanVienLap() != null ? dp.getNhanVienLap().getMaNhanVien() : null);
            stmt.setTimestamp(4, Timestamp.valueOf(dp.getNgayNhanPhong()));
            stmt.setTimestamp(5, Timestamp.valueOf(dp.getNgayTraPhong()));
            stmt.setTimestamp(6, dp.getThoiGianNhanThucTe() != null ? Timestamp.valueOf(dp.getThoiGianNhanThucTe()) : null);
            stmt.setTimestamp(7, dp.getThoiGianTraThucTe() != null ? Timestamp.valueOf(dp.getThoiGianTraThucTe()) : null);
            stmt.setInt(8, dp.getSoLuongKhach());
            stmt.setString(9, dp.getTrangThai().name());
            stmt.setBigDecimal(10, dp.getTienCoc());
            stmt.setString(11, dp.getMaDatPhong());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi cập nhật phiếu đặt phòng: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(String maDatPhong) {
        // Hủy đặt phòng bằng cách cập nhật trạng thái DA_HUY
        return updateTrangThai(maDatPhong, TrangThaiDatPhong.DA_HUY);
    }

    @Override
    public boolean updateTrangThai(String maDatPhong, TrangThaiDatPhong trangThaiMoi) {
        String sql = "UPDATE dat_phong SET trang_thai = ? WHERE ma_dat_phong = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, trangThaiMoi.name());
            stmt.setString(2, maDatPhong);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi cập nhật trạng thái đặt phòng: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean xacNhanCheckIn(String maDatPhong, LocalDateTime thoiGianCheckInThucTe) {
        String sql = "UPDATE dat_phong SET thoi_gian_nhan_thuc_te = ?, trang_thai = 'DANG_O' WHERE ma_dat_phong = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(thoiGianCheckInThucTe));
            stmt.setString(2, maDatPhong);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi xác nhận Check-in: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean xacNhanCheckOut(String maDatPhong, LocalDateTime thoiGianCheckOutThucTe) {
        String sql = "UPDATE dat_phong SET thoi_gian_tra_thuc_te = ?, trang_thai = 'DA_TRA_PHONG' WHERE ma_dat_phong = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(thoiGianCheckOutThucTe));
            stmt.setString(2, maDatPhong);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi xác nhận Check-out: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isPhongBiTrungLich(String maPhong, LocalDateTime ngayNhanMoi, LocalDateTime ngayTraMoi, String excludeMaDatPhong) {
        StringBuilder sql = new StringBuilder(
                "SELECT COUNT(*) FROM dat_phong " +
                "WHERE ma_phong = ? " +
                "AND trang_thai NOT IN ('DA_HUY', 'DA_TRA_PHONG') " +
                "AND ? < ngay_tra_phong AND ? > ngay_nhan_phong"
        );

        if (excludeMaDatPhong != null && !excludeMaDatPhong.trim().isEmpty()) {
            sql.append(" AND ma_dat_phong != ?");
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            stmt.setString(1, maPhong);
            stmt.setTimestamp(2, Timestamp.valueOf(ngayNhanMoi));
            stmt.setTimestamp(3, Timestamp.valueOf(ngayTraMoi));
            if (excludeMaDatPhong != null && !excludeMaDatPhong.trim().isEmpty()) {
                stmt.setString(4, excludeMaDatPhong);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0; // > 0 tức là BỊ TRÙNG LỊCH
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi kiểm tra trùng lịch phòng: " + e.getMessage(), e);
        }
        return false;
    }
}
