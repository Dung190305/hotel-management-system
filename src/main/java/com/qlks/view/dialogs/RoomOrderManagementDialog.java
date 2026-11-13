package com.qlks.view.dialogs;

import com.qlks.model.DonDichVuPhong;
import com.qlks.model.DonDichVuPhong.TrangThaiOrder;
import com.qlks.model.NhanVien;
import com.qlks.service.AuthService;
import com.qlks.service.RoomOrderService;
import com.qlks.util.AppEventBus;
import com.qlks.util.FormatUtil;
import com.qlks.util.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Hộp thoại quản lý và duyệt các đơn dịch vụ phòng do khách quét mã QR gửi lên (In-Room QR Orders).
 */
public class RoomOrderManagementDialog extends JDialog implements AppEventBus.AppEventListener {

    private final RoomOrderService roomOrderService = new RoomOrderService();
    private DefaultTableModel modelOrders;
    private JTable tblOrders;
    private JCheckBox chkChiChoDuyet;

    public RoomOrderManagementDialog(Window owner) {
        super(owner, "Quản lý Đơn Dịch Vụ Tại Phòng (In-Room QR Orders)", ModalityType.APPLICATION_MODAL);
        initComponents();
        AppEventBus.register(this);
        loadDuLieu();
        setSize(880, 520);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(UIUtil.COLOR_BACKGROUND);

        // Header
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(Color.WHITE);
        pnlHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(230, 233, 238)),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));

        JLabel lblTitle = new JLabel("TIẾP NHẬN YÊU CẦU DỊCH VỤ PHÒNG (ROOM SERVICE)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(new Color(30, 60, 114));

        chkChiChoDuyet = new JCheckBox("Chỉ hiển thị đơn chờ duyệt", true);
        chkChiChoDuyet.setBackground(Color.WHITE);
        chkChiChoDuyet.addActionListener(e -> loadDuLieu());

        pnlHeader.add(lblTitle, BorderLayout.WEST);
        pnlHeader.add(chkChiChoDuyet, BorderLayout.EAST);
        add(pnlHeader, BorderLayout.NORTH);

        // Table
        String[] headers = {"Mã đơn", "Phòng", "Khách hàng", "Danh sách món", "Ghi chú", "Thời gian", "Tổng tiền", "Trạng thái"};
        modelOrders = new DefaultTableModel(headers, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblOrders = new JTable(modelOrders);
        UIUtil.formatTable(tblOrders);
        add(new JScrollPane(tblOrders), BorderLayout.CENTER);

        // Buttons
        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        pnlActions.setBackground(Color.WHITE);

        JButton btnDuyet = UIUtil.createButton("Tiếp nhận & Giao hàng", new Color(40, 167, 69), Color.WHITE);
        btnDuyet.addActionListener(e -> xuLyDuyetDon());

        JButton btnTuChoi = UIUtil.createButton("Từ chối (Hết hàng)", new Color(220, 53, 69), Color.WHITE);
        btnTuChoi.addActionListener(e -> xuLyTuChoiDon());

        JButton btnLamMoi = UIUtil.createButton("Làm mới", new Color(0, 123, 255), Color.WHITE);
        btnLamMoi.addActionListener(e -> loadDuLieu());

        JButton btnDong = UIUtil.createButton("Đóng", new Color(108, 117, 125), Color.WHITE);
        btnDong.addActionListener(e -> dispose());

        pnlActions.add(btnDuyet);
        pnlActions.add(btnTuChoi);
        pnlActions.add(btnLamMoi);
        pnlActions.add(btnDong);
        add(pnlActions, BorderLayout.SOUTH);
    }

    public void loadDuLieu() {
        modelOrders.setRowCount(0);
        List<DonDichVuPhong> list = chkChiChoDuyet.isSelected()
                ? roomOrderService.layDanhSachChoDuyet()
                : roomOrderService.layTatCaDon();

        for (DonDichVuPhong o : list) {
            modelOrders.addRow(new Object[]{
                    o.getOrderId(),
                    "P." + o.getSoPhong(),
                    o.getTenKhach(),
                    o.getTomTatMon(),
                    o.getGhiChu() != null ? o.getGhiChu() : "",
                    FormatUtil.formatNgayGio(o.getThoiGianDat()),
                    FormatUtil.formatTien(o.tinhTongTien()) + " đ",
                    o.getTrangThai().getTenHienThi()
            });
        }
    }

    private void xuLyDuyetDon() {
        int row = tblOrders.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn đơn dịch vụ phòng cần tiếp nhận.");
            return;
        }

        String orderId = (String) modelOrders.getValueAt(row, 0);
        String phong = (String) modelOrders.getValueAt(row, 1);

        if (UIUtil.showConfirm(this, "Xác nhận duyệt và xuất đồ uống/dịch vụ giao lên " + phong + "?\nSố tiền sẽ tự động ghi vào Hóa đơn phòng.")) {
            try {
                NhanVien nv = AuthService.getNhanVienHienTai();
                String maNv = (nv != null) ? nv.getMaNhanVien() : "LE_TAN";
                roomOrderService.tiepNhanVaGiaoHang(orderId, maNv);
                UIUtil.showSuccess(this, "Đã tiếp nhận đơn " + orderId + "!\nĐã cập nhật dịch vụ vào Hóa đơn " + phong + ".");
                loadDuLieu();
            } catch (Exception ex) {
                UIUtil.showError(this, ex.getMessage());
            }
        }
    }

    private void xuLyTuChoiDon() {
        int row = tblOrders.getSelectedRow();
        if (row < 0) {
            UIUtil.showError(this, "Vui lòng chọn đơn dịch vụ phòng cần từ chối.");
            return;
        }

        String orderId = (String) modelOrders.getValueAt(row, 0);
        String lyDo = JOptionPane.showInputDialog(this, "Nhập lý do từ chối (VD: Món này tạm hết hàng):", "Từ chối đơn phòng", JOptionPane.QUESTION_MESSAGE);
        if (lyDo != null && !lyDo.isBlank()) {
            try {
                NhanVien nv = AuthService.getNhanVienHienTai();
                String maNv = (nv != null) ? nv.getMaNhanVien() : "LE_TAN";
                roomOrderService.tuChoiOrder(orderId, lyDo.trim(), maNv);
                UIUtil.showSuccess(this, "Đã từ chối đơn " + orderId);
                loadDuLieu();
            } catch (Exception ex) {
                UIUtil.showError(this, ex.getMessage());
            }
        }
    }

    @Override
    public void onEvent(AppEventBus.EventType eventType) {
        if (eventType == AppEventBus.EventType.ROOM_ORDER_RECEIVED || eventType == AppEventBus.EventType.ROOM_ORDER_UPDATED) {
            SwingUtilities.invokeLater(this::loadDuLieu);
        }
    }

    @Override
    public void dispose() {
        AppEventBus.unregister(this);
        super.dispose();
    }
}

