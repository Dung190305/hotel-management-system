package com.qlks.view;

import com.qlks.model.NhanVien;
import com.qlks.model.TaiKhoan;
import com.qlks.model.enums.VaiTro;
import com.qlks.service.AuthService;
import com.qlks.util.UIUtil;
import com.qlks.view.dialogs.DoiMatKhauDialog;
import com.qlks.view.panels.*;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cửa sổ làm việc chính (Main Dashboard) của Hệ thống Quản lý Khách sạn.
 * Bố cục Sidebar hiện đại kết hợp CardLayout và Phân quyền người dùng.
 */
public class MainFrame extends JFrame {

    private final AuthService authService;
    private final TaiKhoan taiKhoanHienTai;

    private JPanel pnlContentArea;
    private CardLayout cardLayout;
    private final Map<String, JButton> sidebarButtons = new LinkedHashMap<>();

    public MainFrame(AuthService authService, TaiKhoan taiKhoanHienTai) {
        this.authService = authService;
        this.taiKhoanHienTai = taiKhoanHienTai;

        initComponents();
    }

    private void initComponents() {
        setTitle("Hệ thống Quản lý Khách sạn - " + (taiKhoanHienTai.getVaiTro() == VaiTro.ADMIN ? "Quản lý" : "Lễ tân"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1320, 820);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(UIUtil.COLOR_BACKGROUND);

        // 1. TOP HEADER
        rootPanel.add(createHeaderPanel(), BorderLayout.NORTH);

        // 2. MAIN BODY (SIDEBAR + CONTENT)
        JPanel bodyPanel = new JPanel(new BorderLayout());

        // Sidebar
        bodyPanel.add(createSidebarPanel(), BorderLayout.WEST);

        // Content Area (CardLayout)
        cardLayout = new CardLayout();
        pnlContentArea = new JPanel(cardLayout);
        pnlContentArea.setBackground(UIUtil.COLOR_BACKGROUND);

        // Thêm các Panel chức năng vào CardLayout
        PhongPanel pnlPhong = new PhongPanel();
        DatPhongPanel pnlDatPhong = new DatPhongPanel();
        CheckInCheckOutPanel pnlCheckInOut = new CheckInCheckOutPanel();
        HoaDonPanel pnlHoaDon = new HoaDonPanel();
        KhachHangPanel pnlKhachHang = new KhachHangPanel();
        DichVuPanel pnlDichVu = new DichVuPanel();
        ThongKePanel pnlThongKe = new ThongKePanel();

        pnlContentArea.add(pnlPhong, "PHONG");
        pnlContentArea.add(pnlDatPhong, "DAT_PHONG");
        pnlContentArea.add(pnlCheckInOut, "CHECK_IN_OUT");
        pnlContentArea.add(pnlHoaDon, "HOA_DON");
        pnlContentArea.add(pnlKhachHang, "KHACH_HANG");
        pnlContentArea.add(pnlDichVu, "DICH_VU");
        pnlContentArea.add(pnlThongKe, "THONG_KE");

        // Chỉ thêm Panel Nhân viên nếu là Quản lý (Admin)
        if (taiKhoanHienTai.getVaiTro() == VaiTro.ADMIN) {
            NhanVienPanel pnlNhanVien = new NhanVienPanel();
            pnlContentArea.add(pnlNhanVien, "NHAN_VIEN");
        }

        bodyPanel.add(pnlContentArea, BorderLayout.CENTER);
        rootPanel.add(bodyPanel, BorderLayout.CENTER);

        setContentPane(rootPanel);

        // Mặc định chọn màn hình đầu tiên
        chuyenTab("PHONG");
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout(15, 0));
        header.setBackground(UIUtil.COLOR_PRIMARY_DARK);
        header.setPreferredSize(new Dimension(getWidth(), 60));
        header.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

        // Logo thương hiệu bên trái
        JLabel lblBrand = new JLabel("GRAND LUXURY HOTEL PMS");
        lblBrand.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblBrand.setForeground(Color.WHITE);
        header.add(lblBrand, BorderLayout.WEST);

        // Thông tin người dùng & Đăng xuất bên phải
        JPanel pnlUser = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 14));
        pnlUser.setOpaque(false);

        NhanVien nv = AuthService.getNhanVienHienTai();
        String hoTen = nv != null ? nv.getHoTen() : taiKhoanHienTai.getTenDangNhap();
        String roleStr = taiKhoanHienTai.getVaiTro() == VaiTro.ADMIN ? "Quản lý" : "Lễ tân";

        JLabel lblUserInfo = new JLabel("Xin chào, " + hoTen + " (" + roleStr + ")");
        lblUserInfo.setFont(UIUtil.FONT_BOLD);
        lblUserInfo.setForeground(Color.WHITE);
        pnlUser.add(lblUserInfo);

        JButton btnDoiPass = UIUtil.createButton("Đổi mật khẩu", new Color(44, 130, 201), Color.WHITE);
        btnDoiPass.addActionListener(e -> {
            DoiMatKhauDialog dlg = new DoiMatKhauDialog(this, authService, taiKhoanHienTai.getTenDangNhap());
            dlg.setVisible(true);
        });
        pnlUser.add(btnDoiPass);

        JButton btnDangXuat = UIUtil.createButton("Đăng xuất", new Color(220, 53, 69), Color.WHITE);
        btnDangXuat.addActionListener(e -> xuLyDangXuat());
        pnlUser.add(btnDangXuat);

        header.add(pnlUser, BorderLayout.EAST);
        return header;
    }

    private JPanel createSidebarPanel() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Color.WHITE);
        sidebar.setPreferredSize(new Dimension(240, getHeight()));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(230, 233, 238)));

        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));

        // Danh sách các mục điều hướng
        addSidebarItem(sidebar, "Sơ đồ & Danh mục Phòng", "PHONG");
        addSidebarItem(sidebar, "Đặt phòng (Booking)", "DAT_PHONG");
        addSidebarItem(sidebar, "Tiếp tân: Check-in / Out", "CHECK_IN_OUT");
        addSidebarItem(sidebar, "Hóa đơn & Thanh toán", "HOA_DON");
        addSidebarItem(sidebar, "Quản lý Khách hàng", "KHACH_HANG");
        addSidebarItem(sidebar, "Danh mục Dịch vụ", "DICH_VU");

        // Phân quyền: Chỉ Admin mới thấy tab Quản lý nhân viên
        if (taiKhoanHienTai.getVaiTro() == VaiTro.ADMIN) {
            addSidebarItem(sidebar, "Nhân sự & Tính lương", "NHAN_VIEN");
        }

        addSidebarItem(sidebar, "Báo cáo Doanh thu", "THONG_KE");

        sidebar.add(Box.createVerticalGlue());

        // Footer Sidebar
        JLabel lblVer = new JLabel("Phiên bản 1.0.0 (OOP Final)", SwingConstants.CENTER);
        lblVer.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lblVer.setForeground(new Color(150, 155, 160));
        lblVer.setAlignmentX(Component.CENTER_ALIGNMENT);
        sidebar.add(lblVer);
        sidebar.add(Box.createRigidArea(new Dimension(0, 15)));

        return sidebar;
    }

    private void addSidebarItem(JPanel sidebar, String title, String cardName) {
        JButton btn = new JButton(title);
        btn.setFont(UIUtil.FONT_BOLD);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(220, 42));
        btn.setPreferredSize(new Dimension(220, 42));
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 10));

        btn.addActionListener(e -> chuyenTab(cardName));

        sidebarButtons.put(cardName, btn);
        sidebar.add(btn);
        sidebar.add(Box.createRigidArea(new Dimension(0, 6)));
    }

    private void chuyenTab(String cardName) {
        cardLayout.show(pnlContentArea, cardName);

        // Đổi màu active cho button đang chọn
        for (Map.Entry<String, JButton> entry : sidebarButtons.entrySet()) {
            JButton btn = entry.getValue();
            if (entry.getKey().equals(cardName)) {
                btn.setBackground(UIUtil.COLOR_PRIMARY);
                btn.setForeground(Color.WHITE);
            } else {
                btn.setBackground(Color.WHITE);
                btn.setForeground(new Color(60, 64, 67));
            }
        }
    }

    private void xuLyDangXuat() {
        if (UIUtil.showConfirm(this, "Bạn có chắc chắn muốn đăng xuất khỏi hệ thống không?")) {
            authService.dangXuat();
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            });
            this.dispose();
        }
    }
}
