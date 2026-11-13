package com.qlks.util;

import com.qlks.model.enums.TrangThaiPhong;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * Tiện ích hỗ trợ giao diện Java Swing: thông báo, màu sắc, định dạng bảng.
 */
public final class UIUtil {

    // Màu chủ đạo thương hiệu
    public static final Color COLOR_PRIMARY = new Color(24, 119, 242);
    public static final Color COLOR_PRIMARY_DARK = new Color(13, 82, 173);
    public static final Color COLOR_BACKGROUND = new Color(245, 247, 250);
    public static final Color COLOR_CARD = Color.WHITE;

    // Màu sắc theo 5 trạng thái phòng
    public static final Color COLOR_AVAILABLE = new Color(40, 167, 69);    // Xanh lá
    public static final Color COLOR_OCCUPIED = new Color(220, 53, 69);     // Đỏ
    public static final Color COLOR_RESERVED = new Color(253, 126, 20);    // Cam
    public static final Color COLOR_CLEANING = new Color(0, 123, 255);     // Xanh dương
    public static final Color COLOR_MAINTENANCE = new Color(108, 117, 125); // Xám

    // Font chữ chuẩn
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_REGULAR = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);

    private UIUtil() {
    }

    public static Color getMauTrangThaiPhong(TrangThaiPhong tt) {
        if (tt == null) return COLOR_AVAILABLE;
        switch (tt) {
            case OCCUPIED: return COLOR_OCCUPIED;
            case RESERVED: return COLOR_RESERVED;
            case CLEANING: return COLOR_CLEANING;
            case MAINTENANCE: return COLOR_MAINTENANCE;
            case AVAILABLE:
            default:
                return COLOR_AVAILABLE;
        }
    }

    public static void showSuccess(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Thông tin", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Thông báo lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static boolean showConfirm(Component parent, String message) {
        int result = JOptionPane.showConfirmDialog(parent, message, "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }

    /**
     * Cấu hình bảng JTable theo chuẩn hiện đại: không cho sửa trực tiếp, chiều cao dòng 32px, căn giữa tiêu đề.
     */
    public static void formatTable(JTable table) {
        table.setFont(FONT_REGULAR);
        table.setRowHeight(32);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(true);
        table.setGridColor(new Color(230, 233, 238));

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_BOLD);
        header.setPreferredSize(new Dimension(header.getWidth(), 38));
        header.setBackground(new Color(240, 242, 245));
        header.setForeground(new Color(33, 37, 41));

        // Căn giữa header
        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) header.getDefaultRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
    }

    /**
     * Tạo nút bấm phẳng hiện đại với màu sắc tùy chỉnh.
     */
    public static JButton createButton(String text, Color bgColor, Color fgColor) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BOLD);
        btn.setBackground(bgColor);
        btn.setForeground(fgColor);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        return btn;
    }
}
