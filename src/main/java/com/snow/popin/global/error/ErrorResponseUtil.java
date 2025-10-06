package com.snow.popin.global.error;

import com.snow.popin.global.constant.ErrorCode;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class ErrorResponseUtil {

    // ErrorCode를 사용한 JSON 에러 읃답 전송
    public static void sendErrorResponse(HttpServletResponse res, ErrorCode errorCode) throws IOException {
        if (res.isCommitted()) return;

        res.setStatus(errorCode.getHttpStatus().value());
        res.setCharacterEncoding("UTF-8");
        res.setContentType("application/json;charset=UTF-8");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String safeMsg = errorCode.getMessage().replace("\"", "\\\"");

        String jsonRes = String.format(
                "{\"success\":false,\"errorCode\":%d,\"message\":\"%s\",\"timestamp\":\"%s\"}",
                errorCode.getCode(),
                safeMsg,
                timestamp
        );

        res.getWriter().write(jsonRes);
        res.getWriter().flush();
    }

}
