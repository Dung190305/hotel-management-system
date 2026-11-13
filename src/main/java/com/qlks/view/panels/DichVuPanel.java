package com.qlks.view.panels;

import com.qlks.model.DichVu;
import com.qlks.controller.DichVuController;
import com.qlks.util.FormatUtil;
import com.qlks.util.IdGenerator;
import com.qlks.util.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

import com.qlks.view.RefreshablePanel;

/**
 * Màn hình Quản lý Danh mục Dịch vụ khách sạn.
 */
public class DichVuPanel extends JPanel implements RefreshablePanel {

    private final DichVuController dichVuController;
    private JTable tblDichVu;
    private DefaultTableModel modelDichVu;

    public DichVuPanel() {
        this.dichVuController = new DichVuController();
        initComponents();
        loadDuLieu();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UIUtil.COLOR_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 1. THANH TIÊU ĐỀ
        JPanel pnlTop = new JPanel(new BorderLayout(10, 10));
        pnlTop.setBackground(Color.WHITE);
        pnlTop.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 233, 238)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblTitle = new JLabel("DANH MỤC DỊCH VỤ KHÁCH SẠN");
        lblTitle.setFont(UIUtil.FONT_TITLE);
        lblTitle.setForeground(UIUtil.COLOR_PRIMARY);
        pnlTop.add(lblTitle, BorderLayout.WEST);

        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlActions.setBackground(Color.WHITE);

        JButton btnThemDv = UIUtil.createButton("+ Thêm dịch vụ", UIUtil.COLOR_PRIMARY, Color.WHITE);
        btnThemDv.addActionListener(e -> hienThiDialogThemDichVu());
        pnlActions.add(btnThemDv);

        JButton btnLamMoi = UIUtil.createButton("Làm mới", new Color(108, 117, 125), Color.WHITE);
        btnLamMoi.addActionListener(e -> loadDuLieu());
        pnlActions.add(btnLamMoi);

        pnlTop.add(pnlActions, BorderLayout.EAST);
        add(pnlTop, BorderLayout.NORTH);

        // 2. BẢNG DỊCH VỤ
        String[] headers = {"Mã dịch vụ", "Tên dịch vụ", "Đơn giá hiện hành", "Trạng thái kinh doanh"};
        modelDichVu = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblDichVu = new JTable(modelDichVu);
        UIUtil.formatTable(tblDichVu);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        tblDichVu.getColumnModel().getColumn(2).setCellRenderer(right);

        add(new JScrollPane(tblDichVu), BorderLayout.CENTER);

        // 3. THAO TÁC SỬA / XÓA
        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        pnlBottom.setBackground(Color.WHITE);

        JButton btnSua = UIUtil.createButton("Sửa đơn giá / tên", new Color(253, 126, 20), Color.WHITE);
        btnSua.addActionListener(e -> xuLySuaDichVu());
        pnlBottom.add(btnSua);

        JButton btnNgungKd = UIUtil.createButton("Ngừng kinh doanh", new Color(220, 53, 69), Color.WHITE);
        btnNgungKd.addActionListener(e -> xuLyNgungKinhDoanh());
        pnlBottom.add(btnNgungKd);

        add(pnlBottom, BorderLayout.SOUTH);
    }

    public void loadDuLieu() {
        List<DichVu> list = dichVuController.layTatCaDichVu();
        modelDichVu.setRowCount(0);
        for (DichVu dv : list) {
            modelDichVu.addRow(new Object[]{
                    dv.getMaDichVu(),
                    dv.getTenDichVu(),
                    FormatUtil.formatTien(dv.getDonGiaHienTai()),
                    dv.isDangKinhDoanh() ? "Đang phục vụ" : "Tạm ngừng"
            });
        }
    }

    private void hienThiDialogThemDichVu() {
        JTextField txtMa = new JTextField(IdGenerator.next("DV"));
        JTextField txtTen = new JTextField();
        JTextField txtGia = new JTextField("25000");

        JPanel panel = new JPanel(new GridLayout(3, 2, 8, 8));
        panel.add(new JLabel("Mã dịch vụ:"));
        panel.add(txtMa);
        panel.add(new JLabel("Tên dịch vụ:"));
        panel.add(txtTen);
        panel.add(new JLabel("Đơn giá (VNĐ):"));
        panel.add(txtGia);

        int result = JOptionPane.showConfirmDialog(this, panel, "Thêm dịch vụ mới", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                DichVu dv = new DichVu(
                        txtMa.getText().trim(),
                        txtTen.getText().trim(),
                        FormatUtil.parseTien(txtGia.getText()),
                        true
                );
                dichVuController.themDichVu(dv);
                UIUtil.showSuccess(this, "Thêm dịch vụ thành công!");
                loadDuLieu();
            } catch (Exception ex) {
                UIUtil.showError(this, ex.getMessage());
            }
        }
    }

    private void xuLySuaDichVu() {
        int row = tblDichVu.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn dịch vụ cần sửa.");
            return;
        }

        String maDv = (String) modelDichVu.getValueAt(row, 0);
        DichVu dv = dichVuController.timTheoMa(maDv).orElse(null);
        if (dv == null) return;

        JTextField txtTen = new JTextField(dv.getTenDichVu());
        JTextField txtGia = new JTextField(dv.getDonGiaHienTai().toPlainString());
        JCheckBox chkKinhDoanh = new JCheckBox("Đang phục vụ", dv.isDangKinhDoanh());

        JPanel panel = new JPanel(new GridLayout(3, 2, 8, 8));
        panel.add(new JLabel("Tên dịch vụ:"));
        panel.add(txtTen);
        panel.add(new JLabel("Đơn giá mới (VNĐ):"));
        panel.add(txtGia);
        panel.add(new JLabel("Trạng thái:"));
        panel.add(chkKinhDoanh);

        int result = JOptionPane.showConfirmDialog(this, panel, "Cập nhật dịch vụ " + dv.getTenDichVu(), JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                dv.setTenDichVu(txtTen.getText().trim());
                dv.setDonGiaHienTai(FormatUtil.parseTien(txtGia.getText()));
                dv.setDangKinhDoanh(chkKinhDoanh.isSelected());
                dichVuController.capNhatDichVu(dv);
                UIUtil.showSuccess(this, "Cập nhật dịch vụ thành công!");
                loadDuLieu();
            } catch (Exception ex) {
                UIUtil.showError(this, ex.getMessage());
            }
        }
    }

    private void xuLyNgungKinhDoanh() {
        int row = tblDichVu.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn dịch vụ.");
            return;
        }
        String maDv = (String) modelDichVu.getValueAt(row, 0);
        if (UIUtil.showConfirm(this, "Bạn có chắc chắn muốn ngừng kinh doanh dịch vụ này không?")) {
            try {
                dichVuController.ngungKinhDoanh(maDv);
                UIUtil.showSuccess(this, "Đã chuyển trạng thái dịch vụ sang Tạm ngừng.");
                loadDuLieu();
            } catch (Exception ex) {
                UIUtil.showError(this, ex.getMessage());
            }
        }
    }
}
