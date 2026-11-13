package com.qlks.service;

import com.qlks.dao.DatPhongDAO;
import com.qlks.dao.HoaDonDAO;
import com.qlks.dao.PhongDAO;
import com.qlks.dao.impl.DatPhongDAOImpl;
import com.qlks.dao.impl.HoaDonDAOImpl;
import com.qlks.dao.impl.PhongDAOImpl;
import com.qlks.model.DatPhong;
import com.qlks.model.HoaDon;
import com.qlks.model.Phong;
import com.qlks.model.enums.Permission;
import com.qlks.model.enums.TrangThaiPhong;
import com.qlks.model.enums.TrangThaiThanhToan;
import com.qlks.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service xử lý thống kê, báo cáo doanh thu, công suất phòng và lượt đặt phòng.
 * Tích hợp truy vấn tổng hợp SQL SUM ... GROUP BY theo khoảng thời gian.
 */
public class ThongKeService {

    private final PhongDAO phongDAO;
    private final DatPhongDAO datPhongDAO;
    private final HoaDonDAO hoaDonDAO;

    public ThongKeService() {
        this.phongDAO = new PhongDAOImpl();
        this.datPhongDAO = new DatPhongDAOImpl();
        this.hoaDonDAO = new HoaDonDAOImpl();
    }

    public ThongKeService(PhongDAO phongDAO, DatPhongDAO datPhongDAO, HoaDonDAO hoaDonDAO) {
        this.phongDAO = phongDAO;
        this.datPhongDAO = datPhongDAO;
        this.hoaDonDAO = hoaDonDAO;
    }

    public Map<String, Object> layTongQuan() {
        AuthorizationService.require(Permission.XEM_BAO_CAO);

        Map<String, Object> kq = new HashMap<>();
        List<Phong> dsPhong = phongDAO.findAll();
        long soPhongDangO = dsPhong.stream().filter(p -> p.getTrangThaiHienTai() == TrangThaiPhong.OCCUPIED).count();
        kq.put("tongSoPhong", dsPhong.size());
        kq.put("soPhongDangO", soPhongDangO);

        List<DatPhong> dsDatPhong = datPhongDAO.findAll();
        kq.put("tongDatPhong", dsDatPhong.size());

        List<HoaDon> dsHoaDon = hoaDonDAO.findAll();
        BigDecimal tongDoanhThu = BigDecimal.ZERO;
        BigDecimal tongTienDv = BigDecimal.ZERO;
        List<HoaDon> dsDaThanhToan = new ArrayList<>();

        for (HoaDon hd : dsHoaDon) {
            if (hd.getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN) {
                tongDoanhThu = tongDoanhThu.add(hd.getTongTien() != null ? hd.getTongTien() : BigDecimal.ZERO);
                tongTienDv = tongTienDv.add(hd.getTienDichVu() != null ? hd.getTienDichVu() : BigDecimal.ZERO);
                dsDaThanhToan.add(hd);
            }
        }
        kq.put("tongDoanhThu", tongDoanhThu);
        kq.put("tongTienDichVu", tongTienDv);
        kq.put("danhSachHoaDon", dsDaThanhToan);

        return kq;
    }

    /**
     * Thống kê doanh thu theo từng ngày bằng câu lệnh SQL SUM ... GROUP BY.
     */
    public List<Map<String, Object>> thongKeDoanhThuTheoNgay(LocalDate tuNgay, LocalDate denNgay) {
        AuthorizationService.require(Permission.XEM_BAO_CAO);
        List<Map<String, Object>> result = new ArrayList<>();

        String sql = "SELECT DATE(ngay_lap) AS ngay, " +
                "COUNT(*) AS so_hoa_don, " +
                "SUM(tien_phong) AS tong_tien_phong, " +
                "SUM(tien_dich_vu) AS tong_tien_dv, " +
                "SUM(tong_tien) AS tong_doanh_thu " +
                "FROM hoa_don " +
                "WHERE trang_thai = 'DA_THANH_TOAN' AND ngay_lap >= ? AND ngay_lap <= ? " +
                "GROUP BY DATE(ngay_lap) " +
                "ORDER BY ngay DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(tuNgay.atStartOfDay()));
            ps.setTimestamp(2, java.sql.Timestamp.valueOf(denNgay.atTime(23, 59, 59)));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    row.put("ngay", rs.getDate("ngay").toLocalDate());
                    row.put("soHoaDon", rs.getInt("so_hoa_don"));
                    row.put("tienPhong", rs.getBigDecimal("tong_tien_phong"));
                    row.put("tienDichVu", rs.getBigDecimal("tong_tien_dv"));
                    row.put("tongDoanhThu", rs.getBigDecimal("tong_doanh_thu"));
                    result.add(row);
                }
            }
        } catch (Exception ex) {
            // Fallback in-memory nếu đang chạy test không có DB kết nối
            Map<LocalDate, Map<String, Object>> map = new TreeMap<>(Comparator.reverseOrder());
            for (HoaDon hd : hoaDonDAO.findAll()) {
                if (hd.getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN && hd.getNgayLap() != null) {
                    LocalDate d = hd.getNgayLap().toLocalDate();
                    if (!d.isBefore(tuNgay) && !d.isAfter(denNgay)) {
                        Map<String, Object> row = map.computeIfAbsent(d, k -> {
                            Map<String, Object> r = new HashMap<>();
                            r.put("ngay", k);
                            r.put("soHoaDon", 0);
                            r.put("tienPhong", BigDecimal.ZERO);
                            r.put("tienDichVu", BigDecimal.ZERO);
                            r.put("tongDoanhThu", BigDecimal.ZERO);
                            return r;
                        });
                        row.put("soHoaDon", (Integer) row.get("soHoaDon") + 1);
                        row.put("tienPhong", ((BigDecimal) row.get("tienPhong")).add(hd.getTienPhong() != null ? hd.getTienPhong() : BigDecimal.ZERO));
                        row.put("tienDichVu", ((BigDecimal) row.get("tienDichVu")).add(hd.getTienDichVu() != null ? hd.getTienDichVu() : BigDecimal.ZERO));
                        row.put("tongDoanhThu", ((BigDecimal) row.get("tongDoanhThu")).add(hd.getTongTien() != null ? hd.getTongTien() : BigDecimal.ZERO));
                    }
                }
            }
            result.addAll(map.values());
        }

        return result;
    }
}

