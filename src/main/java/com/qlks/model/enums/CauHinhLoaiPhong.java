package com.qlks.model.enums;

import java.util.Collections;
import java.util.List;

/**
 * 4 Cấu hình phòng chuẩn bắt buộc của toàn bộ hệ thống:
 * 1. Phòng đơn thường (1 giường, tối đa 2 người)
 * 2. Phòng đơn VIP (1 giường, tối đa 2 người)
 * 3. Phòng đôi thường (2 giường, tối đa 4 người)
 * 4. Phòng đôi VIP (2 giường, tối đa 4 người)
 *
 * Tuyệt đối không tạo cấu hình thứ 5 nào khác.
 */
public enum CauHinhLoaiPhong {
    PHONG_DON_THUONG("Phòng đơn thường", LoaiGiuong.DON, HangPhong.THUONG,
            List.of("Đệm cao su non", "Điều hòa", "Nhà tắm", "Nhà vệ sinh", "Tủ quần áo")),

    PHONG_DON_VIP("Phòng đơn VIP", LoaiGiuong.DON, HangPhong.VIP,
            List.of("Đệm cao su non", "Điều hòa", "Nhà tắm", "Nhà vệ sinh", "Tủ quần áo", "Tivi", "Bồn tắm", "Tủ lạnh")),

    PHONG_DOI_THUONG("Phòng đôi thường", LoaiGiuong.DOI, HangPhong.THUONG,
            List.of("Đệm cao su non", "Điều hòa", "Nhà tắm", "Nhà vệ sinh", "Tủ quần áo")),

    PHONG_DOI_VIP("Phòng đôi VIP", LoaiGiuong.DOI, HangPhong.VIP,
            List.of("Đệm cao su non", "Điều hòa", "Nhà tắm", "Nhà vệ sinh", "Tủ quần áo", "Tivi", "Bồn tắm", "Tủ lạnh"));

    private final String tenHienThi;
    private final LoaiGiuong loaiGiuong;
    private final HangPhong hangPhong;
    private final List<String> danhSachTienNghi;

    CauHinhLoaiPhong(String tenHienThi, LoaiGiuong loaiGiuong, HangPhong hangPhong, List<String> danhSachTienNghi) {
        this.tenHienThi = tenHienThi;
        this.loaiGiuong = loaiGiuong;
        this.hangPhong = hangPhong;
        this.danhSachTienNghi = Collections.unmodifiableList(danhSachTienNghi);
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    public LoaiGiuong getLoaiGiuong() {
        return loaiGiuong;
    }

    public HangPhong getHangPhong() {
        return hangPhong;
    }

    public int getSoGiuong() {
        return loaiGiuong.getSoGiuong();
    }

    public int getSucChuaToiDa() {
        return loaiGiuong.getSucChuaToiDa();
    }

    public List<String> getDanhSachTienNghi() {
        return danhSachTienNghi;
    }

    @Override
    public String toString() {
        return tenHienThi;
    }

    /**
     * Tìm cấu hình từ cặp LoaiGiuong và HangPhong.
     */
    public static CauHinhLoaiPhong tuLoaiGiuongVaHang(LoaiGiuong loaiGiuong, HangPhong hangPhong) {
        if (loaiGiuong == null) loaiGiuong = LoaiGiuong.DON;
        if (hangPhong == null) hangPhong = HangPhong.THUONG;

        if (loaiGiuong == LoaiGiuong.DON) {
            return hangPhong == HangPhong.VIP ? PHONG_DON_VIP : PHONG_DON_THUONG;
        } else {
            return hangPhong == HangPhong.VIP ? PHONG_DOI_VIP : PHONG_DOI_THUONG;
        }
    }

    /**
     * Chuyển đổi từ chuỗi định danh hoặc chuỗi hiển thị.
     * Chặn triệt để các cấu hình ngoài phạm vi.
     */
    public static CauHinhLoaiPhong fromString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return PHONG_DON_THUONG;
        }
        String clean = text.trim();
        for (CauHinhLoaiPhong ch : CauHinhLoaiPhong.values()) {
            if (ch.name().equalsIgnoreCase(clean) || ch.tenHienThi.equalsIgnoreCase(clean)) {
                return ch;
            }
        }

        // Tương thích ngược với các tên cũ nếu có trong CSDL
        if (clean.equalsIgnoreCase("PHONG_THUONG") || clean.equalsIgnoreCase("Phòng Thường")) {
            return PHONG_DON_THUONG;
        }
        if (clean.equalsIgnoreCase("PHONG_VIP") || clean.equalsIgnoreCase("Phòng VIP")) {
            return PHONG_DON_VIP;
        }

        // Nhận diện theo từ khóa Đơn/Đôi + Thường/VIP
        String lower = clean.toLowerCase();
        boolean isDoi = lower.contains("đôi") || lower.contains("doi") || lower.contains("double");
        boolean isVip = lower.contains("vip");

        if (isDoi) {
            return isVip ? PHONG_DOI_VIP : PHONG_DOI_THUONG;
        } else {
            return isVip ? PHONG_DON_VIP : PHONG_DON_THUONG;
        }
    }
}
