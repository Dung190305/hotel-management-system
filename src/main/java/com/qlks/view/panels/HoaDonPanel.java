package com.qlks.view.panels;

import com.qlks.model.ChiTietDichVu;
import com.qlks.model.DichVu;
import com.qlks.model.HoaDon;
import com.qlks.model.enums.PhuongThucThanhToan;
import com.qlks.model.enums.TrangThaiThanhToan;
import com.qlks.service.DichVuService;
import com.qlks.service.HoaDonService;
import com.qlks.util.FormatUtil;
import com.qlks.util.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Màn hình Quản lý Hóa đơn, Thêm dịch vụ và Thanh toán (Chống thanh toán trùng lặp).
 */
public class HoaDonPanel extends JPanel {

    private final HoaDonService hoaDonService;
    private final DichVuService dichVuService;

    private JTable tblHoaDon;
    private DefaultTableModel modelHoaDon;
    private JComboBox<String> cboLocTrangThai;

    public HoaDonPanel() {
        this.hoaDonService = new HoaDonService();
        this.dichVuService = new DichVuService();

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
        String[] headers = {"Mã HĐ", "Mã Đặt phòng", "Khách hàng", "Phòng", "Ngày lập", "Tiền phòng", "Tiền dịch vụ", "Giảm trừ (Cọc)", "Tổng thanh toán", "Trạng thái"};
        modelHoaDon = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblHoaDon = new JTable(modelHoaDon);
        UIUtil.formatTable(tblHoaDon);

        // Căn lề tiền căn phải
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tblHoaDon.getColumnModel().getColumn(5).setCellRenderer(rightRenderer);
        tblHoaDon.getColumnModel().getColumn(6).setCellRenderer(rightRenderer);
        tblHoaDon.getColumnModel().getColumn(7).setCellRenderer(rightRenderer);
        tblHoaDon.getColumnModel().getColumn(8).setCellRenderer(rightRenderer);

        add(new JScrollPane(tblHoaDon), BorderLayout.CENTER);

        // 3. CÁC NÚT THAO TÁC DƯỚI BẢNG
        JPanel pnlBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        pnlBottom.setBackground(Color.WHITE);

        JButton btnThemDichVu = UIUtil.createButton("+ Thêm dịch vụ vào phòng", new Color(0, 123, 255), Color.WHITE);
        btnThemDichVu.addActionListener(e -> xuLyThemDichVu());
        pnlBottom.add(btnThemDichVu);

        JButton btnXemChiTiet = UIUtil.createButton("Xem chi tiết hóa đơn", new Color(108, 117, 125), Color.WHITE);
        btnXemChiTiet.addActionListener(e -> xuLyXemChiTiet());
        pnlBottom.add(btnXemChiTiet);

        JButton btnThanhToan = UIUtil.createButton("THANH TOÁN NGAY", new Color(40, 167, 69), Color.WHITE);
        btnThanhToan.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnThanhToan.addActionListener(e -> xuLyThanhToan());
        pnlBottom.add(btnThanhToan);

        add(pnlBottom, BorderLayout.SOUTH);
    }

    public void loadDuLieu() {
        List<HoaDon> list = hoaDonService.layTatCaHoaDon();
        hienThiLenBang(list);
    }

    private void locDuLieu() {
        int idx = cboLocTrangThai.getSelectedIndex();
        List<HoaDon> list = hoaDonService.layTatCaHoaDon();
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
                    FormatUtil.formatTien(hd.getTongTien()),
                    hd.getTrangThai().getTenHienThi()
            });
        }
    }

    private void xuLyThemDichVu() {
        int row = tblHoaDon.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn một hóa đơn để thêm dịch vụ.");
            return;
        }

        String maHd = (String) modelHoaDon.getValueAt(row, 0);
        HoaDon hd = hoaDonService.timTheoMa(maHd).orElse(null);
        if (hd == null) return;

        if (hd.getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN) {
            UIUtil.showError(this, "Hóa đơn này đã được thanh toán hoàn tất, không thể thêm dịch vụ mới!");
            return;
        }

        List<DichVu> dsDichVu = dichVuService.layDichVuDangKinhDoanh();
        if (dsDichVu.isEmpty()) {
            UIUtil.showError(this, "Không có dịch vụ nào đang kinh doanh.");
            return;
        }

        JComboBox<DichVu> cboDichVu = new JComboBox<>(dsDichVu.toArray(new DichVu[0]));
        JSpinner spnSoLuong = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));

        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.add(new JLabel("Chọn dịch vụ:"));
        panel.add(cboDichVu);
        panel.add(new JLabel("Số lượng:"));
        panel.add(spnSoLuong);

        int result = JOptionPane.showConfirmDialog(this, panel, "Gọi dịch vụ cho phòng", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                DichVu dv = (DichVu) cboDichVu.getSelectedItem();
                int soLuong = (Integer) spnSoLuong.getValue();
                if (dv != null) {
                    hoaDonService.themDichVuVaoHoaDon(maHd, dv.getMaDichVu(), soLuong);
                    UIUtil.showSuccess(this, "Đã thêm " + soLuong + "x " + dv.getTenDichVu() + " vào hóa đơn!");
                    loadDuLieu();
                }
            } catch (Exception ex) {
                UIUtil.showError(this, ex.getMessage());
            }
        }
    }

    private void xuLyXemChiTiet() {
        int row = tblHoaDon.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn một hóa đơn để xem chi tiết.");
            return;
        }

        String maHd = (String) modelHoaDon.getValueAt(row, 0);
        HoaDon hd = hoaDonService.timTheoMa(maHd).orElse(null);
        if (hd == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("═══════════════════════════════════════════\n");
        sb.append("          HÓA ĐƠN THANH TOÁN KHÁCH SẠN       \n");
        sb.append("═══════════════════════════════════════════\n");
        sb.append("Mã hóa đơn: ").append(hd.getMaHoaDon()).append("\n");
        sb.append("Ngày lập: ").append(FormatUtil.formatNgayGio(hd.getNgayLap())).append("\n");
        if (hd.getDatPhong() != null) {
            sb.append("Khách hàng: ").append(hd.getDatPhong().getKhachHang().getHoTen()).append("\n");
            sb.append("Số điện thoại: ").append(hd.getDatPhong().getKhachHang().getSoDienThoai()).append("\n");
            sb.append("Phòng: ").append(hd.getDatPhong().getPhong().getSoPhong())
                    .append(" (").append(hd.getDatPhong().getPhong().getTenLoaiPhongHienThi()).append(")\n");
            sb.append("Thời gian: ").append(FormatUtil.formatNgayGio(hd.getDatPhong().getNgayNhanPhong()))
                    .append(" -> ").append(FormatUtil.formatNgayGio(hd.getDatPhong().getNgayTraPhong())).append("\n");
        }
        sb.append("-------------------------------------------\n");
        sb.append("Tiền phòng: ").append(FormatUtil.formatTien(hd.getTienPhong())).append("\n");
        sb.append("DANH SÁCH DỊCH VỤ SỬ DỤNG:\n");
        if (hd.getDanhSachDichVu() != null && !hd.getDanhSachDichVu().isEmpty()) {
            for (ChiTietDichVu ct : hd.getDanhSachDichVu()) {
                sb.append("  + ").append(ct.toString()).append("\n");
            }
        } else {
            sb.append("  (Không sử dụng dịch vụ phát sinh)\n");
        }
        sb.append("Tiền dịch vụ: ").append(FormatUtil.formatTien(hd.getTienDichVu())).append("\n");
        sb.append("Tiền đã cọc trước: -").append(FormatUtil.formatTien(hd.getTienGiamGia())).append("\n");
        sb.append("-------------------------------------------\n");
        sb.append("TỔNG TIỀN PHẢI THANH TOÁN: ").append(FormatUtil.formatTien(hd.getTongTien())).append("\n");
        sb.append("Trạng thái: ").append(hd.getTrangThai().getTenHienThi()).append("\n");
        sb.append("═══════════════════════════════════════════\n");

        JTextArea txtDetail = new JTextArea(sb.toString());
        txtDetail.setFont(new Font("Consolas", Font.PLAIN, 13));
        txtDetail.setEditable(false);

        JScrollPane scroll = new JScrollPane(txtDetail);
        scroll.setPreferredSize(new Dimension(460, 420));
        JOptionPane.showMessageDialog(this, scroll, "Chi tiết hóa đơn " + hd.getMaHoaDon(), JOptionPane.INFORMATION_MESSAGE);
    }

    private void xuLyThanhToan() {
        int row = tblHoaDon.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn một hóa đơn cần thanh toán.");
            return;
        }

        String maHd = (String) modelHoaDon.getValueAt(row, 0);
        HoaDon hd = hoaDonService.timTheoMa(maHd).orElse(null);
        if (hd == null) return;

        // Kiểm tra chống thanh toán trùng lặp ngay từ UI
        if (hd.getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN) {
            UIUtil.showError(this, "Hóa đơn này đã được thanh toán đầy đủ trước đó! Hệ thống ngăn chặn thanh toán trùng.");
            return;
        }

        JTextField txtSoTien = new JTextField(hd.getTongTien().toPlainString());
        JComboBox<PhuongThucThanhToan> cboPhuongThuc = new JComboBox<>(PhuongThucThanhToan.values());
        JTextField txtGhiChu = new JTextField("Thanh toán tiền phòng và dịch vụ");

        JPanel panel = new JPanel(new GridLayout(4, 2, 8, 8));
        panel.add(new JLabel("Hóa đơn:"));
        panel.add(new JLabel(hd.getMaHoaDon() + " (Cần thu: " + FormatUtil.formatTien(hd.getTongTien()) + ")"));
        panel.add(new JLabel("Số tiền khách trả:"));
        panel.add(txtSoTien);
        panel.add(new JLabel("Phương thức:"));
        panel.add(cboPhuongThuc);
        panel.add(new JLabel("Ghi chú:"));
        panel.add(txtGhiChu);

        int result = JOptionPane.showConfirmDialog(this, panel, "Xác nhận thanh toán hóa đơn", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            try {
                BigDecimal soTien = FormatUtil.parseTien(txtSoTien.getText());
                PhuongThucThanhToan pt = (PhuongThucThanhToan) cboPhuongThuc.getSelectedItem();
                String ghiChu = txtGhiChu.getText().trim();

                hoaDonService.thanhToanHoaDon(maHd, soTien, pt, ghiChu);
                UIUtil.showSuccess(this, "Thanh toán thành công hóa đơn " + maHd + "!\nĐã cập nhật trạng thái: ĐÃ THANH TOÁN.");
                loadDuLieu();
            } catch (Exception ex) {
                UIUtil.showError(this, ex.getMessage());
            }
        }
    }
}
