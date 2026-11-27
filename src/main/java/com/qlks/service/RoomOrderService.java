package com.qlks.service;

import com.qlks.dao.DatPhongDAO;
import com.qlks.dao.DichVuDAO;
import com.qlks.dao.PhongDAO;
import com.qlks.dao.impl.DatPhongDAOImpl;
import com.qlks.dao.impl.DichVuDAOImpl;
import com.qlks.dao.impl.PhongDAOImpl;
import com.qlks.exception.BusinessException;
import com.qlks.model.DatPhong;
import com.qlks.model.DichVu;
import com.qlks.model.DonDichVuPhong;
import com.qlks.model.DonDichVuPhong.ChiTietMon;
import com.qlks.model.DonDichVuPhong.HinhThucThanhToanOrder;
import com.qlks.model.DonDichVuPhong.TrangThaiOrder;
import com.qlks.model.Phong;
import com.qlks.model.enums.TrangThaiDatPhong;
import com.qlks.model.enums.TrangThaiPhong;
import com.qlks.util.AppEventBus;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service quản lý đơn đặt dịch vụ tại phòng qua mã QR (In-Room QR Order Service).
 * Tích hợp kiểm soát tương tranh toàn diện:
 * 1. Chống Double-Submit qua Idempotency Key (UUID).
 * 2. Chống Race Condition với luồng Check-out qua Pessimistic Row Lock.
 * 3. Kiểm soát khung giờ phục vụ và trạng thái phòng đang lưu trú (DANG_O).
 */
public class RoomOrderService {

    private static final Logger LOGGER = Logger.getLogger(RoomOrderService.class.getName());

    // Bộ nhớ lưu trữ đơn hàng đang phục vụ trong phiên làm việc
    private static final Map<String, DonDichVuPhong> ORDERS_MAP = new ConcurrentHashMap<>();
    // Bộ nhớ đệm Idempotency Key (chống gửi trùng đơn khi mạng điện thoại lag)
    private static final Map<String, DonDichVuPhong> IDEMPOTENCY_CACHE = new ConcurrentHashMap<>();
    // Bộ đếm sinh mã đơn tuần tự an toàn đa luồng chống va chạm ID
    private static final AtomicLong ORDER_COUNTER = new AtomicLong(0);

    private final DatPhongDAO datPhongDAO;
    private final PhongDAO phongDAO;
    private final DichVuDAO dichVuDAO;
    private final DichVuService dichVuService;

    // Cấu hình khung giờ phục vụ Room Service (Mặc định 06:00 - 23:59)
    private static volatile LocalTime gioMoCua = LocalTime.of(6, 0);
    private static volatile LocalTime gioDongCua = LocalTime.of(23, 59);

    public RoomOrderService() {
        this.datPhongDAO = new DatPhongDAOImpl();
        this.phongDAO = new PhongDAOImpl();
        this.dichVuDAO = new DichVuDAOImpl();
        this.dichVuService = new DichVuService();
    }

    public RoomOrderService(DatPhongDAO datPhongDAO, PhongDAO phongDAO, DichVuDAO dichVuDAO, DichVuService dichVuService) {
        this.datPhongDAO = datPhongDAO;
        this.phongDAO = phongDAO;
        this.dichVuDAO = dichVuDAO;
        this.dichVuService = dichVuService;
    }

    public static void setKhungGioPhucVu(LocalTime moCua, LocalTime dongCua) {
        gioMoCua = moCua;
        gioDongCua = dongCua;
    }

    public static boolean isInOperatingHours() {
        LocalTime now = LocalTime.now();
        return !now.isBefore(gioMoCua) && !now.isAfter(gioDongCua);
    }

    /**
     * Tạo yêu cầu gọi dịch vụ phòng từ điện thoại khách.
     *
     * @param maPhongOrSoPhong Mã phòng hoặc số phòng (VD: "P101" hoặc "101")
     * @param clientToken      Khóa Idempotency sinh bởi thiết bị khách (chống gửi lặp)
     * @param items            Danh sách món và số lượng
     * @param ghiChu           Ghi chú của khách (mang thêm đá, ly...)
     * @param hinhThuc         Hình thức thanh toán (Ghi nợ phòng / VietQR)
     * @return DonDichVuPhong đơn đặt món đã tạo
     */
    public DonDichVuPhong taoYeuCauOrder(String maPhongOrSoPhong, String clientToken,
                                         List<ChiTietMon> items, String ghiChu,
                                         HinhThucThanhToanOrder hinhThuc) {
        // 1. Kiểm tra Idempotency Token chống Double-Submit bằng khóa nguyên tử theo token
        if (clientToken != null && !clientToken.isBlank()) {
            synchronized (clientToken.intern()) {
                DonDichVuPhong existing = IDEMPOTENCY_CACHE.get(clientToken);
                if (existing != null) {
                    return existing;
                }
                return thucHienTaoOrder(maPhongOrSoPhong, clientToken, items, ghiChu, hinhThuc);
            }
        }

        return thucHienTaoOrder(maPhongOrSoPhong, clientToken, items, ghiChu, hinhThuc);
    }

    private DonDichVuPhong thucHienTaoOrder(String maPhongOrSoPhong, String clientToken,
                                            List<ChiTietMon> items, String ghiChu,
                                            HinhThucThanhToanOrder hinhThuc) {
        // 2. Kiểm tra giờ phục vụ
        if (!isInOperatingHours()) {
            throw new BusinessException(String.format(
                    "Dịch vụ phòng hiện đang tạm nghỉ (Giờ phục vụ: %s - %s). Vui lòng liên hệ Lễ tân qua điện thoại để được hỗ trợ khẩn cấp.",
                    gioMoCua, gioDongCua));
        }

        if (items == null || items.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất 1 dịch vụ.");
        }

        // 3. Tìm thông tin phòng
        Optional<Phong> phongOpt = phongDAO.findById(maPhongOrSoPhong);
        if (phongOpt.isEmpty()) {
            phongOpt = phongDAO.findBySoPhong(maPhongOrSoPhong);
        }
        if (phongOpt.isEmpty()) {
            throw new BusinessException("Không tìm thấy thông tin phòng: " + maPhongOrSoPhong);
        }
        Phong phong = phongOpt.get();

        // 4. Kiểm tra phòng bắt buộc phải đang có khách lưu trú (OCCUPIED)
        if (phong.getTrangThaiHienTai() != TrangThaiPhong.OCCUPIED) {
            throw new BusinessException("Phòng " + phong.getSoPhong() + " hiện không có khách lưu trú. Chỉ phòng đang có khách mới có thể gọi dịch vụ!");
        }

        // 5. Tìm phiếu đặt phòng đang ở (DANG_O) của phòng này (Single-row indexed query)
        DatPhong dpHienTai = datPhongDAO.findDangOByPhong(phong.getMaPhong())
                .orElseThrow(() -> new BusinessException("Không tìm thấy phiếu lưu trú hợp lệ cho phòng " + phong.getSoPhong()));

        // 6. Kiểm tra giá và chụp ảnh đơn giá Snapshot của từng món
        List<ChiTietMon> danhSachMonValidated = new ArrayList<>();
        for (ChiTietMon m : items) {
            if (m.getSoLuong() <= 0) continue;
            Optional<DichVu> dvOpt = dichVuDAO.findById(m.getMaDichVu());
            if (dvOpt.isEmpty() || !dvOpt.get().isDangKinhDoanh()) {
                throw new BusinessException("Dịch vụ '" + m.getTenDichVu() + "' hiện tạm ngừng kinh doanh.");
            }
            DichVu dv = dvOpt.get();
            danhSachMonValidated.add(new ChiTietMon(dv.getMaDichVu(), dv.getTenDichVu(), m.getSoLuong(), dv.getDonGiaHienTai()));
        }

        if (danhSachMonValidated.isEmpty()) {
            throw new BusinessException("Vui lòng chọn ít nhất 1 món với số lượng lớn hơn 0.");
        }

        // 7. Tạo đối tượng đơn hàng chờ tiếp nhận với mã định danh duy nhất tuyệt đối (Zero-collision)
        long seq = ORDER_COUNTER.incrementAndGet() % 10000;
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        String salt = Integer.toHexString(ThreadLocalRandom.current().nextInt(0x100, 0xFFF)).toUpperCase();
        String orderId = String.format("ORD-%s-%04d-%s", timestamp, seq, salt);
        String tenKhach = (dpHienTai.getKhachHang() != null) ? dpHienTai.getKhachHang().getHoTen() : "Khách lưu trú";

        DonDichVuPhong order = new DonDichVuPhong(
                orderId,
                phong.getMaPhong(),
                phong.getSoPhong(),
                dpHienTai.getMaDatPhong(),
                tenKhach,
                danhSachMonValidated,
                ghiChu,
                hinhThuc
        );

        ORDERS_MAP.put(orderId, order);
        if (clientToken != null && !clientToken.isBlank()) {
            IDEMPOTENCY_CACHE.put(clientToken, order);
        }

        LOGGER.info(String.format("Nhận đơn dịch vụ phòng mới: %s - Phòng: %s - Tổng tiền: %s VNĐ",
                orderId, phong.getSoPhong(), order.tinhTongTien().toPlainString()));

        // 8. Bắn sự kiện thời gian thực cho màn hình Lễ tân
        AppEventBus.fireRoomOrderReceived();

        return order;
    }

    /**
     * Lễ tân tiếp nhận đơn và xác nhận xuất đồ giao lên phòng.
     * Tự động ghi nhận dịch vụ vào Hóa đơn phòng có khóa dòng bi quan chống Race Condition với Check-out.
     */
    public synchronized void tiepNhanVaGiaoHang(String orderId, String maNhanVienDuyet) {
        DonDichVuPhong order = ORDERS_MAP.get(orderId);
        if (order == null) {
            throw new BusinessException("Không tìm thấy đơn dịch vụ: " + orderId);
        }

        if (order.getTrangThai() != TrangThaiOrder.CHO_TIEP_NHAN) {
            throw new BusinessException("Đơn dịch vụ này đã được xử lý trước đó (Trạng thái: " + order.getTrangThai().getTenHienThi() + ")!");
        }

        try {
            // Ghi nhận toàn bộ danh sách món trong 1 Transaction nguyên tử duy nhất (Batch Processing)
            dichVuService.ghiNhanDanhSachDichVu(order.getMaDatPhong(), order.getDanhSachMon());

            order.setTrangThai(TrangThaiOrder.DA_TIEP_NHAN);
            order.setNguoiDuyet(maNhanVienDuyet);
            LOGGER.info("Đã duyệt đơn phòng " + orderId + " bởi nhân viên " + maNhanVienDuyet);
            AppEventBus.fireRoomOrderUpdated();

        } catch (BusinessException be) {
            // Nếu luồng Check-out đã hoàn tất trước đó, phòng không còn ở trạng thái DANG_O
            order.setTrangThai(TrangThaiOrder.TU_CHOI);
            order.setLyDoTuChoi("Đơn bị hủy tự động: " + be.getMessage());
            AppEventBus.fireRoomOrderUpdated();
            throw new BusinessException("Không thể duyệt đơn: " + be.getMessage(), be);
        }
    }

    /**
     * Từ chối đơn dịch vụ phòng (ví dụ hết hàng hoặc khách gọi nhầm).
     */
    public synchronized void tuChoiOrder(String orderId, String lyDo, String maNhanVien) {
        DonDichVuPhong order = ORDERS_MAP.get(orderId);
        if (order == null) {
            throw new BusinessException("Không tìm thấy đơn dịch vụ: " + orderId);
        }

        order.setTrangThai(TrangThaiOrder.TU_CHOI);
        order.setLyDoTuChoi(lyDo != null ? lyDo : "Hết hàng hoặc Lễ tân từ chối");
        order.setNguoiDuyet(maNhanVien);
        AppEventBus.fireRoomOrderUpdated();
    }

    /**
     * Lấy danh sách các đơn đang chờ tiếp nhận (phục vụ hiển thị trên giao diện Lễ tân).
     */
    public List<DonDichVuPhong> layDanhSachChoDuyet() {
        List<DonDichVuPhong> ketQua = new ArrayList<>();
        for (DonDichVuPhong o : ORDERS_MAP.values()) {
            if (o.getTrangThai() == TrangThaiOrder.CHO_TIEP_NHAN) {
                ketQua.add(o);
            }
        }
        ketQua.sort((a, b) -> b.getThoiGianDat().compareTo(a.getThoiGianDat()));
        return Collections.unmodifiableList(ketQua);
    }

    /**
     * Lấy toàn bộ danh sách đơn đặt trong ngày.
     */
    public List<DonDichVuPhong> layTatCaDon() {
        List<DonDichVuPhong> ketQua = new ArrayList<>(ORDERS_MAP.values());
        ketQua.sort((a, b) -> b.getThoiGianDat().compareTo(a.getThoiGianDat()));
        return Collections.unmodifiableList(ketQua);
    }

    /**
     * Kiểm tra xem phòng có đơn hàng nào đang ở trạng thái CHỜ TIẾP NHẬN hay không.
     * Sử dụng để cảnh báo Lễ tân khi chuẩn bị bấm Check-out.
     */
    public boolean coDonChoDuyetCuaPhong(String maPhong) {
        if (maPhong == null) return false;
        for (DonDichVuPhong o : ORDERS_MAP.values()) {
            if (maPhong.equals(o.getMaPhong()) && o.getTrangThai() == TrangThaiOrder.CHO_TIEP_NHAN) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tự động hủy các đơn chờ duyệt khi phòng đã hoàn tất Check-out.
     */
    public void huyCacDonChoDuyetCuaPhong(String maPhong, String lyDo) {
        if (maPhong == null) return;
        for (DonDichVuPhong o : ORDERS_MAP.values()) {
            if (maPhong.equals(o.getMaPhong()) && o.getTrangThai() == TrangThaiOrder.CHO_TIEP_NHAN) {
                o.setTrangThai(TrangThaiOrder.TU_CHOI);
                o.setLyDoTuChoi(lyDo);
            }
        }
        AppEventBus.fireRoomOrderUpdated();
    }

    public Optional<DonDichVuPhong> timTheoId(String orderId) {
        if (orderId == null) return Optional.empty();
        return Optional.ofNullable(ORDERS_MAP.get(orderId));
    }

    public static void clearAllForTest() {
        ORDERS_MAP.clear();
        IDEMPOTENCY_CACHE.clear();
    }
}

