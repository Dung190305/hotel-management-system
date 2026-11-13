package com.qlks.model;

import com.qlks.model.enums.CauHinhLoaiPhong;
import com.qlks.model.enums.HangPhong;
import com.qlks.model.enums.LoaiGiuong;
import com.qlks.model.enums.TrangThaiPhong;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lớp đại diện cho Phòng VIP.
 * Kế thừa từ Phong, sở hữu TOÀN BỘ tiện nghi của Phòng Thường và BỔ SUNG thêm:
 * - Tivi
 * - Bồn tắm
 * - Tủ lạnh
 */
public class PhongVIP extends Phong {
    private static final long serialVersionUID = 1L;

    public static final List<String> TIEN_NGHI_VIP_BO_SUNG = List.of("Tivi", "Bồn tắm", "Tủ lạnh");

    private boolean coBanCongViewBien;

    @NotNull(message = "Phụ phí dịch vụ VIP không được để trống")
    @PositiveOrZero(message = "Phụ phí dịch vụ VIP không được âm")
    private BigDecimal phiDichVuVip;

    public PhongVIP() {
        super();
        this.coBanCongViewBien = true;
        this.phiDichVuVip = BigDecimal.ZERO;
    }

    public PhongVIP(String maPhong, String soPhong, int tang, BigDecimal donGiaNgay,
                    TrangThaiPhong trangThaiHienTai, boolean dangKinhDoanh,
                    boolean coBanCongViewBien, BigDecimal phiDichVuVip) {
        super(maPhong, soPhong, tang, donGiaNgay, trangThaiHienTai, LoaiGiuong.DON, dangKinhDoanh);
        this.coBanCongViewBien = coBanCongViewBien;
        this.phiDichVuVip = phiDichVuVip != null ? phiDichVuVip : BigDecimal.ZERO;
    }

    public PhongVIP(String maPhong, String soPhong, int tang, BigDecimal donGiaNgay,
                    TrangThaiPhong trangThaiHienTai, LoaiGiuong loaiGiuong, boolean dangKinhDoanh,
                    boolean coBanCongViewBien, BigDecimal phiDichVuVip) {
        super(maPhong, soPhong, tang, donGiaNgay, trangThaiHienTai, loaiGiuong, dangKinhDoanh);
        this.coBanCongViewBien = coBanCongViewBien;
        this.phiDichVuVip = phiDichVuVip != null ? phiDichVuVip : BigDecimal.ZERO;
    }

    /**
     * Đa hình: Tính tiền phòng VIP.
     * Tiền phòng = (Đơn giá ngày + Phí dịch vụ VIP) * Số ngày lưu trú.
     * Ưu đãi: Khách ở từ 5 ngày trở lên được giảm giá 10% tổng tiền phòng.
     */
    @Override
    public BigDecimal tinhTienPhong(long soNgay) {
        long soNgayThucTe = Math.max(1, soNgay);
        BigDecimal giaMoiNgay = getDonGiaNgay().add(phiDichVuVip);
        BigDecimal tong = giaMoiNgay.multiply(BigDecimal.valueOf(soNgayThucTe));

        if (soNgayThucTe >= 5) {
            BigDecimal giamGia = tong.multiply(new BigDecimal("0.10"));
            tong = tong.subtract(giamGia);
        }
        return tong.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public HangPhong getHangPhong() {
        return HangPhong.VIP;
    }

    @Override
    public CauHinhLoaiPhong getCauHinh() {
        return CauHinhLoaiPhong.tuLoaiGiuongVaHang(getLoaiGiuong(), HangPhong.VIP);
    }

    /**
     * Thể hiện tính ĐA HÌNH và KẾ THỪA:
     * Phòng VIP thừa hưởng toàn bộ danh mục tiện nghi cơ bản của Phòng Thường và bổ sung thêm tiện nghi VIP.
     */
    @Override
    public List<String> getDanhSachTienNghi() {
        List<String> list = new ArrayList<>(PhongThuong.TIEN_NGHI_CO_BAN);
        list.addAll(TIEN_NGHI_VIP_BO_SUNG);
        return Collections.unmodifiableList(list);
    }

    @Override
    public String getLoaiPhong() {
        return getCauHinh().name();
    }

    @Override
    public String getTenLoaiPhongHienThi() {
        return getCauHinh().getTenHienThi();
    }

    public boolean isCoBanCongViewBien() {
        return coBanCongViewBien;
    }

    public void setCoBanCongViewBien(boolean coBanCongViewBien) {
        this.coBanCongViewBien = coBanCongViewBien;
    }

    public BigDecimal getPhiDichVuVip() {
        return phiDichVuVip;
    }

    public void setPhiDichVuVip(BigDecimal phiDichVuVip) {
        this.phiDichVuVip = phiDichVuVip;
    }
}
