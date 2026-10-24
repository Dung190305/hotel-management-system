package com.qlks.view.panels;

import com.qlks.model.HoaDon;
import com.qlks.model.Phong;
import com.qlks.model.enums.TrangThaiPhong;
import com.qlks.model.enums.TrangThaiThanhToan;
import com.qlks.service.DatPhongService;
import com.qlks.service.HoaDonService;
import com.qlks.service.PhongService;
import com.qlks.util.FormatUtil;
import com.qlks.util.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

/**
 * Màn hình Báo cáo Thống kê Doanh thu & Công suất phòng.
 */
public class ThongKePanel extends JPanel {

    private final PhongService phongService;
    private final DatPhongService datPhongService;
    private final HoaDonService hoaDonService;

    private JLabel lblTongDoanhThu;
    private JLabel lblPhongDangO;
    private JLabel lblTongDatPhong;
    private JLabel lblDoanhThuDichVu;

    private JTable tblGiaoDich;
    private DefaultTableModel modelGiaoDich;

    public ThongKePanel() {
        this.phongService = new PhongService();
        this.datPhongService = new DatPhongService();
        this.hoaDonService = new HoaDonService();

        initComponents();
        loadDuLieu();
    }

    private void initComponents() {
        setLayout(new BorderLayout(15, 15));
        setBackground(UIUtil.COLOR_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // 1. THANH TIÊU ĐỀ
        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setBackground(Color.WHITE);
        pnlTop.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 233, 238)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblTitle = new JLabel("BÁO CÁO TỔNG QUAN & DOANH THU");
        lblTitle.setFont(UIUtil.FONT_TITLE);
        lblTitle.setForeground(UIUtil.COLOR_PRIMARY);
        pnlTop.add(lblTitle, BorderLayout.WEST);

        JButton btnLamMoi = UIUtil.createButton("Cập nhật số liệu", UIUtil.COLOR_PRIMARY, Color.WHITE);
        btnLamMoi.addActionListener(e -> loadDuLieu());
        pnlTop.add(btnLamMoi, BorderLayout.EAST);
        add(pnlTop, BorderLayout.NORTH);

        // 2. 4 THẺ THỐNG KÊ (STAT CARDS)
        JPanel pnlCards = new JPanel(new GridLayout(1, 4, 15, 0));
        pnlCards.setBackground(UIUtil.COLOR_BACKGROUND);

        pnlCards.add(createStatCard("TỔNG DOANH THU", lblTongDoanhThu = new JLabel("0 VNĐ"), new Color(40, 167, 69)));
        pnlCards.add(createStatCard("PHÒNG ĐANG Ở", lblPhongDangO = new JLabel("0 / 0"), new Color(220, 53, 69)));
        pnlCards.add(createStatCard("LƯỢT ĐẶT PHÒNG", lblTongDatPhong = new JLabel("0 đơn"), new Color(24, 119, 242)));
        pnlCards.add(createStatCard("DOANH THU DỊCH VỤ", lblDoanhThuDichVu = new JLabel("0 VNĐ"), new Color(253, 126, 20)));

        // 3. BẢNG HÓA ĐƠN ĐÃ THANH TOÁN
        JPanel pnlCenter = new JPanel(new BorderLayout(10, 10));
        pnlCenter.setBackground(Color.WHITE);
        pnlCenter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 233, 238)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblListTitle = new JLabel("DANH SÁCH HÓA ĐƠN ĐÃ THANH TOÁN THÀNH CÔNG");
        lblListTitle.setFont(UIUtil.FONT_HEADER);
        pnlCenter.add(lblListTitle, BorderLayout.NORTH);

        String[] headers = {"Mã HĐ", "Mã đặt phòng", "Ngày thanh toán", "Tiền phòng", "Tiền dịch vụ", "Giảm trừ", "Thực thu"};
        modelGiaoDich = new DefaultTableModel(headers, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblGiaoDich = new JTable(modelGiaoDich);
        UIUtil.formatTable(tblGiaoDich);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        tblGiaoDich.getColumnModel().getColumn(3).setCellRenderer(right);
        tblGiaoDich.getColumnModel().getColumn(4).setCellRenderer(right);
        tblGiaoDich.getColumnModel().getColumn(5).setCellRenderer(right);
        tblGiaoDich.getColumnModel().getColumn(6).setCellRenderer(right);

        pnlCenter.add(new JScrollPane(tblGiaoDich), BorderLayout.CENTER);

        JPanel pnlMainContent = new JPanel(new BorderLayout(15, 15));
        pnlMainContent.setBackground(UIUtil.COLOR_BACKGROUND);
        pnlMainContent.add(pnlCards, BorderLayout.NORTH);
        pnlMainContent.add(pnlCenter, BorderLayout.CENTER);

        add(pnlMainContent, BorderLayout.CENTER);
    }

    private JPanel createStatCard(String title, JLabel lblValue, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(new Color(108, 117, 125));

        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblValue.setForeground(accentColor);

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        return card;
    }

    public void loadDuLieu() {
        // 1. Thống kê phòng
        List<Phong> dsPhong = phongService.layTatCaPhong();
        long soPhongDangO = dsPhong.stream().filter(p -> p.getTrangThaiHienTai() == TrangThaiPhong.OCCUPIED).count();
        lblPhongDangO.setText(soPhongDangO + " / " + dsPhong.size() + " phòng");

        // 2. Thống kê đặt phòng
        int tongDp = datPhongService.layTatCaDatPhong().size();
        lblTongDatPhong.setText(tongDp + " lượt");

        // 3. Thống kê doanh thu thực tế
        List<HoaDon> dsHoaDon = hoaDonService.layTatCaHoaDon();
        BigDecimal tongDoanhThu = BigDecimal.ZERO;
        BigDecimal tongTienDv = BigDecimal.ZERO;

        modelGiaoDich.setRowCount(0);
        for (HoaDon hd : dsHoaDon) {
            if (hd.getTrangThai() == TrangThaiThanhToan.DA_THANH_TOAN) {
                tongDoanhThu = tongDoanhThu.add(hd.getTongTien());
                tongTienDv = tongTienDv.add(hd.getTienDichVu());

                modelGiaoDich.addRow(new Object[]{
                        hd.getMaHoaDon(),
                        hd.getDatPhong() != null ? hd.getDatPhong().getMaDatPhong() : "",
                        FormatUtil.formatNgayGio(hd.getNgayLap()),
                        FormatUtil.formatTien(hd.getTienPhong()),
                        FormatUtil.formatTien(hd.getTienDichVu()),
                        FormatUtil.formatTien(hd.getTienGiamGia()),
                        FormatUtil.formatTien(hd.getTongTien())
                });
            }
        }

        lblTongDoanhThu.setText(FormatUtil.formatTien(tongDoanhThu));
        lblDoanhThuDichVu.setText(FormatUtil.formatTien(tongTienDv));
    }
}
