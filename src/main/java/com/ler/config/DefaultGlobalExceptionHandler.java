package com.ler.config;

import com.alibaba.fastjson.JSON;
import com.ler.exception.UserFriendlyException;
import com.ler.vo.HttpResult;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * @author lww
 */
@ResponseBody
@ControllerAdvice
public class DefaultGlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultGlobalExceptionHandler.class);

    private static final Integer MAX_DEPTH = 3;

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {
        HttpResult httpResult = HttpResult.failure(status.is5xxServerError() ? "服务器异常！" : "参数错误！");
        LOGGER.error("handleException, ex caught, contextPath={}, httpResult={}, ex.msg={}", request.getContextPath(), JSON.toJSONString(httpResult), ex.getMessage());
        return super.handleExceptionInternal(ex, httpResult, headers, status, request);
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity handleException(HttpServletRequest request, Exception ex) {
        boolean is5xxServerError;
        HttpStatus httpStatus;
        HttpResult httpResult;
        if (ex instanceof IllegalArgumentException) {
            // 参数判断用到了Spring断言，requireTrue会抛出 IllegalArgumentException，客户端处理不了5xx异常，所以还是返回200
            //XXX 正常逻辑的断言不能用Spring断言，避免出错了不知道
            httpStatus = HttpStatus.OK;
            is5xxServerError = false;
            httpResult = HttpResult.failure(ex.getMessage());
        } else if (ex instanceof UserFriendlyException) {
            // 参数判断用到了Spring断言，requireTrue会抛出 IllegalArgumentException，客户端处理不了5xx异常，所以还是返回200
            //XXX 正常逻辑的断言不能用Spring断言，避免出错了不知道
            httpStatus = HttpStatus.OK;
            is5xxServerError = false;
            httpResult = HttpResult.failure(ex.getMessage());
        } else {
            httpStatus = HttpStatus.OK;
            is5xxServerError = true;
            httpResult = HttpResult.failure("服务器异常！");
        }
        if (is5xxServerError) {
            LOGGER.error("handleException, ex caught, uri={}, httpResult={}", request.getRequestURI(), JSON.toJSONString(httpResult), ex);
        } else {
            List<StackTraceElement> stackTraceElements = Arrays.asList(ex.getStackTrace());
            if (stackTraceElements.size() > MAX_DEPTH) {
                stackTraceElements = stackTraceElements.subList(0, MAX_DEPTH);
            }
            ex.setStackTrace(stackTraceElements.toArray(new StackTraceElement[stackTraceElements.size()]));
            LOGGER.error("handleException, ex caught, uri={}, httpResult={}", request.getRequestURI(), JSON.toJSONString(httpResult), ex);
        }
        return new ResponseEntity<>(httpResult, httpStatus);
    }

}