package com.qlks.view.panels;

import com.qlks.model.DatPhong;
import com.qlks.model.KhachHang;
import com.qlks.model.NhanVien;
import com.qlks.model.Phong;
import com.qlks.model.enums.TrangThaiDatPhong;
import com.qlks.service.AuthService;
import com.qlks.service.DatPhongService;
import com.qlks.service.KhachHangService;
import com.qlks.service.PhongService;
import com.qlks.util.FormatUtil;
import com.qlks.util.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Màn hình Đặt phòng (Booking Management) & Chống trùng lịch đặt phòng.
 */
public class DatPhongPanel extends JPanel {

    private final DatPhongService datPhongService;
    private final KhachHangService khachHangService;
    private final PhongService phongService;

    private JTable tblDatPhong;
    private DefaultTableModel modelDatPhong;
    private JComboBox<String> cboLocTrangThai;

    public DatPhongPanel() {
        this.datPhongService = new DatPhongService();
        this.khachHangService = new KhachHangService();
        this.phongService = new PhongService();

        initComponents();
        loadDuLieu();
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

        JButton btnCheckInNhanh = UIUtil.createButton("Nhận phòng (Check-in)", new Color(40, 167, 69), Color.WHITE);
        btnCheckInNhanh.addActionListener(e -> xuLyCheckInNhanh());
        pnlBottom.add(btnCheckInNhanh);

        JButton btnHuyDat = UIUtil.createButton("Hủy đặt phòng", new Color(220, 53, 69), Color.WHITE);
        btnHuyDat.addActionListener(e -> xuLyHuyDatPhong());
        pnlBottom.add(btnHuyDat);

        add(pnlBottom, BorderLayout.SOUTH);
    }

    public void loadDuLieu() {
        List<DatPhong> list = datPhongService.layTatCaDatPhong();
        hienThiLenBang(list);
    }

    private void locDuLieu() {
        int idx = cboLocTrangThai.getSelectedIndex();
        List<DatPhong> list = datPhongService.layTatCaDatPhong();

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
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Tạo phiếu đặt phòng mới", true);
        dialog.setSize(520, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridLayout(8, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        form.setBackground(Color.WHITE);

        JTextField txtMa = new JTextField("DP" + System.currentTimeMillis() % 100000);

        // Khách hàng ComboBox
        List<KhachHang> dsKhach = khachHangService.layTatCaKhachHang();
        JComboBox<KhachHang> cboKhach = new JComboBox<>(dsKhach.toArray(new KhachHang[0]));

        // Ngày nhận và Ngày trả
        JTextField txtNgayNhan = new JTextField(FormatUtil.formatNgay(LocalDate.now()));
        JTextField txtNgayTra = new JTextField(FormatUtil.formatNgay(LocalDate.now().plusDays(1)));

        // Phòng trống
        JComboBox<Phong> cboPhong = new JComboBox<>();
        JLabel lblSoDemVaTien = new JLabel("Tạm tính: 0 VNĐ");
        lblSoDemVaTien.setFont(UIUtil.FONT_BOLD);
        lblSoDemVaTien.setForeground(UIUtil.COLOR_PRIMARY);

        Runnable capNhatPhongTrong = () -> {
            try {
                LocalDate dNhan = LocalDate.parse(txtNgayNhan.getText().trim(), FormatUtil.DATE_FORMATTER);
                LocalDate dTra = LocalDate.parse(txtNgayTra.getText().trim(), FormatUtil.DATE_FORMATTER);
                LocalDateTime dtNhan = LocalDateTime.of(dNhan, LocalTime.of(14, 0));
                LocalDateTime dtTra = LocalDateTime.of(dTra, LocalTime.of(12, 0));

                List<Phong> phongTrong = phongService.timPhongTrong(dtNhan, dtTra);
                cboPhong.removeAllItems();
                for (Phong p : phongTrong) {
                    cboPhong.addItem(p);
                }

                Phong pChon = (Phong) cboPhong.getSelectedItem();
                if (pChon != null) {
                    long soDem = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(dNhan, dTra));
                    BigDecimal tien = pChon.tinhTienPhong(soDem);
                    lblSoDemVaTien.setText(soDem + " đêm = " + FormatUtil.formatTien(tien));
                } else {
                    lblSoDemVaTien.setText("Hết phòng trong khoảng này!");
                }
            } catch (Exception ignored) {
            }
        };

        capNhatPhongTrong.run();

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
            try {
                Phong pChon = (Phong) cboPhong.getSelectedItem();
                if (pChon != null) {
                    LocalDate dNhan = LocalDate.parse(txtNgayNhan.getText().trim(), FormatUtil.DATE_FORMATTER);
                    LocalDate dTra = LocalDate.parse(txtNgayTra.getText().trim(), FormatUtil.DATE_FORMATTER);
                    long soDem = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(dNhan, dTra));
                    BigDecimal tien = pChon.tinhTienPhong(soDem);
                    lblSoDemVaTien.setText(soDem + " đêm = " + FormatUtil.formatTien(tien));
                }
            } catch (Exception ignored) {
            }
        });

        JSpinner spnKhach = new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));
        JTextField txtTienCoc = new JTextField("0");

        form.add(new JLabel("Mã đặt phòng:"));
        form.add(txtMa);
        form.add(new JLabel("Khách hàng:"));
        form.add(cboKhach);
        form.add(new JLabel("Ngày nhận (dd/MM/yyyy):"));
        form.add(txtNgayNhan);
        form.add(new JLabel("Ngày trả (dd/MM/yyyy):"));
        form.add(txtNgayTra);
        form.add(new JLabel("Phòng còn trống:"));
        form.add(cboPhong);
        form.add(new JLabel("Số lượng khách:"));
        form.add(spnKhach);
        form.add(new JLabel("Tiền đặt cọc (VNĐ):"));
        form.add(txtTienCoc);
        form.add(new JLabel("Ước tính chi phí:"));
        form.add(lblSoDemVaTien);

        dialog.add(form, BorderLayout.CENTER);

        // Buttons
        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton btnLuu = UIUtil.createButton("Xác nhận đặt phòng", UIUtil.COLOR_PRIMARY, Color.WHITE);
        JButton btnHuy = UIUtil.createButton("Đóng", new Color(108, 117, 125), Color.WHITE);

        btnLuu.addActionListener(e -> {
            try {
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

                NhanVien nv = AuthService.getNhanVienHienTai();
                int soKhach = (Integer) spnKhach.getValue();
                BigDecimal tienCoc = FormatUtil.parseTien(txtTienCoc.getText());

                DatPhong dp = new DatPhong(txtMa.getText().trim(), kh, phong, nv, dtNhan, dtTra,
                        null, null, soKhach, TrangThaiDatPhong.DA_XAC_NHAN, tienCoc, LocalDateTime.now());

                datPhongService.taoDatPhong(dp);
                UIUtil.showSuccess(dialog, "Tạo đơn đặt phòng thành công!");
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

    private void xuLyCheckInNhanh() {
        int row = tblDatPhong.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn phiếu đặt phòng cần check-in.");
            return;
        }

        String maDp = (String) modelDatPhong.getValueAt(row, 0);
        try {
            datPhongService.xacNhanCheckIn(maDp);
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
        if (UIUtil.showConfirm(this, "Bạn có chắc chắn muốn hủy đặt phòng " + maDp + " không?")) {
            try {
                datPhongService.huyDatPhong(maDp);
                UIUtil.showSuccess(this, "Đã hủy phiếu đặt phòng thành công.");
                loadDuLieu();
            } catch (Exception ex) {
                UIUtil.showError(this, ex.getMessage());
            }
        }
    }
}
