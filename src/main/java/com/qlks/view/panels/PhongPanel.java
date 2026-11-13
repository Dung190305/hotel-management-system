package com.qlks.view.panels;

import com.qlks.controller.PhongController;
import com.qlks.model.Phong;
import com.qlks.model.PhongThuong;
import com.qlks.model.PhongVIP;
import com.qlks.model.enums.TrangThaiPhong;
import com.qlks.util.AppEventBus;
import com.qlks.util.FormatUtil;
import com.qlks.util.UIUtil;
import com.qlks.view.RefreshablePanel;
import com.qlks.view.dialogs.InRoomQRCodeDialog;
import com.qlks.view.dialogs.RoomOrderManagementDialog;

import javax.swing.*;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Màn hình Quản lý Danh mục Phòng và Sơ đồ phòng trực quan 5 màu sắc.
 * Tích hợp cơ chế tự động đồng bộ thời gian thực qua AppEventBus và SwingWorker Timer.
 */
public class PhongPanel extends JPanel implements RefreshablePanel, AppEventBus.AppEventListener {

    private final PhongController phongController;
    private JTable tblPhong;
    private DefaultTableModel modelPhong;
    private JPanel pnlSoDoPhong;

    private JComboBox<String> cboLocTrangThai;
    private JComboBox<String> cboLocLoaiPhong;

    private Timer autoRefreshTimer;
    private final AtomicBoolean isRefreshing = new AtomicBoolean(false);

    public PhongPanel() {
        this.phongController = new PhongController();
        initComponents();
        AppEventBus.register(this);
        initAutoRefresh();
        loadDuLieu();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UIUtil.COLOR_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 1. THANH TIÊU ĐỀ & CÔNG CỤ LỌC (Bố cục chống tràn chữ)
        JPanel pnlTop = new JPanel(new BorderLayout(10, 10));
        pnlTop.setBackground(Color.WHITE);
        pnlTop.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 233, 238)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblTitle = new JLabel("SƠ ĐỒ & QUẢN LÝ PHÒNG");
        lblTitle.setFont(UIUtil.FONT_TITLE);
        lblTitle.setForeground(UIUtil.COLOR_PRIMARY);
        pnlTop.add(lblTitle, BorderLayout.WEST);

        // Thanh công cụ lọc và nút thao tác
        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlActions.setBackground(Color.WHITE);

        pnlActions.add(new JLabel("Cấu hình:"));
        cboLocLoaiPhong = new JComboBox<>(new String[]{
                "-- Tất cả --",
                "Phòng đơn thường",
                "Phòng đơn VIP",
                "Phòng đôi thường",
                "Phòng đôi VIP"
        });
        cboLocLoaiPhong.setPreferredSize(new Dimension(160, 32));
        cboLocLoaiPhong.addActionListener(e -> locDuLieu());
        pnlActions.add(cboLocLoaiPhong);

        pnlActions.add(new JLabel("Trạng thái:"));
        cboLocTrangThai = new JComboBox<>(new String[]{
                "-- Tất cả --",
                "Còn trống (AVAILABLE)",
                "Đang có khách (OCCUPIED)",
                "Đã đặt (RESERVED)",
                "Đang dọn dẹp (CLEANING)",
                "Đang bảo trì (MAINTENANCE)"
        });
        cboLocTrangThai.setPreferredSize(new Dimension(190, 32));
        cboLocTrangThai.addActionListener(e -> locDuLieu());
        pnlActions.add(cboLocTrangThai);

        JButton btnThemPhong = UIUtil.createButton("+ Thêm phòng", UIUtil.COLOR_PRIMARY, Color.WHITE);
        btnThemPhong.addActionListener(e -> hienThiDialogThemPhong());
        pnlActions.add(btnThemPhong);

        JButton btnDonPhong = UIUtil.createButton("Đơn gọi phòng (QR)", new Color(23, 162, 184), Color.WHITE);
        btnDonPhong.addActionListener(e -> new RoomOrderManagementDialog(SwingUtilities.getWindowAncestor(this)).setVisible(true));
        pnlActions.add(btnDonPhong);

        JButton btnLamMoi = UIUtil.createButton("Làm mới", new Color(108, 117, 125), Color.WHITE);
        btnLamMoi.addActionListener(e -> loadDuLieu());
        pnlActions.add(btnLamMoi);

        pnlTop.add(pnlActions, BorderLayout.EAST);
        add(pnlTop, BorderLayout.NORTH);

        // 2. CHÚ THÍCH MÀU SẮC TRẠNG THÁI (LEGEND)
        JPanel pnlLegend = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 8));
        pnlLegend.setBackground(Color.WHITE);
        pnlLegend.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 233, 238)));
        pnlLegend.add(createLegendItem(UIUtil.COLOR_AVAILABLE, "Còn trống (AVAILABLE)"));
        pnlLegend.add(createLegendItem(UIUtil.COLOR_OCCUPIED, "Đang có khách (OCCUPIED)"));
        pnlLegend.add(createLegendItem(UIUtil.COLOR_RESERVED, "Đã đặt (RESERVED)"));
        pnlLegend.add(createLegendItem(UIUtil.COLOR_CLEANING, "Đang dọn dẹp (CLEANING)"));
        pnlLegend.add(createLegendItem(UIUtil.COLOR_MAINTENANCE, "Đang bảo trì (MAINTENANCE)"));

        // 3. TABBED PANE: TAB 1 SƠ ĐỒ THẺ MÀU & TAB 2 DANH SÁCH BẢNG
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIUtil.FONT_BOLD);

        // TAB 1: SƠ ĐỒ PHÒNG
        JPanel pnlTabSoDo = new JPanel(new BorderLayout(5, 5));
        pnlTabSoDo.setBackground(UIUtil.COLOR_BACKGROUND);
        pnlTabSoDo.add(pnlLegend, BorderLayout.NORTH);

        pnlSoDoPhong = new JPanel(new GridLayout(0, 4, 15, 15));
        pnlSoDoPhong.setBackground(UIUtil.COLOR_BACKGROUND);
        pnlSoDoPhong.setBorder(BorderFactory.createEmptyBorder(15, 10, 15, 10));

        JScrollPane scrollSoDo = new JScrollPane(pnlSoDoPhong);
        scrollSoDo.setBorder(BorderFactory.createEmptyBorder());
        pnlTabSoDo.add(scrollSoDo, BorderLayout.CENTER);
        tabbedPane.addTab("Sơ đồ phòng trực quan", pnlTabSoDo);

        // TAB 2: BẢNG DANH SÁCH CHI TIẾT
        JPanel pnlTabBang = new JPanel(new BorderLayout(5, 5));
        pnlTabBang.setBackground(Color.WHITE);

        String[] headers = {"Mã phòng", "Số phòng", "Tầng", "Cấu hình phòng", "Số giường", "Sức chứa", "Đơn giá/ngày", "Trạng thái", "Tiện nghi phòng"};
        modelPhong = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblPhong = new JTable(modelPhong);
        UIUtil.formatTable(tblPhong);

        // Căn lề tiền căn phải (cột 6)
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tblPhong.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);

        JScrollPane scrollBang = new JScrollPane(tblPhong);
        pnlTabBang.add(scrollBang, BorderLayout.CENTER);

        // Nút thao tác dưới bảng
        JPanel pnlTableActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        pnlTableActions.setBackground(Color.WHITE);

        JButton btnHoanTatDon = UIUtil.createButton("Hoàn tất dọn phòng", new Color(0, 123, 255), Color.WHITE);
        btnHoanTatDon.addActionListener(e -> xuLyHoanTatDonPhongTuBang());
        pnlTableActions.add(btnHoanTatDon);

        JButton btnDoiTrangThai = UIUtil.createButton("Đổi trạng thái", new Color(40, 167, 69), Color.WHITE);
        btnDoiTrangThai.addActionListener(e -> xuLyDoiTrangThaiPhong());
        pnlTableActions.add(btnDoiTrangThai);

        JButton btnSua = UIUtil.createButton("Sửa phòng", new Color(253, 126, 20), Color.WHITE);
        btnSua.addActionListener(e -> xuLySuaPhong());
        pnlTableActions.add(btnSua);

        JButton btnQRCode = UIUtil.createButton("Mã QR Menu phòng", new Color(111, 66, 193), Color.WHITE);
        btnQRCode.addActionListener(e -> xuLyXemQRCodePhong());
        pnlTableActions.add(btnQRCode);

        pnlTabBang.add(pnlTableActions, BorderLayout.SOUTH);
        tabbedPane.addTab("Danh sách chi tiết (Table)", pnlTabBang);

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createLegendItem(Color color, String text) {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pnl.setBackground(Color.WHITE);

        JPanel box = new JPanel();
        box.setPreferredSize(new Dimension(14, 14));
        box.setBackground(color);
        box.setBorder(BorderFactory.createLineBorder(color.darker()));

        JLabel lbl = new JLabel(text);
        lbl.setFont(UIUtil.FONT_REGULAR);
        pnl.add(box);
        pnl.add(lbl);
        return pnl;
    }

    private void initAutoRefresh() {
        // Cơ chế Event-Driven qua AppEventBus cập nhật tức thời (< 5ms) khi có bất kỳ thay đổi nào.
        // Timer đóng vai trò Heartbeat dự phòng (60 giây/lần) chỉ chạy khi tab đang hiển thị,
        // loại bỏ hoàn toàn hiện tượng dội polling dồn dập (4s) gây nghẽn kết nối và Deadlock.
        autoRefreshTimer = new Timer(60000, e -> {
            if (!isShowing() || isRefreshing.get()) {
                return;
            }
            loadDuLieu();
        });
        autoRefreshTimer.start();

        // Dừng timer khi panel bị đóng hoặc gỡ khỏi giao diện
        addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                if (autoRefreshTimer != null && !autoRefreshTimer.isRunning()) {
                    autoRefreshTimer.start();
                }
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                if (autoRefreshTimer != null && autoRefreshTimer.isRunning()) {
                    autoRefreshTimer.stop();
                }
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
            }
        });
    }

    public void stopAutoRefresh() {
        if (autoRefreshTimer != null) {
            autoRefreshTimer.stop();
        }
        AppEventBus.unregister(this);
    }

    @Override
    public void onEvent(AppEventBus.EventType eventType) {
        if (eventType == AppEventBus.EventType.ROOM_STATUS_CHANGED || eventType == AppEventBus.EventType.BOOKING_CHANGED) {
            loadDuLieu();
        }
    }

    @Override
    public void loadDuLieu() {
        if (isRefreshing.get()) return;
        isRefreshing.set(true);

        SwingWorker<List<Phong>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Phong> doInBackground() {
                return phongController.layTatCaPhong();
            }

            @Override
            protected void done() {
                try {
                    List<Phong> list = get();
                    apDungLocVaHienThi(list);
                } catch (Exception ex) {
                    // Xử lý lỗi kết nối nhẹ nhàng
                } finally {
                    isRefreshing.set(false);
                }
            }
        };
        worker.execute();
    }

    private void locDuLieu() {
        loadDuLieu();
    }

    private void apDungLocVaHienThi(List<Phong> tatCaPhong) {
        if (tatCaPhong == null) {
            tatCaPhong = new ArrayList<>();
        }

        int loaiIndex = cboLocLoaiPhong.getSelectedIndex();
        int ttIndex = cboLocTrangThai.getSelectedIndex();

        List<Phong> filtered = tatCaPhong.stream().filter(p -> {
            boolean matchLoai = true;
            if (loaiIndex == 1) matchLoai = "PHONG_DON_THUONG".equalsIgnoreCase(p.getLoaiPhong());
            if (loaiIndex == 2) matchLoai = "PHONG_DON_VIP".equalsIgnoreCase(p.getLoaiPhong());
            if (loaiIndex == 3) matchLoai = "PHONG_DOI_THUONG".equalsIgnoreCase(p.getLoaiPhong());
            if (loaiIndex == 4) matchLoai = "PHONG_DOI_VIP".equalsIgnoreCase(p.getLoaiPhong());

            boolean matchTT = true;
            if (ttIndex == 1) matchTT = p.getTrangThaiHienTai() == TrangThaiPhong.AVAILABLE;
            if (ttIndex == 2) matchTT = p.getTrangThaiHienTai() == TrangThaiPhong.OCCUPIED;
            if (ttIndex == 3) matchTT = p.getTrangThaiHienTai() == TrangThaiPhong.RESERVED;
            if (ttIndex == 4) matchTT = p.getTrangThaiHienTai() == TrangThaiPhong.CLEANING;
            if (ttIndex == 5) matchTT = p.getTrangThaiHienTai() == TrangThaiPhong.MAINTENANCE;

            return matchLoai && matchTT;
        }).toList();

        hienThiLenGiaoDien(filtered);
    }

    private void hienThiLenGiaoDien(List<Phong> list) {
        // 1. Cập nhật bảng chi tiết với đầy đủ 4 cấu hình, số giường, sức chứa và tiện nghi
        modelPhong.setRowCount(0);
        for (Phong p : list) {
            modelPhong.addRow(new Object[]{
                    p.getMaPhong(),
                    p.getSoPhong(),
                    "Tầng " + p.getTang(),
                    p.getTenLoaiPhongHienThi(),
                    p.getSoGiuong() + " giường",
                    "Tối đa " + p.getSucChuaToiDa() + " người",
                    FormatUtil.formatTien(p.getDonGiaNgay()),
                    p.getTrangThaiHienTai().getTenHienThi(),
                    String.join(", ", p.getDanhSachTienNghi())
            });
        }

        // 2. Cập nhật thẻ màu sơ đồ phòng (Xóa thẻ cũ trước để tránh tạo thẻ trùng lặp)
        pnlSoDoPhong.removeAll();
        for (Phong p : list) {
            pnlSoDoPhong.add(createRoomCard(p));
        }
        pnlSoDoPhong.revalidate();
        pnlSoDoPhong.repaint();
    }

    private JPanel createRoomCard(Phong p) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230), 1, true),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        // Header thẻ: Số phòng và loại phòng chuẩn
        JPanel pnlCardTop = new JPanel(new BorderLayout());
        pnlCardTop.setBackground(Color.WHITE);

        JLabel lblSoPhong = new JLabel("P." + p.getSoPhong());
        lblSoPhong.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblSoPhong.setForeground(new Color(33, 37, 41));

        JLabel lblLoai = new JLabel(p.getTenLoaiPhongHienThi());
        lblLoai.setFont(UIUtil.FONT_REGULAR);
        lblLoai.setForeground(new Color(108, 117, 125));

        pnlCardTop.add(lblSoPhong, BorderLayout.WEST);
        pnlCardTop.add(lblLoai, BorderLayout.EAST);
        card.add(pnlCardTop, BorderLayout.NORTH);

        // Body: Đơn giá, Sức chứa và Tầng
        JPanel pnlCardCenter = new JPanel(new GridLayout(3, 1, 2, 2));
        pnlCardCenter.setBackground(Color.WHITE);

        JLabel lblGia = new JLabel("Giá: " + FormatUtil.formatTien(p.getDonGiaNgay()) + "/ngày");
        lblGia.setFont(UIUtil.FONT_BOLD);

        JLabel lblQuyMo = new JLabel(p.getSoGiuong() + " giường | Tối đa " + p.getSucChuaToiDa() + " người");
        lblQuyMo.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblQuyMo.setForeground(new Color(73, 80, 87));

        JLabel lblTang = new JLabel("Vị trí: Tầng " + p.getTang());
        lblTang.setFont(UIUtil.FONT_REGULAR);

        pnlCardCenter.add(lblGia);
        pnlCardCenter.add(lblQuyMo);
        pnlCardCenter.add(lblTang);
        card.add(pnlCardCenter, BorderLayout.CENTER);

        // Tooltip hiển thị đầy đủ danh sách tiện nghi của phòng
        card.setToolTipText("<html><b>Tiện nghi phòng:</b><br/>• " + String.join("<br/>• ", p.getDanhSachTienNghi()) + "</html>");

        // Footer: Badge màu trạng thái chuẩn 5 màu
        Color colorStatus = UIUtil.getMauTrangThaiPhong(p.getTrangThaiHienTai());
        JLabel lblStatus = new JLabel(p.getTrangThaiHienTai().getTenHienThi(), SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setOpaque(true);
        lblStatus.setBackground(colorStatus);
        lblStatus.setForeground(Color.WHITE);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        card.add(lblStatus, BorderLayout.SOUTH);

        // Sự kiện click vào thẻ để mở Menu thao tác nhanh
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                hienThiMenuNhanhPhong(p, card);
            }
        });

        return card;
    }

    private void hienThiMenuNhanhPhong(Phong p, Component comp) {
        JPopupMenu menu = new JPopupMenu();

        // 1. Tùy chọn xem tiện nghi chi tiết
        JMenuItem itemTienNghi = new JMenuItem("🛋️ Xem tiện nghi phòng " + p.getSoPhong());
        itemTienNghi.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            sb.append("CẤU HÌNH: ").append(p.getTenLoaiPhongHienThi().toUpperCase()).append("\n");
            sb.append("Số giường: ").append(p.getSoGiuong()).append(" giường\n");
            sb.append("Sức chứa tối đa: ").append(p.getSucChuaToiDa()).append(" người\n\n");
            sb.append("DANH MỤC TIỆN NGHI:\n");
            for (String tn : p.getDanhSachTienNghi()) {
                sb.append("  ✔ ").append(tn).append("\n");
            }
            JOptionPane.showMessageDialog(this, sb.toString(), "Tiện nghi phòng " + p.getSoPhong(), JOptionPane.INFORMATION_MESSAGE);
        });
        menu.add(itemTienNghi);

        // Tùy chọn xem và in mã QR Menu phục vụ tại phòng
        JMenuItem itemQRCode = new JMenuItem("📱 Mã QR Menu phòng " + p.getSoPhong());
        itemQRCode.addActionListener(e -> new InRoomQRCodeDialog(SwingUtilities.getWindowAncestor(this), p).setVisible(true));
        menu.add(itemQRCode);
        menu.addSeparator();

        // 2. Tùy chọn hoàn tất dọn dẹp nếu phòng đang dọn dẹp (CLEANING)
        if (p.getTrangThaiHienTai() == TrangThaiPhong.CLEANING) {
            JMenuItem itemHoanTatDon = new JMenuItem("✨ Hoàn tất dọn dẹp phòng " + p.getSoPhong());
            itemHoanTatDon.setFont(UIUtil.FONT_BOLD);
            itemHoanTatDon.addActionListener(e -> {
                try {
                    TrangThaiPhong ttMoi = phongController.hoanTatDonPhong(p.getMaPhong());
                    UIUtil.showSuccess(this, String.format(
                            "Phòng %s đã hoàn tất dọn dẹp!\nTrạng thái chuyển sang: %s",
                            p.getSoPhong(), ttMoi.getTenHienThi()
                    ));
                    loadDuLieu();
                } catch (Exception ex) {
                    UIUtil.showError(this, ex.getMessage());
                }
            });
            menu.add(itemHoanTatDon);
            menu.addSeparator();
        }

        // 2. Tùy chọn đổi trạng thái có kiểm soát nghiệp vụ (chỉ các trạng thái buồng phòng cho phép)
        JMenuItem itemDoiTT = new JMenuItem("⚙️ Đổi trạng thái phòng (" + p.getSoPhong() + ")...");
        itemDoiTT.addActionListener(e -> {
            TrangThaiPhong[] options = new TrangThaiPhong[]{
                    TrangThaiPhong.AVAILABLE,
                    TrangThaiPhong.CLEANING,
                    TrangThaiPhong.MAINTENANCE
            };
            TrangThaiPhong chon = (TrangThaiPhong) JOptionPane.showInputDialog(
                    this,
                    "Chọn trạng thái mới cho phòng " + p.getSoPhong() + ":",
                    "Cập nhật trạng thái",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    p.getTrangThaiHienTai()
            );
            if (chon != null && chon != p.getTrangThaiHienTai()) {
                try {
                    TrangThaiPhong ttThucTe = phongController.chuyenTrangThaiPhong(p.getMaPhong(), chon);
                    UIUtil.showSuccess(this, "Đã cập nhật trạng thái phòng " + p.getSoPhong() + " thành: " + ttThucTe.getTenHienThi());
                    loadDuLieu();
                } catch (Exception ex) {
                    UIUtil.showError(this, ex.getMessage());
                }
            }
        });
        menu.add(itemDoiTT);

        menu.show(comp, comp.getWidth() / 2, comp.getHeight() / 2);
    }

    private void xuLyHoanTatDonPhongTuBang() {
        int row = tblPhong.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn một phòng trong danh sách bảng.");
            return;
        }
        String maPhong = (String) modelPhong.getValueAt(row, 0);
        Phong p = phongController.timTheoMa(maPhong).orElse(null);
        if (p == null) return;

        if (p.getTrangThaiHienTai() != TrangThaiPhong.CLEANING) {
            UIUtil.showError(this, "Chỉ có thể hoàn tất dọn dẹp cho phòng đang ở trạng thái Đang dọn dẹp (CLEANING)!");
            return;
        }

        try {
            TrangThaiPhong ttMoi = phongController.hoanTatDonPhong(maPhong);
            UIUtil.showSuccess(this, String.format(
                    "Hoàn tất dọn phòng %s thành công!\nTrạng thái mới: %s",
                    p.getSoPhong(), ttMoi.getTenHienThi()
            ));
            loadDuLieu();
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }

    private void hienThiDialogThemPhong() {
        JTextField txtMa = new JTextField();
        JTextField txtSo = new JTextField();
        JSpinner spnTang = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
        JTextField txtGia = new JTextField("450000");

        JComboBox<String> cboLoaiGiuong = new JComboBox<>(new String[]{"Phòng đơn (1 giường)", "Phòng đôi (2 giường)"});
        JComboBox<String> cboHangPhong = new JComboBox<>(new String[]{"Phòng thường", "Phòng VIP"});

        JLabel lblCauHinh = new JLabel("✔ Cấu hình: Phòng đơn thường");
        lblCauHinh.setFont(UIUtil.FONT_BOLD);
        lblCauHinh.setForeground(UIUtil.COLOR_PRIMARY);

        JLabel lblQuyMo = new JLabel("🛏️ 1 giường  |  👥 Sức chứa tối đa: 2 người");
        lblQuyMo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblQuyMo.setForeground(new Color(40, 167, 69));

        JTextArea txtTienNghi = new JTextArea(5, 25);
        txtTienNghi.setEditable(false);
        txtTienNghi.setBackground(new Color(248, 249, 250));
        txtTienNghi.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtTienNghi.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JCheckBox chkCuaSo = new JCheckBox("Có cửa sổ", true);
        JCheckBox chkBanCong = new JCheckBox("Có ban công view biển", false);
        JTextField txtPhiVip = new JTextField("0");

        chkBanCong.setEnabled(false);
        txtPhiVip.setEnabled(false);

        Runnable capNhatPreview = () -> {
            boolean isDoi = cboLoaiGiuong.getSelectedIndex() == 1;
            boolean isVip = cboHangPhong.getSelectedIndex() == 1;
            com.qlks.model.enums.CauHinhLoaiPhong ch = isDoi ?
                    (isVip ? com.qlks.model.enums.CauHinhLoaiPhong.PHONG_DOI_VIP : com.qlks.model.enums.CauHinhLoaiPhong.PHONG_DOI_THUONG) :
                    (isVip ? com.qlks.model.enums.CauHinhLoaiPhong.PHONG_DON_VIP : com.qlks.model.enums.CauHinhLoaiPhong.PHONG_DON_THUONG);

            lblCauHinh.setText("✔ Cấu hình chuẩn: " + ch.getTenHienThi());
            lblQuyMo.setText("🛏️ " + ch.getSoGiuong() + " giường  |  👥 Sức chứa tối đa: " + ch.getSucChuaToiDa() + " người");
            txtTienNghi.setText("Danh mục tiện nghi tự động:\n• " + String.join("\n• ", ch.getDanhSachTienNghi()));

            chkCuaSo.setEnabled(!isVip);
            chkBanCong.setEnabled(isVip);
            txtPhiVip.setEnabled(isVip);
            if (isVip && txtPhiVip.getText().equals("0")) {
                txtPhiVip.setText("200000");
            }
        };

        cboLoaiGiuong.addActionListener(e -> capNhatPreview.run());
        cboHangPhong.addActionListener(e -> capNhatPreview.run());
        capNhatPreview.run();

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Mã phòng (VD: P105):"), gbc);
        gbc.gridx = 1; panel.add(txtMa, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Số phòng (VD: 105):"), gbc);
        gbc.gridx = 1; panel.add(txtSo, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Tầng:"), gbc);
        gbc.gridx = 1; panel.add(spnTang, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Loại phòng (Giường):"), gbc);
        gbc.gridx = 1; panel.add(cboLoaiGiuong, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Hạng phòng:"), gbc);
        gbc.gridx = 1; panel.add(cboHangPhong, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Đơn giá ngày (VNĐ):"), gbc);
        gbc.gridx = 1; panel.add(txtGia, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        panel.add(lblCauHinh, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        panel.add(lblQuyMo, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2;
        panel.add(new JScrollPane(txtTienNghi), gbc);
        gbc.gridwidth = 1;

        row++;
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Đặc điểm phòng thường:"), gbc);
        gbc.gridx = 1; panel.add(chkCuaSo, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Ban công view biển:"), gbc);
        gbc.gridx = 1; panel.add(chkBanCong, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row; panel.add(new JLabel("Phụ phí VIP (VNĐ):"), gbc);
        gbc.gridx = 1; panel.add(txtPhiVip, gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Thêm phòng mới (Đúng 4 cấu hình)", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String ma = txtMa.getText().trim();
                String so = txtSo.getText().trim();
                int tang = (Integer) spnTang.getValue();
                BigDecimal gia = FormatUtil.parseTien(txtGia.getText());

                boolean isDoi = cboLoaiGiuong.getSelectedIndex() == 1;
                boolean isVip = cboHangPhong.getSelectedIndex() == 1;
                com.qlks.model.enums.LoaiGiuong lg = isDoi ? com.qlks.model.enums.LoaiGiuong.DOI : com.qlks.model.enums.LoaiGiuong.DON;

                Phong phongMoi;
                if (isVip) {
                    BigDecimal phiVip = FormatUtil.parseTien(txtPhiVip.getText());
                    phongMoi = new PhongVIP(ma, so, tang, gia, TrangThaiPhong.AVAILABLE, lg, true, chkBanCong.isSelected(), phiVip);
                } else {
                    phongMoi = new PhongThuong(ma, so, tang, gia, TrangThaiPhong.AVAILABLE, lg, true, chkCuaSo.isSelected());
                }

                phongController.themPhong(phongMoi);
                UIUtil.showSuccess(this, "Thêm phòng mới thành công! Cấu hình: " + phongMoi.getTenLoaiPhongHienThi());
                loadDuLieu();
            } catch (Exception ex) {
                UIUtil.showError(this, ex.getMessage());
            }
        }
    }

    private void xuLyDoiTrangThaiPhong() {
        int row = tblPhong.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn một phòng trong bảng.");
            return;
        }
        String maPhong = (String) modelPhong.getValueAt(row, 0);
        Phong p = phongController.timTheoMa(maPhong).orElse(null);
        if (p == null) return;

        TrangThaiPhong[] options = new TrangThaiPhong[]{
                TrangThaiPhong.AVAILABLE,
                TrangThaiPhong.CLEANING,
                TrangThaiPhong.MAINTENANCE
        };
        TrangThaiPhong chon = (TrangThaiPhong) JOptionPane.showInputDialog(
                this,
                "Chọn trạng thái mới cho phòng " + p.getSoPhong() + ":",
                "Cập nhật trạng thái",
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                p.getTrangThaiHienTai()
        );
        if (chon != null && chon != p.getTrangThaiHienTai()) {
            try {
                TrangThaiPhong ttThucTe = phongController.chuyenTrangThaiPhong(maPhong, chon);
                UIUtil.showSuccess(this, "Đã cập nhật trạng thái phòng thành công: " + ttThucTe.getTenHienThi());
                loadDuLieu();
            } catch (Exception ex) {
                UIUtil.showError(this, ex.getMessage());
            }
        }
    }

    private void xuLySuaPhong() {
        int row = tblPhong.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn một phòng để sửa.");
            return;
        }
        String maPhong = (String) modelPhong.getValueAt(row, 0);
        Phong p = phongController.timTheoMa(maPhong).orElse(null);
        if (p == null) return;

        JTextField txtSo = new JTextField(p.getSoPhong());
        JSpinner spnTang = new JSpinner(new SpinnerNumberModel(p.getTang(), 1, 50, 1));
        JTextField txtGia = new JTextField(p.getDonGiaNgay().toPlainString());

        JComboBox<String> cboLoaiGiuong = new JComboBox<>(new String[]{"Phòng đơn (1 giường)", "Phòng đôi (2 giường)"});
        cboLoaiGiuong.setSelectedIndex(p.getLoaiGiuong() == com.qlks.model.enums.LoaiGiuong.DOI ? 1 : 0);

        JLabel lblCauHinh = new JLabel("✔ Cấu hình: " + p.getTenLoaiPhongHienThi());
        lblCauHinh.setFont(UIUtil.FONT_BOLD);
        lblCauHinh.setForeground(UIUtil.COLOR_PRIMARY);

        JLabel lblQuyMo = new JLabel("🛏️ " + p.getSoGiuong() + " giường  |  👥 Sức chứa tối đa: " + p.getSucChuaToiDa() + " người");
        lblQuyMo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblQuyMo.setForeground(new Color(40, 167, 69));

        JTextArea txtTienNghi = new JTextArea(5, 25);
        txtTienNghi.setEditable(false);
        txtTienNghi.setBackground(new Color(248, 249, 250));
        txtTienNghi.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtTienNghi.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        Runnable capNhatPreview = () -> {
            boolean isDoi = cboLoaiGiuong.getSelectedIndex() == 1;
            boolean isVip = p instanceof PhongVIP;
            com.qlks.model.enums.CauHinhLoaiPhong ch = isDoi ?
                    (isVip ? com.qlks.model.enums.CauHinhLoaiPhong.PHONG_DOI_VIP : com.qlks.model.enums.CauHinhLoaiPhong.PHONG_DOI_THUONG) :
                    (isVip ? com.qlks.model.enums.CauHinhLoaiPhong.PHONG_DON_VIP : com.qlks.model.enums.CauHinhLoaiPhong.PHONG_DON_THUONG);

            lblCauHinh.setText("✔ Cấu hình chuẩn: " + ch.getTenHienThi());
            lblQuyMo.setText("🛏️ " + ch.getSoGiuong() + " giường  |  👥 Sức chứa tối đa: " + ch.getSucChuaToiDa() + " người");
            txtTienNghi.setText("Danh mục tiện nghi tự động:\n• " + String.join("\n• ", ch.getDanhSachTienNghi()));
        };

        cboLoaiGiuong.addActionListener(e -> capNhatPreview.run());
        capNhatPreview.run();

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int r = 0;
        gbc.gridx = 0; gbc.gridy = r; panel.add(new JLabel("Mã phòng:"), gbc);
        gbc.gridx = 1; panel.add(new JLabel(p.getMaPhong()), gbc);

        r++;
        gbc.gridx = 0; gbc.gridy = r; panel.add(new JLabel("Số phòng:"), gbc);
        gbc.gridx = 1; panel.add(txtSo, gbc);

        r++;
        gbc.gridx = 0; gbc.gridy = r; panel.add(new JLabel("Tầng:"), gbc);
        gbc.gridx = 1; panel.add(spnTang, gbc);

        r++;
        gbc.gridx = 0; gbc.gridy = r; panel.add(new JLabel("Loại phòng (Giường):"), gbc);
        gbc.gridx = 1; panel.add(cboLoaiGiuong, gbc);

        r++;
        gbc.gridx = 0; gbc.gridy = r; panel.add(new JLabel("Hạng phòng:"), gbc);
        gbc.gridx = 1; panel.add(new JLabel(p.getHangPhong().getTenHienThi()), gbc);

        r++;
        gbc.gridx = 0; gbc.gridy = r; panel.add(new JLabel("Đơn giá ngày (VNĐ):"), gbc);
        gbc.gridx = 1; panel.add(txtGia, gbc);

        r++;
        gbc.gridx = 0; gbc.gridy = r; gbc.gridwidth = 2;
        panel.add(lblCauHinh, gbc);

        r++;
        gbc.gridx = 0; gbc.gridy = r; gbc.gridwidth = 2;
        panel.add(lblQuyMo, gbc);

        r++;
        gbc.gridx = 0; gbc.gridy = r; gbc.gridwidth = 2;
        panel.add(new JScrollPane(txtTienNghi), gbc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Cập nhật phòng " + p.getSoPhong(), JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                p.setSoPhong(txtSo.getText().trim());
                p.setTang((Integer) spnTang.getValue());
                p.setDonGiaNgay(FormatUtil.parseTien(txtGia.getText()));
                p.setLoaiGiuong(cboLoaiGiuong.getSelectedIndex() == 1 ? com.qlks.model.enums.LoaiGiuong.DOI : com.qlks.model.enums.LoaiGiuong.DON);

                phongController.capNhatPhong(p);
                UIUtil.showSuccess(this, "Cập nhật thông tin phòng thành công! Cấu hình: " + p.getTenLoaiPhongHienThi());
                loadDuLieu();
            } catch (Exception ex) {
                UIUtil.showError(this, ex.getMessage());
            }
        }
    }

    private void xuLyXemQRCodePhong() {
        int row = tblPhong.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn một phòng trong danh sách bảng để xem/in mã QR Menu.");
            return;
        }
        String maPhong = (String) modelPhong.getValueAt(row, 0);
        Optional<Phong> phongOpt = phongController.timTheoMa(maPhong);
        if (phongOpt.isEmpty()) {
            UIUtil.showError(this, "Không tìm thấy thông tin phòng: " + maPhong);
            return;
        }
        new InRoomQRCodeDialog(SwingUtilities.getWindowAncestor(this), phongOpt.get()).setVisible(true);
    }
}
