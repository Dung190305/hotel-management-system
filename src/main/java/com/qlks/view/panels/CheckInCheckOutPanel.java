package com.qlks.view.panels;

import com.qlks.controller.CheckInCheckOutController;
import com.qlks.model.ChiTietDichVu;
import com.qlks.model.DatPhong;
import com.qlks.model.DichVu;
import com.qlks.model.HoaDon;
import com.qlks.model.enums.TrangThaiDatPhong;
import com.qlks.util.FormatUtil;
import com.qlks.util.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.qlks.util.AppEventBus;
import com.qlks.view.RefreshablePanel;
import com.qlks.service.RoomOrderService;

import java.util.List;
import java.util.Optional;

/**
 * Màn hình Check-in & Check-out chuyên biệt cho Lễ tân khách sạn.
 */
public class CheckInCheckOutPanel extends JPanel implements RefreshablePanel, AppEventBus.AppEventListener {

    private final CheckInCheckOutController controller;

    private JTable tblChoCheckIn;
    private DefaultTableModel modelChoCheckIn;

    private JTable tblDangO;
    private DefaultTableModel modelDangO;

    private JButton btnXacNhanCheckIn;
    private JButton btnXacNhanCheckOut;

    public CheckInCheckOutPanel() {
        this.controller = new CheckInCheckOutController();

        initComponents();
        AppEventBus.register(this);
        loadDuLieu();
    }

    @Override
    public void onEvent(AppEventBus.EventType eventType) {
        if (eventType == AppEventBus.EventType.BOOKING_CHANGED || eventType == AppEventBus.EventType.ROOM_STATUS_CHANGED) {
            loadDuLieu();
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UIUtil.COLOR_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header
        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setBackground(Color.WHITE);
        pnlTop.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 233, 238)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblTitle = new JLabel("TIẾP TÂN: CHECK-IN & CHECK-OUT");
        lblTitle.setFont(UIUtil.FONT_TITLE);
        lblTitle.setForeground(UIUtil.COLOR_PRIMARY);
        pnlTop.add(lblTitle, BorderLayout.WEST);

        JButton btnLamMoi = UIUtil.createButton("Làm mới danh sách", new Color(108, 117, 125), Color.WHITE);
        btnLamMoi.addActionListener(e -> loadDuLieu());
        pnlTop.add(btnLamMoi, BorderLayout.EAST);

        add(pnlTop, BorderLayout.NORTH);

        // Center: 2 Tab
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIUtil.FONT_BOLD);

        // TAB 1: DANH SÁCH CHỜ CHECK-IN
        JPanel pnlTab1 = new JPanel(new BorderLayout(8, 8));
        pnlTab1.setBackground(Color.WHITE);
        pnlTab1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] headersCheckIn = {"Mã ĐP", "Khách hàng", "Số điện thoại", "Phòng", "Loại phòng", "Ngày nhận", "Ngày trả", "Tiền cọc"};
        modelChoCheckIn = new DefaultTableModel(headersCheckIn, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblChoCheckIn = new JTable(modelChoCheckIn);
        UIUtil.formatTable(tblChoCheckIn);
        pnlTab1.add(new JScrollPane(tblChoCheckIn), BorderLayout.CENTER);

        JPanel pnlBtn1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlBtn1.setBackground(Color.WHITE);
        JButton btnXemKhach = UIUtil.createButton("Xem khách lưu trú (CCCD)", new Color(23, 162, 184), Color.WHITE);
        btnXemKhach.addActionListener(e -> xemKhachLuuTruChoCheckIn());
        pnlBtn1.add(btnXemKhach);

        btnXacNhanCheckIn = UIUtil.createButton("Xác nhận Check-in", new Color(40, 167, 69), Color.WHITE);
        btnXacNhanCheckIn.addActionListener(e -> xuLyCheckIn());
        pnlBtn1.add(btnXacNhanCheckIn);
        pnlTab1.add(pnlBtn1, BorderLayout.SOUTH);

        tabbedPane.addTab("1. Khách đến nhận phòng (Check-in)", pnlTab1);

        // TAB 2: DANH SÁCH ĐANG Ở CHỜ CHECK-OUT
        JPanel pnlTab2 = new JPanel(new BorderLayout(8, 8));
        pnlTab2.setBackground(Color.WHITE);
        pnlTab2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] headersCheckOut = {"Mã ĐP", "Khách hàng", "Số điện thoại", "Phòng", "Giờ nhận thực tế", "Dự kiến trả", "Tạm tính"};
        modelDangO = new DefaultTableModel(headersCheckOut, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblDangO = new JTable(modelDangO);
        UIUtil.formatTable(tblDangO);
        pnlTab2.add(new JScrollPane(tblDangO), BorderLayout.CENTER);

        JPanel pnlBtn2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlBtn2.setBackground(Color.WHITE);
        JButton btnXemKhachDangO = UIUtil.createButton("Xem khách lưu trú (CCCD)", new Color(23, 162, 184), Color.WHITE);
        btnXemKhachDangO.addActionListener(e -> xemKhachLuuTruDangO());
        pnlBtn2.add(btnXemKhachDangO);

        JButton btnGhiDichVu = UIUtil.createButton("+ Gọi dịch vụ", new Color(0, 123, 255), Color.WHITE);
        btnGhiDichVu.addActionListener(e -> xuLyGhiDichVu());
        pnlBtn2.add(btnGhiDichVu);

        JButton btnXemDichVu = UIUtil.createButton("Dịch vụ đã dùng", new Color(108, 117, 125), Color.WHITE);
        btnXemDichVu.addActionListener(e -> xuLyXemDichVu());
        pnlBtn2.add(btnXemDichVu);

        btnXacNhanCheckOut = UIUtil.createButton("Trả phòng & Lập hóa đơn (Check-out)", new Color(220, 53, 69), Color.WHITE);
        btnXacNhanCheckOut.addActionListener(e -> xuLyCheckOut());
        pnlBtn2.add(btnXacNhanCheckOut);
        pnlTab2.add(pnlBtn2, BorderLayout.SOUTH);

        tabbedPane.addTab("2. Khách đang lưu trú (Check-out)", pnlTab2);

        add(tabbedPane, BorderLayout.CENTER);
    }

    @Override
    public void loadDuLieu() {
        SwingWorker<List<DatPhong>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<DatPhong> doInBackground() {
                return controller.layTatCaDatPhong();
            }

            @Override
            protected void done() {
                try {
                    List<DatPhong> tatCa = get();
                    hienThiDuLieuLenBang(tatCa);
                } catch (Exception e) {
                    // Xử lý an toàn nếu xảy ra lỗi trong background worker
                }
            }
        };
        worker.execute();
    }

    private void hienThiDuLieuLenBang(List<DatPhong> tatCa) {
        if (tatCa == null) return;

        // 1. Lọc các phiếu chờ check-in (Đã xác nhận)
        modelChoCheckIn.setRowCount(0);
        for (DatPhong dp : tatCa) {
            if (dp.getTrangThai() == TrangThaiDatPhong.DA_XAC_NHAN) {
                modelChoCheckIn.addRow(new Object[]{
                        dp.getMaDatPhong(),
                        dp.getKhachHang() != null ? dp.getKhachHang().getHoTen() : "",
                        dp.getKhachHang() != null ? dp.getKhachHang().getSoDienThoai() : "",
                        dp.getPhong() != null ? "P." + dp.getPhong().getSoPhong() : "",
                        dp.getPhong() != null ? dp.getPhong().getTenLoaiPhongHienThi() : "",
                        FormatUtil.formatNgayGio(dp.getNgayNhanPhong()),
                        FormatUtil.formatNgayGio(dp.getNgayTraPhong()),
                        FormatUtil.formatTien(dp.getTienCoc())
                });
            }
        }

        // 2. Lọc các phiếu đang ở (DANG_O)
        modelDangO.setRowCount(0);
        for (DatPhong dp : tatCa) {
            if (dp.getTrangThai() == TrangThaiDatPhong.DANG_O) {
                modelDangO.addRow(new Object[]{
                        dp.getMaDatPhong(),
                        dp.getKhachHang() != null ? dp.getKhachHang().getHoTen() : "",
                        dp.getKhachHang() != null ? dp.getKhachHang().getSoDienThoai() : "",
                        dp.getPhong() != null ? "P." + dp.getPhong().getSoPhong() : "",
                        FormatUtil.formatNgayGio(dp.getThoiGianNhanThucTe()),
                        FormatUtil.formatNgayGio(dp.getNgayTraPhong()),
                        FormatUtil.formatTien(dp.tinhTienTamTinh())
                });
            }
        }
    }

    private void xemKhachLuuTruChoCheckIn() {
        int row = tblChoCheckIn.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn một phiếu đặt phòng để xem danh sách khách lưu trú.");
            return;
        }
        String maDp = (String) modelChoCheckIn.getValueAt(row, 0);
        hienThiDanhSachKhachDialog(maDp);
    }

    private void xemKhachLuuTruDangO() {
        int row = tblDangO.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn một phiếu đặt phòng để xem danh sách khách lưu trú.");
            return;
        }
        String maDp = (String) modelDangO.getValueAt(row, 0);
        hienThiDanhSachKhachDialog(maDp);
    }

    private void hienThiDanhSachKhachDialog(String maDp) {
        DatPhong dp = controller.timTheoMa(maDp).orElse(null);
        if (dp == null) {
            UIUtil.showError(this, "Không tìm thấy thông tin đặt phòng " + maDp);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("DANH SÁCH KHÁCH LƯU TRÚ (ĐỐI CHIẾU CCCD)\n");
        sb.append("Mã đặt phòng: ").append(dp.getMaDatPhong()).append("\n");
        sb.append("Phòng: P.").append(dp.getPhong() != null ? dp.getPhong().getSoPhong() : "N/A")
                .append(" (").append(dp.getPhong() != null ? dp.getPhong().getTenLoaiPhongHienThi() : "").append(")\n");
        sb.append("Sức chứa tối đa: ").append(dp.getPhong() != null ? dp.getPhong().getSucChuaToiDa() : 0).append(" người\n");
        sb.append("--------------------------------------------------------------------\n");
        sb.append(String.format("%-4s | %-22s | %-16s | %-12s | %s\n", "STT", "Họ và tên", "CCCD (Che bảo mật)", "SĐT", "Vai trò"));
        sb.append("--------------------------------------------------------------------\n");

        if (dp.getDanhSachKhach() != null && !dp.getDanhSachKhach().isEmpty()) {
            int stt = 1;
            for (com.qlks.model.KhachLuuTru k : dp.getDanhSachKhach()) {
                sb.append(String.format("%-4d | %-22s | %-16s | %-12s | %s\n",
                        stt++,
                        k.getHoTen(),
                        k.getSoCCCDChe(),
                        k.getSoDienThoai() != null ? k.getSoDienThoai() : "",
                        k.getVaiTro() != null ? k.getVaiTro().getTenHienThi() : ""));
            }
        } else {
            sb.append("Chưa có thông tin khách lưu trú chi tiết!\n");
        }
        sb.append("--------------------------------------------------------------------\n");
        sb.append("(*) Yêu cầu: Tất cả khách lưu trú đều phải có CCCD 12 số hợp lệ.");

        JTextArea txt = new JTextArea(sb.toString());
        txt.setFont(new Font("Consolas", Font.PLAIN, 13));
        txt.setEditable(false);
        JScrollPane sp = new JScrollPane(txt);
        sp.setPreferredSize(new Dimension(550, 300));
        JOptionPane.showMessageDialog(this, sp, "Khách lưu trú - " + maDp, JOptionPane.INFORMATION_MESSAGE);
    }

    private void xuLyCheckIn() {
        int row = tblChoCheckIn.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn khách hàng nhận phòng.");
            return;
        }

        String maDp = (String) modelChoCheckIn.getValueAt(row, 0);
        DatPhong dp = controller.timTheoMa(maDp).orElse(null);
        if (dp == null) {
            UIUtil.showError(this, "Không tìm thấy thông tin đặt phòng.");
            return;
        }

        // Kiểm tra danh sách khách trước khi check-in
        if (dp.getDanhSachKhach() == null || dp.getDanhSachKhach().isEmpty()) {
            UIUtil.showError(this, "Không thể check-in: Phiếu đặt phòng chưa có thông tin khách lưu trú kèm CCCD 12 số!");
            return;
        }

        // Hiển thị danh sách khách và yêu cầu Lễ tân đối chiếu CCCD thực tế
        StringBuilder sb = new StringBuilder();
        sb.append("XÁC NHẬN CHECK-IN VÀ ĐỐI CHIẾU CCCD THỰC TẾ\n\n");
        sb.append("Mã ĐP: ").append(dp.getMaDatPhong()).append(" - Phòng: P.").append(dp.getPhong().getSoPhong()).append("\n");
        sb.append("Khách đại diện: ").append(dp.getKhachHang().getHoTen()).append(" (SĐT: ").append(dp.getKhachHang().getSoDienThoai()).append(")\n");
        sb.append("Danh sách khách lưu trú (").append(dp.getDanhSachKhach().size()).append(" người):\n");
        for (int i = 0; i < dp.getDanhSachKhach().size(); i++) {
            com.qlks.model.KhachLuuTru k = dp.getDanhSachKhach().get(i);
            sb.append(String.format("  %d. %s - CCCD: %s (%s)\n",
                    i + 1, k.getHoTen(), k.getSoCCCDChe(), k.getVaiTro().getTenHienThi()));
        }
        sb.append("\nLễ tân đã kiểm tra và đối chiếu CCCD gốc của tất cả khách trên?");

        int confirm = JOptionPane.showConfirmDialog(
                this,
                sb.toString(),
                "Đối chiếu CCCD và Check-in",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        // Kiểm tra và cảnh báo Phụ thu Nhận phòng sớm (Early Check-in)
        LocalDateTime thoiGianCheckIn = LocalDateTime.now();
        com.qlks.model.dto.ThongTinPhuThu phuThuEarly = controller.tinhPhuThuEarlyCheckIn(dp.getPhong(), thoiGianCheckIn, dp.getNgayNhanPhong());
        if (phuThuEarly.coPhuThu()) {
            StringBuilder sbEarly = new StringBuilder();
            sbEarly.append("CẢNH BÁO: NHẬN PHÒNG SỚM (EARLY CHECK-IN)\n\n");
            sbEarly.append("• Giờ quy chuẩn nhận phòng: 14:00 ngày ").append(dp.getNgayNhanPhong().toLocalDate()).append("\n");
            sbEarly.append("• Thời gian khách đến:      ").append(FormatUtil.formatNgayGio(thoiGianCheckIn)).append("\n");
            sbEarly.append("• Quy định phụ thu:         ").append(phuThuEarly.getMoTa()).append("\n");
            sbEarly.append("• Mức phụ thu:              ").append(phuThuEarly.getPhanTramPhuThu()).append("% giá phòng 1 ngày\n");
            sbEarly.append("• Số tiền phụ thu dự kiến:  ").append(FormatUtil.formatTien(phuThuEarly.getSoTienPhuThu())).append(" VNĐ\n\n");
            sbEarly.append("Khoản phụ thu này sẽ tự động được cộng vào Tiền phòng khi trả phòng (Check-out).\n");
            sbEarly.append("Lễ tân đã thông báo và khách hàng đã đồng ý mức phụ thu này?");

            int opt = JOptionPane.showConfirmDialog(
                    this,
                    sbEarly.toString(),
                    "Xác nhận Phụ thu Early Check-in",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (opt != JOptionPane.YES_OPTION) {
                return;
            }
        }

        // Chống race condition & debounce: Vô hiệu hóa nút và đổi con trỏ chuột sang trạng thái chờ
        if (btnXacNhanCheckIn != null) {
            btnXacNhanCheckIn.setEnabled(false);
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                controller.xacNhanCheckIn(maDp, thoiGianCheckIn);
                return null;
            }

            @Override
            protected void done() {
                if (btnXacNhanCheckIn != null) {
                    btnXacNhanCheckIn.setEnabled(true);
                }
                setCursor(Cursor.getDefaultCursor());
                try {
                    get();
                    UIUtil.showSuccess(CheckInCheckOutPanel.this,
                            "Khách đã Check-in thành công!\nPhòng P." + dp.getPhong().getSoPhong() + " đã chuyển sang trạng thái ĐANG CÓ KHÁCH.");
                    loadDuLieu();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    UIUtil.showError(CheckInCheckOutPanel.this, cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void xuLyCheckOut() {
        int row = tblDangO.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn phòng cần làm thủ tục trả phòng.");
            return;
        }

        String maDp = (String) modelDangO.getValueAt(row, 0);
        String phong = (String) modelDangO.getValueAt(row, 3);

        // Kiểm tra xem phòng có đơn gọi dịch vụ qua QR đang chờ tiếp nhận hay không
        RoomOrderService roomOrderService = new RoomOrderService();
        Optional<DatPhong> dpCheckOpt = controller.timTheoMa(maDp);
        if (dpCheckOpt.isPresent() && dpCheckOpt.get().getPhong() != null) {
            String maPhongCheck = dpCheckOpt.get().getPhong().getMaPhong();
            if (roomOrderService.coDonChoDuyetCuaPhong(maPhongCheck)) {
                int opt = JOptionPane.showConfirmDialog(
                        this,
                        "CẢNH BÁO: Phòng " + phong + " hiện đang có đơn dịch vụ đặt qua mã QR chưa được tiếp nhận!\n" +
                        "Nếu tiếp tục làm thủ tục trả phòng, các đơn chờ này sẽ tự động bị hủy.\n" +
                        "Bạn có chắc chắn muốn tiếp tục Check-out?",
                        "Cảnh báo đơn dịch vụ phòng",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (opt != JOptionPane.YES_OPTION) {
                    return;
                }
                roomOrderService.huyCacDonChoDuyetCuaPhong(maPhongCheck, "Khách đã làm thủ tục trả phòng (Check-out)");
            }
        }

        if (!UIUtil.showConfirm(this, "Xác nhận trả phòng cho " + phong + " và tạo hóa đơn thanh toán?")) {
            return;
        }

        // Chống race condition & debounce: Vô hiệu hóa nút và đổi con trỏ chuột sang trạng thái chờ
        if (btnXacNhanCheckOut != null) {
            btnXacNhanCheckOut.setEnabled(false);
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        SwingWorker<HoaDon, Void> worker = new SwingWorker<>() {
            @Override
            protected HoaDon doInBackground() throws Exception {
                return controller.xacNhanCheckOut(maDp);
            }

            @Override
            protected void done() {
                if (btnXacNhanCheckOut != null) {
                    btnXacNhanCheckOut.setEnabled(true);
                }
                setCursor(Cursor.getDefaultCursor());
                try {
                    HoaDon hd = get();
                    String msg = String.format(
                            "Trả phòng thành công!\n" +
                            "Phòng %s đã chuyển sang trạng thái ĐANG DỌN DẸP.\n\n" +
                            "══════════════ CHI TIẾT HÓA ĐƠN: %s ══════════════\n" +
                            "• Tiền phòng:              %s\n" +
                            "• Tiền dịch vụ:            %s\n" +
                            "• Giảm giá khuyến mãi:    -%s\n" +
                            "--------------------------------------------------\n" +
                            "• TỔNG CHI PHÍ:            %s\n" +
                            "• Tiền cọc đã nhận:       -%s\n" +
                            "==================================================\n" +
                            "• THỰC THU CÒN LẠI:        %s",
                            phong,
                            hd.getMaHoaDon(),
                            FormatUtil.formatTien(hd.getTienPhong()),
                            FormatUtil.formatTien(hd.getTienDichVu()),
                            FormatUtil.formatTien(hd.getTienGiamGia()),
                            FormatUtil.formatTien(hd.getTongChiPhi()),
                            FormatUtil.formatTien(hd.getTienCoc()),
                            FormatUtil.formatTien(hd.getConLaiPhaiThu())
                    );
                    UIUtil.showSuccess(CheckInCheckOutPanel.this, msg);
                    loadDuLieu();
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    UIUtil.showError(CheckInCheckOutPanel.this, cause.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void xuLyGhiDichVu() {
        int row = tblDangO.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn phòng đang lưu trú để gọi dịch vụ.");
            return;
        }

        String maDp = (String) modelDangO.getValueAt(row, 0);
        String soPhong = (String) modelDangO.getValueAt(row, 3);

        List<DichVu> dsDichVu = controller.layDichVuDangKinhDoanh();
        if (dsDichVu.isEmpty()) {
            UIUtil.showError(this, "Hiện không có dịch vụ nào đang kinh doanh.");
            return;
        }

        JComboBox<DichVu> cboDichVu = new JComboBox<>(dsDichVu.toArray(new DichVu[0]));
        JSpinner spnSoLuong = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));

        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.add(new JLabel("Chọn dịch vụ:"));
        panel.add(cboDichVu);
        panel.add(new JLabel("Số lượng:"));
        panel.add(spnSoLuong);

        int result = JOptionPane.showConfirmDialog(this, panel, "Gọi dịch vụ cho " + soPhong + " (" + maDp + ")", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                DichVu dv = (DichVu) cboDichVu.getSelectedItem();
                int soLuong = (Integer) spnSoLuong.getValue();
                if (dv != null) {
                    controller.ghiNhanDichVu(maDp, dv.getMaDichVu(), soLuong);
                    UIUtil.showSuccess(this, "Đã ghi nhận " + soLuong + "x " + dv.getTenDichVu() + " cho phòng " + soPhong + "!");
                    loadDuLieu();
                }
            } catch (Exception ex) {
                UIUtil.showError(this, ex.getMessage());
            }
        }
    }

    private void xuLyXemDichVu() {
        int row = tblDangO.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn phòng đang lưu trú để xem dịch vụ.");
            return;
        }

        String maDp = (String) modelDangO.getValueAt(row, 0);
        String soPhong = (String) modelDangO.getValueAt(row, 3);

        List<ChiTietDichVu> ds = controller.layDichVuTheoDatPhong(maDp);
        if (ds.isEmpty()) {
            UIUtil.showInfo(this, "Phòng " + soPhong + " (" + maDp + ") chưa sử dụng dịch vụ nào.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("DANH SÁCH DỊCH VỤ ĐÃ DÙNG - ").append(soPhong).append(" (").append(maDp).append(")\n\n");
        java.math.BigDecimal tongTien = java.math.BigDecimal.ZERO;
        for (int i = 0; i < ds.size(); i++) {
            ChiTietDichVu ct = ds.get(i);
            tongTien = tongTien.add(ct.tinhThanhTien());
            sb.append(String.format("  %d. %s x %d = %s (Đơn giá: %s lúc %s)\n",
                    i + 1,
                    ct.getDichVu() != null ? ct.getDichVu().getTenDichVu() : ct.getMaDichVu(),
                    ct.getSoLuong(),
                    FormatUtil.formatTien(ct.tinhThanhTien()),
                    FormatUtil.formatTien(ct.getDonGiaLucDung()),
                    FormatUtil.formatNgayGio(ct.getThoiGianSuDung())));
        }
        sb.append("\n═══════════════════════════════════════════\n");
        sb.append("TỔNG TIỀN DỊCH VỤ: ").append(FormatUtil.formatTien(tongTien));

        JTextArea txt = new JTextArea(sb.toString());
        txt.setFont(new Font("Consolas", Font.PLAIN, 13));
        txt.setEditable(false);
        JScrollPane scroll = new JScrollPane(txt);
        scroll.setPreferredSize(new Dimension(520, 300));
        JOptionPane.showMessageDialog(this, scroll, "Dịch vụ phòng " + soPhong, JOptionPane.INFORMATION_MESSAGE);
    }
}
