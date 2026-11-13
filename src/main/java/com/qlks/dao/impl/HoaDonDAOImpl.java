package com.qlks.dao.impl;

import com.qlks.dao.ChiTietDichVuDAO;
import com.qlks.dao.DatPhongDAO;
import com.qlks.dao.HoaDonDAO;
import com.qlks.dao.NhanVienDAO;
import com.qlks.exception.DataAccessException;
import com.qlks.model.DatPhong;
import com.qlks.model.HoaDon;
import com.qlks.model.NhanVien;
import com.qlks.model.enums.TrangThaiThanhToan;
import com.qlks.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Triển khai JDBC thuần cho HoaDonDAO.
 * Nạp quan hệ Composition giữa HoaDon và ChiTietDichVu.
 */
public class HoaDonDAOImpl implements HoaDonDAO {

    private final DatPhongDAO datPhongDAO = new DatPhongDAOImpl();
    private final NhanVienDAO nhanVienDAO = new NhanVienDAOImpl();
    private final ChiTietDichVuDAO chiTietDichVuDAO = new ChiTietDichVuDAOImpl();

    private HoaDon mapResultSetToHoaDon(ResultSet rs) throws SQLException {
        String maHoaDon = rs.getString("ma_hoa_don");
        String maDatPhong = rs.getString("ma_dat_phong");
        String maNhanVien = rs.getString("ma_nhan_vien_lap");
        Timestamp tsLap = rs.getTimestamp("ngay_lap");
        BigDecimal tienPhong = rs.getBigDecimal("tien_phong");
        BigDecimal tienDichVu = rs.getBigDecimal("tien_dich_vu");
        BigDecimal tienGiamGia = rs.getBigDecimal("tien_giam_gia");
        BigDecimal tienCoc = BigDecimal.ZERO;
        try {
            BigDecimal tc = rs.getBigDecimal("tien_coc");
            if (tc != null) tienCoc = tc;
        } catch (SQLException ignored) {
        }
        BigDecimal tongTien = rs.getBigDecimal("tong_tien");
        TrangThaiThanhToan trangThai = TrangThaiThanhToan.fromString(rs.getString("trang_thai"));

        DatPhong dp = datPhongDAO.findById(maDatPhong).orElse(null);
        NhanVien nv = nhanVienDAO.findById(maNhanVien).orElse(null);

        HoaDon hd = new HoaDon(maHoaDon, dp, nv,
                tsLap != null ? tsLap.toLocalDateTime() : null,
                tienPhong, tienDichVu, tienGiamGia, tienCoc, tongTien, trangThai);

        // Nạp danh sách dịch vụ đi kèm (gắn theo phiếu đặt phòng)
        try {
            if (maDatPhong != null && !maDatPhong.isBlank()) {
                hd.setDanhSachDichVu(chiTietDichVuDAO.findByMaDatPhong(maDatPhong));
            } else {
                hd.setDanhSachDichVu(chiTietDichVuDAO.findByMaHoaDon(maHoaDon));
            }
        } catch (Exception ignored) {
        }
        return hd;
    }

    @Override
    public Optional<HoaDon> findById(String maHoaDon) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findById(conn, maHoaDon);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tìm hóa đơn theo mã: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<HoaDon> findById(Connection conn, String maHoaDon) {
        String sql = "SELECT * FROM hoa_don WHERE ma_hoa_don = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maHoaDon);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToHoaDon(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tìm hóa đơn theo mã trong transaction: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<HoaDon> findByIdForUpdate(Connection conn, String maHoaDon) {
        String sql = "SELECT * FROM hoa_don WHERE ma_hoa_don = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maHoaDon);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToHoaDon(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi khóa hóa đơn (FOR UPDATE): " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<HoaDon> findByMaDatPhong(String maDatPhong) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return findByMaDatPhong(conn, maDatPhong);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi kết nối khi tìm hóa đơn theo mã đặt phòng: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<HoaDon> findByMaDatPhong(Connection conn, String maDatPhong) {
        String sql = "SELECT * FROM hoa_don WHERE ma_dat_phong = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maDatPhong);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToHoaDon(rs));
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi tìm hóa đơn theo mã đặt phòng trong transaction: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<HoaDon> findAll() {
        List<HoaDon> list = new ArrayList<>();
        String sql = "SELECT * FROM hoa_don ORDER BY ngay_lap DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToHoaDon(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi lấy danh sách hóa đơn: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public boolean save(HoaDon hd) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return save(conn, hd);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi kết nối khi lưu hóa đơn: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean save(Connection conn, HoaDon hd) {
        String sql = "INSERT INTO hoa_don (ma_hoa_don, ma_dat_phong, ma_nhan_vien_lap, ngay_lap, " +
                "tien_phong, tien_dich_vu, tien_giam_gia, tien_coc, tong_tien, trang_thai) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hd.getMaHoaDon());
            stmt.setString(2, hd.getDatPhong() != null ? hd.getDatPhong().getMaDatPhong() : null);
            stmt.setString(3, hd.getNhanVienLap() != null ? hd.getNhanVienLap().getMaNhanVien() : null);
            stmt.setTimestamp(4, Timestamp.valueOf(hd.getNgayLap() != null ? hd.getNgayLap() : LocalDateTime.now()));
            stmt.setBigDecimal(5, hd.getTienPhong());
            stmt.setBigDecimal(6, hd.getTienDichVu());
            stmt.setBigDecimal(7, hd.getTienGiamGia());
            stmt.setBigDecimal(8, hd.getTienCoc());
            stmt.setBigDecimal(9, hd.getTongTien());
            stmt.setString(10, hd.getTrangThai().name());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi lưu hóa đơn trong transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(HoaDon hd) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return update(conn, hd);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi cập nhật hóa đơn: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Connection conn, HoaDon hd) {
        String sql = "UPDATE hoa_don SET tien_phong = ?, tien_dich_vu = ?, tien_giam_gia = ?, tien_coc = ?, tong_tien = ?, trang_thai = ? " +
                "WHERE ma_hoa_don = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, hd.getTienPhong());
            stmt.setBigDecimal(2, hd.getTienDichVu());
            stmt.setBigDecimal(3, hd.getTienGiamGia());
            stmt.setBigDecimal(4, hd.getTienCoc());
            stmt.setBigDecimal(5, hd.getTongTien());
            stmt.setString(6, hd.getTrangThai().name());
            stmt.setString(7, hd.getMaHoaDon());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi cập nhật hóa đơn trong transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateTrangThai(String maHoaDon, TrangThaiThanhToan trangThaiMoi) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return updateTrangThai(conn, maHoaDon, trangThaiMoi);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi kết nối khi cập nhật trạng thái hóa đơn: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateTrangThai(Connection conn, String maHoaDon, TrangThaiThanhToan trangThaiMoi) {
        String sql = "UPDATE hoa_don SET trang_thai = ? WHERE ma_hoa_don = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, trangThaiMoi.name());
            stmt.setString(2, maHoaDon);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi cập nhật trạng thái hóa đơn trong transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateTien(String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return updateTien(conn, maHoaDon, tienPhong, tienDichVu, tienGiamGia, tienCoc, tongTien);
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi kết nối khi cập nhật số tiền hóa đơn: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateTien(Connection conn, String maHoaDon, BigDecimal tienPhong, BigDecimal tienDichVu, BigDecimal tienGiamGia, BigDecimal tienCoc, BigDecimal tongTien) {
        String sql = "UPDATE hoa_don SET tien_phong = ?, tien_dich_vu = ?, tien_giam_gia = ?, tien_coc = ?, tong_tien = ? WHERE ma_hoa_don = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, tienPhong);
            stmt.setBigDecimal(2, tienDichVu);
            stmt.setBigDecimal(3, tienGiamGia);
            stmt.setBigDecimal(4, tienCoc);
            stmt.setBigDecimal(5, tongTien);
            stmt.setString(6, maHoaDon);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Lỗi khi cập nhật số tiền hóa đơn trong transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(String maHoaDon) {
        String sql = "DELETE FROM hoa_don WHERE ma_hoa_don = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, maHoaDon);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DataAccessException("Không thể xóa hóa đơn đã có giao dịch: " + e.getMessage(), e);
        }
    }
}
