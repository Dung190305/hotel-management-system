package com.qlks.view.panels;

import com.qlks.controller.ThongKeController;
import com.qlks.model.HoaDon;
import com.qlks.util.AppEventBus;
import com.qlks.util.FormatUtil;
import com.qlks.util.UIUtil;
import com.qlks.view.RefreshablePanel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Màn hình Báo cáo Thống kê Doanh thu & Công suất phòng.
 * Tích hợp ThongKeController và SwingWorker cập nhật bất đồng bộ.
 */
public class ThongKePanel extends JPanel implements RefreshablePanel, AppEventBus.AppEventListener {

    private final ThongKeController thongKeController;

    private JLabel lblTongDoanhThu;
    private JLabel lblPhongDangO;
    private JLabel lblTongDatPhong;
    private JLabel lblDoanhThuDichVu;

    private JTable tblGiaoDich;
    private DefaultTableModel modelGiaoDich;

    public ThongKePanel() {
        this.thongKeController = new ThongKeController();

        initComponents();
        AppEventBus.register(this);
        loadDuLieu();
    }

    @Override
    public void onEvent(AppEventBus.EventType eventType) {
        if (!com.qlks.service.AuthorizationService.hasPermission(com.qlks.model.enums.Permission.XEM_BAO_CAO)) {
            return;
        }
        if (eventType == AppEventBus.EventType.INVOICE_CHANGED || eventType == AppEventBus.EventType.ROOM_STATUS_CHANGED || eventType == AppEventBus.EventType.BOOKING_CHANGED) {
            loadDuLieu();
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout(15, 15));
        setBackground(UIUtil.COLOR_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 1. THANH TIÊU ĐỀ
        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setBackground(Color.WHITE);
        pnlTop.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 233, 238)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblTitle = new JLabel("BÁO CÁO TỔNG QUAN & DOANH THU");
        lblTitle.setFont(UIUtil.FONT_TITLE);
        lblTitle.setForeground(UIUtil.COLOR_PRIMARY);
        pnlTop.add(lblTitle, BorderLayout.WEST);

        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlRight.setBackground(Color.WHITE);

        JButton btnBaoCaoNgay = UIUtil.createButton("Báo cáo theo ngày (SQL)", new Color(23, 162, 184), Color.WHITE);
        btnBaoCaoNgay.addActionListener(e -> hienThiBaoCaoTheoNgayDialog());
        pnlRight.add(btnBaoCaoNgay);

        JButton btnLamMoi = UIUtil.createButton("Cập nhật số liệu", UIUtil.COLOR_PRIMARY, Color.WHITE);
        btnLamMoi.addActionListener(e -> loadDuLieu());
        pnlRight.add(btnLamMoi);

        pnlTop.add(pnlRight, BorderLayout.EAST);
        add(pnlTop, BorderLayout.NORTH);

        // 2. 4 THẺ THỐNG KÊ (STAT CARDS)
        JPanel pnlCards = new JPanel(new GridLayout(1, 4, 15, 0));
        pnlCards.setBackground(UIUtil.COLOR_BACKGROUND);

        pnlCards.add(createStatCard("TỔNG DOANH THU", lblTongDoanhThu = new JLabel("0 VNĐ"), new Color(40, 167, 69)));
        pnlCards.add(createStatCard("CÔNG SUẤT PHÒNG (OCCUPANCY)", lblPhongDangO = new JLabel("0 / 0 (0%)"), new Color(220, 53, 69)));
        pnlCards.add(createStatCard("LƯỢT ĐẶT PHÒNG", lblTongDatPhong = new JLabel("0 đơn"), new Color(24, 119, 242)));
        pnlCards.add(createStatCard("DOANH THU DỊCH VỤ", lblDoanhThuDichVu = new JLabel("0 VNĐ"), new Color(253, 126, 20)));

        // 3. BẢNG HÓA ĐƠN ĐÃ THANH TOÁN
        JPanel pnlCenter = new JPanel(new BorderLayout(10, 10));
        pnlCenter.setBackground(Color.WHITE);
        pnlCenter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 233, 238)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblListTitle = new JLabel("DANH SÁCH HÓA ĐƠN ĐÃ THANH TOÁN THÀNH CÔNG");
        lblListTitle.setFont(UIUtil.FONT_HEADER);
        pnlCenter.add(lblListTitle, BorderLayout.NORTH);

        String[] headers = {"Mã HĐ", "Mã đặt phòng", "Ngày thanh toán", "Tiền phòng", "Tiền dịch vụ", "Giảm trừ", "Thực thu"};
        modelGiaoDich = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblGiaoDich = new JTable(modelGiaoDich);
        UIUtil.formatTable(tblGiaoDich);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        tblGiaoDich.getColumnModel().getColumn(3).setCellRenderer(right);
        tblGiaoDich.getColumnModel().getColumn(4).setCellRenderer(right);
        tblGiaoDich.getColumnModel().getColumn(5).setCellRenderer(right);
        tblGiaoDich.getColumnModel().getColumn(6).setCellRenderer(right);

        pnlCenter.add(new JScrollPane(tblGiaoDich), BorderLayout.CENTER);

        JPanel pnlMainContent = new JPanel(new BorderLayout(15, 15));
        pnlMainContent.setBackground(UIUtil.COLOR_BACKGROUND);
        pnlMainContent.add(pnlCards, BorderLayout.NORTH);
        pnlMainContent.add(pnlCenter, BorderLayout.CENTER);

        add(pnlMainContent, BorderLayout.CENTER);
    }

    private JPanel createStatCard(String title, JLabel lblValue, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(new Color(108, 117, 125));

        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblValue.setForeground(accentColor);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        return card;
    }

    public void loadDuLieu() {
        if (!com.qlks.service.AuthorizationService.hasPermission(com.qlks.model.enums.Permission.XEM_BAO_CAO)) {
            return;
        }
        thongKeController.layTongQuanAsync(this, data -> {
            int tongPhong = (Integer) data.getOrDefault("tongSoPhong", 0);
            long dangO = (Long) data.getOrDefault("soPhongDangO", 0L);
            double rate = tongPhong > 0 ? (dangO * 100.0 / tongPhong) : 0.0;
            lblPhongDangO.setText(String.format("%d / %d (%.1f%%)", dangO, tongPhong, rate));

            int tongDp = (Integer) data.getOrDefault("tongDatPhong", 0);
            lblTongDatPhong.setText(tongDp + " lượt");

            BigDecimal tongDoanhThu = (BigDecimal) data.getOrDefault("tongDoanhThu", BigDecimal.ZERO);
            BigDecimal tongDv = (BigDecimal) data.getOrDefault("tongTienDichVu", BigDecimal.ZERO);
            lblTongDoanhThu.setText(FormatUtil.formatTien(tongDoanhThu));
            lblDoanhThuDichVu.setText(FormatUtil.formatTien(tongDv));

            @SuppressWarnings("unchecked")
            List<HoaDon> dsHoaDon = (List<HoaDon>) data.get("danhSachHoaDon");
            modelGiaoDich.setRowCount(0);
            if (dsHoaDon != null) {
                for (HoaDon hd : dsHoaDon) {
                    modelGiaoDich.addRow(new Object[]{
                            hd.getMaHoaDon(),
                            hd.getDatPhong() != null ? hd.getDatPhong().getMaDatPhong() : "",
                            FormatUtil.formatNgayGio(hd.getNgayLap()),
                            FormatUtil.formatTien(hd.getTienPhong()),
                            FormatUtil.formatTien(hd.getTienDichVu()),
                            FormatUtil.formatTien(hd.getTienGiamGia()),
                            FormatUtil.formatTien(hd.getTongTien())
                    });
                }
            }
        });
    }

    private void hienThiBaoCaoTheoNgayDialog() {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Thống kê doanh thu theo ngày (SQL SUM...GROUP BY)", true);
        dlg.setSize(820, 520);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));

        // Thanh chọn khoảng thời gian (Từ ngày - Đến ngày)
        JPanel pnlFilter = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        pnlFilter.setBackground(Color.WHITE);
        pnlFilter.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 233, 238)));

        JTextField txtTuNgay = new JTextField(FormatUtil.formatNgay(LocalDate.now().minusDays(30)), 8);
        JTextField txtDenNgay = new JTextField(FormatUtil.formatNgay(LocalDate.now()), 8);
        JButton btnLoc = UIUtil.createButton("Lọc báo cáo", UIUtil.COLOR_PRIMARY, Color.WHITE);

        pnlFilter.add(new JLabel("Từ ngày (dd/MM/yyyy):"));
        pnlFilter.add(txtTuNgay);
        pnlFilter.add(new JLabel("Đến ngày:"));
        pnlFilter.add(txtDenNgay);
        pnlFilter.add(btnLoc);
        dlg.add(pnlFilter, BorderLayout.NORTH);

        String[] headers = {"Ngày", "Số đơn", "Tiền phòng", "Tiền dịch vụ", "Tổng doanh thu ngày"};
        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        UIUtil.formatTable(table);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        table.getColumnModel().getColumn(1).setCellRenderer(right);
        table.getColumnModel().getColumn(2).setCellRenderer(right);
        table.getColumnModel().getColumn(3).setCellRenderer(right);
        table.getColumnModel().getColumn(4).setCellRenderer(right);

        JLabel lblTongTatCa = new JLabel("Tổng cộng doanh thu kỳ này: 0 VNĐ");
        lblTongTatCa.setFont(UIUtil.FONT_BOLD);
        lblTongTatCa.setForeground(UIUtil.COLOR_PRIMARY);

        Consumer<Void> taiDuLieu = (ignored) -> {
            try {
                LocalDate tuNgay = LocalDate.parse(txtTuNgay.getText().trim(), FormatUtil.DATE_FORMATTER);
                LocalDate denNgay = LocalDate.parse(txtDenNgay.getText().trim(), FormatUtil.DATE_FORMATTER);
                if (tuNgay.isAfter(denNgay)) {
                    UIUtil.showError(dlg, "Từ ngày không thể lớn hơn Đến ngày!");
                    return;
                }

                thongKeController.thongKeDoanhThuTheoNgayAsync(dlg, tuNgay, denNgay, list -> {
                    model.setRowCount(0);
                    BigDecimal tongTatCa = BigDecimal.ZERO;
                    for (Map<String, Object> r : list) {
                        BigDecimal tt = (BigDecimal) r.get("tongDoanhThu");
                        tongTatCa = tongTatCa.add(tt != null ? tt : BigDecimal.ZERO);
                        model.addRow(new Object[]{
                                FormatUtil.formatNgay((LocalDate) r.get("ngay")),
                                r.get("soHoaDon"),
                                FormatUtil.formatTien((BigDecimal) r.get("tienPhong")),
                                FormatUtil.formatTien((BigDecimal) r.get("tienDichVu")),
                                FormatUtil.formatTien(tt)
                        });
                    }
                    lblTongTatCa.setText("Tổng cộng doanh thu kỳ này: " + FormatUtil.formatTien(tongTatCa));
                });
            } catch (Exception ex) {
                UIUtil.showError(dlg, "Định dạng ngày không hợp lệ (dd/MM/yyyy): " + ex.getMessage());
            }
        };

        btnLoc.addActionListener(e -> taiDuLieu.accept(null));
        taiDuLieu.accept(null); // Tải 30 ngày mặc định ban đầu

        dlg.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel pnlBottom = new JPanel(new BorderLayout(10, 10));
        pnlBottom.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        pnlBottom.setBackground(Color.WHITE);
        pnlBottom.add(lblTongTatCa, BorderLayout.WEST);

        JButton btnDong = UIUtil.createButton("Đóng", new Color(108, 117, 125), Color.WHITE);
        btnDong.addActionListener(e -> dlg.dispose());
        pnlBottom.add(btnDong, BorderLayout.EAST);
        dlg.add(pnlBottom, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }
}
