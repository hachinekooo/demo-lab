package com.github.service.impl;

import com.github.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Value("${dyn.user.name}")
    private String userName;

    @Override
    public String getName() {
        log.info("用户名：{}", userName);
        return "用户名：" + userName;
    }
}
