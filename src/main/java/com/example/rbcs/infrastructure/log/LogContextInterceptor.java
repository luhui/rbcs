package com.example.rbcs.infrastructure.log;

import org.slf4j.MDC;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

public class LogContextInterceptor implements WebRequestInterceptor {
    @Override
    public void preHandle(WebRequest request) throws Exception {
        final var traceId = request.getHeader("x-apm-traceid");
        MDC.put("traceId", traceId);
        final var userId = request.getHeader("x-auth-userid");
        MDC.put("userId", userId);
        if (request.getContextPath().startsWith("/api/v1/transactions/")) {
            // 取得transactionId /api/v1/transactions/{transactionId}/xx
            var transactionId = request.getContextPath().substring("/api/v1/transactions/".length());
            var index = transactionId.indexOf('/');
            if (index > 0) {
                transactionId = transactionId.substring(0, index);
            }
            MDC.put("transactionId", transactionId);
        }
    }

    @Override
    public void postHandle(WebRequest request, ModelMap model) throws Exception {

    }

    @Override
    public void afterCompletion(WebRequest request, Exception ex) throws Exception {

    }
}
