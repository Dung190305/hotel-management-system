package com.qlks.util;

import com.qlks.exception.ValidationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lớp tiện ích kiểm tra dữ liệu đối tượng bằng Hibernate Validator (Jakarta Bean Validation).
 */
public final class ValidationUtil {
    private static final Validator VALIDATOR;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = factory.getValidator();
        }
    }

    private ValidationUtil() {
        // Utility class, không khởi tạo trực tiếp
    }

    /**
     * Kiểm tra tính hợp lệ của một đối tượng.
     * Nếu có lỗi ràng buộc dữ liệu, ném ra ValidationException kèm danh sách thông báo.
     *
     * @param object Đối tượng cần kiểm tra (Model)
     * @param <T>    Kiểu đối tượng
     * @throws ValidationException nếu có lỗi vi phạm
     */
    public static <T> void validate(T object) {
        if (object == null) {
            throw new ValidationException("Đối tượng kiểm tra không được để trống (null).");
        }

        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(object);
        if (!violations.isEmpty()) {
            List<String> loiList = new ArrayList<>();
            for (ConstraintViolation<T> violation : violations) {
                loiList.add(violation.getMessage());
            }
            throw new ValidationException(loiList);
        }
    }
}
