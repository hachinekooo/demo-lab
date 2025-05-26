package com.github.filter;

import com.github.wrapper.CacheRequestBodyWrapper;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 *
 * 一个缓存请求体的过滤器，用于缓存请求体的内容。
 *
 * @author wangwenpeng
 * @date 2025/05/24
 */
public class CacheRequestBodyFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        filterChain.doFilter(new CacheRequestBodyWrapper(request), response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String contentType = request.getContentType();
        if (contentType == null) {
            return true; // 没有 Content-Type，比如 GET 请求
        }
        // 只缓存 JSON 和 form 表单请求
        return !(MediaType.APPLICATION_JSON_VALUE.equals(contentType)
                || MediaType.APPLICATION_FORM_URLENCODED_VALUE.equals(contentType));
    }

}
