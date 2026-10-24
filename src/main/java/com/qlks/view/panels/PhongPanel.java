package com.qlks.view.panels;

import com.qlks.model.Phong;
import com.qlks.model.PhongThuong;
import com.qlks.model.PhongVIP;
import com.qlks.model.enums.TrangThaiPhong;
import com.qlks.service.PhongService;
import com.qlks.util.FormatUtil;
import com.qlks.util.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Màn hình Quản lý Danh mục Phòng và Sơ đồ phòng trực quan 5 màu sắc.
 */
public class PhongPanel extends JPanel {

    private final PhongService phongService;
    private JTable tblPhong;
    private DefaultTableModel modelPhong;
    private JPanel pnlSoDoPhong;

    private JComboBox<String> cboLocTrangThai;
    private JComboBox<String> cboLocLoaiPhong;

    public PhongPanel() {
        this.phongService = new PhongService();
        initComponents();
        loadDuLieu();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UIUtil.COLOR_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 1. THANH TIÊU ĐỀ & CÔNG CỤ LỌC
        JPanel pnlTop = new JPanel(new BorderLayout(10, 10));
        pnlTop.setBackground(Color.WHITE);
        pnlTop.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 233, 238)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblTitle = new JLabel("QUẢN LÝ PHÒNG & SƠ ĐỒ TRẠNG THÁI");
        lblTitle.setFont(UIUtil.FONT_TITLE);
        lblTitle.setForeground(UIUtil.COLOR_PRIMARY);
        pnlTop.add(lblTitle, BorderLayout.WEST);

        // Thanh công cụ lọc và nút thao tác
        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlActions.setBackground(Color.WHITE);

        pnlActions.add(new JLabel("Loại phòng:"));
        cboLocLoaiPhong = new JComboBox<>(new String[]{"-- Tất cả loại --", "Phòng Thường", "Phòng VIP"});
        cboLocLoaiPhong.addActionListener(e -> locDuLieu());
        pnlActions.add(cboLocLoaiPhong);

        pnlActions.add(new JLabel("Trạng thái:"));
        cboLocTrangThai = new JComboBox<>(new String[]{
                "-- Tất cả trạng thái --",
                "Còn trống (AVAILABLE)",
                "Đang có khách (OCCUPIED)",
                "Đã đặt (RESERVED)",
                "Đang dọn dẹp (CLEANING)",
                "Đang bảo trì (MAINTENANCE)"
        });
        cboLocTrangThai.addActionListener(e -> locDuLieu());
        pnlActions.add(cboLocTrangThai);

        JButton btnThemPhong = UIUtil.createButton("+ Thêm phòng", UIUtil.COLOR_PRIMARY, Color.WHITE);
        btnThemPhong.addActionListener(e -> hienThiDialogThemPhong());
        pnlActions.add(btnThemPhong);

        JButton btnLamMoi = UIUtil.createButton("Làm mới", new Color(108, 117, 125), Color.WHITE);
        btnLamMoi.addActionListener(e -> loadDuLieu());
        pnlActions.add(btnLamMoi);

        pnlTop.add(pnlActions, BorderLayout.EAST);
        add(pnlTop, BorderLayout.NORTH);

        // 2. CHÚ THÍCH MÀU SẮC TRẠNG THÁI (LEGEND)
        JPanel pnlLegend = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
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

        String[] headers = {"Mã phòng", "Số phòng", "Tầng", "Loại phòng", "Đơn giá/ngày", "Trạng thái", "Đặc điểm chi tiết"};
        modelPhong = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblPhong = new JTable(modelPhong);
        UIUtil.formatTable(tblPhong);

        // Căn lề tiền căn phải
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tblPhong.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        JScrollPane scrollBang = new JScrollPane(tblPhong);
        pnlTabBang.add(scrollBang, BorderLayout.CENTER);

        // Nút thao tác dưới bảng
        JPanel pnlTableActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        pnlTableActions.setBackground(Color.WHITE);

        JButton btnDoiTrangThai = UIUtil.createButton("Đổi trạng thái", new Color(40, 167, 69), Color.WHITE);
        btnDoiTrangThai.addActionListener(e -> xuLyDoiTrangThaiPhong());
        pnlTableActions.add(btnDoiTrangThai);

        JButton btnSua = UIUtil.createButton("Sửa phòng", new Color(253, 126, 20), Color.WHITE);
        btnSua.addActionListener(e -> xuLySuaPhong());
        pnlTableActions.add(btnSua);

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

    public void loadDuLieu() {
        List<Phong> list = phongService.layTatCaPhong();
        hienThiLenGiaoDien(list);
    }

    private void locDuLieu() {
        List<Phong> list = phongService.layTatCaPhong();
        int loaiIndex = cboLocLoaiPhong.getSelectedIndex();
        int ttIndex = cboLocTrangThai.getSelectedIndex();

        list = list.stream().filter(p -> {
            boolean matchLoai = true;
            if (loaiIndex == 1) matchLoai = p instanceof PhongThuong;
            if (loaiIndex == 2) matchLoai = p instanceof PhongVIP;

            boolean matchTT = true;
            if (ttIndex == 1) matchTT = p.getTrangThaiHienTai() == TrangThaiPhong.AVAILABLE;
            if (ttIndex == 2) matchTT = p.getTrangThaiHienTai() == TrangThaiPhong.OCCUPIED;
            if (ttIndex == 3) matchTT = p.getTrangThaiHienTai() == TrangThaiPhong.RESERVED;
            if (ttIndex == 4) matchTT = p.getTrangThaiHienTai() == TrangThaiPhong.CLEANING;
            if (ttIndex == 5) matchTT = p.getTrangThaiHienTai() == TrangThaiPhong.MAINTENANCE;

            return matchLoai && matchTT;
        }).toList();

        hienThiLenGiaoDien(list);
    }

    private void hienThiLenGiaoDien(List<Phong> list) {
        // 1. Cập nhật bảng
        modelPhong.setRowCount(0);
        for (Phong p : list) {
            String dacDiem = "";
            if (p instanceof PhongThuong pt) {
                dacDiem = pt.isCoCuaSo() ? "Có cửa sổ thoáng mát" : "Không có cửa sổ";
            } else if (p instanceof PhongVIP pv) {
                dacDiem = (pv.isCoBanCongViewBien() ? "Ban công view biển" : "Không ban công") +
                        " | Phụ phí VIP: " + FormatUtil.formatTien(pv.getPhiDichVuVip());
            }

            modelPhong.addRow(new Object[]{
                    p.getMaPhong(),
                    p.getSoPhong(),
                    "Tầng " + p.getTang(),
                    p.getTenLoaiPhongHienThi(),
                    FormatUtil.formatTien(p.getDonGiaNgay()),
                    p.getTrangThaiHienTai().getTenHienThi(),
                    dacDiem
            });
        }

        // 2. Cập nhật thẻ màu sơ đồ phòng
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

        // Header thẻ: Số phòng và loại phòng
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

        // Body: Đơn giá và Tầng
        JPanel pnlCardCenter = new JPanel(new GridLayout(2, 1, 4, 4));
        pnlCardCenter.setBackground(Color.WHITE);
        JLabel lblGia = new JLabel("Giá: " + FormatUtil.formatTien(p.getDonGiaNgay()) + "/ngày");
        lblGia.setFont(UIUtil.FONT_BOLD);
        JLabel lblTang = new JLabel("Vị trí: Tầng " + p.getTang());
        lblTang.setFont(UIUtil.FONT_REGULAR);
        pnlCardCenter.add(lblGia);
        pnlCardCenter.add(lblTang);
        card.add(pnlCardCenter, BorderLayout.CENTER);

        // Footer: Badge màu trạng thái
        Color colorStatus = UIUtil.getMauTrangThaiPhong(p.getTrangThaiHienTai());
        JLabel lblStatus = new JLabel(p.getTrangThaiHienTai().getTenHienThi(), SwingConstants.CENTER);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblStatus.setOpaque(true);
        lblStatus.setBackground(colorStatus);
        lblStatus.setForeground(Color.WHITE);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        card.add(lblStatus, BorderLayout.SOUTH);

        // Sự kiện click vào thẻ
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
        JMenuItem itemDoiTT = new JMenuItem("Đổi trạng thái phòng (" + p.getSoPhong() + ")");
        itemDoiTT.addActionListener(e -> {
            TrangThaiPhong[] options = TrangThaiPhong.values();
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
                    phongService.chuyenTrangThaiPhong(p.getMaPhong(), chon);
                    UIUtil.showSuccess(this, "Đã cập nhật trạng thái phòng " + p.getSoPhong() + " thành: " + chon.getTenHienThi());
                    loadDuLieu();
                } catch (Exception ex) {
                    UIUtil.showError(this, ex.getMessage());
                }
            }
        });
        menu.add(itemDoiTT);
        menu.show(comp, comp.getWidth() / 2, comp.getHeight() / 2);
    }

    private void hienThiDialogThemPhong() {
        JTextField txtMa = new JTextField();
        JTextField txtSo = new JTextField();
        JSpinner spnTang = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
        JTextField txtGia = new JTextField("500000");
        JComboBox<String> cboLoai = new JComboBox<>(new String[]{"Phòng Thường", "Phòng VIP"});
        JCheckBox chkCuaSo = new JCheckBox("Có cửa sổ", true);
        JCheckBox chkBanCong = new JCheckBox("Có ban công view biển", false);
        JTextField txtPhiVip = new JTextField("0");

        chkBanCong.setEnabled(false);
        txtPhiVip.setEnabled(false);

        cboLoai.addActionListener(e -> {
            boolean isVip = cboLoai.getSelectedIndex() == 1;
            chkCuaSo.setEnabled(!isVip);
            chkBanCong.setEnabled(isVip);
            txtPhiVip.setEnabled(isVip);
            if (isVip && txtPhiVip.getText().equals("0")) {
                txtPhiVip.setText("200000");
            }
        });

        JPanel panel = new JPanel(new GridLayout(8, 2, 8, 8));
        panel.add(new JLabel("Mã phòng (VD: P105):"));
        panel.add(txtMa);
        panel.add(new JLabel("Số phòng (VD: 105):"));
        panel.add(txtSo);
        panel.add(new JLabel("Tầng:"));
        panel.add(spnTang);
        panel.add(new JLabel("Đơn giá ngày (VNĐ):"));
        panel.add(txtGia);
        panel.add(new JLabel("Loại phòng:"));
        panel.add(cboLoai);
        panel.add(new JLabel("Đặc điểm thường:"));
        panel.add(chkCuaSo);
        panel.add(new JLabel("Ban công view biển:"));
        panel.add(chkBanCong);
        panel.add(new JLabel("Phụ phí VIP (VNĐ):"));
        panel.add(txtPhiVip);

        int result = JOptionPane.showConfirmDialog(this, panel, "Thêm phòng mới", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String ma = txtMa.getText().trim();
                String so = txtSo.getText().trim();
                int tang = (Integer) spnTang.getValue();
                BigDecimal gia = FormatUtil.parseTien(txtGia.getText());

                Phong phongMoi;
                if (cboLoai.getSelectedIndex() == 1) {
                    BigDecimal phiVip = FormatUtil.parseTien(txtPhiVip.getText());
                    phongMoi = new PhongVIP(ma, so, tang, gia, TrangThaiPhong.AVAILABLE, true, chkBanCong.isSelected(), phiVip);
                } else {
                    phongMoi = new PhongThuong(ma, so, tang, gia, TrangThaiPhong.AVAILABLE, true, chkCuaSo.isSelected());
                }

                phongService.themPhong(phongMoi);
                UIUtil.showSuccess(this, "Thêm phòng mới thành công!");
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
        Phong p = phongService.timTheoMa(maPhong).orElse(null);
        if (p == null) return;

        TrangThaiPhong[] options = TrangThaiPhong.values();
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
                phongService.chuyenTrangThaiPhong(maPhong, chon);
                UIUtil.showSuccess(this, "Đã cập nhật trạng thái phòng thành công.");
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
        Phong p = phongService.timTheoMa(maPhong).orElse(null);
        if (p == null) return;

        JTextField txtSo = new JTextField(p.getSoPhong());
        JSpinner spnTang = new JSpinner(new SpinnerNumberModel(p.getTang(), 1, 50, 1));
        JTextField txtGia = new JTextField(p.getDonGiaNgay().toPlainString());

        JPanel panel = new JPanel(new GridLayout(3, 2, 8, 8));
        panel.add(new JLabel("Số phòng:"));
        panel.add(txtSo);
        panel.add(new JLabel("Tầng:"));
        panel.add(spnTang);
        panel.add(new JLabel("Đơn giá ngày (VNĐ):"));
        panel.add(txtGia);

        int result = JOptionPane.showConfirmDialog(this, panel, "Cập nhật phòng " + p.getSoPhong(), JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                p.setSoPhong(txtSo.getText().trim());
                p.setTang((Integer) spnTang.getValue());
                p.setDonGiaNgay(FormatUtil.parseTien(txtGia.getText()));
                phongService.capNhatPhong(p);
                UIUtil.showSuccess(this, "Cập nhật thông tin phòng thành công!");
                loadDuLieu();
            } catch (Exception ex) {
                UIUtil.showError(this, ex.getMessage());
            }
        }
    }
}
