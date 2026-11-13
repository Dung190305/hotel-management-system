package com.qlks.view.dialogs;

import com.qlks.controller.AuthController;
import com.qlks.util.UIUtil;

import javax.swing.*;
import java.awt.*;

/**
 * Hộp thoại đổi mật khẩu cá nhân.
 */
public class DoiMatKhauDialog extends JDialog {

    private final AuthController authController;
    private final String tenDangNhap;

    private JPasswordField txtMatKhauCu;
    private JPasswordField txtMatKhauMoi;
    private JPasswordField txtXacNhan;

    public DoiMatKhauDialog(Frame parent, AuthController authController, String tenDangNhap) {
        super(parent, "Đổi mật khẩu - " + tenDangNhap, true);
        this.authController = authController;
        this.tenDangNhap = tenDangNhap;

        initComponents();
        setSize(420, 320);
        setLocationRelativeTo(parent);
        setResizable(false);
    }

    private void initComponents() {
        JPanel contentPane = new JPanel(new BorderLayout(15, 15));
        contentPane.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        contentPane.setBackground(Color.WHITE);

        // Header
        JLabel lblTitle = new JLabel("ĐỔI MẬT KHẨU TÀI KHOẢN", SwingConstants.CENTER);
        lblTitle.setFont(UIUtil.FONT_TITLE);
        lblTitle.setForeground(UIUtil.COLOR_PRIMARY);
        contentPane.add(lblTitle, BorderLayout.NORTH);

        // Form
        JPanel pnlForm = new JPanel(new GridLayout(3, 2, 10, 15));
        pnlForm.setBackground(Color.WHITE);

        JLabel lblCu = new JLabel("Mật khẩu hiện tại:");
        lblCu.setFont(UIUtil.FONT_BOLD);
        txtMatKhauCu = new JPasswordField();

        JLabel lblMoi = new JLabel("Mật khẩu mới:");
        lblMoi.setFont(UIUtil.FONT_BOLD);
        txtMatKhauMoi = new JPasswordField();

        JLabel lblXn = new JLabel("Xác nhận mật khẩu:");
        lblXn.setFont(UIUtil.FONT_BOLD);
        txtXacNhan = new JPasswordField();

        pnlForm.add(lblCu);
        pnlForm.add(txtMatKhauCu);
        pnlForm.add(lblMoi);
        pnlForm.add(txtMatKhauMoi);
        pnlForm.add(lblXn);
        pnlForm.add(txtXacNhan);

        contentPane.add(pnlForm, BorderLayout.CENTER);

        // Buttons
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlButtons.setBackground(Color.WHITE);

        JButton btnLuu = UIUtil.createButton("Lưu mật khẩu", UIUtil.COLOR_PRIMARY, Color.WHITE);
        JButton btnHuy = UIUtil.createButton("Hủy bỏ", new Color(220, 224, 230), Color.BLACK);

        btnLuu.addActionListener(e -> xuLyDoiMatKhau());
        btnHuy.addActionListener(e -> dispose());

        pnlButtons.add(btnHuy);
        pnlButtons.add(btnLuu);

        contentPane.add(pnlButtons, BorderLayout.SOUTH);
        setContentPane(contentPane);
    }

    private void xuLyDoiMatKhau() {
        String cu = new String(txtMatKhauCu.getPassword());
        String moi = new String(txtMatKhauMoi.getPassword());
        String xn = new String(txtXacNhan.getPassword());

        try {
            authController.doiMatKhau(tenDangNhap, cu, moi, xn);
            UIUtil.showSuccess(this, "Đổi mật khẩu thành công! Vui lòng ghi nhớ mật khẩu mới.");
            dispose();
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }
}
