package com.qlks.view;

import com.qlks.controller.AuthController;
import com.qlks.controller.ThongBaoController;
import com.qlks.model.NhanVien;
import com.qlks.model.TaiKhoan;
import com.qlks.model.enums.Permission;
import com.qlks.model.enums.VaiTro;
import com.qlks.service.AuthService;
import com.qlks.service.AuthorizationService;
import com.qlks.service.ThongBaoService.TrangThaiChuong;
import com.qlks.util.AppEventBus;
import com.qlks.util.BellIcon;
import com.qlks.util.UIUtil;
import com.qlks.view.dialogs.DoiMatKhauDialog;
import com.qlks.view.dialogs.ThongBaoDialog;
import com.qlks.view.panels.*;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Cửa sổ làm việc chính (Main Dashboard) của Hệ thống Quản lý Khách sạn.
 * Bố cục Sidebar hiện đại kết hợp CardLayout và Phân quyền người dùng.
 */
public class MainFrame extends JFrame implements AppEventBus.AppEventListener {

    private final AuthController authController;
    private final TaiKhoan taiKhoanHienTai;
    private final ThongBaoController thongBaoController;

    private JPanel pnlContentArea;
    private CardLayout cardLayout;
    private final Map<String, JButton> sidebarButtons = new LinkedHashMap<>();
    private final Map<String, RefreshablePanel> panelMap = new LinkedHashMap<>();
    private PhongPanel pnlPhongRef;
    private BellIcon bellIconHeader;
    private JButton btnThongBaoHeader;
    private Timer timerThongBao;

    public MainFrame(AuthService authService, TaiKhoan taiKhoanHienTai) {
        this(new AuthController(authService), taiKhoanHienTai);
    }

    public MainFrame(AuthController authController, TaiKhoan taiKhoanHienTai) {
        this.authController = authController;
        this.taiKhoanHienTai = taiKhoanHienTai;
        this.thongBaoController = new ThongBaoController();

        initComponents();
        AppEventBus.register(this);
        khoiTaoTimerThongBao();
        capNhatSoLuongThongBaoHeader();
    }

    private void initComponents() {
        setTitle("Hệ thống Quản lý Khách sạn - " + taiKhoanHienTai.getVaiTro().getTenHienThi());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1320, 820);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        // Đảm bảo dừng Timer khi tắt cửa sổ
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                if (pnlPhongRef != null) {
                    pnlPhongRef.stopAutoRefresh();
                }
                if (timerThongBao != null) {
                    timerThongBao.stop();
                }
                AppEventBus.unregister(MainFrame.this);
            }
        });

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
        this.pnlPhongRef = pnlPhong;
        DatPhongPanel pnlDatPhong = new DatPhongPanel();
        CheckInCheckOutPanel pnlCheckInOut = new CheckInCheckOutPanel();
        HoaDonPanel pnlHoaDon = new HoaDonPanel();
        KhachHangPanel pnlKhachHang = new KhachHangPanel();
        DichVuPanel pnlDichVu = new DichVuPanel();
        ThongBaoPanel pnlThongBao = new ThongBaoPanel(this::chuyenTab);

        pnlContentArea.add(pnlPhong, "PHONG");
        pnlContentArea.add(pnlDatPhong, "DAT_PHONG");
        pnlContentArea.add(pnlCheckInOut, "CHECK_IN_OUT");
        pnlContentArea.add(pnlHoaDon, "HOA_DON");
        pnlContentArea.add(pnlKhachHang, "KHACH_HANG");
        pnlContentArea.add(pnlDichVu, "DICH_VU");
        pnlContentArea.add(pnlThongBao, "THONG_BAO");

        panelMap.put("PHONG", pnlPhong);
        panelMap.put("DAT_PHONG", pnlDatPhong);
        panelMap.put("CHECK_IN_OUT", pnlCheckInOut);
        panelMap.put("HOA_DON", pnlHoaDon);
        panelMap.put("KHACH_HANG", pnlKhachHang);
        panelMap.put("DICH_VU", pnlDichVu);
        panelMap.put("THONG_BAO", pnlThongBao);

        // Phân quyền theo Permission: Xem báo cáo doanh thu
        if (AuthorizationService.hasPermission(taiKhoanHienTai.getVaiTro(), Permission.XEM_BAO_CAO)) {
            ThongKePanel pnlThongKe = new ThongKePanel();
            pnlContentArea.add(pnlThongKe, "THONG_KE");
            panelMap.put("THONG_KE", pnlThongKe);
        }

        // Phân quyền theo Permission: Quản lý tài khoản & nhân viên
        if (AuthorizationService.hasPermission(taiKhoanHienTai.getVaiTro(), Permission.QUAN_LY_TAI_KHOAN)) {
            NhanVienPanel pnlNhanVien = new NhanVienPanel();
            pnlContentArea.add(pnlNhanVien, "NHAN_VIEN");
            panelMap.put("NHAN_VIEN", pnlNhanVien);
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
        JLabel lblBrand = new JLabel("LUXURY HOTEL PMS");
        lblBrand.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblBrand.setForeground(Color.WHITE);
        header.add(lblBrand, BorderLayout.WEST);

        // Thông tin người dùng & Đăng xuất bên phải
        JPanel pnlUser = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 14));
        pnlUser.setOpaque(false);

        NhanVien nv = AuthService.getNhanVienHienTai();
        String hoTen = nv != null ? nv.getHoTen() : taiKhoanHienTai.getTenDangNhap();
        String roleStr = taiKhoanHienTai.getVaiTro().getTenHienThi();

        JLabel lblUserInfo = new JLabel("Xin chào, " + hoTen + " (" + roleStr + ")");
        lblUserInfo.setFont(UIUtil.FONT_BOLD);
        lblUserInfo.setForeground(Color.WHITE);
        pnlUser.add(lblUserInfo);

        bellIconHeader = new BellIcon(28);
        btnThongBaoHeader = new JButton("Thông báo", bellIconHeader);
        btnThongBaoHeader.setFont(UIUtil.FONT_BOLD);
        btnThongBaoHeader.setForeground(Color.WHITE);
        btnThongBaoHeader.setBackground(new Color(24, 119, 242));
        btnThongBaoHeader.setFocusPainted(false);
        btnThongBaoHeader.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnThongBaoHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 255, 255, 140), 1, true),
                BorderFactory.createEmptyBorder(6, 12, 6, 14)
        ));
        btnThongBaoHeader.addActionListener(e -> {
            ThongBaoDialog dlg = new ThongBaoDialog(this, thongBaoController, this::chuyenTab);
            dlg.setVisible(true);
        });
        pnlUser.add(btnThongBaoHeader);

        JButton btnDoiPass = UIUtil.createButton("Đổi mật khẩu", new Color(44, 130, 201), Color.WHITE);
        btnDoiPass.addActionListener(e -> {
            DoiMatKhauDialog dlg = new DoiMatKhauDialog(this, authController, taiKhoanHienTai.getTenDangNhap());
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
        addSidebarItem(sidebar, "Sơ đồ phòng", "PHONG");
        addSidebarItem(sidebar, "Đặt phòng (Booking)", "DAT_PHONG");
        addSidebarItem(sidebar, "Tiếp tân: Check-in / Out", "CHECK_IN_OUT");
        addSidebarItem(sidebar, "Thông báo tới hạn", "THONG_BAO");
        addSidebarItem(sidebar, "Hóa đơn & Thanh toán", "HOA_DON");
        addSidebarItem(sidebar, "Quản lý Khách hàng", "KHACH_HANG");
        addSidebarItem(sidebar, "Danh mục Dịch vụ", "DICH_VU");

        // Phân quyền theo Permission: Chỉ quyền QUAN_LY_TAI_KHOAN mới thấy tab Nhân sự
        if (AuthorizationService.hasPermission(taiKhoanHienTai.getVaiTro(), Permission.QUAN_LY_TAI_KHOAN)) {
            addSidebarItem(sidebar, "Nhân sự & Tính lương", "NHAN_VIEN");
        }

        // Phân quyền theo Permission: Chỉ quyền XEM_BAO_CAO mới thấy tab Báo cáo
        if (AuthorizationService.hasPermission(taiKhoanHienTai.getVaiTro(), Permission.XEM_BAO_CAO)) {
            addSidebarItem(sidebar, "Báo cáo Doanh thu", "THONG_KE");
        }

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

    public void chuyenTab(String cardName) {
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

        // Tự động tải lại dữ liệu mới nhất từ CSDL khi chuyển sang Tab tương ứng
        RefreshablePanel activePanel = panelMap.get(cardName);
        if (activePanel != null) {
            activePanel.loadDuLieu();
        }
    }

    private void xuLyDangXuat() {
        if (UIUtil.showConfirm(this, "Bạn có chắc chắn muốn đăng xuất khỏi hệ thống không?")) {
            if (pnlPhongRef != null) {
                pnlPhongRef.stopAutoRefresh();
            }
            if (timerThongBao != null) {
                timerThongBao.stop();
            }
            AppEventBus.unregister(this);
            authController.dangXuat();
            SwingUtilities.invokeLater(() -> {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
            });
            this.dispose();
        }
    }

    @Override
    public void onEvent(AppEventBus.EventType eventType) {
        if (eventType == AppEventBus.EventType.BOOKING_CHANGED
                || eventType == AppEventBus.EventType.ROOM_STATUS_CHANGED
                || eventType == AppEventBus.EventType.INVOICE_CHANGED) {
            capNhatSoLuongThongBaoHeader();
        }
    }

    private void khoiTaoTimerThongBao() {
        timerThongBao = new Timer(60000, e -> capNhatSoLuongThongBaoHeader());
        timerThongBao.start();
    }

    public void capNhatSoLuongThongBaoHeader() {
        try {
            TrangThaiChuong tt = thongBaoController.getTrangThaiChuong();
            int tong = thongBaoController.demTongSoLuongToiHan();
            int chuaDoc = thongBaoController.demSoLuongChuaDoc();

            if (bellIconHeader != null) {
                bellIconHeader.setTrangThai(tt);
            }

            if (btnThongBaoHeader != null) {
                if (tt == TrangThaiChuong.CO_CHUA_DOC) {
                    btnThongBaoHeader.setText("Thông báo (" + chuaDoc + ")");
                    btnThongBaoHeader.setToolTipText("Có " + chuaDoc + " thông báo chưa đọc (Dấu ! màu ĐỎ)");
                } else if (tt == TrangThaiChuong.DA_DOC_HET) {
                    btnThongBaoHeader.setText("Thông báo (" + tong + ")");
                    btnThongBaoHeader.setToolTipText("Tất cả thông báo đã đọc (Dấu ! màu XANH)");
                } else {
                    btnThongBaoHeader.setText("Thông báo");
                    btnThongBaoHeader.setToolTipText("Không có thông báo nào (Không có dấu !)");
                }
                btnThongBaoHeader.repaint();
            }
        } catch (Exception ignored) {
        }
    }
}
