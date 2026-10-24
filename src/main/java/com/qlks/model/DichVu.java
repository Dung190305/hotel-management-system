package com.qlks.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Lớp đại diện cho danh mục Dịch vụ khách sạn (ăn uống, giặt là, minibar...).
 */
public class DichVu implements Serializable {
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Mã dịch vụ không được để trống")
    private String maDichVu;

    @NotBlank(message = "Tên dịch vụ không được để trống")
    @Size(max = 100, message = "Tên dịch vụ không quá 100 ký tự")
    private String tenDichVu;

    @NotNull(message = "Đơn giá hiện tại không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Đơn giá dịch vụ phải lớn hơn 0")
    private BigDecimal donGiaHienTai;

    private boolean dangKinhDoanh;

    public DichVu() {
        this.dangKinhDoanh = true;
        this.donGiaHienTai = BigDecimal.ZERO;
    }

    public DichVu(String maDichVu, String tenDichVu, BigDecimal donGiaHienTai, boolean dangKinhDoanh) {
        this.maDichVu = maDichVu;
        this.tenDichVu = tenDichVu;
        this.donGiaHienTai = donGiaHienTai != null ? donGiaHienTai : BigDecimal.ZERO;
        this.dangKinhDoanh = dangKinhDoanh;
    }

    public String getMaDichVu() {
        return maDichVu;
    }

    public void setMaDichVu(String maDichVu) {
        this.maDichVu = maDichVu;
    }

    public String getTenDichVu() {
        return tenDichVu;
    }

    public void setTenDichVu(String tenDichVu) {
        this.tenDichVu = tenDichVu;
    }

    public BigDecimal getDonGiaHienTai() {
        return donGiaHienTai;
    }

    public void setDonGiaHienTai(BigDecimal donGiaHienTai) {
        this.donGiaHienTai = donGiaHienTai;
    }

    public boolean isDangKinhDoanh() {
        return dangKinhDoanh;
    }

    public void setDangKinhDoanh(boolean dangKinhDoanh) {
        this.dangKinhDoanh = dangKinhDoanh;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DichVu dichVu = (DichVu) o;
        return Objects.equals(maDichVu, dichVu.maDichVu);
    }

    @Override
    public int hashCode() {
        return Objects.hash(maDichVu);
    }

    @Override
    public String toString() {
        return tenDichVu + " (" + donGiaHienTai.toPlainString() + " VNĐ)";
    }
}
