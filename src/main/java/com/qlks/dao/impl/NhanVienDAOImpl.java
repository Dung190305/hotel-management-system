package com.qlks.dao.impl;

import com.qlks.dao.NhanVienDAO;
import com.qlks.exception.DataAccessException;
import com.qlks.model.LeTan;
import com.qlks.model.NhanVien;
import com.qlks.model.QuanLy;
import com.qlks.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Triển khai JDBC thuần cho NhanVienDAO.
 * Minh họa tính Đa hình: Ánh xạ kết quả truy vấn thành đối tượng con cụ thể (LeTan hoặc QuanLy).
 */
public class NhanVienDAOImpl implements NhanVienDAO {

    private NhanVien mapResultSetToNhanVien(ResultSet rs) throws SQLException {
        String ma = rs.getString("ma_nhan_vien");
        String tenDangNhap = rs.getString("ten_dang_nhap");
        String hoTen = rs.getString("ho_ten");
        String sdt = rs.getString("so_dien_thoai");
        String email = rs.getString("email");
        String chucVu = rs.getString("chuc_vu");
        Date ngayVaoLam = rs.getDate("ngay_vao_lam");
        BigDecimal luongCoBan = rs.getBigDecimal("luong_co_ban");
        boolean dangHoatDong = rs.getBoolean("dang_hoat_dong");

        LocalDate ngayVl = (ngayVaoLam != null) ? ngayVaoLam.toLocalDate() : LocalDate.now();

        if ("QUAN_LY".equalsIgnoreCase(chucVu)) {
            double heSo = rs.getDouble("he_so_trach_nhiem");
            BigDecimal thuong = rs.getBigDecimal("thuong_hieu_qua");
            return new QuanLy(ma, hoTen, sdt, email, tenDangNhap, ngayVl, luongCoBan, dangHoatDong, heSo, thuong);
        } else {
            // Mặc định là Lễ tân
            int soCa = rs.getInt("so_ca_truc");
            BigDecimal thuongCa = rs.getBigDecimal("thuong_ca");
            return new LeTan(ma, hoTen, sdt, email, tenDangNhap, ngayVl, luongCoBan, dangHoatDong, soCa, thuongCa);
        }
    }

    @Override
    public Optional<NhanVien> findById(String maNhanVien) {
        String sql = "SELECT * FROM nhan_vien WHERE ma_nhan_vien = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maNhanVien);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToNhanVien(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tìm nhân viên theo mã: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<NhanVien> findByTenDangNhap(String tenDangNhap) {
        String sql = "SELECT * FROM nhan_vien WHERE ten_dang_nhap = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tenDangNhap);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToNhanVien(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tìm nhân viên theo tài khoản: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<NhanVien> search(String keyword) {
        List<NhanVien> list = new ArrayList<>();
        String sql = "SELECT * FROM nhan_vien WHERE ho_ten LIKE ? OR so_dien_thoai LIKE ? OR email LIKE ? ORDER BY ho_ten ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String kw = "%" + (keyword != null ? keyword.trim() : "") + "%";
            stmt.setString(1, kw);
            stmt.setString(2, kw);
            stmt.setString(3, kw);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToNhanVien(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tìm kiếm nhân viên: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<NhanVien> findAll() {
        List<NhanVien> list = new ArrayList<>();
        String sql = "SELECT * FROM nhan_vien ORDER BY ngay_vao_lam DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToNhanVien(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi lấy danh sách nhân viên: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean save(NhanVien nv) {
        String sql = "INSERT INTO nhan_vien (ma_nhan_vien, ten_dang_nhap, ho_ten, so_dien_thoai, email, chuc_vu, " +
                "ngay_vao_lam, luong_co_ban, so_ca_truc, thuong_ca, he_so_trach_nhiem, thuong_hieu_qua, dang_hoat_dong) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nv.getMaNhanVien());
            stmt.setString(2, nv.getTenDangNhap());
            stmt.setString(3, nv.getHoTen());
            stmt.setString(4, nv.getSoDienThoai());
            stmt.setString(5, nv.getEmail());
            stmt.setDate(7, Date.valueOf(nv.getNgayVaoLam() != null ? nv.getNgayVaoLam() : LocalDate.now()));
            stmt.setBigDecimal(8, nv.getLuongCoBan());
            stmt.setBoolean(13, nv.isDangHoatDong());

            if (nv instanceof QuanLy) {
                QuanLy ql = (QuanLy) nv;
                stmt.setString(6, "QUAN_LY");
                stmt.setInt(9, 0);
                stmt.setBigDecimal(10, BigDecimal.ZERO);
                stmt.setDouble(11, ql.getHeSoTrachNhiem());
                stmt.setBigDecimal(12, ql.getThuongHieuQua());
            } else if (nv instanceof LeTan) {
                LeTan lt = (LeTan) nv;
                stmt.setString(6, "LE_TAN");
                stmt.setInt(9, lt.getSoCaTruc());
                stmt.setBigDecimal(10, lt.getThuongCa());
                stmt.setDouble(11, 1.0);
                stmt.setBigDecimal(12, BigDecimal.ZERO);
            }

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi thêm nhân viên: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(NhanVien nv) {
        String sql = "UPDATE nhan_vien SET ho_ten = ?, so_dien_thoai = ?, email = ?, luong_co_ban = ?, " +
                "so_ca_truc = ?, thuong_ca = ?, he_so_trach_nhiem = ?, thuong_hieu_qua = ?, dang_hoat_dong = ? " +
                "WHERE ma_nhan_vien = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nv.getHoTen());
            stmt.setString(2, nv.getSoDienThoai());
            stmt.setString(3, nv.getEmail());
            stmt.setBigDecimal(4, nv.getLuongCoBan());
            stmt.setBoolean(9, nv.isDangHoatDong());
            stmt.setString(10, nv.getMaNhanVien());

            if (nv instanceof QuanLy) {
                QuanLy ql = (QuanLy) nv;
                stmt.setInt(5, 0);
                stmt.setBigDecimal(6, BigDecimal.ZERO);
                stmt.setDouble(7, ql.getHeSoTrachNhiem());
                stmt.setBigDecimal(8, ql.getThuongHieuQua());
            } else if (nv instanceof LeTan) {
                LeTan lt = (LeTan) nv;
                stmt.setInt(5, lt.getSoCaTruc());
                stmt.setBigDecimal(6, lt.getThuongCa());
                stmt.setDouble(7, 1.0);
                stmt.setBigDecimal(8, BigDecimal.ZERO);
            }

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi cập nhật thông tin nhân viên: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(String maNhanVien) {
        // Soft delete: chuyển dang_hoat_dong = false để bảo vệ tính toàn vẹn dữ liệu
        return updateTrangThaiHoatDong(maNhanVien, false);
    }

    @Override
    public boolean updateTrangThaiHoatDong(String maNhanVien, boolean dangHoatDong) {
        String sql = "UPDATE nhan_vien SET dang_hoat_dong = ? WHERE ma_nhan_vien = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, dangHoatDong);
            stmt.setString(2, maNhanVien);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi cập nhật trạng thái hoạt động của nhân viên: " + e.getMessage(), e);
        }
    }
}
