package com.qlks.exception;

import java.util.Collections;
import java.util.List;

/**
 * Ngoại lệ ném ra khi đối tượng vi phạm ràng buộc Hibernate Validator (Bean Validation).
 */
public class ValidationException extends HotelException {
    private final List<String> danhSachLoi;

    public ValidationException(String message) {
        super(message);
        this.danhSachLoi = Collections.singletonList(message);
    }

    public ValidationException(List<String> danhSachLoi) {
        super(String.join("; ", danhSachLoi));
        this.danhSachLoi = danhSachLoi != null ? danhSachLoi : Collections.emptyList();
    }

    public List<String> getDanhSachLoi() {
        return danhSachLoi;
    }
}
