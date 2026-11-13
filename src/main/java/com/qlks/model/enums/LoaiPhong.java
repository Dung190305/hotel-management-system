package com.qlks.model.enums;

/**
 * Enum biểu diễn phân loại phòng khách sạn.
 * Chuẩn hóa theo đúng 4 cấu hình:
 * 1. PHONG_DON_THUONG (Phòng đơn thường)
 * 2. PHONG_DON_VIP (Phòng đơn VIP)
 * 3. PHONG_DOI_THUONG (Phòng đôi thường)
 * 4. PHONG_DOI_VIP (Phòng đôi VIP)
 */
public enum LoaiPhong {
    PHONG_DON_THUONG("Phòng đơn thường"),
    PHONG_DON_VIP("Phòng đơn VIP"),
    PHONG_DOI_THUONG("Phòng đôi thường"),
    PHONG_DOI_VIP("Phòng đôi VIP"),

    @Deprecated
    PHONG_THUONG("Phòng thường"),
    @Deprecated
    PHONG_VIP("Phòng VIP");

    private final String tenHienThi;

    LoaiPhong(String tenHienThi) {
        this.tenHienThi = tenHienThi;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    @Override
    public String toString() {
        return tenHienThi;
    }

    public CauHinhLoaiPhong toCauHinh() {
        return CauHinhLoaiPhong.fromString(this.name());
    }

    public static LoaiPhong fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return PHONG_DON_THUONG;
        }
        String clean = text.trim();
        for (LoaiPhong lp : LoaiPhong.values()) {
            if (lp.name().equalsIgnoreCase(clean) || lp.tenHienThi.equalsIgnoreCase(clean)) {
                return lp;
            }
        }
        CauHinhLoaiPhong ch = CauHinhLoaiPhong.fromString(clean);
        return LoaiPhong.valueOf(ch.name());
    }
}
