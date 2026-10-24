package com.qlks.model;

import com.qlks.model.enums.TrangThaiPhong;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Lớp đại diện cho Phòng Thường (Standard Room).
 * Kế thừa từ Phong.
 */
public class PhongThuong extends Phong {
    private static final long serialVersionUID = 1L;

    private boolean coCuaSo;

    public PhongThuong() {
        super();
        this.coCuaSo = true;
    }

    public PhongThuong(String maPhong, String soPhong, int tang, BigDecimal donGiaNgay,
                       TrangThaiPhong trangThaiHienTai, boolean dangKinhDoanh, boolean coCuaSo) {
        super(maPhong, soPhong, tang, donGiaNgay, trangThaiHienTai, dangKinhDoanh);
        this.coCuaSo = coCuaSo;
    }

    /**
     * Đa hình: Tính tiền phòng thường.
     * Chính sách:
     * - Tối thiểu 1 ngày nếu ở trong ngày (soNgay <= 0).
     * - Nếu khách ở từ 7 ngày trở lên: giảm giá 5% trên tiền phòng.
     */
    @Override
    public BigDecimal tinhTienPhong(long soNgay) {
        long soNgayThucTe = Math.max(1, soNgay);
        BigDecimal tong = getDonGiaNgay().multiply(BigDecimal.valueOf(soNgayThucTe));

        if (soNgayThucTe >= 7) {
            BigDecimal giamGia = tong.multiply(new BigDecimal("0.05"));
            tong = tong.subtract(giamGia);
        }
        return tong.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getLoaiPhong() {
        return "PHONG_THUONG";
    }

    @Override
    public String getTenLoaiPhongHienThi() {
        return "Phòng Thường";
    }

    public boolean isCoCuaSo() {
        return coCuaSo;
    }

    public void setCoCuaSo(boolean coCuaSo) {
        this.coCuaSo = coCuaSo;
    }
}
