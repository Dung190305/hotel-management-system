package com.qlks.view.panels;

import com.qlks.model.LeTan;
import com.qlks.model.NhanVien;
import com.qlks.model.QuanLy;
import com.qlks.model.enums.VaiTro;
import com.qlks.service.NhanVienService;
import com.qlks.util.FormatUtil;
import com.qlks.util.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Màn hình Quản lý Nhân viên & Tính Bảng Lương Đa hình (Dành cho Quản lý / Admin).
 */
public class NhanVienPanel extends JPanel {

    private final NhanVienService nhanVienService;
    private JTable tblNhanVien;
    private DefaultTableModel modelNhanVien;

    public NhanVienPanel() {
        this.nhanVienService = new NhanVienService();
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

        JLabel lblTitle = new JLabel("QUẢN LÝ NHÂN SỰ & TÍNH LƯƠNG ĐA HÌNH");
        lblTitle.setFont(UIUtil.FONT_TITLE);
        lblTitle.setForeground(UIUtil.COLOR_PRIMARY);
        pnlTop.add(lblTitle, BorderLayout.WEST);

        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlActions.setBackground(Color.WHITE);

        JButton btnThemNv = UIUtil.createButton("+ Thêm nhân viên", UIUtil.COLOR_PRIMARY, Color.WHITE);
        btnThemNv.addActionListener(e -> hienThiDialogThemNhanVien());
        pnlActions.add(btnThemNv);

        JButton btnBangLuong = UIUtil.createButton("💰 BẢNG TÍNH LƯƠNG (OOP)", new Color(40, 167, 69), Color.WHITE);
        btnBangLuong.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnBangLuong.addActionListener(e -> hienThiBangLuongPolymorphism());
        pnlActions.add(btnBangLuong);

        JButton btnLamMoi = UIUtil.createButton("Làm mới", new Color(108, 117, 125), Color.WHITE);
        btnLamMoi.addActionListener(e -> loadDuLieu());
        pnlActions.add(btnLamMoi);

        pnlTop.add(pnlActions, BorderLayout.EAST);
        add(pnlTop, BorderLayout.NORTH);

        // 2. BẢNG DANH SÁCH NHÂN VIÊN
        String[] headers = {"Mã NV", "Họ và tên", "Chức vụ", "Tài khoản", "Số điện thoại", "Email", "Ngày vào làm", "Lương cơ bản", "Chi tiết phụ cấp / Hệ số", "Trạng thái"};
        modelNhanVien = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblNhanVien = new JTable(modelNhanVien);
        UIUtil.formatTable(tblNhanVien);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tblNhanVien.getColumnModel().getColumn(7).setCellRenderer(rightRenderer);

        add(new JScrollPane(tblNhanVien), BorderLayout.CENTER);
    }

    public void loadDuLieu() {
        List<NhanVien> list = nhanVienService.layTatCaNhanVien();
        modelNhanVien.setRowCount(0);

        for (NhanVien nv : list) {
            String chiTiet = "";
            if (nv instanceof LeTan lt) {
                chiTiet = "Ca trực: " + lt.getSoCaTruc() + " | Thưởng ca: " + FormatUtil.formatTien(lt.getThuongCa());
            } else if (nv instanceof QuanLy ql) {
                chiTiet = "Hệ số: " + ql.getHeSoTrachNhiem() + " | Thưởng HQ: " + FormatUtil.formatTien(ql.getThuongHieuQua());
            }

            modelNhanVien.addRow(new Object[]{
                    nv.getMaNhanVien(),
                    nv.getHoTen(),
                    nv.getChucVu(),
                    nv.getTenDangNhap() != null ? nv.getTenDangNhap() : "",
                    nv.getSoDienThoai(),
                    nv.getEmail(),
                    FormatUtil.formatNgay(nv.getNgayVaoLam()),
                    FormatUtil.formatTien(nv.getLuongCoBan()),
                    chiTiet,
                    nv.isDangHoatDong() ? "Đang làm việc" : "Đã nghỉ việc"
            });
        }
    }

    /**
     * Minh họa ĐA HÌNH OOP khi bảo vệ đồ án:
     * Gọi nv.tinhLuong() tự động phân giải tại Runtime cho LeTan và QuanLy.
     */
    private void hienThiBangLuongPolymorphism() {
        Map<String, Object> ketQua = nhanVienService.tinhBangLuong();
        @SuppressWarnings("unchecked")
        List<NhanVien> ds = (List<NhanVien>) ketQua.get("danhSachNhanVien");
        BigDecimal tongQuyLuong = (BigDecimal) ketQua.get("tongQuyLuong");

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Bảng Lương Toàn Bộ Nhân Viên (Đa Hình OOP)", true);
        dlg.setSize(750, 480);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout(10, 10));

        String[] headers = {"Mã NV", "Họ và tên", "Chức vụ", "Lương cơ bản", "Công thức tính lương", "Lương thực nhận"};
        DefaultTableModel model = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (NhanVien nv : ds) {
            if (nv.isDangHoatDong()) {
                String congThuc = "";
                if (nv instanceof LeTan lt) {
                    congThuc = "Lương CB + " + lt.getSoCaTruc() + " ca * " + FormatUtil.formatTien(lt.getThuongCa());
                } else if (nv instanceof QuanLy ql) {
                    congThuc = "Lương CB * " + ql.getHeSoTrachNhiem() + " + " + FormatUtil.formatTien(ql.getThuongHieuQua());
                }

                // ĐA HÌNH Ở ĐÂY: nv.tinhLuong()
                model.addRow(new Object[]{
                        nv.getMaNhanVien(),
                        nv.getHoTen(),
                        nv.getChucVu(),
                        FormatUtil.formatTien(nv.getLuongCoBan()),
                        congThuc,
                        FormatUtil.formatTien(nv.tinhLuong())
                });
            }
        }

        JTable tbl = new JTable(model);
        UIUtil.formatTable(tbl);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        tbl.getColumnModel().getColumn(3).setCellRenderer(right);
        tbl.getColumnModel().getColumn(5).setCellRenderer(right);

        dlg.add(new JScrollPane(tbl), BorderLayout.CENTER);

        // Footer hiển thị tổng quỹ lương
        JPanel pnlFooter = new JPanel(new BorderLayout());
        pnlFooter.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        pnlFooter.setBackground(Color.WHITE);

        JLabel lblTong = new JLabel("TỔNG QUỸ LƯƠNG THÁNG NÀY: " + FormatUtil.formatTien(tongQuyLuong));
        lblTong.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTong.setForeground(new Color(220, 53, 69));
        pnlFooter.add(lblTong, BorderLayout.WEST);

        JButton btnDong = UIUtil.createButton("Đóng", UIUtil.COLOR_PRIMARY, Color.WHITE);
        btnDong.addActionListener(e -> dlg.dispose());
        pnlFooter.add(btnDong, BorderLayout.EAST);

        dlg.add(pnlFooter, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private void hienThiDialogThemNhanVien() {
        JTextField txtMa = new JTextField("NV" + System.currentTimeMillis() % 10000);
        JTextField txtTen = new JTextField();
        JTextField txtSdt = new JTextField();
        JTextField txtEmail = new JTextField();
        JComboBox<String> cboChucVu = new JComboBox<>(new String[]{"Lễ tân", "Quản lý"});
        JTextField txtLuongCb = new JTextField("7000000");

        JTextField txtUser = new JTextField();
        JPasswordField txtPass = new JPasswordField("123456");

        // Tham số chức vụ
        JSpinner spnCa = new JSpinner(new SpinnerNumberModel(26, 0, 60, 1));
        JTextField txtThuongCa = new JTextField("150000");
        JSpinner spnHeSo = new JSpinner(new SpinnerNumberModel(1.5, 1.0, 5.0, 0.1));
        JTextField txtThuongHq = new JTextField("2000000");

        spnHeSo.setEnabled(false);
        txtThuongHq.setEnabled(false);

        cboChucVu.addActionListener(e -> {
            boolean isQl = cboChucVu.getSelectedIndex() == 1;
            spnCa.setEnabled(!isQl);
            txtThuongCa.setEnabled(!isQl);
            spnHeSo.setEnabled(isQl);
            txtThuongHq.setEnabled(isQl);
            if (isQl && txtLuongCb.getText().equals("7000000")) {
                txtLuongCb.setText("15000000");
            }
        });

        JPanel panel = new JPanel(new GridLayout(10, 2, 8, 8));
        panel.add(new JLabel("Mã nhân viên:"));
        panel.add(txtMa);
        panel.add(new JLabel("Họ và tên:"));
        panel.add(txtTen);
        panel.add(new JLabel("Số điện thoại:"));
        panel.add(txtSdt);
        panel.add(new JLabel("Email:"));
        panel.add(txtEmail);
        panel.add(new JLabel("Chức vụ:"));
        panel.add(cboChucVu);
        panel.add(new JLabel("Lương cơ bản (VNĐ):"));
        panel.add(txtLuongCb);
        panel.add(new JLabel("Tên đăng nhập hệ thống:"));
        panel.add(txtUser);
        panel.add(new JLabel("Mật khẩu khởi tạo:"));
        panel.add(txtPass);
        panel.add(new JLabel("Số ca / Hệ số TN:"));
        panel.add(cboChucVu.getSelectedIndex() == 1 ? spnHeSo : spnCa);
        panel.add(new JLabel("Thưởng ca / Thưởng HQ:"));
        panel.add(cboChucVu.getSelectedIndex() == 1 ? txtThuongHq : txtThuongCa);

        int result = JOptionPane.showConfirmDialog(this, panel, "Thêm nhân viên mới", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                String ma = txtMa.getText().trim();
                String ten = txtTen.getText().trim();
                String sdt = txtSdt.getText().trim();
                String email = txtEmail.getText().trim();
                BigDecimal luongCb = FormatUtil.parseTien(txtLuongCb.getText());
                String user = txtUser.getText().trim();
                String pass = new String(txtPass.getPassword());

                NhanVien nvMoi;
                VaiTro vaiTro;
                if (cboChucVu.getSelectedIndex() == 1) {
                    double heSo = (Double) spnHeSo.getValue();
                    BigDecimal thuongHq = FormatUtil.parseTien(txtThuongHq.getText());
                    nvMoi = new QuanLy(ma, ten, sdt, email, user, LocalDate.now(), luongCb, true, heSo, thuongHq);
                    vaiTro = VaiTro.ADMIN;
                } else {
                    int ca = (Integer) spnCa.getValue();
                    BigDecimal thuongCa = FormatUtil.parseTien(txtThuongCa.getText());
                    nvMoi = new LeTan(ma, ten, sdt, email, user, LocalDate.now(), luongCb, true, ca, thuongCa);
                    vaiTro = VaiTro.LE_TAN;
                }

                nhanVienService.themNhanVien(nvMoi, user, pass, vaiTro);
                UIUtil.showSuccess(this, "Thêm nhân viên và tạo tài khoản thành công!");
                loadDuLieu();
            } catch (Exception ex) {
                UIUtil.showError(this, ex.getMessage());
            }
        }
    }
}
