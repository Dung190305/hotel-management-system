package com.qlks.view.panels;

import com.qlks.controller.HoaDonController;
import com.qlks.model.ChiTietDichVu;
import com.qlks.model.HoaDon;
import com.qlks.model.enums.PhuongThucThanhToan;
import com.qlks.model.enums.TrangThaiThanhToan;
import com.qlks.util.FormatUtil;
import com.qlks.util.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

import com.qlks.util.AppEventBus;
import com.qlks.view.RefreshablePanel;

/**
 * Màn hình Quản lý Hóa đơn, Thêm dịch vụ và Thanh toán (Chống thanh toán trùng lặp).
 */
public class HoaDonPanel extends JPanel implements RefreshablePanel, AppEventBus.AppEventListener {

    private final HoaDonController hoaDonController;

    private JTable tblHoaDon;
    private DefaultTableModel modelHoaDon;
    private JComboBox<String> cboLocTrangThai;

    public HoaDonPanel() {
        this.hoaDonController = new HoaDonController();

        initComponents();
        AppEventBus.register(this);
        loadDuLieu();
    }

    @Override
    public void onEvent(AppEventBus.EventType eventType) {
        if (eventType == AppEventBus.EventType.INVOICE_CHANGED || eventType == AppEventBus.EventType.BOOKING_CHANGED) {
            loadDuLieu();
        }
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

        JLabel lblTitle = new JLabel("QUẢN LÝ HÓA ĐƠN & THANH TOÁN");
        lblTitle.setFont(UIUtil.FONT_TITLE);
        lblTitle.setForeground(UIUtil.COLOR_PRIMARY);
        pnlTop.add(lblTitle, BorderLayout.WEST);

        JPanel pnlFilter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlFilter.setBackground(Color.WHITE);

        pnlFilter.add(new JLabel("Trạng thái:"));
        cboLocTrangThai = new JComboBox<>(new String[]{"-- Tất cả --", "Chưa thanh toán", "Đã thanh toán"});
        cboLocTrangThai.addActionListener(e -> locDuLieu());
        pnlFilter.add(cboLocTrangThai);

        JButton btnLamMoi = UIUtil.createButton("Làm mới", new Color(108, 117, 125), Color.WHITE);
        btnLamMoi.addActionListener(e -> loadDuLieu());
        pnlFilter.add(btnLamMoi);

        pnlTop.add(pnlFilter, BorderLayout.EAST);
        add(pnlTop, BorderLayout.NORTH);

        // 2. BẢNG HÓA ĐƠN
        String[] headers = {"Mã HĐ", "Mã Đặt phòng", "Khách hàng", "Phòng", "Ngày lập", "Tiền phòng", "Tiền dịch vụ", "Giảm giá", "Tiền cọc", "Tổng chi phí", "Còn lại phải thu", "Trạng thái"};
        modelHoaDon = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblHoaDon = new JTable(modelHoaDon);
        UIUtil.formatTable(tblHoaDon);

        // Căn lề tiền căn phải cho các cột số tiền (5 -> 10)
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int col = 5; col <= 10; col++) {
            tblHoaDon.getColumnModel().getColumn(col).setCellRenderer(rightRenderer);
        }

        add(new JScrollPane(tblHoaDon), BorderLayout.CENTER);

        // 3. CÁC NÚT THAO TÁC DƯỚI BẢNG
        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        pnlBottom.setBackground(Color.WHITE);

        JButton btnXemChiTiet = UIUtil.createButton("Xem chi tiết hóa đơn", new Color(108, 117, 125), Color.WHITE);
        btnXemChiTiet.addActionListener(e -> xuLyXemChiTiet());
        pnlBottom.add(btnXemChiTiet);

        JButton btnInBienLai = UIUtil.createButton("📄 In / Xuất biên lai", new Color(23, 162, 184), Color.WHITE);
        btnInBienLai.addActionListener(e -> xuLyInBienLai());
        pnlBottom.add(btnInBienLai);

        JButton btnThanhToan = UIUtil.createButton("THANH TOÁN NGAY", new Color(40, 167, 69), Color.WHITE);
        btnThanhToan.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnThanhToan.addActionListener(e -> xuLyThanhToan());
        pnlBottom.add(btnThanhToan);

        add(pnlBottom, BorderLayout.SOUTH);
    }

    public void loadDuLieu() {
        List<HoaDon> list = hoaDonController.layTatCaHoaDon();
        hienThiLenBang(list);
    }

    private void locDuLieu() {
        int idx = cboLocTrangThai.getSelectedIndex();
        List<HoaDon> list = hoaDonController.layTatCaHoaDon();
        if (idx == 1) list = list.stream().filter(h -> h.getTrangThai() == TrangThaiThanhToan.CHUA_THANH_TOAN).toList();
        if (idx == 2) list = list.stream().filter(h -> h.getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN).toList();
        hienThiLenBang(list);
    }

    private void hienThiLenBang(List<HoaDon> list) {
        modelHoaDon.setRowCount(0);
        for (HoaDon hd : list) {
            String khach = hd.getDatPhong() != null && hd.getDatPhong().getKhachHang() != null ?
                    hd.getDatPhong().getKhachHang().getHoTen() : "";
            String phong = hd.getDatPhong() != null && hd.getDatPhong().getPhong() != null ?
                    "P." + hd.getDatPhong().getPhong().getSoPhong() : "";

            modelHoaDon.addRow(new Object[]{
                    hd.getMaHoaDon(),
                    hd.getDatPhong() != null ? hd.getDatPhong().getMaDatPhong() : "",
                    khach,
                    phong,
                    FormatUtil.formatNgayGio(hd.getNgayLap()),
                    FormatUtil.formatTien(hd.getTienPhong()),
                    FormatUtil.formatTien(hd.getTienDichVu()),
                    FormatUtil.formatTien(hd.getTienGiamGia()),
                    FormatUtil.formatTien(hd.getTienCoc()),
                    FormatUtil.formatTien(hd.getTongChiPhi()),
                    FormatUtil.formatTien(hd.getConLaiPhaiThu()),
                    hd.getTrangThai().getTenHienThi()
            });
        }
    }

    private void xuLyXemChiTiet() {
        int row = tblHoaDon.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn một hóa đơn để xem chi tiết.");
            return;
        }

        String maHd = (String) modelHoaDon.getValueAt(row, 0);
        HoaDon hd = hoaDonController.timTheoMa(maHd).orElse(null);
        if (hd == null) return;

        String noiDungBienLai = taoVanBanBienLai(hd);

        JTextArea txtDetail = new JTextArea(noiDungBienLai);
        txtDetail.setFont(new Font("Consolas", Font.PLAIN, 12));
        txtDetail.setEditable(false);

        JScrollPane scroll = new JScrollPane(txtDetail);
        scroll.setPreferredSize(new Dimension(550, 480));

        JPanel pnlDlg = new JPanel(new BorderLayout(5, 5));
        pnlDlg.add(scroll, BorderLayout.CENTER);

        JButton btnXuatFile = UIUtil.createButton("💾 Xuất biên lai ra file (.txt)", new Color(23, 162, 184), Color.WHITE);
        btnXuatFile.addActionListener(e -> xuLyInBienLai());

        JPanel pnlBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlBtns.add(btnXuatFile);
        pnlDlg.add(pnlBtns, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(this, pnlDlg, "Chi tiết hóa đơn & Biên lai " + hd.getMaHoaDon(), JOptionPane.PLAIN_MESSAGE);
    }

    private void xuLyInBienLai() {
        int row = tblHoaDon.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn một hóa đơn để in / xuất biên lai.");
            return;
        }

        String maHd = (String) modelHoaDon.getValueAt(row, 0);
        HoaDon hd = hoaDonController.timTheoMa(maHd).orElse(null);
        if (hd == null) return;

        String vanBan = taoVanBanBienLai(hd);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Lưu biên lai thanh toán");
        fileChooser.setSelectedFile(new java.io.File("BienLai_" + hd.getMaHoaDon() + ".txt"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            java.io.File fileToSave = fileChooser.getSelectedFile();
            try {
                java.nio.file.Files.writeString(fileToSave.toPath(), vanBan, java.nio.charset.StandardCharsets.UTF_8);
                UIUtil.showSuccess(this, "Đã xuất biên lai thanh toán thành công tại:\n" + fileToSave.getAbsolutePath());
            } catch (Exception ex) {
                UIUtil.showError(this, "Lỗi khi lưu file biên lai: " + ex.getMessage());
            }
        }
    }

    private String taoVanBanBienLai(HoaDon hd) {
        StringBuilder sb = new StringBuilder();
        sb.append("=================================================================\n");
        sb.append("                   KHÁCH SẠN GRAND LUXURY HOTEL                   \n");
        sb.append("            Địa chỉ: 123 Đường Biển, TP. Nha Trang                \n");
        sb.append("            Hotline: 1900 6868 - Email: contact@grandluxury.com   \n");
        sb.append("=================================================================\n");
        sb.append("                 PHIẾU THANH TOÁN (FOLIO RECEIPT)                 \n");
        sb.append("-----------------------------------------------------------------\n");
        sb.append("Mã hóa đơn:       ").append(hd.getMaHoaDon()).append("\n");
        sb.append("Thời gian in:     ").append(FormatUtil.formatNgayGio(java.time.LocalDateTime.now())).append("\n");
        sb.append("Thu ngân:         ").append(hd.getNhanVienLap() != null ? hd.getNhanVienLap().getHoTen() : "Lễ tân quầy").append("\n");
        if (hd.getDatPhong() != null) {
            sb.append("Khách hàng:       ").append(hd.getDatPhong().getKhachHang().getHoTen()).append("\n");
            sb.append("Số điện thoại:    ").append(hd.getDatPhong().getKhachHang().getSoDienThoai()).append("\n");
            sb.append("CCCD:             ").append(hd.getDatPhong().getKhachHang().getSoCCCD()).append("\n");
            sb.append("Phòng:            P.").append(hd.getDatPhong().getPhong().getSoPhong())
                    .append(" (").append(hd.getDatPhong().getPhong().getTenLoaiPhongHienThi()).append(")\n");
            sb.append("Thời gian ở:      ").append(FormatUtil.formatNgayGio(hd.getDatPhong().getNgayNhanPhong()))
                    .append(" -> ").append(FormatUtil.formatNgayGio(hd.getDatPhong().getNgayTraPhong())).append("\n");
        }
        sb.append("-----------------------------------------------------------------\n");
        sb.append(String.format("%-35s %12s %15s\n", "Khoản mục", "Số lượng", "Thành tiền"));
        sb.append("-----------------------------------------------------------------\n");
        sb.append(String.format("%-35s %12s %15s\n", "1. Tiền phòng lưu trú", "1 phòng", FormatUtil.formatTien(hd.getTienPhong())));
        if (hd.getDanhSachDichVu() != null && !hd.getDanhSachDichVu().isEmpty()) {
            sb.append("2. Dịch vụ phát sinh:\n");
            for (ChiTietDichVu ct : hd.getDanhSachDichVu()) {
                String tenDv = ct.getDichVu() != null ? ct.getDichVu().getTenDichVu() : ct.getMaDichVu();
                sb.append(String.format("   + %-32s x%-10d %15s\n", tenDv, ct.getSoLuong(), FormatUtil.formatTien(ct.tinhThanhTien())));
            }
        } else {
            sb.append("2. Dịch vụ phát sinh:             (Không sử dụng)\n");
        }
        sb.append("-----------------------------------------------------------------\n");
        sb.append(String.format("%-48s %15s\n", "TỔNG CHI PHÍ THỰC TẾ:", FormatUtil.formatTien(hd.getTongChiPhi())));
        if (hd.getTienGiamGia() != null && hd.getTienGiamGia().compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("%-48s -%15s\n", "Chiết khấu / Giảm giá:", FormatUtil.formatTien(hd.getTienGiamGia())));
        }
        sb.append(String.format("%-48s -%15s\n", "Tiền cọc đã nhận trước:", FormatUtil.formatTien(hd.getTienCoc())));
        sb.append("=================================================================\n");
        sb.append(String.format("%-48s %15s\n", "SỐ TIỀN THỰC THU:", FormatUtil.formatTien(hd.getConLaiPhaiThu())));
        sb.append("Trạng thái:       ").append(hd.getTrangThai().getTenHienThi().toUpperCase()).append("\n");

        List<com.qlks.model.ThanhToan> dsThanhToan = hoaDonController.layLichSuThanhToan(hd.getMaHoaDon());
        if (dsThanhToan != null && !dsThanhToan.isEmpty()) {
            sb.append("-----------------------------------------------------------------\n");
            sb.append("LỊCH SỬ CÁC ĐỢT THANH TOÁN:\n");
            int dot = 1;
            for (com.qlks.model.ThanhToan tt : dsThanhToan) {
                sb.append(String.format("   + Đợt %d: %-15s [%s] %-20s (%s)\n",
                        dot++,
                        FormatUtil.formatTien(tt.getSoTien()),
                        tt.getPhuongThuc().getTenHienThi(),
                        FormatUtil.formatNgayGio(tt.getThoiGianGiaoDich()),
                        tt.getGhiChu() != null ? tt.getGhiChu() : ""));
            }
        }

        sb.append("=================================================================\n\n");
        sb.append("      Khách hàng ký nhận                          Thu ngân lập biểu     \n");
        sb.append("   (Ký và ghi rõ họ tên)                        (Ký và ghi rõ họ tên)   \n\n\n\n");
        sb.append("=================================================================\n");
        sb.append("             Cảm ơn Quý khách & Hẹn gặp lại!                     \n");
        return sb.toString();
    }

    private void xuLyThanhToan() {
        int row = tblHoaDon.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn một hóa đơn cần thanh toán.");
            return;
        }

        String maHd = (String) modelHoaDon.getValueAt(row, 0);
        HoaDon hd = hoaDonController.timTheoMa(maHd).orElse(null);
        if (hd == null) return;

        // Kiểm tra chống thanh toán trùng lặp ngay từ UI
        if (hd.getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN) {
            UIUtil.showError(this, "Hóa đơn này đã được thanh toán đầy đủ trước đó! Hệ thống ngăn chặn thanh toán trùng.");
            return;
        }

        BigDecimal conLaiCanThu = hoaDonController.tinhSoTienConLaiCanThu(maHd);
        BigDecimal daThanhToan = hoaDonController.tinhTongTienDaThanhToan(maHd);

        JTextField txtSoTien = new JTextField(conLaiCanThu.toPlainString());
        JComboBox<PhuongThucThanhToan> cboPhuongThuc = new JComboBox<>(PhuongThucThanhToan.values());
        
        String ghiChuMacDinh = "Thanh toán tiền phòng và dịch vụ";
        if (conLaiCanThu.compareTo(BigDecimal.ZERO) == 0) {
            ghiChuMacDinh = "Tất toán bằng tiền đặt cọc";
            if (hd.getTienCoc() != null && hd.getTienCoc().compareTo(hd.getTongChiPhi()) > 0) {
                BigDecimal tienHoan = hd.getTienCoc().subtract(hd.getTongChiPhi());
                ghiChuMacDinh = "Tất toán cọc (Hoàn lại " + FormatUtil.formatTien(tienHoan) + ")";
            }
        }
        JTextField txtGhiChu = new JTextField(ghiChuMacDinh);

        JPanel panel = new JPanel(new GridLayout(6, 2, 8, 8));
        panel.add(new JLabel("Mã hóa đơn:"));
        panel.add(new JLabel(hd.getMaHoaDon()));
        panel.add(new JLabel("Tổng chi phí thực tế:"));
        panel.add(new JLabel(FormatUtil.formatTien(hd.getTongChiPhi())));
        panel.add(new JLabel("Đã thanh toán trước đó:"));
        panel.add(new JLabel(FormatUtil.formatTien(daThanhToan)));
        panel.add(new JLabel("Số tiền còn lại cần thu:"));
        panel.add(new JLabel(FormatUtil.formatTien(conLaiCanThu)));
        panel.add(new JLabel("Số tiền khách trả đợt này:"));
        panel.add(txtSoTien);
        panel.add(new JLabel("Phương thức thanh toán:"));
        panel.add(cboPhuongThuc);

        int result = JOptionPane.showConfirmDialog(this, panel, "Xác nhận thanh toán hóa đơn (Hỗ trợ nhiều đợt)", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                BigDecimal soTien = FormatUtil.parseTien(txtSoTien.getText());
                PhuongThucThanhToan pt = (PhuongThucThanhToan) cboPhuongThuc.getSelectedItem();
                String ghiChu = txtGhiChu.getText().trim();

                hoaDonController.thanhToanHoaDon(maHd, soTien, pt, ghiChu);
                BigDecimal conLaiSau = conLaiCanThu.subtract(soTien);
                if (conLaiSau.compareTo(BigDecimal.ZERO) <= 0) {
                    UIUtil.showSuccess(this, "Thanh toán thành công hóa đơn " + maHd + "!\nĐã thu đủ 100% - Trạng thái: ĐÃ THANH TOÁN.");
                } else {
                    UIUtil.showSuccess(this, String.format(
                            "Đã ghi nhận thanh toán đợt này: %s (%s).\nSố tiền còn lại cần thanh toán tiếp: %s.",
                            FormatUtil.formatTien(soTien), pt.getTenHienThi(), FormatUtil.formatTien(conLaiSau)));
                }
                loadDuLieu();
            } catch (Exception ex) {
                UIUtil.showError(this, ex.getMessage());
            }
        }
    }
}
