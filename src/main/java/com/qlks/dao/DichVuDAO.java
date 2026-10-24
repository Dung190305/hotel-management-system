package com.qlks.dao;

import com.qlks.model.DichVu;

import java.util.List;

/**
 * Interface DAO dành cho bảng dich_vu.
 */
public interface DichVuDAO extends BaseDAO<DichVu, String> {

    /**
     * Lấy danh sách dịch vụ đang kinh doanh (sẵn sàng phục vụ khách).
     */
    List<DichVu> findDangKinhDoanh();

    /**
     * Tìm kiếm dịch vụ theo tên.
     */
    List<DichVu> search(String keyword);
}
