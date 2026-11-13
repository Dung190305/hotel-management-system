package com.qlks.util;

import com.qlks.service.ThongBaoService.TrangThaiChuong;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Custom Vector Icon vẽ hình quả chuông 3D vàng nổi bật kèm dấu chấm than (!) theo đúng thiết kế:
 * 1. Có thông báo chưa đọc: Dấu chấm than màu ĐỎ.
 * 2. Tất cả thông báo đã đọc: Dấu chấm than màu XANH (như ảnh mẫu).
 * 3. Không có thông báo: KHÔNG CÓ dấu chấm than (chỉ có quả chuông vàng).
 */
public class BellIcon implements Icon {

    private final int width;
    private final int height;
    private TrangThaiChuong trangThai;

    public BellIcon() {
        this(34, 34, TrangThaiChuong.KHONG_CO_THONG_BAO);
    }

    public BellIcon(int size) {
        this(size, size, TrangThaiChuong.KHONG_CO_THONG_BAO);
    }

    public BellIcon(int width, int height, TrangThaiChuong trangThai) {
        this.width = width;
        this.height = height;
        this.trangThai = trangThai != null ? trangThai : TrangThaiChuong.KHONG_CO_THONG_BAO;
    }

    public void setTrangThai(TrangThaiChuong trangThai) {
        this.trangThai = trangThai != null ? trangThai : TrangThaiChuong.KHONG_CO_THONG_BAO;
    }

    public TrangThaiChuong getTrangThai() {
        return trangThai;
    }

    @Override
    public int getIconWidth() {
        return width;
    }

    @Override
    public int getIconHeight() {
        return height;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        float w = width;
        float h = height;

        // Tọa độ gốc tương đối
        float bx = x;
        float by = y;

        // 1. Quả lắc chuông (Clapper bên dưới)
        float clapperW = w * 0.22f;
        float clapperH = h * 0.22f;
        float clapperX = bx + (w - clapperW) / 2.0f;
        float clapperY = by + h * 0.70f;
        g2.setColor(new Color(230, 130, 0));
        g2.fill(new Ellipse2D.Float(clapperX, clapperY, clapperW, clapperH));

        // 2. Vòng khuyên trên đầu chuông (Top loop/ring)
        float ringW = w * 0.24f;
        float ringH = h * 0.20f;
        float ringX = bx + (w - ringW) / 2.0f;
        float ringY = by + h * 0.05f;
        g2.setStroke(new BasicStroke(Math.max(2.0f, w * 0.07f)));
        g2.setColor(new Color(245, 160, 0));
        g2.draw(new RoundRectangle2D.Float(ringX, ringY, ringW, ringH, ringW, ringH));

        // 3. Thân chuông 3D vàng (Bell Body)
        Path2D.Float bell = new Path2D.Float();
        float topW = w * 0.24f;
        float topX = bx + (w - topW) / 2.0f;
        float topY = by + h * 0.16f;

        float bottomW = w * 0.76f;
        float bottomX = bx + (w - bottomW) / 2.0f;
        float bottomY = by + h * 0.78f;

        bell.moveTo(topX, topY + h * 0.06f);
        // Đỉnh vòm chuông
        bell.quadTo(bx + w * 0.5f, topY - h * 0.02f, topX + topW, topY + h * 0.06f);
        // Sườn phải uốn cong loe ra
        bell.curveTo(bx + w * 0.65f, by + h * 0.35f,
                     bx + w * 0.88f, by + h * 0.66f,
                     bottomX + bottomW, bottomY);
        // Đáy chuông
        bell.quadTo(bx + w * 0.5f, bottomY + h * 0.06f, bottomX, bottomY);
        // Sườn trái uốn cong lên
        bell.curveTo(bx + w * 0.12f, by + h * 0.66f,
                     bx + w * 0.35f, by + h * 0.35f,
                     topX, topY + h * 0.06f);
        bell.closePath();

        // Đổ bóng & Gradient 3D vàng ấm (Warm 3D Gold)
        GradientPaint bellGradient = new GradientPaint(
                bx + w * 0.25f, topY, new Color(255, 218, 68),
                bx + w * 0.85f, bottomY, new Color(245, 140, 0)
        );
        g2.setPaint(bellGradient);
        g2.fill(bell);

        // Vành dưới chuông (Rim Highlight)
        g2.setColor(new Color(255, 235, 130, 200));
        g2.setStroke(new BasicStroke(Math.max(1.2f, w * 0.04f)));
        Path2D.Float rimHighlight = new Path2D.Float();
        rimHighlight.moveTo(bottomX + w * 0.06f, bottomY - h * 0.01f);
        rimHighlight.quadTo(bx + w * 0.5f, bottomY + h * 0.04f, bottomX + bottomW - w * 0.06f, bottomY - h * 0.01f);
        g2.draw(rimHighlight);

        // Ánh sáng phản chiếu sườn trái (3D specular highlight)
        g2.setColor(new Color(255, 255, 255, 140));
        Path2D.Float shine = new Path2D.Float();
        shine.moveTo(topX + w * 0.04f, topY + h * 0.10f);
        shine.quadTo(bx + w * 0.28f, by + h * 0.45f, bottomX + w * 0.10f, bottomY - h * 0.08f);
        g2.setStroke(new BasicStroke(Math.max(1.5f, w * 0.05f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(shine);

        // 4. VẼ DẤU CHẤM THAN (!) THEO TRẠNG THÁI
        if (trangThai != TrangThaiChuong.KHONG_CO_THONG_BAO) {
            float badgeDiameter = w * 0.44f;
            float badgeX = bx + w * 0.58f;
            float badgeY = by + h * 0.02f;

            Color badgeColor;
            Color badgeBorderColor;

            if (trangThai == TrangThaiChuong.CO_CHUA_DOC) {
                // Có thông báo chưa đọc -> DẤU CHẤM THAN MÀU ĐỎ
                badgeColor = new Color(225, 30, 45); // Đỏ tươi
                badgeBorderColor = new Color(255, 255, 255);
            } else {
                // Tất cả thông báo đã đọc -> DẤU CHẤM THAN MÀU XANH (như ảnh mẫu)
                badgeColor = new Color(24, 144, 255); // Xanh dương tươi sáng
                badgeBorderColor = new Color(255, 255, 255);
            }

            // Vẽ viền ngoài trắng để badge tách biệt rõ trên nền chuông
            g2.setColor(badgeBorderColor);
            g2.fill(new Ellipse2D.Float(badgeX - 1.5f, badgeY - 1.5f, badgeDiameter + 3.0f, badgeDiameter + 3.0f));

            // Nền badge tròn
            g2.setColor(badgeColor);
            g2.fill(new Ellipse2D.Float(badgeX, badgeY, badgeDiameter, badgeDiameter));

            // Dấu chấm than (!) màu trắng chính giữa
            g2.setColor(Color.WHITE);
            int fontSize = Math.max(9, Math.round(badgeDiameter * 0.75f));
            Font font = new Font("Segoe UI", Font.BOLD, fontSize);
            g2.setFont(font);

            FontMetrics fm = g2.getFontMetrics(font);
            String text = "!";
            int textW = fm.stringWidth(text);
            int textH = fm.getAscent();

            float textX = badgeX + (badgeDiameter - textW) / 2.0f;
            float textY = badgeY + (badgeDiameter + textH) / 2.0f - fm.getDescent() * 0.6f;

            g2.drawString(text, textX, textY);
        }

        g2.dispose();
    }
}

