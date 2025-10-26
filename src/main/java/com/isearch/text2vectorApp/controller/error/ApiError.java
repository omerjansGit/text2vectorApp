package com.isearch.text2vectorApp.controller.error;

import java.time.LocalDateTime;

public record ApiError(LocalDateTime timestamp, int status, String error, String message, String path) {
}
