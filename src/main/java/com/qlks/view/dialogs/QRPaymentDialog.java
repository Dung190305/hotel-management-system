package com.qlks.view.dialogs;

import com.qlks.util.FormatUtil;
import com.qlks.util.QRCodeUtil;
import com.qlks.util.UIUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;

/**
 * Hộp thoại hiển thị mã QR chuyển khoản tiền cọc ngân hàng chuẩn VietQR.
 * Lễ tân chỉ được tạo đơn sau khi kiểm tra tài khoản đã nhận tiền và bấm "Đã nhận được tiền".
 */
public class QRPaymentDialog extends JDialog {

    private boolean daXacNhanThanhToan = false;

    public QRPaymentDialog(Dialog parent, String maDatPhong, String hoTenKhach, BigDecimal soTien) {
        super(parent, "Thanh toán cọc qua Mã QR - " + maDatPhong, true);
        initComponents(maDatPhong, hoTenKhach, soTien);
    }

    public QRPaymentDialog(Frame parent, String maDatPhong, String hoTenKhach, BigDecimal soTien) {
        super(parent, "Thanh toán cọc qua Mã QR - " + maDatPhong, true);
        initComponents(maDatPhong, hoTenKhach, soTien);
    }

    private void initComponents(String maDatPhong, String hoTenKhach, BigDecimal soTien) {
        setSize(650, 480);
        setLocationRelativeTo(getParent());
        setResizable(false);
        setLayout(new BorderLayout(15, 15));
        getContentPane().setBackground(Color.WHITE);

        // 1. Header
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(UIUtil.COLOR_PRIMARY_DARK);
        pnlHeader.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel lblTitle = new JLabel("QUÉT MÃ QR CHUYỂN TIỀN CỌC PHÒNG");
        lblTitle.setFont(UIUtil.FONT_TITLE);
        lblTitle.setForeground(Color.WHITE);
        pnlHeader.add(lblTitle, BorderLayout.WEST);
        add(pnlHeader, BorderLayout.NORTH);

        // 2. Body
        JPanel pnlBody = new JPanel(new BorderLayout(20, 10));
        pnlBody.setBackground(Color.WHITE);
        pnlBody.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Ảnh QR Code bên trái
        BufferedImage imgQR = QRCodeUtil.taoMaQRTienCoc(maDatPhong, hoTenKhach, soTien, 230, 230);
        JLabel lblQR = new JLabel(new ImageIcon(imgQR));
        lblQR.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 224, 230), 2),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        pnlBody.add(lblQR, BorderLayout.WEST);

        // Thông tin chuyển khoản bên phải
        JPanel pnlInfo = new JPanel();
        pnlInfo.setLayout(new BoxLayout(pnlInfo, BoxLayout.Y_AXIS));
        pnlInfo.setBackground(Color.WHITE);

        String noiDungCK = QRCodeUtil.taoNoiDungChuyenKhoan(maDatPhong, hoTenKhach);

        addInfoRow(pnlInfo, "Ngân hàng:", QRCodeUtil.DEFAULT_BANK_NAME);
        addInfoRow(pnlInfo, "Số tài khoản:", QRCodeUtil.DEFAULT_ACCOUNT_NO);
        addInfoRow(pnlInfo, "Chủ tài khoản:", QRCodeUtil.DEFAULT_ACCOUNT_NAME);
        addInfoRow(pnlInfo, "Số tiền cọc:", FormatUtil.formatTien(soTien), true);
        addInfoRow(pnlInfo, "Nội dung CK:", noiDungCK);

        pnlInfo.add(Box.createRigidArea(new Dimension(0, 12)));

        JLabel lblHuongDan = new JLabel("<html><i>Khách mở ứng dụng Ngân hàng (Vietcombank, MB, Techcombank,...) hoặc Ví điện tử quét mã QR phía trên để chuyển tiền tự động.</i></html>");
        lblHuongDan.setFont(UIUtil.FONT_REGULAR);
        lblHuongDan.setForeground(new Color(108, 117, 125));
        pnlInfo.add(lblHuongDan);

        pnlInfo.add(Box.createRigidArea(new Dimension(0, 10)));

        JLabel lblCanhBao = new JLabel("<html><b style='color: #c92a2a;'>⚠️ Lưu ý cho Tiếp tân:</b> Hãy kiểm tra tin nhắn/app ngân hàng báo đã nhận tiền trước khi ấn xác nhận!</html>");
        lblCanhBao.setFont(UIUtil.FONT_REGULAR);
        pnlInfo.add(lblCanhBao);

        pnlBody.add(pnlInfo, BorderLayout.CENTER);
        add(pnlBody, BorderLayout.CENTER);

        // 3. Footer Buttons
        JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        pnlFooter.setBackground(new Color(245, 247, 250));
        pnlFooter.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 233, 238)));

        JButton btnHuy = UIUtil.createButton("Hủy bỏ (Khách chưa chuyển)", new Color(108, 117, 125), Color.WHITE);
        btnHuy.addActionListener(e -> {
            daXacNhanThanhToan = false;
            dispose();
        });
        pnlFooter.add(btnHuy);

        JButton btnXacNhan = UIUtil.createButton("✅ Đã nhận được tiền - Xác nhận tạo đơn", new Color(40, 167, 69), Color.WHITE);
        btnXacNhan.setFont(UIUtil.FONT_BOLD);
        btnXacNhan.addActionListener(e -> {
            daXacNhanThanhToan = true;
            dispose();
        });
        pnlFooter.add(btnXacNhan);

        add(pnlFooter, BorderLayout.SOUTH);
    }

    private void addInfoRow(JPanel parent, String label, String value) {
        addInfoRow(parent, label, value, false);
    }

    private void addInfoRow(JPanel parent, String label, String value, boolean highlight) {
        JPanel row = new JPanel(new BorderLayout(5, 0));
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 26));

        JLabel lblL = new JLabel(label);
        lblL.setFont(UIUtil.FONT_REGULAR);
        lblL.setForeground(new Color(90, 95, 105));
        lblL.setPreferredSize(new Dimension(105, 22));

        JLabel lblV = new JLabel(value);
        if (highlight) {
            lblV.setFont(new Font("Segoe UI", Font.BOLD, 16));
            lblV.setForeground(new Color(220, 53, 69)); // Màu đỏ tiền
        } else {
            lblV.setFont(UIUtil.FONT_BOLD);
            lblV.setForeground(Color.BLACK);
        }

        row.add(lblL, BorderLayout.WEST);
        row.add(lblV, BorderLayout.CENTER);

        parent.add(row);
        parent.add(Box.createRigidArea(new Dimension(0, 3)));
    }

    public boolean isDaXacNhanThanhToan() {
        return daXacNhanThanhToan;
    }
}

