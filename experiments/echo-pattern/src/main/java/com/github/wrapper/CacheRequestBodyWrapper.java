package com.github.wrapper;

import cn.hutool.extra.servlet.ServletUtil;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;


/**
 * 一个具体的包装器类，用于包装 HttpServletRequest 对象，使其支持多次读取请求体。
 * 这个类继承自 HttpServletRequestWrapper，它允许我们在不修改原始请求对象的情况下，对其进行扩展和增强。
 *
 * @author wangwenpeng
 * @date 2025/05/24
 */
public class CacheRequestBodyWrapper extends HttpServletRequestWrapper {

    /**
     * 读取请求体，然后缓存到 body 变量中
     * 这样就可以多次读取了
     */
    private final byte[] body;

    public CacheRequestBodyWrapper(HttpServletRequest request) {
        super(request);

        this.body = ServletUtil.getBodyBytes(request);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(this.getInputStream()));
    }


    /**
     * 重写方法，读取的时候从我们定义的 body 变量中读取
     *
     * @return {@link ServletInputStream }
     */
    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(body);
        // 返回 ServletInputStream
        return new ServletInputStream() {

            @Override
            public int read() {
                return inputStream.read();
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener) {}

            @Override
            public int available() {
                return body.length;
            }

        };
    }

}
