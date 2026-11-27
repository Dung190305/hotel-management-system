package com.qlks.service;

import com.qlks.dao.ChiTietDichVuDAO;
import com.qlks.dao.DatPhongDAO;
import com.qlks.dao.HoaDonDAO;
import com.qlks.dao.KhachLuuTruDAO;
import com.qlks.dao.PhongDAO;
import com.qlks.dao.ThanhToanDAO;
import com.qlks.dao.impl.ChiTietDichVuDAOImpl;
import com.qlks.dao.impl.DatPhongDAOImpl;
import com.qlks.dao.impl.HoaDonDAOImpl;
import com.qlks.dao.impl.KhachLuuTruDAOImpl;
import com.qlks.dao.impl.PhongDAOImpl;
import com.qlks.dao.impl.ThanhToanDAOImpl;
import com.qlks.exception.BusinessException;
import com.qlks.model.ChiTietDichVu;
import com.qlks.model.DatPhong;
import com.qlks.model.HoaDon;
import com.qlks.model.KhachLuuTru;
import com.qlks.model.NhanVien;
import com.qlks.model.Phong;
import com.qlks.model.ThanhToan;
import com.qlks.model.dto.KetQuaHuyDatPhong;
import com.qlks.model.dto.ThongTinPhuThu;
import com.qlks.model.enums.Permission;
import com.qlks.model.enums.PhuongThucThanhToan;
import com.qlks.model.enums.TrangThaiDatPhong;
import com.qlks.model.enums.TrangThaiPhong;
import com.qlks.model.enums.TrangThaiThanhToan;
import com.qlks.model.enums.VaiTroKhachLuuTru;
import com.qlks.util.AppEventBus;
import com.qlks.util.DatabaseConnection;
import com.qlks.util.FormatUtil;
import com.qlks.util.HolidayUtil;
import com.qlks.util.IdGenerator;
import com.qlks.util.ValidationUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service xử lý các quy tắc nghiệp vụ cốt lõi:
 * - Đặt phòng & quản lý khách lưu trú kèm CCCD (Transaction nguyên tử, chống xung đột đồng thời).
 * - Sửa lỗi tính tiền cọc và hóa đơn.
 * - Check-in và Check-out đồng bộ trạng thái vật lý của phòng.
 */
public class DatPhongService {

    private final DatPhongDAO datPhongDAO;
    private final PhongDAO phongDAO;
    private final HoaDonDAO hoaDonDAO;
    private final KhachLuuTruDAO khachLuuTruDAO;
    private final ChiTietDichVuDAO chiTietDichVuDAO;
    private final ThanhToanDAO thanhToanDAO;

    public DatPhongService() {
        this.datPhongDAO = new DatPhongDAOImpl();
        this.phongDAO = new PhongDAOImpl();
        this.hoaDonDAO = new HoaDonDAOImpl();
        this.khachLuuTruDAO = new KhachLuuTruDAOImpl();
        this.chiTietDichVuDAO = new ChiTietDichVuDAOImpl();
        this.thanhToanDAO = new ThanhToanDAOImpl();
    }

    public DatPhongService(DatPhongDAO datPhongDAO, PhongDAO phongDAO, HoaDonDAO hoaDonDAO) {
        this.datPhongDAO = datPhongDAO;
        this.phongDAO = phongDAO;
        this.hoaDonDAO = hoaDonDAO;
        this.khachLuuTruDAO = new KhachLuuTruDAOImpl();
        this.chiTietDichVuDAO = new ChiTietDichVuDAOImpl();
        this.thanhToanDAO = new ThanhToanDAOImpl();
    }

    public DatPhongService(DatPhongDAO datPhongDAO, PhongDAO phongDAO, HoaDonDAO hoaDonDAO, KhachLuuTruDAO khachLuuTruDAO) {
        this.datPhongDAO = datPhongDAO;
        this.phongDAO = phongDAO;
        this.hoaDonDAO = hoaDonDAO;
        this.khachLuuTruDAO = khachLuuTruDAO;
        this.chiTietDichVuDAO = new ChiTietDichVuDAOImpl();
        this.thanhToanDAO = new ThanhToanDAOImpl();
    }

    public DatPhongService(DatPhongDAO datPhongDAO, PhongDAO phongDAO, HoaDonDAO hoaDonDAO, KhachLuuTruDAO khachLuuTruDAO, ChiTietDichVuDAO chiTietDichVuDAO) {
        this.datPhongDAO = datPhongDAO;
        this.phongDAO = phongDAO;
        this.hoaDonDAO = hoaDonDAO;
        this.khachLuuTruDAO = khachLuuTruDAO;
        this.chiTietDichVuDAO = chiTietDichVuDAO;
        this.thanhToanDAO = new ThanhToanDAOImpl();
    }

    public DatPhongService(DatPhongDAO datPhongDAO, PhongDAO phongDAO, HoaDonDAO hoaDonDAO,
                           KhachLuuTruDAO khachLuuTruDAO, ChiTietDichVuDAO chiTietDichVuDAO, ThanhToanDAO thanhToanDAO) {
        this.datPhongDAO = datPhongDAO;
        this.phongDAO = phongDAO;
        this.hoaDonDAO = hoaDonDAO;
        this.khachLuuTruDAO = khachLuuTruDAO;
        this.chiTietDichVuDAO = chiTietDichVuDAO;
        this.thanhToanDAO = thanhToanDAO;
    }

    protected Connection layKetNoi() throws SQLException {
        return DatabaseConnection.getConnection();
    }

    public List<DatPhong> layTatCaDatPhong() {
        return datPhongDAO.findAll();
    }

    public Optional<DatPhong> timTheoMa(String maDatPhong) {
        return datPhongDAO.findById(maDatPhong);
    }

    public List<DatPhong> layDanhSachDangO() {
        return datPhongDAO.findByTrangThai(TrangThaiDatPhong.DANG_O);
    }

    /**
     * Tính toán số tiền cọc tối thiểu theo quy định:
     * - Ngày thường: 30% tiền phòng tạm tính.
     * - Cuối tuần (Thứ 7, CN) hoặc Ngày Lễ: 50% tiền phòng tạm tính.
     */
    public BigDecimal tinhTienCocToiThieu(Phong phong, LocalDateTime ngayNhan, LocalDateTime ngayTra) {
        if (phong == null || ngayNhan == null || ngayTra == null || !ngayTra.isAfter(ngayNhan)) {
            return BigDecimal.ZERO;
        }
        long soDem = Math.max(1, ChronoUnit.DAYS.between(ngayNhan.toLocalDate(), ngayTra.toLocalDate()));
        BigDecimal tienPhong = phong.tinhTienPhong(soDem);
        BigDecimal tyLe = HolidayUtil.tinhTyLeCoc(ngayNhan.toLocalDate(), ngayTra.toLocalDate());
        return tienPhong.multiply(tyLe).setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * Quy tắc nghiệp vụ 1: Tạo phiếu đặt phòng và ghi nhận CCCD của tất cả khách lưu trú.
     * Áp dụng Transaction nguyên tử (ACID) và khóa dòng (SELECT ... FOR UPDATE) chống xung đột đồng thời.
     */
    public void taoDatPhong(DatPhong dp) {
        AuthorizationService.require(Permission.DAT_PHONG);
        ValidationUtil.validate(dp);

        // 1. Kiểm tra ngày giờ hợp lệ
        if (!dp.getNgayNhanPhong().isBefore(dp.getNgayTraPhong())) {
            throw new BusinessException("Ngày trả phòng dự kiến phải diễn ra sau ngày nhận phòng.");
        }

        if (dp.getNgayNhanPhong().toLocalDate().isBefore(LocalDate.now())) {
            throw new BusinessException("Ngày nhận phòng không thể nằm trong quá khứ.");
        }

        // 2. Kiểm tra mã đặt phòng duy nhất
        if (datPhongDAO.findById(dp.getMaDatPhong()).isPresent()) {
            throw new BusinessException("Mã đặt phòng '" + dp.getMaDatPhong() + "' đã tồn tại.");
        }

        // 3. Kiểm tra thông tin phòng cơ bản
        Optional<Phong> phongOpt = phongDAO.findById(dp.getPhong().getMaPhong());
        if (phongOpt.isEmpty()) {
            throw new BusinessException("Phòng được chọn không tồn tại.");
        }
        Phong phong = phongOpt.get();
        if (!phong.isDangKinhDoanh()) {
            throw new BusinessException("Phòng " + phong.getSoPhong() + " hiện đang ngừng kinh doanh.");
        }
        if (phong.getTrangThaiHienTai() == TrangThaiPhong.MAINTENANCE) {
            throw new BusinessException("Phòng " + phong.getSoPhong() + " hiện đang bảo trì thiết bị, không thể nhận đặt.");
        }

        // 4. KIỂM TRA BẮT BUỘC KHÁCH LƯU TRÚ VÀ CCCD ĐẦY ĐỦ
        List<KhachLuuTru> danhSachKhach = dp.getDanhSachKhach();
        if (danhSachKhach == null || danhSachKhach.isEmpty()) {
            // Tự động thêm khách đại diện nếu có thông tin khách hàng và danh sách rỗng
            if (dp.getKhachHang() != null && dp.getKhachHang().getSoCCCD() != null) {
                KhachLuuTru daiDien = new KhachLuuTru(
                        dp.getMaDatPhong(),
                        dp.getKhachHang().getHoTen(),
                        dp.getKhachHang().getSoCCCD(),
                        dp.getKhachHang().getSoDienThoai(),
                        VaiTroKhachLuuTru.NGUOI_DAI_DIEN
                );
                dp.addKhachLuuTru(daiDien);
                danhSachKhach = dp.getDanhSachKhach();
            } else {
                throw new BusinessException("Đơn đặt phòng bắt buộc phải khai báo danh sách khách lưu trú kèm CCCD.");
            }
        }

        int sucChuaToiDa = phong.getSucChuaToiDa();
        if (danhSachKhach.size() > sucChuaToiDa) {
            throw new BusinessException("Số lượng khách lưu trú (" + danhSachKhach.size() +
                    ") vượt quá sức chứa tối đa của " + phong.getTenLoaiPhongHienThi() +
                    " (" + sucChuaToiDa + " người).");
        }

        // Kiểm tra CCCD 12 số của từng khách và chống trùng trong đơn
        Set<String> setCCCD = new HashSet<>();
        for (KhachLuuTru khach : danhSachKhach) {
            if (khach.getHoTen() == null || khach.getHoTen().trim().isEmpty()) {
                throw new BusinessException("Họ tên của khách lưu trú không được để trống.");
            }
            if (!KhachLuuTru.kiemTraCCCD(khach.getSoCCCD())) {
                throw new BusinessException("Khách lưu trú '" + khach.getHoTen() +
                        "' có số CCCD không hợp lệ. Số CCCD phải gồm đúng 12 chữ số.");
            }
            String cccdSach = khach.getSoCCCD().trim();
            if (setCCCD.contains(cccdSach)) {
                throw new BusinessException("Phát hiện số CCCD bị trùng lặp trong cùng một đơn đặt phòng: " + khach.getSoCCCDChe());
            }
            setCCCD.add(cccdSach);
        }

        // Cập nhật số lượng khách tương ứng danh sách thực tế
        dp.setSoLuongKhach(danhSachKhach.size());
        dp.setTrangThai(TrangThaiDatPhong.DA_XAC_NHAN);

        // 4.5. Kiểm tra mức tiền cọc tối thiểu theo quy định: 30% ngày thường, 50% cuối tuần & ngày lễ
        BigDecimal tienCocToiThieu = tinhTienCocToiThieu(phong, dp.getNgayNhanPhong(), dp.getNgayTraPhong());
        BigDecimal tienCoc = dp.getTienCoc() != null ? dp.getTienCoc() : BigDecimal.ZERO;
        if (tienCoc.compareTo(BigDecimal.ZERO) > 0 && tienCoc.compareTo(tienCocToiThieu) < 0) {
            String moTaLoaiNgay = HolidayUtil.getMoTaLoaiNgay(dp.getNgayNhanPhong().toLocalDate(), dp.getNgayTraPhong().toLocalDate());
            throw new BusinessException(String.format(
                    "Tiền đặt cọc không đạt mức tối thiểu quy định! %s: Yêu cầu tối thiểu %s VNĐ, bạn đã nhập: %s VNĐ.",
                    moTaLoaiNgay,
                    FormatUtil.formatTien(tienCocToiThieu),
                    FormatUtil.formatTien(tienCoc)
            ));
        }

        // 5. THỰC THI GIAO DỊCH NGUYÊN TỬ (TRANSACTION) VÀ KHÓA DÒNG CHỐNG XUNG ĐỘT
        try (Connection conn = layKetNoi()) {
            conn.setAutoCommit(false);
            try {
                // Khóa phòng để chống 2 nhân viên cùng bấm đặt một phòng tại một thời điểm
                phongDAO.findByIdForUpdate(conn, phong.getMaPhong());

                boolean biTrungLich = datPhongDAO.isPhongBiTrungLich(
                        conn,
                        phong.getMaPhong(),
                        dp.getNgayNhanPhong(),
                        dp.getNgayTraPhong(),
                        null
                );
                if (biTrungLich) {
                    throw new BusinessException("Phòng " + phong.getSoPhong() +
                            " đã có khách đặt trong khoảng thời gian này! Vui lòng chọn phòng khác hoặc đổi ngày.");
                }

                // Lưu phiếu đặt phòng
                boolean okDp = datPhongDAO.save(conn, dp);
                if (!okDp) {
                    throw new BusinessException("Lưu phiếu đặt phòng thất bại.");
                }

                // Lưu toàn bộ danh sách khách lưu trú
                boolean okKhach = khachLuuTruDAO.saveAll(conn, dp.getMaDatPhong(), danhSachKhach);
                if (!okKhach) {
                    throw new BusinessException("Lưu danh sách khách lưu trú thất bại.");
                }

                // Nếu ngày nhận phòng là hôm nay, cập nhật trạng thái phòng sang RESERVED
                if (dp.getNgayNhanPhong().toLocalDate().isEqual(LocalDate.now()) &&
                        phong.getTrangThaiHienTai() == TrangThaiPhong.AVAILABLE) {
                    phongDAO.updateTrangThai(conn, phong.getMaPhong(), TrangThaiPhong.RESERVED);
                }

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof BusinessException) {
                    throw (BusinessException) ex;
                }
                throw new BusinessException("Giao dịch đặt phòng thất bại: " + ex.getMessage(), ex);
            }
        } catch (SQLException ex) {
            throw new BusinessException("Lỗi kết nối cơ sở dữ liệu khi đặt phòng: " + ex.getMessage(), ex);
        }

        // Đồng bộ sự kiện thời gian thực
        AppEventBus.fireBookingChanged();
        AppEventBus.fireRoomStatusChanged();
    }

    /**
     * Tính toán phụ thu nhận phòng sớm (Early Check-in) theo chuẩn PMS quốc tế:
     * - Giờ nhận phòng tiêu chuẩn: 14:00 ngày nhận phòng.
     * - Nếu nhận phòng cùng ngày trước 14:00:
     *   + Trước 06:00 sáng: Phụ thu 100% giá phòng 1 ngày (chiếm phòng qua đêm).
     *   + Từ 06:00 đến trước 09:00: Phụ thu 50% giá phòng 1 ngày.
     *   + Từ 09:00 đến trước 12:00: Phụ thu 30% giá phòng 1 ngày.
     *   + Từ 12:00 đến 14:00: Miễn phí (0%) nếu phòng đã sẵn sàng (chính sách hỗ trợ linh hoạt).
     *   + Từ 14:00 trở đi: 0%.
     */
    public ThongTinPhuThu tinhPhuThuEarlyCheckIn(Phong phong, LocalDateTime thoiGianCheckIn, LocalDateTime ngayNhanDuKien) {
        if (phong == null || thoiGianCheckIn == null || ngayNhanDuKien == null) {
            return ThongTinPhuThu.khongPhuThu(thoiGianCheckIn, "Không đủ thông tin tính phụ thu");
        }

        LocalDate ngayCheckIn = thoiGianCheckIn.toLocalDate();
        LocalDate ngayNhan = ngayNhanDuKien.toLocalDate();
        BigDecimal gia1Ngay = phong.tinhTienPhong(1);

        if (ngayCheckIn.isEqual(ngayNhan)) {
            LocalTime gioCheckIn = thoiGianCheckIn.toLocalTime();
            if (!gioCheckIn.isBefore(LocalTime.of(14, 0))) {
                return ThongTinPhuThu.khongPhuThu(thoiGianCheckIn, "Nhận phòng đúng giờ quy định (sau 14:00).");
            }
            if (!gioCheckIn.isBefore(LocalTime.of(12, 0))) {
                return ThongTinPhuThu.khongPhuThu(thoiGianCheckIn, "Nhận phòng từ 12:00 - 14:00: Miễn phí hỗ trợ khách hàng.");
            }

            if (gioCheckIn.isBefore(LocalTime.of(6, 0))) {
                BigDecimal phuThu = gia1Ngay;
                String moTa = String.format("Nhận phòng trước 06:00 sáng (%s): Phụ thu 100%% giá phòng 1 ngày.",
                        gioCheckIn.format(DateTimeFormatter.ofPattern("HH:mm")));
                return new ThongTinPhuThu(ThongTinPhuThu.LoaiPhuThu.EARLY_CHECK_IN, BigDecimal.valueOf(1.0), phuThu, thoiGianCheckIn, moTa);
            } else if (gioCheckIn.isBefore(LocalTime.of(9, 0))) {
                BigDecimal phuThu = gia1Ngay.multiply(BigDecimal.valueOf(0.5));
                String moTa = String.format("Nhận phòng sáng sớm từ 06:00 - 09:00 (%s): Phụ thu 50%% giá phòng 1 ngày.",
                        gioCheckIn.format(DateTimeFormatter.ofPattern("HH:mm")));
                return new ThongTinPhuThu(ThongTinPhuThu.LoaiPhuThu.EARLY_CHECK_IN, BigDecimal.valueOf(0.5), phuThu, thoiGianCheckIn, moTa);
            } else { // 09:00 -> 12:00
                BigDecimal phuThu = gia1Ngay.multiply(BigDecimal.valueOf(0.3));
                String moTa = String.format("Nhận phòng sớm từ 09:00 - 12:00 (%s): Phụ thu 30%% giá phòng 1 ngày.",
                        gioCheckIn.format(DateTimeFormatter.ofPattern("HH:mm")));
                return new ThongTinPhuThu(ThongTinPhuThu.LoaiPhuThu.EARLY_CHECK_IN, BigDecimal.valueOf(0.3), phuThu, thoiGianCheckIn, moTa);
            }
        }

        return ThongTinPhuThu.khongPhuThu(thoiGianCheckIn, "Không thuộc diện phụ thu Early Check-in cùng ngày.");
    }

    /**
     * Tính toán phụ thu trả phòng trễ (Late Check-out) theo chuẩn PMS quốc tế:
     * - Giờ trả phòng tiêu chuẩn: trước 12:00 (cho phép linh hoạt đến 12:30).
     * - Từ 12:30 đến trước 18:00: Phụ thu 30% giá phòng 1 ngày.
     * - Từ 18:00 trở đi: Phụ thu 100% giá phòng 1 ngày.
     */
    public ThongTinPhuThu tinhPhuThuLateCheckOut(Phong phong, LocalDateTime thoiGianCheckOut) {
        if (phong == null || thoiGianCheckOut == null) {
            return ThongTinPhuThu.khongPhuThu(thoiGianCheckOut, "Không đủ thông tin tính phụ thu");
        }
        LocalTime gioCheckOut = thoiGianCheckOut.toLocalTime();
        BigDecimal gia1Ngay = phong.tinhTienPhong(1);

        if (gioCheckOut.isAfter(LocalTime.of(12, 30))) {
            if (gioCheckOut.isBefore(LocalTime.of(18, 0))) {
                BigDecimal phuThu30 = gia1Ngay.multiply(BigDecimal.valueOf(0.30));
                String moTa = String.format("Trả phòng trễ từ 12:30 - 18:00 (%s): Phụ thu 30%% giá phòng 1 ngày.",
                        gioCheckOut.format(DateTimeFormatter.ofPattern("HH:mm")));
                return new ThongTinPhuThu(ThongTinPhuThu.LoaiPhuThu.LATE_CHECK_OUT, BigDecimal.valueOf(0.30), phuThu30, thoiGianCheckOut, moTa);
            } else {
                BigDecimal phuThu100 = gia1Ngay;
                String moTa = String.format("Trả phòng trễ sau 18:00 (%s): Phụ thu 100%% giá phòng 1 ngày.",
                        gioCheckOut.format(DateTimeFormatter.ofPattern("HH:mm")));
                return new ThongTinPhuThu(ThongTinPhuThu.LoaiPhuThu.LATE_CHECK_OUT, BigDecimal.valueOf(1.00), phuThu100, thoiGianCheckOut, moTa);
            }
        }
        return ThongTinPhuThu.khongPhuThu(thoiGianCheckOut, "Trả phòng đúng giờ quy chuẩn (trước 12:30).");
    }

    /**
     * Quy tắc nghiệp vụ 2: Xác nhận Check-in cho khách đến nhận phòng.
     * Kiểm tra đầy đủ điều kiện: ngày giờ, trạng thái phòng, danh sách khách lưu trú kèm CCCD.
     */
    public void xacNhanCheckIn(String maDatPhong) {
        xacNhanCheckIn(maDatPhong, LocalDateTime.now());
    }

    public void xacNhanCheckIn(String maDatPhong, LocalDateTime thoiGianCheckIn) {
        AuthorizationService.require(Permission.DAT_PHONG);
        if (thoiGianCheckIn == null) {
            thoiGianCheckIn = LocalDateTime.now();
        }

        Optional<DatPhong> dpOpt = datPhongDAO.findById(maDatPhong);
        if (dpOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy phiếu đặt phòng: " + maDatPhong);
        }

        DatPhong dp = dpOpt.get();
        // BR-04: Chỉ nhận phòng từ phiếu có trạng thái DA_XAC_NHAN
        if (dp.getTrangThai() == TrangThaiDatPhong.CHO_XAC_NHAN) {
            throw new BusinessException("Phiếu đặt phòng đang ở trạng thái 'Chờ xác nhận'. Vui lòng xác nhận phiếu trước khi Check-in.");
        }
        if (dp.getTrangThai() == TrangThaiDatPhong.DANG_O) {
            throw new BusinessException("Khách đã nhận phòng này trước đó.");
        }
        if (dp.getTrangThai() != TrangThaiDatPhong.DA_XAC_NHAN) {
            throw new BusinessException("Chỉ có thể check-in cho phiếu đặt phòng đã xác nhận (DA_XAC_NHAN). Trạng thái hiện tại: " + dp.getTrangThai().getTenHienThi());
        }

        // Kiểm tra ngày nhận phòng: Không được check-in trước ngày nhận phòng quy định
        LocalDate ngayNhan = dp.getNgayNhanPhong().toLocalDate();
        LocalDate ngayCheckIn = thoiGianCheckIn.toLocalDate();
        if (ngayCheckIn.isBefore(ngayNhan)) {
            throw new BusinessException("Chưa đến ngày nhận phòng (" + ngayNhan + "). Không thể thực hiện Check-in trước hạn.");
        }

        // Kiểm tra phòng sẵn sàng sử dụng (Phải sạch sẽ và không có khách đang ở)
        Phong phong = dp.getPhong();
        if (phong != null) {
            Optional<Phong> pDbOpt = phongDAO.findById(phong.getMaPhong());
            if (pDbOpt.isPresent()) {
                phong = pDbOpt.get();
            }

            if (phong.getTrangThaiHienTai() == TrangThaiPhong.MAINTENANCE) {
                throw new BusinessException("Phòng " + phong.getSoPhong() + " hiện đang bảo trì, không thể bàn giao cho khách.");
            }
            if (phong.getTrangThaiHienTai() == TrangThaiPhong.CLEANING) {
                throw new BusinessException("Phòng " + phong.getSoPhong() + " hiện đang dọn dẹp (CLEANING), chưa sẵn sàng đón khách. Vui lòng hoàn tất dọn phòng trước khi Check-in.");
            }
            if (phong.getTrangThaiHienTai() == TrangThaiPhong.OCCUPIED) {
                throw new BusinessException("Phòng " + phong.getSoPhong() + " hiện đang có khách lưu trú (OCCUPIED). Vui lòng hoàn tất thủ tục trả phòng của khách trước.");
            }
        }

        // Xác minh danh sách khách lưu trú và CCCD
        List<KhachLuuTru> danhSachKhach = dp.getDanhSachKhach();
        if (danhSachKhach == null || danhSachKhach.isEmpty()) {
            danhSachKhach = khachLuuTruDAO.findByMaDatPhong(maDatPhong);
        }
        if (danhSachKhach.isEmpty()) {
            throw new BusinessException("Chưa có danh sách khách lưu trú kèm CCCD hợp lệ cho đơn đặt phòng này. Vui lòng bổ sung trước khi Check-in.");
        }
        for (KhachLuuTru k : danhSachKhach) {
            if (!KhachLuuTru.kiemTraCCCD(k.getSoCCCD())) {
                throw new BusinessException("Khách lưu trú '" + k.getHoTen() + "' chưa có số CCCD 12 chữ số hợp lệ.");
            }
        }

        // Giao dịch Check-in nguyên tử với Khóa dòng (FOR UPDATE) chống xung đột tương tranh
        try (Connection conn = layKetNoi()) {
            conn.setAutoCommit(false);
            try {
                // Khóa dòng đặt phòng chống xung đột đồng thời (Double Check-in hoặc vừa hủy vừa check-in)
                DatPhong dpLocked = datPhongDAO.findByIdForUpdate(conn, maDatPhong)
                        .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu đặt phòng: " + maDatPhong));
                if (dpLocked.getTrangThai() != TrangThaiDatPhong.DA_XAC_NHAN) {
                    if (dpLocked.getTrangThai() == TrangThaiDatPhong.DANG_O) {
                        throw new BusinessException("Phiếu đặt phòng " + maDatPhong + " đã được Check-in trước đó bởi nhân viên khác.");
                    }
                    throw new BusinessException("Chỉ có thể check-in cho phiếu đặt phòng đã xác nhận (DA_XAC_NHAN). Trạng thái hiện tại: " + dpLocked.getTrangThai().getTenHienThi());
                }

                String maPhong = dpLocked.getPhong() != null ? dpLocked.getPhong().getMaPhong() : null;
                if (maPhong == null) {
                    throw new BusinessException("Phiếu đặt phòng chưa được gán phòng.");
                }

                // Khóa dòng phòng và kiểm tra trạng thái vật lý
                Phong pLocked = phongDAO.findByIdForUpdate(conn, maPhong)
                        .orElseThrow(() -> new BusinessException("Phòng không tồn tại: " + maPhong));
                if (pLocked.getTrangThaiHienTai() == TrangThaiPhong.OCCUPIED) {
                    throw new BusinessException("Phòng " + pLocked.getSoPhong() + " hiện đang có khách lưu trú (OCCUPIED). Vui lòng hoàn tất thủ tục trả phòng của khách trước.");
                }
                if (pLocked.getTrangThaiHienTai() == TrangThaiPhong.MAINTENANCE) {
                    throw new BusinessException("Phòng " + pLocked.getSoPhong() + " hiện đang bảo trì, không thể bàn giao cho khách.");
                }
                if (pLocked.getTrangThaiHienTai() == TrangThaiPhong.CLEANING) {
                    throw new BusinessException("Phòng " + pLocked.getSoPhong() + " hiện đang dọn dẹp (CLEANING), chưa sẵn sàng đón khách.");
                }

                // Rào chắn kiểm tra chéo: Có đơn nào khác đang DANG_O tại phòng này không?
                if (datPhongDAO.isPhongDangCoNguoiO(conn, maPhong, maDatPhong)) {
                    throw new BusinessException("Phòng " + pLocked.getSoPhong() + " hiện đang có khách từ đơn đặt khác lưu trú (DANG_O). Không thể thực hiện Check-in!");
                }

                NhanVien nvHienTai = AuthService.getNhanVienHienTai();
                String maNvCi = nvHienTai != null ? nvHienTai.getMaNhanVien() : (dpLocked.getNhanVienLap() != null ? dpLocked.getNhanVienLap().getMaNhanVien() : null);
                boolean okDp = datPhongDAO.xacNhanCheckIn(conn, maDatPhong, thoiGianCheckIn, maNvCi);
                if (!okDp) {
                    throw new BusinessException("Phiếu đặt phòng " + maDatPhong + " đã được Check-in bởi nhân viên khác hoặc trạng thái không còn hợp lệ.");
                }

                boolean okPhong = phongDAO.updateTrangThaiCheckIn(conn, maPhong);
                if (!okPhong) {
                    throw new BusinessException("Cập nhật trạng thái phòng " + pLocked.getSoPhong() + " sang OCCUPIED thất bại (phòng có thể đã bị chiếm hoặc đổi trạng thái).");
                }

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof BusinessException) throw (BusinessException) ex;
                throw new BusinessException("Lỗi giao dịch khi Check-in: " + ex.getMessage(), ex);
            }
        } catch (SQLException ex) {
            throw new BusinessException("Lỗi kết nối CSDL khi Check-in: " + ex.getMessage(), ex);
        }

        // Đồng bộ sự kiện thời gian thực
        AppEventBus.fireBookingChanged();
        AppEventBus.fireRoomStatusChanged();
    }

    /**
     * Quy tắc nghiệp vụ 3: Xác nhận Check-out cho khách trả phòng và tự động lập Hóa đơn.
     * Áp dụng chuẩn PMS quốc tế:
     * - Tính số đêm thực tế (tối thiểu 1 đêm).
     * - Quy chuẩn trả phòng sau 12:30 trưa (Late Check-out):
     *   + Từ 12:30 đến 18:00: Phụ thu quá giờ 30% giá phòng 1 ngày.
     *   + Sau 18:00: Phụ thu quá giờ 100% giá phòng 1 ngày.
     * - SỬA TRIỆT ĐỂ LỖI TIỀN CỌC: Tiền cọc là tiền trả trước (deposit), KHÔNG ĐƯỢC coi là tiền giảm giá (discount).
     */
    public HoaDon xacNhanCheckOut(String maDatPhong) {
        return xacNhanCheckOut(maDatPhong, LocalDateTime.now());
    }

    public HoaDon xacNhanCheckOut(String maDatPhong, LocalDateTime thoiGianCheckOut) {
        AuthorizationService.require(Permission.DAT_PHONG);
        Optional<DatPhong> dpOpt = datPhongDAO.findById(maDatPhong);
        if (dpOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy phiếu đặt phòng: " + maDatPhong);
        }

        DatPhong dp = dpOpt.get();
        if (dp.getTrangThai() != TrangThaiDatPhong.DANG_O) {
            throw new BusinessException("Chỉ có thể check-out cho phòng đang có khách lưu trú (trạng thái DANG_O).");
        }

        if (thoiGianCheckOut == null) {
            thoiGianCheckOut = LocalDateTime.now();
        }

        // Tính toán số đêm thực tế (tối thiểu 1 đêm)
        LocalDateTime checkInThucTe = dp.getThoiGianNhanThucTe() != null ? dp.getThoiGianNhanThucTe() : dp.getNgayNhanPhong();
        long soDemThucTe = ChronoUnit.DAYS.between(checkInThucTe.toLocalDate(), thoiGianCheckOut.toLocalDate());
        if (soDemThucTe <= 0) {
            soDemThucTe = 1;
        }

        // Đa hình tính tiền phòng cơ bản theo số đêm
        BigDecimal tienPhong = dp.getPhong().tinhTienPhong(soDemThucTe);

        // Quy tắc phụ thu nhận phòng sớm (Early Check-in) chuẩn PMS quốc tế
        ThongTinPhuThu phuThuEarly = tinhPhuThuEarlyCheckIn(dp.getPhong(), checkInThucTe, dp.getNgayNhanPhong());
        if (phuThuEarly.coPhuThu()) {
            tienPhong = tienPhong.add(phuThuEarly.getSoTienPhuThu());
        }

        // Quy tắc phụ thu trả phòng trễ (Late Check-out) chuẩn PMS quốc tế
        ThongTinPhuThu phuThuLate = tinhPhuThuLateCheckOut(dp.getPhong(), thoiGianCheckOut);
        if (phuThuLate.coPhuThu()) {
            tienPhong = tienPhong.add(phuThuLate.getSoTienPhuThu());
        }

        // PHÂN BIỆT RẠCH RÒI: Tiền cọc và Tiền giảm giá
        BigDecimal tienCoc = dp.getTienCoc() != null ? dp.getTienCoc() : BigDecimal.ZERO;
        BigDecimal tienGiamGia = BigDecimal.ZERO; // Giảm giá khuyến mại riêng biệt, không gán tiền cọc vào đây!

        HoaDon resultHd;

        // Giao dịch Check-out nguyên tử
        try (Connection conn = layKetNoi()) {
            conn.setAutoCommit(false);
            try {
                // 1. Khóa dòng độc quyền trên dat_phong để đồng bộ thứ tự khóa với ghiNhanDichVu
                DatPhong dpLocked = datPhongDAO.findByIdForUpdate(conn, maDatPhong)
                        .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu đặt phòng: " + maDatPhong));

                if (dpLocked.getTrangThai() != TrangThaiDatPhong.DANG_O) {
                    if (dpLocked.getTrangThai() == TrangThaiDatPhong.DA_TRA_PHONG) {
                        throw new BusinessException("Phiếu đặt phòng " + maDatPhong + " đã được Check-out trước đó.");
                    }
                    throw new BusinessException("Chỉ có thể check-out cho phòng đang có khách lưu trú (trạng thái DANG_O). Trạng thái hiện tại: " + dpLocked.getTrangThai().getTenHienThi());
                }

                // 2. Cập nhật đặt phòng -> DA_TRA_PHONG kèm lưu vết nhân viên Check-out
                NhanVien nvHienTai = AuthService.getNhanVienHienTai();
                String maNvCo = nvHienTai != null ? nvHienTai.getMaNhanVien() : (dpLocked.getNhanVienLap() != null ? dpLocked.getNhanVienLap().getMaNhanVien() : null);
                boolean okDp = datPhongDAO.xacNhanCheckOut(conn, maDatPhong, thoiGianCheckOut, maNvCo);
                if (!okDp) {
                    throw new BusinessException("Phiếu đặt phòng " + maDatPhong + " đã được Check-out trước đó hoặc không còn ở trạng thái Đang ở (DANG_O).");
                }

                // 3. Chuyển phòng sang trạng thái CLEANING (buồng phòng vệ sinh trước khi nhận khách mới)
                String maPhong = dpLocked.getPhong() != null ? dpLocked.getPhong().getMaPhong() : dp.getPhong().getMaPhong();
                boolean okPhong = phongDAO.updateTrangThai(conn, maPhong, TrangThaiPhong.CLEANING);
                if (!okPhong) {
                    throw new BusinessException("Cập nhật trạng thái phòng sang CLEANING thất bại.");
                }

                // 4. Lấy tổng tiền dịch vụ bằng Current Read có khóa dòng (FOR UPDATE) chống thất thoát doanh thu
                List<ChiTietDichVu> dsDichVu = chiTietDichVuDAO.findByMaDatPhongForUpdate(conn, maDatPhong);
                BigDecimal tienDichVu = BigDecimal.ZERO;
                if (dsDichVu != null) {
                    for (ChiTietDichVu ct : dsDichVu) {
                        tienDichVu = tienDichVu.add(ct.tinhThanhTien());
                    }
                }

                // 5. Tạo hoặc cập nhật hóa đơn thanh toán
                Optional<HoaDon> hdCuOpt = hoaDonDAO.findByMaDatPhong(conn, maDatPhong);
                if (hdCuOpt.isPresent()) {
                    HoaDon hd = hdCuOpt.get();
                    // Khóa dòng hóa đơn nếu đã có
                    hoaDonDAO.findByIdForUpdate(conn, hd.getMaHoaDon());
                    hd.setTienPhong(tienPhong);
                    hd.setTienDichVu(tienDichVu);
                    hd.setTienGiamGia(tienGiamGia);
                    hd.setTienCoc(tienCoc);
                    hd.setTongTien(hd.tinhTongTien());
                    hoaDonDAO.update(conn, hd);
                    resultHd = hd;
                } else {
                    String maHoaDon = IdGenerator.next(conn, "HD");
                    BigDecimal tongChiPhi = tienPhong.add(tienDichVu).subtract(tienGiamGia).max(BigDecimal.ZERO);
                    NhanVien nvLapHd = nvHienTai != null ? nvHienTai : dpLocked.getNhanVienLap();
                    HoaDon hdMoi = new HoaDon(
                            maHoaDon,
                            dpLocked,
                            nvLapHd,
                            thoiGianCheckOut,
                            tienPhong,
                            tienDichVu,
                            tienGiamGia,
                            tienCoc,
                            tongChiPhi,
                            TrangThaiThanhToan.CHUA_THANH_TOAN
                    );
                    hoaDonDAO.save(conn, hdMoi);
                    resultHd = hdMoi;
                }

                conn.commit();
            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof BusinessException) throw (BusinessException) ex;
                throw new BusinessException("Lỗi giao dịch khi Check-out: " + ex.getMessage(), ex);
            }
        } catch (SQLException ex) {
            throw new BusinessException("Lỗi kết nối CSDL khi Check-out: " + ex.getMessage(), ex);
        }

        // Đồng bộ sự kiện thời gian thực
        AppEventBus.fireBookingChanged();
        AppEventBus.fireRoomStatusChanged();
        AppEventBus.fireInvoiceChanged();

        return resultHd;
    }

    /**
     * Quy tắc nghiệp vụ 4: Hủy phiếu đặt phòng và quyết toán tài chính cọc.
     * Áp dụng chính sách 3 mốc thời gian:
     * - Hủy trước giờ nhận phòng >= 24h: Hoàn 100% tiền cọc.
     * - Hủy từ 2h đến 24h trước giờ nhận phòng: Hoàn 50% cọc, phạt 50% cọc (tính vào doanh thu).
     * - Hủy dưới 2h trước giờ nhận phòng hoặc quá giờ check-in: CẤM HỦY (Chặn tuyệt đối).
     *
     * Thực thi trong 1 Transaction ACID duy nhất với khóa dòng SELECT ... FOR UPDATE chống tranh chấp đồng thời.
     */
    public KetQuaHuyDatPhong huyDatPhong(String maDatPhong) {
        return huyDatPhong(maDatPhong, "Khách yêu cầu hủy phòng", PhuongThucThanhToan.TIEN_MAT, LocalDateTime.now());
    }

    public KetQuaHuyDatPhong huyDatPhong(String maDatPhong, String lyDoHuy, PhuongThucThanhToan phuongThucHoan) {
        return huyDatPhong(maDatPhong, lyDoHuy, phuongThucHoan, LocalDateTime.now());
    }

    public KetQuaHuyDatPhong huyDatPhong(String maDatPhong, String lyDoHuy, PhuongThucThanhToan phuongThucHoan, LocalDateTime thoiDiemHuy) {
        AuthorizationService.require(Permission.DAT_PHONG);
        if (thoiDiemHuy == null) {
            thoiDiemHuy = LocalDateTime.now();
        }

        try (Connection conn = layKetNoi()) {
            conn.setAutoCommit(false);
            try {
                // 1. Đọc và KHÓA DÒNG phiếu đặt phòng chống tranh chấp đồng thời
                DatPhong dp = datPhongDAO.findByIdForUpdate(conn, maDatPhong)
                        .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu đặt phòng để hủy: " + maDatPhong));

                // 2. Kiểm tra trạng thái phiếu
                if (dp.getTrangThai() == TrangThaiDatPhong.DA_HUY) {
                    throw new BusinessException("Phiếu đặt phòng này đã được hủy trước đó.");
                }
                if (dp.getTrangThai() == TrangThaiDatPhong.DANG_O || dp.getTrangThai() == TrangThaiDatPhong.DA_TRA_PHONG) {
                    throw new BusinessException("Không thể hủy đặt phòng khi khách đang ở hoặc đã hoàn tất trả phòng.");
                }

                // 3. Kiểm tra thời hạn hủy phòng (Cancellation Deadline):
                // Tính khoảng cách thời gian từ lúc hủy đến giờ nhận phòng dự kiến
                Duration duration = Duration.between(thoiDiemHuy, dp.getNgayNhanPhong());
                long soPhutConLai = duration.toMinutes();

                // Mốc 3: Hủy sát giờ dưới 2 tiếng hoặc quá giờ nhận phòng -> CẤM HỦY
                if (soPhutConLai < 120) {
                    throw new BusinessException(String.format(
                            "Không thể hủy đặt phòng! Theo quy định của khách sạn, đơn đặt phòng chỉ được phép hủy trước giờ nhận phòng tối thiểu 2 tiếng.\n" +
                            "• Giờ nhận phòng: %s\n" +
                            "• Hạn chót hủy phòng: %s\n" +
                            "Hiện tại đã quá hạn hủy phòng! Quý khách vui lòng liên hệ Quản lý khách sạn để được hỗ trợ.",
                            FormatUtil.formatNgayGio(dp.getNgayNhanPhong()),
                            FormatUtil.formatNgayGio(dp.getNgayNhanPhong().minusHours(2))
                    ));
                }

                // 4. Tính toán tỷ lệ hoàn cọc và tiền hoàn/phạt
                BigDecimal tienCocBanDau = dp.getTienCoc() != null ? dp.getTienCoc() : BigDecimal.ZERO;
                BigDecimal tyLeHoan;
                BigDecimal tienHoanLai;
                BigDecimal phiPhatHuy;

                if (soPhutConLai >= 1440) {
                    // Mốc 1: Hủy trước >= 24h -> Hoàn 100% tiền cọc, phạt 0đ
                    tyLeHoan = new BigDecimal("1.0");
                    tienHoanLai = tienCocBanDau;
                    phiPhatHuy = BigDecimal.ZERO;
                } else {
                    // Mốc 2: Hủy từ 2h đến 24h -> Hoàn 50% tiền cọc, phạt 50% tiền cọc
                    tyLeHoan = new BigDecimal("0.5");
                    tienHoanLai = tienCocBanDau.multiply(new BigDecimal("0.5")).setScale(0, RoundingMode.HALF_UP);
                    phiPhatHuy = tienCocBanDau.subtract(tienHoanLai);
                }

                // 5. Cập nhật trạng thái phiếu đặt phòng -> DA_HUY
                boolean okDp = datPhongDAO.updateTrangThai(conn, maDatPhong, TrangThaiDatPhong.DA_HUY);
                if (!okDp) {
                    throw new BusinessException("Cập nhật trạng thái hủy thất bại.");
                }

                // 6. Khóa và giải phóng trạng thái phòng về AVAILABLE nếu đang là RESERVED
                if (dp.getPhong() != null) {
                    String maPhong = dp.getPhong().getMaPhong();
                    Optional<Phong> pOpt = phongDAO.findByIdForUpdate(conn, maPhong);
                    if (pOpt.isPresent()) {
                        Phong p = pOpt.get();
                        if (p.getTrangThaiHienTai() == TrangThaiPhong.RESERVED) {
                            boolean conDatPhongKhac = datPhongDAO.hasDatPhongXacNhanTrongNgay(conn, maPhong, LocalDate.now(), maDatPhong);
                            if (!conDatPhongKhac) {
                                phongDAO.updateTrangThai(conn, maPhong, TrangThaiPhong.AVAILABLE);
                            }
                        }
                    }
                }

                String maHoaDonQuyetToan = null;
                String maThanhToanHoan = null;

                // 7. Thiết lập Bút toán Tài chính đối ứng nếu đơn có tiền cọc
                if (tienCocBanDau.compareTo(BigDecimal.ZERO) > 0) {
                    maHoaDonQuyetToan = IdGenerator.next(conn, "HD");
                    NhanVien nvHienTai = AuthService.getNhanVienHienTai();
                    NhanVien nvLap = nvHienTai != null ? nvHienTai : dp.getNhanVienLap();

                    // Tạo hóa đơn quyết toán thanh lý cọc (tong_tien = phí phạt hủy tính vào doanh thu)
                    HoaDon hdHuy = new HoaDon(
                            maHoaDonQuyetToan,
                            dp,
                            nvLap,
                            thoiDiemHuy,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            tienCocBanDau,
                            phiPhatHuy,
                            TrangThaiThanhToan.DA_THANH_TOAN
                    );
                    hoaDonDAO.save(conn, hdHuy);

                    // Nếu có tiền hoàn lại khách -> tạo bản ghi chi trả hoàn cọc
                    if (tienHoanLai.compareTo(BigDecimal.ZERO) > 0) {
                        maThanhToanHoan = IdGenerator.next(conn, "TT");
                        PhuongThucThanhToan pt = phuongThucHoan != null ? phuongThucHoan : PhuongThucThanhToan.TIEN_MAT;
                        String strTyLe = tyLeHoan.compareTo(new BigDecimal("1.0")) == 0 ? "100%" : "50%";
                        String ghiChu = String.format("Hoàn %s tiền cọc do hủy phòng (Còn %d giờ trước check-in). Lý do: %s",
                                strTyLe, soPhutConLai / 60, (lyDoHuy != null ? lyDoHuy : "Khách yêu cầu"));

                        ThanhToan ttHoan = new ThanhToan(
                                maThanhToanHoan,
                                maHoaDonQuyetToan,
                                tienHoanLai,
                                pt,
                                thoiDiemHuy,
                                nvHienTai != null ? nvHienTai.getMaNhanVien() : null,
                                ghiChu
                        );
                        thanhToanDAO.save(conn, ttHoan);
                    }
                }

                conn.commit();

                KetQuaHuyDatPhong ketQua = new KetQuaHuyDatPhong(
                        maDatPhong,
                        tienCocBanDau,
                        tyLeHoan,
                        tienHoanLai,
                        phiPhatHuy,
                        thoiDiemHuy,
                        soPhutConLai / 60,
                        maHoaDonQuyetToan,
                        maThanhToanHoan,
                        lyDoHuy
                );

                // Đồng bộ sự kiện thời gian thực
                AppEventBus.fireBookingChanged();
                AppEventBus.fireRoomStatusChanged();
                if (maHoaDonQuyetToan != null) {
                    AppEventBus.fireInvoiceChanged();
                }

                return ketQua;
            } catch (Exception ex) {
                conn.rollback();
                if (ex instanceof BusinessException) throw (BusinessException) ex;
                throw new BusinessException("Lỗi khi hủy đặt phòng: " + ex.getMessage(), ex);
            }
        } catch (SQLException ex) {
            throw new BusinessException("Lỗi kết nối CSDL khi hủy đặt phòng: " + ex.getMessage(), ex);
        }
    }
}
