package com.qlks.view.panels;

import com.qlks.model.KhachHang;
import com.qlks.service.KhachHangService;
import com.qlks.util.FormatUtil;
import com.qlks.util.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Màn hình Quản lý Khách hàng lưu trú.
 */
public class KhachHangPanel extends JPanel {

    private final KhachHangService khachHangService;
    private JTable tblKhachHang;
    private DefaultTableModel modelKhachHang;
    private JTextField txtTimKiem;

    public KhachHangPanel() {
        this.khachHangService = new KhachHangService();
        initComponents();
        loadDuLieu();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UIUtil.COLOR_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 1. THANH TIÊU ĐỀ & TÌM KIẾM
        JPanel pnlTop = new JPanel(new BorderLayout(10, 10));
        pnlTop.setBackground(Color.WHITE);
        pnlTop.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 233, 238)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblTitle = new JLabel("QUẢN LÝ KHÁCH HÀNG");
        lblTitle.setFont(UIUtil.FONT_TITLE);
        lblTitle.setForeground(UIUtil.COLOR_PRIMARY);
        pnlTop.add(lblTitle, BorderLayout.WEST);

        JPanel pnlSearchAndActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlSearchAndActions.setBackground(Color.WHITE);

        txtTimKiem = new JTextField(15);
        txtTimKiem.setPreferredSize(new Dimension(180, 32));
        txtTimKiem.putClientProperty("JTextField.placeholderText", "Tìm theo tên, SĐT, CCCD...");
        pnlSearchAndActions.add(txtTimKiem);

        JButton btnTim = UIUtil.createButton("Tìm kiếm", UIUtil.COLOR_PRIMARY, Color.WHITE);
        btnTim.addActionListener(e -> xuLyTimKiem());
        pnlSearchAndActions.add(btnTim);

        JButton btnThem = UIUtil.createButton("+ Thêm khách hàng", new Color(40, 167, 69), Color.WHITE);
        btnThem.addActionListener(e -> hienThiDialogThemKhachHang());
        pnlSearchAndActions.add(btnThem);

        JButton btnLamMoi = UIUtil.createButton("Làm mới", new Color(108, 117, 125), Color.WHITE);
        btnLamMoi.addActionListener(e -> {
            txtTimKiem.setText("");
            loadDuLieu();
        });
        pnlSearchAndActions.add(btnLamMoi);

        pnlTop.add(pnlSearchAndActions, BorderLayout.EAST);
        add(pnlTop, BorderLayout.NORTH);

        // 2. BẢNG DỮ LIỆU KHÁCH HÀNG
        String[] headers = {"Mã KH", "Họ và tên", "Số điện thoại", "Email", "Số CCCD/Hộ chiếu", "Ngày đăng ký"};
        modelKhachHang = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblKhachHang = new JTable(modelKhachHang);
        UIUtil.formatTable(tblKhachHang);

        JScrollPane scrollPane = new JScrollPane(tblKhachHang);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(230, 233, 238)));
        add(scrollPane, BorderLayout.CENTER);

        // 3. THANH THAO TÁC DƯỚI
        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        pnlBottom.setBackground(Color.WHITE);

        JButton btnSua = UIUtil.createButton("Sửa thông tin", new Color(253, 126, 20), Color.WHITE);
        btnSua.addActionListener(e -> xuLySuaKhachHang());
        pnlBottom.add(btnSua);

        add(pnlBottom, BorderLayout.SOUTH);
    }

    public void loadDuLieu() {
        List<KhachHang> list = khachHangService.layTatCaKhachHang();
        hienThiLenBang(list);
    }

    private void xuLyTimKiem() {
        String kw = txtTimKiem.getText().trim();
        List<KhachHang> list = khachHangService.timKiem(kw);
        hienThiLenBang(list);
    }

    private void hienThiLenBang(List<KhachHang> list) {
        modelKhachHang.setRowCount(0);
        for (KhachHang kh : list) {
            modelKhachHang.addRow(new Object[]{
                    kh.getMaKhachHang(),
                    kh.getHoTen(),
                    kh.getSoDienThoai(),
                    kh.getEmail() != null ? kh.getEmail() : "",
                    kh.getSoCCCD(),
                    FormatUtil.formatNgay(kh.getNgayDangKy())
            });
        }
    }

    private void hienThiDialogThemKhachHang() {
        JTextField txtMa = new JTextField("KH" + System.currentTimeMillis() % 10000);
        JTextField txtTen = new JTextField();
        JTextField txtSdt = new JTextField();
        JTextField txtEmail = new JTextField();
        JTextField txtCccd = new JTextField();

        JPanel panel = new JPanel(new GridLayout(5, 2, 8, 8));
        panel.add(new JLabel("Mã khách hàng:"));
        panel.add(txtMa);
        panel.add(new JLabel("Họ và tên:"));
        panel.add(txtTen);
        panel.add(new JLabel("Số điện thoại:"));
        panel.add(txtSdt);
        panel.add(new JLabel("Email:"));
        panel.add(txtEmail);
        panel.add(new JLabel("Số CCCD/Hộ chiếu:"));
        panel.add(txtCccd);

        int result = JOptionPane.showConfirmDialog(this, panel, "Thêm khách hàng mới", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                KhachHang kh = new KhachHang(
                        txtMa.getText().trim(),
                        txtTen.getText().trim(),
                        txtSdt.getText().trim(),
                        txtEmail.getText().trim(),
                        txtCccd.getText().trim(),
                        LocalDate.now()
                );
                khachHangService.themKhachHang(kh);
                UIUtil.showSuccess(this, "Thêm khách hàng thành công!");
                loadDuLieu();
            } catch (Exception ex) {
                UIUtil.showError(this, ex.getMessage());
            }
        }
    }

    private void xuLySuaKhachHang() {
        int row = tblKhachHang.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn khách hàng cần sửa thông tin.");
            return;
        }

        String maKh = (String) modelKhachHang.getValueAt(row, 0);
        KhachHang kh = khachHangService.timTheoMa(maKh).orElse(null);
        if (kh == null) return;

        JTextField txtTen = new JTextField(kh.getHoTen());
        JTextField txtSdt = new JTextField(kh.getSoDienThoai());
        JTextField txtEmail = new JTextField(kh.getEmail() != null ? kh.getEmail() : "");
        JTextField txtCccd = new JTextField(kh.getSoCCCD());

        JPanel panel = new JPanel(new GridLayout(4, 2, 8, 8));
        panel.add(new JLabel("Họ và tên:"));
        panel.add(txtTen);
        panel.add(new JLabel("Số điện thoại:"));
        panel.add(txtSdt);
        panel.add(new JLabel("Email:"));
        panel.add(txtEmail);
        panel.add(new JLabel("Số CCCD/Hộ chiếu:"));
        panel.add(txtCccd);

        int result = JOptionPane.showConfirmDialog(this, panel, "Cập nhật khách hàng " + kh.getHoTen(), JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                kh.setHoTen(txtTen.getText().trim());
                kh.setSoDienThoai(txtSdt.getText().trim());
                kh.setEmail(txtEmail.getText().trim());
                kh.setSoCCCD(txtCccd.getText().trim());

                khachHangService.capNhatKhachHang(kh);
                UIUtil.showSuccess(this, "Cập nhật thông tin khách hàng thành công!");
                loadDuLieu();
            } catch (Exception ex) {
                UIUtil.showError(this, ex.getMessage());
            }
        }
    }
}
