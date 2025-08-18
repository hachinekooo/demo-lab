package com.github.config;

import com.github.filter.ApiAccessLogFilter;
import com.github.filter.CacheRequestBodyFilter;
import com.github.interceptor.ApiAccessLogInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.Filter;

@Slf4j
@AutoConfiguration
public class WebAutoConfiguration implements WebMvcConfigurer {

    @Bean
    public FilterRegistrationBean<CacheRequestBodyFilter> requestBodyCacheFilter() {
        log.info("注册 CacheRequestBodyFilter");
        return createFilterBean(new CacheRequestBodyFilter(), 1);
    }

    @Bean
    public FilterRegistrationBean<ApiAccessLogFilter> apiAccessLogFilter() {
        log.info("注册 ApiAccessLogFilter");
        return createFilterBean(new ApiAccessLogFilter(), 2);
    }


    public static <T extends Filter> FilterRegistrationBean<T> createFilterBean(T filter, Integer order) {
        FilterRegistrationBean<T> bean = new FilterRegistrationBean<>(filter);
        bean.setOrder(order);
        return bean;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("注册 ApiAccessLogInterceptor");
        registry.addInterceptor(new ApiAccessLogInterceptor());
    }
}
