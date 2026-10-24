package com.qlks.view;

import com.qlks.model.TaiKhoan;
import com.qlks.service.AuthService;
import com.qlks.util.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Màn hình đăng nhập hệ thống Quản lý khách sạn.
 */
public class LoginFrame extends JFrame {

    private final AuthService authService;
    private JTextField txtTenDangNhap;
    private JPasswordField txtMatKhau;
    private JCheckBox chkHienThiMatKhau;
    private JButton btnDangNhap;

    public LoginFrame() {
        this.authService = new AuthService();
        initComponents();
    }

    private void initComponents() {
        setTitle("Đăng nhập - Hệ thống Quản lý Khách sạn");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 420);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);

        // Header Panel
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(UIUtil.COLOR_PRIMARY);
        pnlHeader.setBorder(BorderFactory.createEmptyBorder(25, 20, 25, 20));

        JLabel lblHotelIcon = new JLabel("HOTEL MANAGEMENT SYSTEM", SwingConstants.CENTER);
        lblHotelIcon.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblHotelIcon.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Hệ thống Quản lý Khách sạn Chuyên nghiệp", SwingConstants.CENTER);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(new Color(225, 238, 255));

        pnlHeader.add(lblHotelIcon, BorderLayout.CENTER);
        pnlHeader.add(lblSub, BorderLayout.SOUTH);
        mainPanel.add(pnlHeader, BorderLayout.NORTH);

        // Form Panel
        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setBackground(Color.WHITE);
        pnlForm.setBorder(BorderFactory.createEmptyBorder(20, 40, 20, 40));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);

        // Tên đăng nhập
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel lblUser = new JLabel("Tên đăng nhập:");
        lblUser.setFont(UIUtil.FONT_BOLD);
        pnlForm.add(lblUser, gbc);

        gbc.gridy = 1;
        txtTenDangNhap = new JTextField();
        txtTenDangNhap.setFont(UIUtil.FONT_REGULAR);
        txtTenDangNhap.setPreferredSize(new Dimension(300, 36));
        pnlForm.add(txtTenDangNhap, gbc);

        // Mật khẩu
        gbc.gridy = 2;
        JLabel lblPass = new JLabel("Mật khẩu:");
        lblPass.setFont(UIUtil.FONT_BOLD);
        pnlForm.add(lblPass, gbc);

        gbc.gridy = 3;
        txtMatKhau = new JPasswordField();
        txtMatKhau.setFont(UIUtil.FONT_REGULAR);
        txtMatKhau.setPreferredSize(new Dimension(300, 36));
        pnlForm.add(txtMatKhau, gbc);

        // Checkbox hiển thị mật khẩu
        gbc.gridy = 4;
        chkHienThiMatKhau = new JCheckBox("Hiển thị mật khẩu");
        chkHienThiMatKhau.setFont(UIUtil.FONT_REGULAR);
        chkHienThiMatKhau.setBackground(Color.WHITE);
        chkHienThiMatKhau.addActionListener(e -> {
            if (chkHienThiMatKhau.isSelected()) {
                txtMatKhau.setEchoChar((char) 0);
            } else {
                txtMatKhau.setEchoChar('•');
            }
        });
        pnlForm.add(chkHienThiMatKhau, gbc);

        // Nút đăng nhập
        gbc.gridy = 5;
        gbc.insets = new Insets(15, 0, 10, 0);
        btnDangNhap = UIUtil.createButton("ĐĂNG NHẬP", UIUtil.COLOR_PRIMARY, Color.WHITE);
        btnDangNhap.setPreferredSize(new Dimension(300, 40));
        btnDangNhap.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDangNhap.addActionListener(e -> xuLyDangNhap());
        pnlForm.add(btnDangNhap, gbc);

        mainPanel.add(pnlForm, BorderLayout.CENTER);

        // Lắng nghe phím Enter
        KeyAdapter enterKeyAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    xuLyDangNhap();
                }
            }
        };
        txtTenDangNhap.addKeyListener(enterKeyAdapter);
        txtMatKhau.addKeyListener(enterKeyAdapter);

        setContentPane(mainPanel);
    }

    private void xuLyDangNhap() {
        String username = txtTenDangNhap.getText().trim();
        String password = new String(txtMatKhau.getPassword());

        try {
            TaiKhoan tk = authService.dangNhap(username, password);
            // Mở MainFrame và truyền AuthService
            SwingUtilities.invokeLater(() -> {
                MainFrame mainFrame = new MainFrame(authService, tk);
                mainFrame.setVisible(true);
            });
            this.dispose(); // Đóng LoginFrame
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
            txtMatKhau.requestFocus();
            txtMatKhau.selectAll();
        }
    }
}
