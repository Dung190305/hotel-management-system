package com.qlks.view.panels;

import com.qlks.controller.DatPhongController;
import com.qlks.model.DatPhong;
import com.qlks.model.KhachHang;
import com.qlks.model.NhanVien;
import com.qlks.model.Phong;
import com.qlks.model.enums.PhuongThucThanhToan;
import com.qlks.model.enums.TrangThaiDatPhong;
import com.qlks.util.AppEventBus;
import com.qlks.util.FormatUtil;
import com.qlks.util.HolidayUtil;
import com.qlks.util.IdGenerator;
import com.qlks.util.UIUtil;
import com.qlks.view.RefreshablePanel;
import com.qlks.view.dialogs.QRPaymentDialog;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Màn hình Đặt phòng (Booking Management) & Chống trùng lịch đặt phòng.
 */
public class DatPhongPanel extends JPanel implements RefreshablePanel, AppEventBus.AppEventListener {

    private final DatPhongController datPhongController;

    private JTable tblDatPhong;
    private DefaultTableModel modelDatPhong;
    private JComboBox<String> cboLocTrangThai;

    public DatPhongPanel() {
        this.datPhongController = new DatPhongController();

        initComponents();
        AppEventBus.register(this);
        loadDuLieu();
    }

    @Override
    public void onEvent(AppEventBus.EventType eventType) {
        if (eventType == AppEventBus.EventType.BOOKING_CHANGED || eventType == AppEventBus.EventType.CUSTOMER_CHANGED) {
            loadDuLieu();
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UIUtil.COLOR_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 1. THANH TIÊU ĐỀ & NÚT THAO TÁC
        JPanel pnlTop = new JPanel(new BorderLayout(10, 10));
        pnlTop.setBackground(Color.WHITE);
        pnlTop.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 233, 238)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblTitle = new JLabel("QUẢN LÝ ĐẶT PHÒNG (BOOKINGS)");
        lblTitle.setFont(UIUtil.FONT_TITLE);
        lblTitle.setForeground(UIUtil.COLOR_PRIMARY);
        pnlTop.add(lblTitle, BorderLayout.WEST);

        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlActions.setBackground(Color.WHITE);

        pnlActions.add(new JLabel("Lọc trạng thái:"));
        cboLocTrangThai = new JComboBox<>(new String[]{
                "-- Tất cả trạng thái --",
                "Đã xác nhận (DA_XAC_NHAN)",
                "Đang ở (DANG_O)",
                "Đã trả phòng (DA_TRA_PHONG)",
                "Đã hủy (DA_HUY)"
        });
        cboLocTrangThai.addActionListener(e -> locDuLieu());
        pnlActions.add(cboLocTrangThai);

        JButton btnTaoMoi = UIUtil.createButton("+ Tạo đơn đặt phòng", UIUtil.COLOR_PRIMARY, Color.WHITE);
        btnTaoMoi.addActionListener(e -> hienThiDialogTaoDatPhong());
        pnlActions.add(btnTaoMoi);

        JButton btnLamMoi = UIUtil.createButton("Làm mới", new Color(108, 117, 125), Color.WHITE);
        btnLamMoi.addActionListener(e -> loadDuLieu());
        pnlActions.add(btnLamMoi);

        pnlTop.add(pnlActions, BorderLayout.EAST);
        add(pnlTop, BorderLayout.NORTH);

        // 2. BẢNG DANH SÁCH ĐẶT PHÒNG
        String[] headers = {"Mã ĐP", "Khách hàng", "Số điện thoại", "Phòng", "Loại phòng", "Ngày nhận", "Ngày trả", "Số đêm", "Tiền cọc", "Tạm tính", "Trạng thái"};
        modelDatPhong = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblDatPhong = new JTable(modelDatPhong);
        UIUtil.formatTable(tblDatPhong);

        // Căn lề tiền và số đêm
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tblDatPhong.getColumnModel().getColumn(7).setCellRenderer(rightRenderer);
        tblDatPhong.getColumnModel().getColumn(8).setCellRenderer(rightRenderer);
        tblDatPhong.getColumnModel().getColumn(9).setCellRenderer(rightRenderer);

        JScrollPane scrollPane = new JScrollPane(tblDatPhong);
        add(scrollPane, BorderLayout.CENTER);

        // 3. THANH THAO TÁC DƯỚI BẢNG
        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        pnlBottom.setBackground(Color.WHITE);

        JButton btnXemKhach = UIUtil.createButton("Xem khách lưu trú (CCCD)", new Color(23, 162, 184), Color.WHITE);
        btnXemKhach.addActionListener(e -> xuLyXemKhachLuuTru());
        pnlBottom.add(btnXemKhach);

        JButton btnCheckInNhanh = UIUtil.createButton("Nhận phòng (Check-in)", new Color(40, 167, 69), Color.WHITE);
        btnCheckInNhanh.addActionListener(e -> xuLyCheckInNhanh());
        pnlBottom.add(btnCheckInNhanh);

        JButton btnHuyDat = UIUtil.createButton("Hủy đặt phòng", new Color(220, 53, 69), Color.WHITE);
        btnHuyDat.addActionListener(e -> xuLyHuyDatPhong());
        pnlBottom.add(btnHuyDat);

        add(pnlBottom, BorderLayout.SOUTH);
    }

    public void loadDuLieu() {
        List<DatPhong> list = datPhongController.layTatCaDatPhong();
        hienThiLenBang(list);
    }

    private void locDuLieu() {
        int idx = cboLocTrangThai.getSelectedIndex();
        List<DatPhong> list = datPhongController.layTatCaDatPhong();

        if (idx == 1) list = list.stream().filter(d -> d.getTrangThai() == TrangThaiDatPhong.DA_XAC_NHAN).toList();
        else if (idx == 2) list = list.stream().filter(d -> d.getTrangThai() == TrangThaiDatPhong.DANG_O).toList();
        else if (idx == 3) list = list.stream().filter(d -> d.getTrangThai() == TrangThaiDatPhong.DA_TRA_PHONG).toList();
        else if (idx == 4) list = list.stream().filter(d -> d.getTrangThai() == TrangThaiDatPhong.DA_HUY).toList();

        hienThiLenBang(list);
    }

    private void hienThiLenBang(List<DatPhong> list) {
        modelDatPhong.setRowCount(0);
        for (DatPhong dp : list) {
            modelDatPhong.addRow(new Object[]{
                    dp.getMaDatPhong(),
                    dp.getKhachHang() != null ? dp.getKhachHang().getHoTen() : "",
                    dp.getKhachHang() != null ? dp.getKhachHang().getSoDienThoai() : "",
                    dp.getPhong() != null ? "P." + dp.getPhong().getSoPhong() : "",
                    dp.getPhong() != null ? dp.getPhong().getTenLoaiPhongHienThi() : "",
                    FormatUtil.formatNgayGio(dp.getNgayNhanPhong()),
                    FormatUtil.formatNgayGio(dp.getNgayTraPhong()),
                    dp.tinhSoDem() + " đêm",
                    FormatUtil.formatTien(dp.getTienCoc()),
                    FormatUtil.formatTien(dp.tinhTienTamTinh()),
                    dp.getTrangThai().getTenHienThi()
            });
        }
    }

    private void hienThiDialogTaoDatPhong() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Tạo phiếu đặt phòng mới (Kèm CCCD khách lưu trú)", true);
        dialog.setSize(800, 680);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        // 1. Panel thông tin đặt phòng
        JPanel pnlTopForm = new JPanel(new GridLayout(4, 4, 8, 8));
        pnlTopForm.setBorder(BorderFactory.createTitledBorder("Thông tin đặt phòng"));
        pnlTopForm.setBackground(Color.WHITE);

        JTextField txtMa = new JTextField(IdGenerator.next("DP"));
        txtMa.setEditable(false);
        txtMa.setBackground(new Color(245, 247, 250));

        List<KhachHang> dsKhach = datPhongController.layTatCaKhachHang();
        JComboBox<KhachHang> cboKhach = new JComboBox<>(dsKhach.toArray(new KhachHang[0]));

        // Nút thêm nhanh khách hàng mới ngay tại form đặt phòng
        JPanel pnlKhachHangRow = new JPanel(new BorderLayout(5, 0));
        pnlKhachHangRow.setOpaque(false);
        pnlKhachHangRow.add(cboKhach, BorderLayout.CENTER);

        JButton btnThemKhachNhanh = UIUtil.createButton("+", new Color(40, 167, 69), Color.WHITE);
        btnThemKhachNhanh.setToolTipText("Thêm khách hàng mới ngay");
        btnThemKhachNhanh.setPreferredSize(new Dimension(38, 28));
        btnThemKhachNhanh.addActionListener(e -> {
            JTextField tfTen = new JTextField();
            JTextField tfCccd = new JTextField();
            JTextField tfSdt = new JTextField();
            JTextField tfEmail = new JTextField();
            JPanel p = new JPanel(new GridLayout(4, 2, 6, 6));
            p.add(new JLabel("Họ và tên (*):")); p.add(tfTen);
            p.add(new JLabel("Số CCCD (12 số) (*):")); p.add(tfCccd);
            p.add(new JLabel("Số điện thoại (*):")); p.add(tfSdt);
            p.add(new JLabel("Email:")); p.add(tfEmail);
            int opt = JOptionPane.showConfirmDialog(dialog, p, "Đăng ký nhanh khách hàng mới", JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.OK_OPTION) {
                String ten = tfTen.getText().trim();
                String cccd = tfCccd.getText().trim();
                String sdt = tfSdt.getText().trim();
                String em = tfEmail.getText().trim();
                if (ten.isEmpty() || cccd.isEmpty() || sdt.isEmpty()) {
                    UIUtil.showError(dialog, "Vui lòng nhập đầy đủ Họ tên, CCCD và Số điện thoại!");
                    return;
                }
                if (!com.qlks.model.KhachLuuTru.kiemTraCCCD(cccd)) {
                    UIUtil.showError(dialog, "Số CCCD phải gồm đúng 12 chữ số!");
                    return;
                }
                try {
                    String maKhMoi = IdGenerator.next("KH");
                    KhachHang khMoi = new KhachHang(maKhMoi, ten, sdt, em, cccd, LocalDate.now());
                    new com.qlks.controller.KhachHangController().themKhachHang(khMoi);
                    cboKhach.addItem(khMoi);
                    cboKhach.setSelectedItem(khMoi);
                    UIUtil.showSuccess(dialog, "Đã thêm khách hàng " + ten + " thành công!");
                } catch (Exception exKh) {
                    UIUtil.showError(dialog, exKh.getMessage());
                }
            }
        });
        pnlKhachHangRow.add(btnThemKhachNhanh, BorderLayout.EAST);

        JTextField txtNgayNhan = new JTextField(FormatUtil.formatNgay(LocalDate.now()));
        JTextField txtNgayTra = new JTextField(FormatUtil.formatNgay(LocalDate.now().plusDays(1)));

        JComboBox<Phong> cboPhong = new JComboBox<>();
        JLabel lblSoDemVaTien = new JLabel("Tạm tính: 0 VNĐ");
        lblSoDemVaTien.setFont(UIUtil.FONT_BOLD);
        lblSoDemVaTien.setForeground(UIUtil.COLOR_PRIMARY);

        JLabel lblSucChua = new JLabel("Sức chứa: 2 người");
        lblSucChua.setFont(UIUtil.FONT_BOLD);
        lblSucChua.setForeground(new Color(40, 167, 69));

        JTextField txtTienCoc = new JTextField("0");

        // 2. Bảng nhập danh sách khách lưu trú
        String[] guestHeaders = {"STT", "Họ và tên (*)", "Số CCCD (12 số) (*)", "Số điện thoại", "Vai trò"};
        DefaultTableModel modelKhach = new DefaultTableModel(guestHeaders, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 0; // Cho phép sửa họ tên, CCCD, SĐT, Vai trò
            }
        };
        JTable tblKhach = new JTable(modelKhach);
        UIUtil.formatTable(tblKhach);

        // Nạp khách đại diện ban đầu
        Runnable napKhachDaiDien = () -> {
            KhachHang khChon = (KhachHang) cboKhach.getSelectedItem();
            if (khChon != null && modelKhach.getRowCount() == 0) {
                modelKhach.addRow(new Object[]{1, khChon.getHoTen(), khChon.getSoCCCD(), khChon.getSoDienThoai(), "Người đại diện"});
            }
        };

        cboKhach.addActionListener(e -> {
            KhachHang khChon = (KhachHang) cboKhach.getSelectedItem();
            if (khChon != null && modelKhach.getRowCount() > 0) {
                modelKhach.setValueAt(khChon.getHoTen(), 0, 1);
                modelKhach.setValueAt(khChon.getSoCCCD(), 0, 2);
                modelKhach.setValueAt(khChon.getSoDienThoai(), 0, 3);
            }
        });

        Runnable capNhatPhongTrong = () -> {
            try {
                LocalDate dNhan = LocalDate.parse(txtNgayNhan.getText().trim(), FormatUtil.DATE_FORMATTER);
                LocalDate dTra = LocalDate.parse(txtNgayTra.getText().trim(), FormatUtil.DATE_FORMATTER);

                if (!dTra.isAfter(dNhan)) {
                    cboPhong.removeAllItems();
                    lblSoDemVaTien.setText("⚠️ Ngày trả phòng phải sau ngày nhận ít nhất 1 đêm!");
                    lblSoDemVaTien.setForeground(new Color(220, 53, 69));
                    lblSucChua.setText("Chưa chọn phòng");
                    return;
                }

                LocalDateTime dtNhan = LocalDateTime.of(dNhan, LocalTime.of(14, 0));
                LocalDateTime dtTra = LocalDateTime.of(dTra, LocalTime.of(12, 0));

                List<Phong> phongTrong = datPhongController.timPhongTrong(dtNhan, dtTra);
                cboPhong.removeAllItems();
                for (Phong p : phongTrong) {
                    cboPhong.addItem(p);
                }

                Phong pChon = (Phong) cboPhong.getSelectedItem();
                if (pChon != null) {
                    long soDem = java.time.temporal.ChronoUnit.DAYS.between(dNhan, dTra);
                    BigDecimal tien = pChon.tinhTienPhong(soDem);
                    BigDecimal tienCocToiThieu = datPhongController.tinhTienCocToiThieu(pChon, dtNhan, dtTra);
                    String moTaNgay = HolidayUtil.getMoTaLoaiNgay(dNhan, dTra);
                    lblSoDemVaTien.setText(soDem + " đêm = " + FormatUtil.formatTien(tien) + " (" + moTaNgay + ")");
                    lblSoDemVaTien.setForeground(UIUtil.COLOR_PRIMARY);
                    lblSucChua.setText("🛏️ " + pChon.getTenLoaiPhongHienThi() + " | Tối đa: " + pChon.getSucChuaToiDa() + " người");
                    txtTienCoc.setText(FormatUtil.formatTien(tienCocToiThieu));
                } else {
                    lblSoDemVaTien.setText("Hết phòng trong khoảng này!");
                    lblSoDemVaTien.setForeground(new Color(220, 53, 69));
                    lblSucChua.setText("Hết phòng!");
                }
            } catch (Exception ex) {
                lblSoDemVaTien.setText("Định dạng ngày: dd/MM/yyyy");
                lblSoDemVaTien.setForeground(new Color(220, 53, 69));
            }
        };

        capNhatPhongTrong.run();
        napKhachDaiDien.run();

        txtNgayNhan.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                capNhatPhongTrong.run();
            }
        });
        txtNgayTra.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                capNhatPhongTrong.run();
            }
        });
        cboPhong.addActionListener(e -> {
            Phong pChon = (Phong) cboPhong.getSelectedItem();
            if (pChon != null) {
                try {
                    LocalDate dNhan = LocalDate.parse(txtNgayNhan.getText().trim(), FormatUtil.DATE_FORMATTER);
                    LocalDate dTra = LocalDate.parse(txtNgayTra.getText().trim(), FormatUtil.DATE_FORMATTER);
                    LocalDateTime dtNhan = LocalDateTime.of(dNhan, LocalTime.of(14, 0));
                    LocalDateTime dtTra = LocalDateTime.of(dTra, LocalTime.of(12, 0));
                    long soDem = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(dNhan, dTra));
                    BigDecimal tien = pChon.tinhTienPhong(soDem);
                    BigDecimal tienCocToiThieu = datPhongController.tinhTienCocToiThieu(pChon, dtNhan, dtTra);
                    String moTaNgay = HolidayUtil.getMoTaLoaiNgay(dNhan, dTra);
                    lblSoDemVaTien.setText(soDem + " đêm = " + FormatUtil.formatTien(tien) + " (" + moTaNgay + ")");
                    lblSucChua.setText("🛏️ " + pChon.getTenLoaiPhongHienThi() + " | Tối đa: " + pChon.getSucChuaToiDa() + " người");
                    txtTienCoc.setText(FormatUtil.formatTien(tienCocToiThieu));
                } catch (Exception ignored) {
                }
            }
        });

        pnlTopForm.add(new JLabel("Mã đặt phòng:"));
        pnlTopForm.add(txtMa);
        pnlTopForm.add(new JLabel("Khách hàng đại diện:"));
        pnlTopForm.add(pnlKhachHangRow);

        pnlTopForm.add(new JLabel("Ngày nhận (dd/MM/yyyy):"));
        pnlTopForm.add(txtNgayNhan);
        pnlTopForm.add(new JLabel("Ngày trả (dd/MM/yyyy):"));
        pnlTopForm.add(txtNgayTra);

        pnlTopForm.add(new JLabel("Phòng còn trống:"));
        pnlTopForm.add(cboPhong);
        pnlTopForm.add(new JLabel("Sức chứa tối đa:"));
        pnlTopForm.add(lblSucChua);

        pnlTopForm.add(new JLabel("Tiền đặt cọc (VNĐ):"));
        pnlTopForm.add(txtTienCoc);
        pnlTopForm.add(new JLabel("Ước tính tiền phòng:"));
        pnlTopForm.add(lblSoDemVaTien);

        // Lựa chọn phương thức thanh toán tiền cọc: Tiền mặt vs Chuyển khoản QR
        JRadioButton rdoTienMat = new JRadioButton("Tiền mặt", true);
        JRadioButton rdoChuyenKhoan = new JRadioButton("Chuyển khoản (Mã QR)");
        rdoTienMat.setBackground(Color.WHITE);
        rdoChuyenKhoan.setBackground(Color.WHITE);
        rdoTienMat.setFont(UIUtil.FONT_REGULAR);
        rdoChuyenKhoan.setFont(UIUtil.FONT_REGULAR);
        ButtonGroup bgPhuongThuc = new ButtonGroup();
        bgPhuongThuc.add(rdoTienMat);
        bgPhuongThuc.add(rdoChuyenKhoan);
        JPanel pnlPhuongThuc = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        pnlPhuongThuc.setBackground(Color.WHITE);
        pnlPhuongThuc.add(rdoTienMat);
        pnlPhuongThuc.add(rdoChuyenKhoan);

        pnlTopForm.add(new JLabel("Phương thức cọc:"));
        pnlTopForm.add(pnlPhuongThuc);

        // 3. Panel quản lý khách lưu trú
        JPanel pnlGuests = new JPanel(new BorderLayout(5, 5));
        pnlGuests.setBorder(BorderFactory.createTitledBorder("Danh sách khách lưu trú (Bắt buộc nhập CCCD 12 số của TẤT CẢ khách)"));
        pnlGuests.setBackground(Color.WHITE);

        JScrollPane scrollGuests = new JScrollPane(tblKhach);
        pnlGuests.add(scrollGuests, BorderLayout.CENTER);

        JPanel pnlGuestBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        pnlGuestBtns.setBackground(Color.WHITE);

        JButton btnThemKhach = UIUtil.createButton("+ Thêm khách đi cùng", new Color(0, 123, 255), Color.WHITE);
        btnThemKhach.addActionListener(e -> {
            Phong p = (Phong) cboPhong.getSelectedItem();
            int maxCap = p != null ? p.getSucChuaToiDa() : 4;
            if (modelKhach.getRowCount() >= maxCap) {
                UIUtil.showError(dialog, "Không thể thêm! Sức chứa tối đa của phòng này là " + maxCap + " người.");
                return;
            }
            int stt = modelKhach.getRowCount() + 1;
            modelKhach.addRow(new Object[]{stt, "", "", "", "Khách đi cùng"});
        });
        pnlGuestBtns.add(btnThemKhach);

        JButton btnXoaKhach = UIUtil.createButton("Xóa khách chọn", new Color(220, 53, 69), Color.WHITE);
        btnXoaKhach.addActionListener(e -> {
            int selected = tblKhach.getSelectedRow();
            if (selected < 0) {
                UIUtil.showError(dialog, "Vui lòng chọn dòng khách cần xóa.");
                return;
            }
            if (selected == 0 && modelKhach.getRowCount() > 1) {
                UIUtil.showError(dialog, "Không thể xóa người đại diện đặt phòng (dòng đầu tiên).");
                return;
            }
            modelKhach.removeRow(selected);
            // Cập nhật lại số thứ tự
            for (int i = 0; i < modelKhach.getRowCount(); i++) {
                modelKhach.setValueAt(i + 1, i, 0);
            }
        });
        pnlGuestBtns.add(btnXoaKhach);
        pnlGuests.add(pnlGuestBtns, BorderLayout.SOUTH);

        JPanel pnlCenter = new JPanel(new BorderLayout(8, 8));
        pnlCenter.setBackground(Color.WHITE);
        pnlCenter.add(pnlTopForm, BorderLayout.NORTH);
        pnlCenter.add(pnlGuests, BorderLayout.CENTER);
        dialog.add(pnlCenter, BorderLayout.CENTER);

        // 4. Panel nút xác nhận
        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnLuu = UIUtil.createButton("Xác nhận đặt phòng", UIUtil.COLOR_PRIMARY, Color.WHITE);
        JButton btnHuy = UIUtil.createButton("Đóng", new Color(108, 117, 125), Color.WHITE);

        btnLuu.addActionListener(e -> {
            try {
                // Kết thúc chỉnh sửa ô bảng nếu đang focus
                if (tblKhach.isEditing()) {
                    tblKhach.getCellEditor().stopCellEditing();
                }

                KhachHang kh = (KhachHang) cboKhach.getSelectedItem();
                Phong phong = (Phong) cboPhong.getSelectedItem();
                if (kh == null || phong == null) {
                    UIUtil.showError(dialog, "Vui lòng chọn khách hàng và phòng trống.");
                    return;
                }

                LocalDate dNhan = LocalDate.parse(txtNgayNhan.getText().trim(), FormatUtil.DATE_FORMATTER);
                LocalDate dTra = LocalDate.parse(txtNgayTra.getText().trim(), FormatUtil.DATE_FORMATTER);
                LocalDateTime dtNhan = LocalDateTime.of(dNhan, LocalTime.of(14, 0));
                LocalDateTime dtTra = LocalDateTime.of(dTra, LocalTime.of(12, 0));

                NhanVien nv = datPhongController.getNhanVienHienTai();
                BigDecimal tienCoc = FormatUtil.parseTien(txtTienCoc.getText());

                String maDatPhong = txtMa.getText().trim();
                DatPhong dp = new DatPhong(maDatPhong, kh, phong, nv, dtNhan, dtTra,
                        null, null, 1, TrangThaiDatPhong.DA_XAC_NHAN, tienCoc, LocalDateTime.now());

                // Đọc danh sách khách lưu trú từ bảng
                int rowCount = modelKhach.getRowCount();
                if (rowCount == 0) {
                    UIUtil.showError(dialog, "Bắt buộc phải có thông tin ít nhất 1 khách lưu trú kèm CCCD!");
                    return;
                }
                if (rowCount > phong.getSucChuaToiDa()) {
                    UIUtil.showError(dialog, "Số lượng khách lưu trú (" + rowCount + ") vượt quá sức chứa tối đa của " +
                            phong.getTenLoaiPhongHienThi() + " (" + phong.getSucChuaToiDa() + " người).");
                    return;
                }

                java.util.List<com.qlks.model.KhachLuuTru> dsKhachLuuTru = new java.util.ArrayList<>();
                for (int i = 0; i < rowCount; i++) {
                    String hoTen = String.valueOf(modelKhach.getValueAt(i, 1)).trim();
                    String cccd = String.valueOf(modelKhach.getValueAt(i, 2)).trim();
                    String sdt = modelKhach.getValueAt(i, 3) != null ? String.valueOf(modelKhach.getValueAt(i, 3)).trim() : "";
                    String strVaiTro = String.valueOf(modelKhach.getValueAt(i, 4)).trim();

                    if (hoTen.isEmpty()) {
                        UIUtil.showError(dialog, "Họ tên ở dòng " + (i + 1) + " không được để trống!");
                        return;
                    }
                    if (!com.qlks.model.KhachLuuTru.kiemTraCCCD(cccd)) {
                        UIUtil.showError(dialog, "Khách '" + hoTen + "' có số CCCD '" + cccd + "' không hợp lệ. Số CCCD phải gồm đúng 12 chữ số!");
                        return;
                    }

                    com.qlks.model.enums.VaiTroKhachLuuTru vt = strVaiTro.toLowerCase().contains("đại diện") ?
                            com.qlks.model.enums.VaiTroKhachLuuTru.NGUOI_DAI_DIEN : com.qlks.model.enums.VaiTroKhachLuuTru.KHACH_DI_CUNG;

                    dsKhachLuuTru.add(new com.qlks.model.KhachLuuTru(maDatPhong, hoTen, cccd, sdt, vt));
                }

                dp.setDanhSachKhach(dsKhachLuuTru);
                dp.setSoLuongKhach(dsKhachLuuTru.size());

                // Nếu chọn phương thức Chuyển khoản QR và có tiền cọc > 0: Hiển thị Popup QR
                if (rdoChuyenKhoan.isSelected() && tienCoc.compareTo(BigDecimal.ZERO) > 0) {
                    QRPaymentDialog qrDlg = new QRPaymentDialog(dialog, maDatPhong, kh.getHoTen(), tienCoc);
                    qrDlg.setVisible(true);
                    if (!qrDlg.isDaXacNhanThanhToan()) {
                        UIUtil.showInfo(dialog, "Chưa hoàn tất chuyển tiền cọc qua mã QR. Đơn chưa được tạo!");
                        return;
                    }
                }

                datPhongController.taoDatPhong(dp);
                UIUtil.showSuccess(dialog, "Tạo đơn đặt phòng thành công! Đã ghi nhận CCCD của " + dsKhachLuuTru.size() + " khách lưu trú.");
                dialog.dispose();
                loadDuLieu();
            } catch (Exception ex) {
                UIUtil.showError(dialog, ex.getMessage());
            }
        });

        btnHuy.addActionListener(e -> dialog.dispose());
        pnlBtns.add(btnHuy);
        pnlBtns.add(btnLuu);
        dialog.add(pnlBtns, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private void xuLyXemKhachLuuTru() {
        int row = tblDatPhong.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn phiếu đặt phòng để xem danh sách khách lưu trú.");
            return;
        }

        String maDp = (String) modelDatPhong.getValueAt(row, 0);
        DatPhong dp = datPhongController.timTheoMa(maDp).orElse(null);
        if (dp == null) return;

        List<com.qlks.model.KhachLuuTru> ds = dp.getDanhSachKhach();
        if (ds == null || ds.isEmpty()) {
            UIUtil.showInfo(this, "Đơn đặt phòng " + maDp + " chưa có thông tin khách lưu trú chi tiết.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("DANH SÁCH KHÁCH LƯU TRÚ - ĐƠN: ").append(maDp).append("\n");
        sb.append("Phòng: ").append(dp.getPhong() != null ? "P." + dp.getPhong().getSoPhong() : "").append("\n");
        sb.append("----------------------------------------------------------------------\n");
        int idx = 1;
        for (com.qlks.model.KhachLuuTru k : ds) {
            sb.append(idx++).append(". ").append(k.getHoTen())
                    .append(" | Vai trò: ").append(k.getVaiTro() != null ? k.getVaiTro().getTenHienThi() : "")
                    .append(" | CCCD (bảo mật): ").append(k.getSoCCCDChe())
                    .append(" | SĐT: ").append(k.getSoDienThoai() != null ? k.getSoDienThoai() : "")
                    .append("\n");
        }

        JTextArea txt = new JTextArea(sb.toString(), 10, 45);
        txt.setEditable(false);
        txt.setFont(new Font("Consolas", Font.PLAIN, 12));
        JOptionPane.showMessageDialog(this, new JScrollPane(txt), "Khách lưu trú - " + maDp, JOptionPane.INFORMATION_MESSAGE);
    }

    private void xuLyCheckInNhanh() {
        int row = tblDatPhong.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn phiếu đặt phòng cần check-in.");
            return;
        }

        String maDp = (String) modelDatPhong.getValueAt(row, 0);
        try {
            datPhongController.xacNhanCheckIn(maDp);
            UIUtil.showSuccess(this, "Xác nhận nhận phòng (Check-in) thành công!");
            loadDuLieu();
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }

    private void xuLyHuyDatPhong() {
        int row = tblDatPhong.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn phiếu đặt phòng cần hủy.");
            return;
        }

        String maDp = (String) modelDatPhong.getValueAt(row, 0);
        DatPhong dp = datPhongController.timTheoMa(maDp).orElse(null);
        if (dp == null) {
            UIUtil.showError(this, "Không tìm thấy thông tin đơn đặt phòng: " + maDp);
            return;
        }

        if (dp.getTrangThai() == TrangThaiDatPhong.DA_HUY) {
            UIUtil.showError(this, "Đơn đặt phòng này đã được hủy trước đó!");
            return;
        }
        if (dp.getTrangThai() == TrangThaiDatPhong.DANG_O || dp.getTrangThai() == TrangThaiDatPhong.DA_TRA_PHONG) {
            UIUtil.showError(this, "Không thể hủy khi khách đang lưu trú hoặc đã hoàn tất trả phòng!");
            return;
        }

        // Tính khoảng cách thời gian từ hiện tại đến giờ nhận phòng dự kiến
        Duration duration = Duration.between(LocalDateTime.now(), dp.getNgayNhanPhong());
        long soPhutConLai = duration.toMinutes();

        // Mốc 3: Dưới 2 tiếng hoặc đã quá giờ nhận phòng -> CẤM HỦY
        if (soPhutConLai < 120) {
            String msgLoi = String.format(
                    "❌ KHÔNG THỂ HỦY ĐẶT PHÒNG!\n\n" +
                    "Theo quy định khách sạn, chỉ được phép hủy trước giờ nhận phòng tối thiểu 2 tiếng.\n" +
                    "• Giờ nhận phòng dự kiến: %s\n" +
                    "• Hạn chót hủy phòng:      %s\n" +
                    "• Thời gian còn lại:       %d phút (Đã quá hạn hủy phòng!)\n\n" +
                    "Quý khách vui lòng liên hệ trực tiếp Quản lý khách sạn để được hỗ trợ!",
                    FormatUtil.formatNgayGio(dp.getNgayNhanPhong()),
                    FormatUtil.formatNgayGio(dp.getNgayNhanPhong().minusHours(2)),
                    Math.max(0, soPhutConLai)
            );
            UIUtil.showError(this, msgLoi);
            return;
        }

        BigDecimal tienCoc = dp.getTienCoc() != null ? dp.getTienCoc() : BigDecimal.ZERO;
        String lyDoHuy = "Khách yêu cầu hủy phòng";
        PhuongThucThanhToan phuongThucHoan = PhuongThucThanhToan.TIEN_MAT;

        if (tienCoc.compareTo(BigDecimal.ZERO) > 0) {
            long soGio = soPhutConLai / 60;
            String tyLeStr = soPhutConLai >= 1440 ? "HOÀN 100% TIỀN CỌC" : "HOÀN 50% TIỀN CỌC (PHẠT 50%)";
            BigDecimal tienHoan = soPhutConLai >= 1440 ? tienCoc : tienCoc.multiply(new BigDecimal("0.5")).setScale(0, java.math.RoundingMode.HALF_UP);
            BigDecimal tienPhat = tienCoc.subtract(tienHoan);

            String prompt = String.format(
                    "XÁC NHẬN HỦY PHÒNG & QUYẾT TOÁN CỌC\n\n" +
                    "• Mã đặt phòng:            %s - Phòng: P.%s\n" +
                    "• Khách hàng:              %s\n" +
                    "• Giờ nhận phòng:          %s (Còn %d giờ)\n" +
                    "• Tiền cọc đã nhận:        %s VNĐ\n" +
                    "• Chính sách áp dụng:      %s\n" +
                    "--------------------------------------------------\n" +
                    "• TIỀN HOÀN TRẢ KHÁCH:     %s VNĐ\n" +
                    "• PHÍ PHẠT THU DOANH THU:  %s VNĐ\n\n" +
                    "Vui lòng chọn hình thức hoàn cọc cho khách:",
                    maDp,
                    dp.getPhong() != null ? dp.getPhong().getSoPhong() : "",
                    dp.getKhachHang() != null ? dp.getKhachHang().getHoTen() : "",
                    FormatUtil.formatNgayGio(dp.getNgayNhanPhong()),
                    soGio,
                    FormatUtil.formatTien(tienCoc),
                    tyLeStr,
                    FormatUtil.formatTien(tienHoan),
                    FormatUtil.formatTien(tienPhat)
            );

            Object[] options = {"Hoàn tiền mặt", "Hoàn chuyển khoản", "Đóng (Không hủy)"};
            int opt = JOptionPane.showOptionDialog(this, prompt, "Quyết toán hủy phòng - " + maDp,
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);

            if (opt == 0) {
                phuongThucHoan = PhuongThucThanhToan.TIEN_MAT;
            } else if (opt == 1) {
                phuongThucHoan = PhuongThucThanhToan.CHUYEN_KHOAN;
            } else {
                return;
            }
        } else {
            if (!UIUtil.showConfirm(this, "Xác nhận hủy đặt phòng " + maDp + " (Đơn không có tiền cọc)?")) {
                return;
            }
        }

        datPhongController.huyDatPhongAsync(this, maDp, lyDoHuy, phuongThucHoan, res -> loadDuLieu());
    }
}
