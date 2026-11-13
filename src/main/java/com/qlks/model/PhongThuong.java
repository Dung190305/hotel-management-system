package com.qlks.model;

import com.qlks.model.enums.CauHinhLoaiPhong;
import com.qlks.model.enums.HangPhong;
import com.qlks.model.enums.LoaiGiuong;
import com.qlks.model.enums.TrangThaiPhong;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

/**
 * Lớp đại diện cho Phòng Thường.
 * Kế thừa từ Phong, có bộ tiện nghi cơ bản chuẩn:
 * - Đệm cao su non
 * - Điều hòa
 * - Nhà tắm
 * - Nhà vệ sinh
 * - Tủ quần áo
 */
public class PhongThuong extends Phong {
    private static final long serialVersionUID = 1L;

    public static final List<String> TIEN_NGHI_CO_BAN = Collections.unmodifiableList(List.of(
            "Đệm cao su non",
            "Điều hòa",
            "Nhà tắm",
            "Nhà vệ sinh",
            "Tủ quần áo"
    ));

    private boolean coCuaSo;

    public PhongThuong() {
        super();
        this.coCuaSo = true;
    }

    public PhongThuong(String maPhong, String soPhong, int tang, BigDecimal donGiaNgay,
                       TrangThaiPhong trangThaiHienTai, boolean dangKinhDoanh, boolean coCuaSo) {
        super(maPhong, soPhong, tang, donGiaNgay, trangThaiHienTai, LoaiGiuong.DON, dangKinhDoanh);
        this.coCuaSo = coCuaSo;
    }

    public PhongThuong(String maPhong, String soPhong, int tang, BigDecimal donGiaNgay,
                       TrangThaiPhong trangThaiHienTai, LoaiGiuong loaiGiuong, boolean dangKinhDoanh, boolean coCuaSo) {
        super(maPhong, soPhong, tang, donGiaNgay, trangThaiHienTai, loaiGiuong, dangKinhDoanh);
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
    public HangPhong getHangPhong() {
        return HangPhong.THUONG;
    }

    @Override
    public CauHinhLoaiPhong getCauHinh() {
        return CauHinhLoaiPhong.tuLoaiGiuongVaHang(getLoaiGiuong(), HangPhong.THUONG);
    }

    @Override
    public List<String> getDanhSachTienNghi() {
        return TIEN_NGHI_CO_BAN;
    }

    @Override
    public String getLoaiPhong() {
        return getCauHinh().name();
    }

    @Override
    public String getTenLoaiPhongHienThi() {
        return getCauHinh().getTenHienThi();
    }

    public boolean isCoCuaSo() {
        return coCuaSo;
    }

    public void setCoCuaSo(boolean coCuaSo) {
        this.coCuaSo = coCuaSo;
    }
}
