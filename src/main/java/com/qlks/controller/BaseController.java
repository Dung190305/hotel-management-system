package com.qlks.controller;

import com.qlks.exception.BusinessException;
import com.qlks.exception.ValidationException;
import com.qlks.util.UIUtil;

import java.awt.Component;

/**
 * Controller cơ sở xử lý ngoại lệ tập trung cho giao diện Swing.
 */
public abstract class BaseController {

    public void handleException(Component parent, Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null && cause != cause.getCause()) {
            cause = cause.getCause();
        }
        if (cause instanceof BusinessException || cause instanceof ValidationException) {
            UIUtil.showError(parent, cause.getMessage());
        } else {
            UIUtil.showError(parent, "Lỗi hệ thống: " + cause.getMessage());
        }
    }
}

