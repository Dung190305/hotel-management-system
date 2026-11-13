package com.qlks.view.panels;

import com.qlks.model.ThongBaoDon;
import com.qlks.model.ThongBaoDon.LoaiThongBao;
import com.qlks.model.ThongBaoDon.MucDo;
import com.qlks.service.ThongBaoService;
import com.qlks.util.AppEventBus;
import com.qlks.util.FormatUtil;
import com.qlks.util.UIUtil;
import com.qlks.view.RefreshablePanel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Màn hình Trung tâm Thông báo các đơn tới hạn (Đặt phòng / Nhận phòng / Trả phòng / Hóa đơn).
 */
public class ThongBaoPanel extends JPanel implements RefreshablePanel, AppEventBus.AppEventListener {

    private final ThongBaoService thongBaoService;
    private final Consumer<String> tabNavigator;

    private JLabel lblTongQuaHan;
    private JLabel lblCheckInHomNay;
    private JLabel lblCheckOutHomNay;
    private JLabel lblHoaDonChuaTT;

    private JComboBox<String> cboBoLoc;
    private JTable tblThongBao;
    private DefaultTableModel modelThongBao;

    private List<ThongBaoDon> danhSachHienTai = new ArrayList<>();

    public ThongBaoPanel() {
        this(null);
    }

    public ThongBaoPanel(Consumer<String> tabNavigator) {
        this.thongBaoService = new ThongBaoService();
        this.tabNavigator = tabNavigator;

        initComponents();
        AppEventBus.register(this);
        loadDuLieu();
    }

    @Override
    public void onEvent(AppEventBus.EventType eventType) {
        if (eventType == AppEventBus.EventType.BOOKING_CHANGED
                || eventType == AppEventBus.EventType.ROOM_STATUS_CHANGED
                || eventType == AppEventBus.EventType.INVOICE_CHANGED) {
            loadDuLieu();
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout(15, 15));
        setBackground(UIUtil.COLOR_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 1. HEADER PANEL
        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setBackground(Color.WHITE);
        pnlTop.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 233, 238)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblTitle = new JLabel("TRUNG TÂM THÔNG BÁO ĐƠN TỚI HẠN & QUÁ HẠN");
        lblTitle.setFont(UIUtil.FONT_TITLE);
        lblTitle.setForeground(new Color(220, 53, 69)); // Đỏ cảnh báo
        pnlTop.add(lblTitle, BorderLayout.WEST);

        JPanel pnlTopActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlTopActions.setOpaque(false);

        JButton btnDanhDauDaDoc = UIUtil.createButton("✓ Đánh dấu tất cả đã đọc", new Color(40, 167, 69), Color.WHITE);
        btnDanhDauDaDoc.addActionListener(e -> {
            thongBaoService.danhDauTatCaDaDoc();
            loadDuLieu();
            AppEventBus.publish(AppEventBus.EventType.BOOKING_CHANGED);
        });
        pnlTopActions.add(btnDanhDauDaDoc);

        JButton btnLamMoi = UIUtil.createButton("Làm mới", UIUtil.COLOR_PRIMARY, Color.WHITE);
        btnLamMoi.addActionListener(e -> loadDuLieu());
        pnlTopActions.add(btnLamMoi);

        pnlTop.add(pnlTopActions, BorderLayout.EAST);
        add(pnlTop, BorderLayout.NORTH);

        // 2. CENTER PANEL (CARDS + TABLE)
        JPanel pnlCenter = new JPanel(new BorderLayout(15, 15));
        pnlCenter.setOpaque(false);

        // A. 4 STAT CARDS
        JPanel pnlCards = new JPanel(new GridLayout(1, 4, 15, 0));
        pnlCards.setOpaque(false);

        pnlCards.add(createStatCard("QUÁ HẠN XỬ LÝ", lblTongQuaHan = new JLabel("0 đơn"), new Color(220, 53, 69)));
        pnlCards.add(createStatCard("CHECK-IN HÔM NAY", lblCheckInHomNay = new JLabel("0 đơn"), new Color(253, 126, 20)));
        pnlCards.add(createStatCard("CHECK-OUT HÔM NAY", lblCheckOutHomNay = new JLabel("0 đơn"), new Color(255, 193, 7)));
        pnlCards.add(createStatCard("HÓA ĐƠN CHƯA THU", lblHoaDonChuaTT = new JLabel("0 đơn"), new Color(13, 110, 253)));

        pnlCenter.add(pnlCards, BorderLayout.NORTH);

        // B. TABLE CONTAINER
        JPanel pnlTableContainer = new JPanel(new BorderLayout(10, 10));
        pnlTableContainer.setBackground(Color.WHITE);
        pnlTableContainer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 233, 238)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        // Toolbar trên Table
        JPanel pnlFilterBar = new JPanel(new BorderLayout());
        pnlFilterBar.setOpaque(false);

        JLabel lblListTitle = new JLabel("DANH SÁCH CHI TIẾT CÁC ĐƠN CẦN LƯU Ý");
        lblListTitle.setFont(UIUtil.FONT_HEADER);
        pnlFilterBar.add(lblListTitle, BorderLayout.WEST);

        JPanel pnlFilterControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlFilterControls.setOpaque(false);
        pnlFilterControls.add(new JLabel("Bộ lọc:"));

        cboBoLoc = new JComboBox<>(new String[]{
                "Tất cả thông báo",
                "Quá hạn cần xử lý gấp",
                "Tới hạn Check-in (Nhận phòng)",
                "Tới hạn Check-out (Trả phòng)",
                "Hóa đơn chưa thanh toán"
        });
        cboBoLoc.setFont(UIUtil.FONT_REGULAR);
        cboBoLoc.addActionListener(e -> apDungBoLoc());
        pnlFilterControls.add(cboBoLoc);

        pnlFilterBar.add(pnlFilterControls, BorderLayout.EAST);
        pnlTableContainer.add(pnlFilterBar, BorderLayout.NORTH);

        // Table
        String[] columns = {"Trạng thái", "Mức độ", "Loại thông báo", "Mã đơn", "Khách hàng", "Số điện thoại", "Phòng", "Hạn chót", "Chi tiết / Tình trạng"};
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
        tblThongBao.getColumnModel().getColumn(5).setPreferredWidth(95);
        tblThongBao.getColumnModel().getColumn(6).setPreferredWidth(55);
        tblThongBao.getColumnModel().getColumn(7).setPreferredWidth(120);
        tblThongBao.getColumnModel().getColumn(8).setPreferredWidth(210);

        // Renderer cho cột Trạng thái
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

        // Custom Cell Renderer tô màu mức độ cảnh báo
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

        pnlTableContainer.add(new JScrollPane(tblThongBao), BorderLayout.CENTER);

        // Bottom Actions
        JPanel pnlBottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlBottomBar.setOpaque(false);

        JButton btnXuLy = UIUtil.createButton("👉 Điều hướng xử lý ngay", new Color(40, 167, 69), Color.WHITE);
        btnXuLy.addActionListener(e -> xuLyDieuHuongNhanh());
        pnlBottomBar.add(btnXuLy);

        pnlTableContainer.add(pnlBottomBar, BorderLayout.SOUTH);

        pnlCenter.add(pnlTableContainer, BorderLayout.CENTER);
        add(pnlCenter, BorderLayout.CENTER);
    }

    private JPanel createStatCard(String title, JLabel lblValue, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(5, 5));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(230, 233, 238)),
                        BorderFactory.createEmptyBorder(12, 15, 12, 15)
                )
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(new Color(108, 117, 125));
        card.add(lblTitle, BorderLayout.NORTH);

        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblValue.setForeground(accentColor);
        card.add(lblValue, BorderLayout.CENTER);

        return card;
    }

    @Override
    public void loadDuLieu() {
        danhSachHienTai = thongBaoService.layDanhSachThongBaoToiHan();

        // Cập nhật các thẻ đếm
        int demQuaHan = (int) danhSachHienTai.stream()
                .filter(t -> t.getMucDo() == MucDo.NGUY_CAP).count();
        int demCheckInHomNay = (int) danhSachHienTai.stream()
                .filter(t -> t.getLoaiThongBao() == LoaiThongBao.CHECK_IN_HOM_NAY).count();
        int demCheckOutHomNay = (int) danhSachHienTai.stream()
                .filter(t -> t.getLoaiThongBao() == LoaiThongBao.CHECK_OUT_HOM_NAY).count();
        int demHoaDon = (int) danhSachHienTai.stream()
                .filter(t -> t.getLoaiThongBao() == LoaiThongBao.HOA_DON_CHUA_THANH_TOAN).count();

        lblTongQuaHan.setText(demQuaHan + " đơn");
        lblCheckInHomNay.setText(demCheckInHomNay + " đơn");
        lblCheckOutHomNay.setText(demCheckOutHomNay + " đơn");
        lblHoaDonChuaTT.setText(demHoaDon + " đơn");

        apDungBoLoc();
    }

    private void apDungBoLoc() {
        int filterIndex = cboBoLoc.getSelectedIndex();
        modelThongBao.setRowCount(0);

        for (ThongBaoDon tb : danhSachHienTai) {
            boolean thoaMan = false;
            switch (filterIndex) {
                case 0: // Tất cả
                    thoaMan = true;
                    break;
                case 1: // Quá hạn
                    thoaMan = (tb.getMucDo() == MucDo.NGUY_CAP);
                    break;
                case 2: // Check-in
                    thoaMan = (tb.getLoaiThongBao() == LoaiThongBao.CHECK_IN_HOM_NAY || tb.getLoaiThongBao() == LoaiThongBao.QUA_HAN_CHECK_IN);
                    break;
                case 3: // Check-out
                    thoaMan = (tb.getLoaiThongBao() == LoaiThongBao.CHECK_OUT_HOM_NAY || tb.getLoaiThongBao() == LoaiThongBao.QUA_HAN_CHECK_OUT);
                    break;
                case 4: // Hóa đơn
                    thoaMan = (tb.getLoaiThongBao() == LoaiThongBao.HOA_DON_CHUA_THANH_TOAN);
                    break;
            }

            if (thoaMan) {
                modelThongBao.addRow(new Object[]{
                        tb.isDaDoc() ? "🔵 Đã đọc" : "🔴 Chưa đọc",
                        tb.getMucDo().getTenHienThi(),
                        tb.getLoaiThongBao().getTenHienThi(),
                        tb.getMaDon(),
                        tb.getTenKhachHang(),
                        tb.getSoDienThoai(),
                        tb.getSoPhong() != null ? "P." + tb.getSoPhong() : "",
                        FormatUtil.formatNgayGio(tb.getThoiHan()),
                        tb.getTinhTrangChiTiet()
                });
            }
        }
    }

    private void xuLyDieuHuongNhanh() {
        int row = tblThongBao.getSelectedRow();
        if (row < 0) {
            UIUtil.showInfo(this, "Vui lòng chọn một dòng thông báo để điều hướng xử lý.");
            return;
        }

        String maDon = (String) modelThongBao.getValueAt(row, 3);
        String loaiThongBaoStr = (String) modelThongBao.getValueAt(row, 2);

        if (tabNavigator != null) {
            if (loaiThongBaoStr.contains("nhận phòng") || loaiThongBaoStr.contains("trả phòng")
                    || loaiThongBaoStr.contains("Check-in") || loaiThongBaoStr.contains("Check-out")) {
                tabNavigator.accept("CHECK_IN_OUT");
            } else if (loaiThongBaoStr.contains("Hóa đơn")) {
                tabNavigator.accept("HOA_DON");
            }
        } else {
            UIUtil.showInfo(this, "Đơn đã chọn: " + maDon + " (" + loaiThongBaoStr + "). Vui lòng chuyển sang Tab tương ứng để thực hiện thao tác.");
        }
    }
}

