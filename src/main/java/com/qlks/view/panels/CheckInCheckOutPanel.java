package com.qlks.view.panels;

import com.qlks.model.DatPhong;
import com.qlks.model.HoaDon;
import com.qlks.model.enums.TrangThaiDatPhong;
import com.qlks.service.DatPhongService;
import com.qlks.service.HoaDonService;
import com.qlks.util.FormatUtil;
import com.qlks.util.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Màn hình Check-in & Check-out chuyên biệt cho Lễ tân khách sạn.
 */
public class CheckInCheckOutPanel extends JPanel {

    private final DatPhongService datPhongService;
    private final HoaDonService hoaDonService;

    private JTable tblChoCheckIn;
    private DefaultTableModel modelChoCheckIn;

    private JTable tblDangO;
    private DefaultTableModel modelDangO;

    public CheckInCheckOutPanel() {
        this.datPhongService = new DatPhongService();
        this.hoaDonService = new HoaDonService();

        initComponents();
        loadDuLieu();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UIUtil.COLOR_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Header
        JPanel pnlTop = new JPanel(new BorderLayout());
        pnlTop.setBackground(Color.WHITE);
        pnlTop.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 233, 238)),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblTitle = new JLabel("TIẾP TÂN: CHECK-IN & CHECK-OUT");
        lblTitle.setFont(UIUtil.FONT_TITLE);
        lblTitle.setForeground(UIUtil.COLOR_PRIMARY);
        pnlTop.add(lblTitle, BorderLayout.WEST);

        JButton btnLamMoi = UIUtil.createButton("Làm mới danh sách", new Color(108, 117, 125), Color.WHITE);
        btnLamMoi.addActionListener(e -> loadDuLieu());
        pnlTop.add(btnLamMoi, BorderLayout.EAST);

        add(pnlTop, BorderLayout.NORTH);

        // Center: 2 Tab
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UIUtil.FONT_BOLD);

        // TAB 1: DANH SÁCH CHỜ CHECK-IN
        JPanel pnlTab1 = new JPanel(new BorderLayout(8, 8));
        pnlTab1.setBackground(Color.WHITE);
        pnlTab1.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] headersCheckIn = {"Mã ĐP", "Khách hàng", "Số điện thoại", "Phòng", "Loại phòng", "Ngày nhận", "Ngày trả", "Tiền cọc"};
        modelChoCheckIn = new DefaultTableModel(headersCheckIn, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblChoCheckIn = new JTable(modelChoCheckIn);
        UIUtil.formatTable(tblChoCheckIn);
        pnlTab1.add(new JScrollPane(tblChoCheckIn), BorderLayout.CENTER);

        JPanel pnlBtn1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlBtn1.setBackground(Color.WHITE);
        JButton btnXacNhanCheckIn = UIUtil.createButton("Xác nhận Check-in", new Color(40, 167, 69), Color.WHITE);
        btnXacNhanCheckIn.addActionListener(e -> xuLyCheckIn());
        pnlBtn1.add(btnXacNhanCheckIn);
        pnlTab1.add(pnlBtn1, BorderLayout.SOUTH);

        tabbedPane.addTab("1. Khách đến nhận phòng (Check-in)", pnlTab1);

        // TAB 2: DANH SÁCH ĐANG Ở CHỜ CHECK-OUT
        JPanel pnlTab2 = new JPanel(new BorderLayout(8, 8));
        pnlTab2.setBackground(Color.WHITE);
        pnlTab2.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        String[] headersCheckOut = {"Mã ĐP", "Khách hàng", "Số điện thoại", "Phòng", "Giờ nhận thực tế", "Dự kiến trả", "Tạm tính"};
        modelDangO = new DefaultTableModel(headersCheckOut, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblDangO = new JTable(modelDangO);
        UIUtil.formatTable(tblDangO);
        pnlTab2.add(new JScrollPane(tblDangO), BorderLayout.CENTER);

        JPanel pnlBtn2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlBtn2.setBackground(Color.WHITE);
        JButton btnXacNhanCheckOut = UIUtil.createButton("Trả phòng & Lập hóa đơn (Check-out)", new Color(220, 53, 69), Color.WHITE);
        btnXacNhanCheckOut.addActionListener(e -> xuLyCheckOut());
        pnlBtn2.add(btnXacNhanCheckOut);
        pnlTab2.add(pnlBtn2, BorderLayout.SOUTH);

        tabbedPane.addTab("2. Khách đang lưu trú (Check-out)", pnlTab2);

        add(tabbedPane, BorderLayout.CENTER);
    }

    public void loadDuLieu() {
        List<DatPhong> tatCa = datPhongService.layTatCaDatPhong();

        // 1. Lọc các phiếu chờ check-in (Đã xác nhận)
        modelChoCheckIn.setRowCount(0);
        for (DatPhong dp : tatCa) {
            if (dp.getTrangThai() == TrangThaiDatPhong.DA_XAC_NHAN) {
                modelChoCheckIn.addRow(new Object[]{
                        dp.getMaDatPhong(),
                        dp.getKhachHang() != null ? dp.getKhachHang().getHoTen() : "",
                        dp.getKhachHang() != null ? dp.getKhachHang().getSoDienThoai() : "",
                        dp.getPhong() != null ? "P." + dp.getPhong().getSoPhong() : "",
                        dp.getPhong() != null ? dp.getPhong().getTenLoaiPhongHienThi() : "",
                        FormatUtil.formatNgayGio(dp.getNgayNhanPhong()),
                        FormatUtil.formatNgayGio(dp.getNgayTraPhong()),
                        FormatUtil.formatTien(dp.getTienCoc())
                });
            }
        }

        // 2. Lọc các phiếu đang ở (DANG_O)
        modelDangO.setRowCount(0);
        for (DatPhong dp : tatCa) {
            if (dp.getTrangThai() == TrangThaiDatPhong.DANG_O) {
                modelDangO.addRow(new Object[]{
                        dp.getMaDatPhong(),
                        dp.getKhachHang() != null ? dp.getKhachHang().getHoTen() : "",
                        dp.getKhachHang() != null ? dp.getKhachHang().getSoDienThoai() : "",
                        dp.getPhong() != null ? "P." + dp.getPhong().getSoPhong() : "",
                        FormatUtil.formatNgayGio(dp.getThoiGianNhanThucTe()),
                        FormatUtil.formatNgayGio(dp.getNgayTraPhong()),
                        FormatUtil.formatTien(dp.tinhTienTamTinh())
                });
            }
        }
    }

    private void xuLyCheckIn() {
        int row = tblChoCheckIn.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn khách hàng nhận phòng.");
            return;
        }

        String maDp = (String) modelChoCheckIn.getValueAt(row, 0);
        try {
            datPhongService.xacNhanCheckIn(maDp);
            UIUtil.showSuccess(this, "Khách đã Check-in thành công! Phòng đã chuyển sang trạng thái Đang có khách.");
            loadDuLieu();
        } catch (Exception ex) {
            UIUtil.showError(this, ex.getMessage());
        }
    }

    private void xuLyCheckOut() {
        int row = tblDangO.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn phòng cần làm thủ tục trả phòng.");
            return;
        }

        String maDp = (String) modelDangO.getValueAt(row, 0);
        String phong = (String) modelDangO.getValueAt(row, 3);

        if (UIUtil.showConfirm(this, "Xác nhận trả phòng cho " + phong + " và tạo hóa đơn thanh toán?")) {
            try {
                HoaDon hd = datPhongService.xacNhanCheckOut(maDp);
                UIUtil.showSuccess(this, String.format(
                        "Trả phòng thành công!\nPhòng đã chuyển sang trạng thái ĐANG DỌN DẸP.\nHóa đơn đã được tạo: %s (Tổng: %s)",
                        hd.getMaHoaDon(), FormatUtil.formatTien(hd.getTongTien())
                ));
                loadDuLieu();
            } catch (Exception ex) {
                UIUtil.showError(this, ex.getMessage());
            }
        }
    }
}
