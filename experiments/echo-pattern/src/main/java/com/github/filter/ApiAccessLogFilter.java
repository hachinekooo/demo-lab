package com.github.filter;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.extra.servlet.ServletUtil;
import com.github.annotation.ApiAccessLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Slf4j
public class ApiAccessLogFilter extends OncePerRequestFilter {

    public static final String ATTRIBUTE_HANDLER_METHOD = "HANDLER_METHOD";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 静态资源直接放行，不记录日志
        if (isStaticResource(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 非静态资源，记录访问日志
        processApiRequest(request, response, filterChain);
    }

    /**
     * 处理 API 请求，记录访问日志
     */
    private void processApiRequest(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        LocalDateTime beginTime = LocalDateTime.now();
        Map<String, String> queryString = ServletUtil.getParamMap(request);
        String requestBody = ServletUtil.getBody(request);

        try {
            // 执行下一个过滤器
            filterChain.doFilter(request, response);
            // 成功处理，记录正常日志
            createApiAccessLog(request, beginTime, queryString, requestBody, null);
        } catch (Exception ex) {
            // 异常处理，记录异常日志
            createApiAccessLog(request, beginTime, queryString, requestBody, ex);
            throw ex;
        }
    }

    /**
     * 创建 API 访问日志
     */
    private boolean createApiAccessLog(HttpServletRequest request, LocalDateTime beginTime,
                                       Map<String, String> queryString, String requestBody, Exception ex) {
        try {
            HandlerMethod handlerMethod = (HandlerMethod) request.getAttribute(ATTRIBUTE_HANDLER_METHOD);

            // 检查是否需要记录日志
            if (!shouldRecordLog(handlerMethod)) {
                return false;
            }

            // 获取请求基本信息
            String method = request.getMethod();
            String uri = request.getRequestURI();
            long costTime = LocalDateTimeUtil.between(beginTime, LocalDateTime.now(), ChronoUnit.MILLIS);

            // 记录访问日志
            logAccessInfo(method, uri, costTime, queryString, requestBody, handlerMethod, ex);

        } catch (Throwable th) {
            log.error("[AccessLogError][url({})] 记录访问日志时发生异常: {}", request.getRequestURI(), th.getMessage(), th);
        }
        return true;
    }

    /**
     * 判断是否需要记录日志
     */
    private boolean shouldRecordLog(HandlerMethod handlerMethod) {
        if (handlerMethod == null) {
            return true; // 默认记录
        }

        ApiAccessLog accessLogAnnotation = handlerMethod.getMethodAnnotation(ApiAccessLog.class);
        return accessLogAnnotation == null || BooleanUtil.isTrue(accessLogAnnotation.enable());
    }

    /**
     * 记录访问信息
     */
    private void logAccessInfo(String method, String uri, long costTime,
                              Map<String, String> queryString, String requestBody,
                              HandlerMethod handlerMethod, Exception ex) {

        // 记录基本请求信息
        log.info("[AccessLog][耗时: {}ms][{}][{}]", costTime, method, uri);
        log.info("[AccessLog][请求参数: {}]", queryString);
        log.info("[AccessLog][请求体: {}]", requestBody);

        // 记录处理方法信息
        if (handlerMethod != null) {
            String className = handlerMethod.getBean().getClass().getName();
            String methodName = handlerMethod.getMethod().getName();
            log.info("[AccessLog][执行方法: {}#{}]", className, methodName);
        }

        // 记录异常信息
        if (ex != null) {
            log.error("[AccessLog][异常: {}]", ex.getMessage(), ex);
        }
    }

    /**
     * 判断是否为静态资源
     */
    private boolean isStaticResource(HttpServletRequest request) {
        if (request == null) {
            return false;
        }

        String uri = request.getRequestURI();
        if (uri == null) {
            return false;
        }

        // 判断常见的静态资源类型
        return uri.endsWith(".css") ||
               uri.endsWith(".js") ||
               uri.endsWith(".png") ||
               uri.endsWith(".jpg") ||
               uri.endsWith(".jpeg") ||
               uri.endsWith(".gif") ||
               uri.endsWith(".svg") ||
               uri.endsWith(".ico") ||
               uri.endsWith(".woff") ||
               uri.endsWith(".woff2") ||
               uri.endsWith(".ttf") ||
               uri.endsWith(".eot") ||
               uri.endsWith(".map") ||
               uri.startsWith("/css/") ||
               uri.startsWith("/js/") ||
               uri.startsWith("/images/") ||
               uri.startsWith("/static/") ||
               uri.startsWith("/assets/");
    }
}
