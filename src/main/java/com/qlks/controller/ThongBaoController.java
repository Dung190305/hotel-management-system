package com.qlks.controller;

import com.qlks.model.ThongBaoDon;
import com.qlks.service.ThongBaoService;
import com.qlks.service.ThongBaoService.TrangThaiChuong;

import java.util.List;

/**
 * Controller xử lý logic thông báo chuông và đơn tới hạn.
 */
public class ThongBaoController extends BaseController {

    private final ThongBaoService thongBaoService;

    public ThongBaoController() {
        this.thongBaoService = new ThongBaoService();
    }

    public ThongBaoController(ThongBaoService thongBaoService) {
        this.thongBaoService = thongBaoService;
    }

    public List<ThongBaoDon> layDanhSachThongBaoToiHan() {
        return thongBaoService.layDanhSachThongBaoToiHan();
    }

    public int demSoLuongChuaDoc() {
        return thongBaoService.demSoLuongChuaDoc();
    }

    public int demTongSoLuongToiHan() {
        return thongBaoService.demTongSoLuongToiHan();
    }

    public TrangThaiChuong getTrangThaiChuong() {
        return thongBaoService.getTrangThaiChuong();
    }

    public void danhDauDaDoc(String id) {
        thongBaoService.danhDauDaDoc(id);
    }

    public void danhDauTatCaDaDoc() {
        thongBaoService.danhDauTatCaDaDoc();
    }

    public ThongBaoService getThongBaoService() {
        return thongBaoService;
    }
}
