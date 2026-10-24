package com.qlks.dao;

import java.util.List;
import java.util.Optional;

/**
 * Interface cơ sở dùng chung cho tất cả các DAO (Data Access Object).
 * Thể hiện nguyên lý TRỪU TƯỢNG (Interface) và GENERICS trong Java: BaseDAO<T, ID>.
 *
 * @param <T>  Kiểu thực thể Model (Entity)
 * @param <ID> Kiểu khóa chính (Primary Key - thường là String hoặc Long)
 */
public interface BaseDAO<T, ID> {

    /**
     * Tìm kiếm một thực thể theo khóa chính.
     * Sử dụng Optional để tránh lỗi NullPointerException (Best practice hiện đại).
     *
     * @param id Khóa chính
     * @return Optional chứa thực thể nếu tìm thấy, hoặc rỗng nếu không có
     */
    Optional<T> findById(ID id);

    /**
     * Lấy toàn bộ danh sách thực thể.
     *
     * @return List danh sách các đối tượng
     */
    List<T> findAll();

    /**
     * Lưu một thực thể mới vào cơ sở dữ liệu.
     *
     * @param entity Đối tượng cần lưu
     * @return true nếu lưu thành công, false nếu thất bại
     */
    boolean save(T entity);

    /**
     * Cập nhật thông tin thực thể đã có.
     *
     * @param entity Đối tượng cần cập nhật
     * @return true nếu cập nhật thành công, false nếu thất bại
     */
    boolean update(T entity);

    /**
     * Xóa một thực thể theo khóa chính.
     *
     * @param id Khóa chính
     * @return true nếu xóa thành công, false nếu thất bại
     */
    boolean delete(ID id);
}
