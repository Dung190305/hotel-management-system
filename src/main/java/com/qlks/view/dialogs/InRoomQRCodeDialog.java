package com.qlks.view.dialogs;

import com.qlks.model.Phong;
import com.qlks.server.RoomOrderHttpServer;
import com.qlks.util.QRCodeUtil;
import com.qlks.util.UIUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * Hộp thoại hiển thị và in thẻ Mã QR Menu đặt dịch vụ tại phòng (In-Room QR Order Card).
 */
public class InRoomQRCodeDialog extends JDialog {

    private final Phong phong;
    private final String orderUrl;
    private final BufferedImage qrImage;

    public InRoomQRCodeDialog(Window owner, Phong phong) {
        super(owner, "Mã QR Dịch vụ phòng - " + (phong != null ? "P." + phong.getSoPhong() : ""), ModalityType.APPLICATION_MODAL);
        this.phong = phong;
        String soPhong = phong != null ? phong.getSoPhong() : "101";
        this.orderUrl = RoomOrderHttpServer.getOrderUrl(soPhong);
        this.qrImage = QRCodeUtil.taoAnhQRCode(orderUrl, 260, 260);

        initComponents();
        setSize(420, 560);
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(Color.WHITE);

        // Panel thẻ phòng mô phỏng biển mica đặt tại phòng
        JPanel pnlCard = new JPanel();
        pnlCard.setLayout(new BoxLayout(pnlCard, BoxLayout.Y_AXIS));
        pnlCard.setBackground(Color.WHITE);
        pnlCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(15, 20, 15, 20),
                BorderFactory.createLineBorder(new Color(220, 224, 230), 1, true)
        ));

        JLabel lblHotel = new JLabel("GRAND LUXURY HOTEL");
        lblHotel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblHotel.setForeground(new Color(30, 60, 114));
        lblHotel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub = new JLabel("DỊCH VỤ PHỤC VỤ TẬN PHÒNG (ROOM SERVICE)");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        lblSub.setForeground(Color.GRAY);
        lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblRoom = new JLabel("PHÒNG " + (phong != null ? phong.getSoPhong() : ""));
        lblRoom.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblRoom.setForeground(new Color(230, 57, 70));
        lblRoom.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Hiển thị ảnh QR
        JLabel lblQR = new JLabel(new ImageIcon(qrImage));
        lblQR.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblQR.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JLabel lblGuide1 = new JLabel("Quét mã bằng Camera điện thoại hoặc Zalo");
        lblGuide1.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblGuide1.setForeground(new Color(40, 167, 69));
        lblGuide1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblGuide2 = new JLabel("để gọi nước uống & đồ ăn lên tận phòng không cần xuống sảnh");
        lblGuide2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblGuide2.setForeground(Color.DARK_GRAY);
        lblGuide2.setAlignmentX(Component.CENTER_ALIGNMENT);

        pnlCard.add(Box.createVerticalStrut(10));
        pnlCard.add(lblHotel);
        pnlCard.add(Box.createVerticalStrut(2));
        pnlCard.add(lblSub);
        pnlCard.add(Box.createVerticalStrut(10));
        pnlCard.add(lblRoom);
        pnlCard.add(Box.createVerticalStrut(5));
        pnlCard.add(lblQR);
        pnlCard.add(lblGuide1);
        pnlCard.add(Box.createVerticalStrut(2));
        pnlCard.add(lblGuide2);
        pnlCard.add(Box.createVerticalStrut(10));

        add(pnlCard, BorderLayout.CENTER);

        // Panel nút chức năng
        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 10));
        pnlActions.setBackground(new Color(248, 249, 250));

        JButton btnCopy = UIUtil.createButton("Sao chép Link", new Color(108, 117, 125), Color.WHITE);
        btnCopy.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(orderUrl), null);
            UIUtil.showSuccess(this, "Đã sao chép link menu phòng:\n" + orderUrl);
        });

        JButton btnOpen = UIUtil.createButton("Mở thử trên Web", new Color(0, 123, 255), Color.WHITE);
        btnOpen.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(URI.create(orderUrl));
            } catch (Exception ex) {
                UIUtil.showError(this, "Không thể mở trình duyệt: " + ex.getMessage());
            }
        });

        JButton btnSave = UIUtil.createButton("Lưu ảnh QR", new Color(40, 167, 69), Color.WHITE);
        btnSave.addActionListener(e -> luuAnhQR());

        JButton btnClose = UIUtil.createButton("Đóng", new Color(220, 53, 69), Color.WHITE);
        btnClose.addActionListener(e -> dispose());

        pnlActions.add(btnCopy);
        pnlActions.add(btnOpen);
        pnlActions.add(btnSave);
        pnlActions.add(btnClose);

        add(pnlActions, BorderLayout.SOUTH);
    }

    private void luuAnhQR() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("QR_Menu_Phong_" + (phong != null ? phong.getSoPhong() : "room") + ".png"));
        int res = chooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try {
                ImageIO.write(qrImage, "PNG", f);
                UIUtil.showSuccess(this, "Đã lưu mã QR thành công tại:\n" + f.getAbsolutePath());
            } catch (IOException e) {
                UIUtil.showError(this, "Lỗi lưu file ảnh: " + e.getMessage());
            }
        }
    }
}

