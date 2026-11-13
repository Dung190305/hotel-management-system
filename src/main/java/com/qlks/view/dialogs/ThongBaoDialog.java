package com.qlks.view.dialogs;

import com.qlks.controller.ThongBaoController;
import com.qlks.model.ThongBaoDon;
import com.qlks.service.ThongBaoService;
import com.qlks.util.AppEventBus;
import com.qlks.util.FormatUtil;
import com.qlks.util.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Hộp thoại Popup hiển thị danh sách các đơn tới hạn khi bấm chuông thông báo trên Header.
 */
public class ThongBaoDialog extends JDialog {

    private final ThongBaoService thongBaoService;
    private final Consumer<String> tabNavigator;
    private JTable tblThongBao;
    private DefaultTableModel modelThongBao;
    private JLabel lblTieuDeTrangThai;

    public ThongBaoDialog(Frame parent, ThongBaoController thongBaoController, Consumer<String> tabNavigator) {
        this(parent, thongBaoController != null ? thongBaoController.getThongBaoService() : new ThongBaoService(), tabNavigator);
    }

    public ThongBaoDialog(Frame parent, ThongBaoService thongBaoService, Consumer<String> tabNavigator) {
        super(parent, "Thông báo đơn tới hạn", true);
        this.thongBaoService = thongBaoService;
        this.tabNavigator = tabNavigator;

        initComponents();
        loadDuLieu();
    }

    private void initComponents() {
        setSize(920, 500);
        setLocationRelativeTo(getParent());
        setLayout(new BorderLayout(10, 10));

        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(new Color(248, 249, 250));
        pnlHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(222, 226, 230)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        lblTieuDeTrangThai = new JLabel("🔔 CÁC ĐƠN TỚI HẠN & QUÁ HẠN CẦN XỬ LÝ");
        lblTieuDeTrangThai.setFont(UIUtil.FONT_HEADER);
        lblTieuDeTrangThai.setForeground(new Color(220, 53, 69));
        pnlHeader.add(lblTieuDeTrangThai, BorderLayout.WEST);

        JPanel pnlHeaderActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlHeaderActions.setOpaque(false);

        JButton btnDanhDauDaDoc = UIUtil.createButton("✓ Đánh dấu tất cả đã đọc", new Color(40, 167, 69), Color.WHITE);
        btnDanhDauDaDoc.addActionListener(e -> {
            thongBaoService.danhDauTatCaDaDoc();
            loadDuLieu();
            AppEventBus.publish(AppEventBus.EventType.BOOKING_CHANGED);
        });
        pnlHeaderActions.add(btnDanhDauDaDoc);

        pnlHeader.add(pnlHeaderActions, BorderLayout.EAST);
        add(pnlHeader, BorderLayout.NORTH);

        // Bảng danh sách
        String[] columns = {"Trạng thái", "Mức độ", "Loại thông báo", "Mã đơn", "Khách hàng", "Phòng", "Hạn chót", "Tình trạng chi tiết"};
        modelThongBao = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblThongBao = new JTable(modelThongBao);
        UIUtil.formatTable(tblThongBao);
        tblThongBao.getColumnModel().getColumn(0).setPreferredWidth(95);
        tblThongBao.getColumnModel().getColumn(1).setPreferredWidth(85);
        tblThongBao.getColumnModel().getColumn(2).setPreferredWidth(160);
        tblThongBao.getColumnModel().getColumn(3).setPreferredWidth(75);
        tblThongBao.getColumnModel().getColumn(4).setPreferredWidth(130);
        tblThongBao.getColumnModel().getColumn(5).setPreferredWidth(55);
        tblThongBao.getColumnModel().getColumn(6).setPreferredWidth(120);
        tblThongBao.getColumnModel().getColumn(7).setPreferredWidth(200);

        // Renderer cho cột Trạng thái (Chưa đọc: Đỏ, Đã đọc: Xanh)
        tblThongBao.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(UIUtil.FONT_BOLD);
                String val = value != null ? value.toString() : "";
                if (val.contains("Chưa đọc")) {
                    setForeground(new Color(220, 53, 69)); // Đỏ
                } else {
                    setForeground(new Color(24, 144, 255)); // Xanh dương
                }
                return c;
            }
        });

        // Renderer mức độ
        tblThongBao.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                setFont(UIUtil.FONT_BOLD);
                String val = value != null ? value.toString() : "";
                if ("Khẩn cấp".equalsIgnoreCase(val)) {
                    setForeground(new Color(220, 53, 69));
                } else if ("Cảnh báo".equalsIgnoreCase(val)) {
                    setForeground(new Color(253, 126, 20));
                } else {
                    setForeground(new Color(13, 110, 253));
                }
                return c;
            }
        });

        add(new JScrollPane(tblThongBao), BorderLayout.CENTER);

        // Footer buttons
        JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        pnlFooter.setBackground(Color.WHITE);

        JButton btnXemTrungTam = UIUtil.createButton("Mở Trung tâm thông báo", UIUtil.COLOR_PRIMARY, Color.WHITE);
        btnXemTrungTam.addActionListener(e -> {
            dispose();
            if (tabNavigator != null) {
                tabNavigator.accept("THONG_BAO");
            }
        });
        pnlFooter.add(btnXemTrungTam);

        JButton btnDong = UIUtil.createButton("Đóng", new Color(108, 117, 125), Color.WHITE);
        btnDong.addActionListener(e -> dispose());
        pnlFooter.add(btnDong);

        add(pnlFooter, BorderLayout.SOUTH);
    }

    private void loadDuLieu() {
        modelThongBao.setRowCount(0);
        List<ThongBaoDon> list = thongBaoService.layDanhSachThongBaoToiHan();
        int chuaDoc = 0;

        for (ThongBaoDon tb : list) {
            if (!tb.isDaDoc()) {
                chuaDoc++;
            }
            modelThongBao.addRow(new Object[]{
                    tb.isDaDoc() ? "🔵 Đã đọc" : "🔴 Chưa đọc",
                    tb.getMucDo().getTenHienThi(),
                    tb.getLoaiThongBao().getTenHienThi(),
                    tb.getMaDon(),
                    tb.getTenKhachHang(),
                    tb.getSoPhong() != null ? "P." + tb.getSoPhong() : "",
                    FormatUtil.formatNgayGio(tb.getThoiHan()),
                    tb.getTinhTrangChiTiet()
            });
        }

        if (list.isEmpty()) {
            lblTieuDeTrangThai.setText("🔔 Không có thông báo đơn tới hạn");
            lblTieuDeTrangThai.setForeground(new Color(40, 167, 69));
        } else if (chuaDoc > 0) {
            lblTieuDeTrangThai.setText("🔔 Có " + chuaDoc + " thông báo chưa đọc (Dấu ! đỏ)");
            lblTieuDeTrangThai.setForeground(new Color(220, 53, 69));
        } else {
            lblTieuDeTrangThai.setText("🔔 Tất cả " + list.size() + " thông báo đã đọc (Dấu ! xanh)");
            lblTieuDeTrangThai.setForeground(new Color(24, 144, 255));
        }
    }
}
