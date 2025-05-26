package com.github.Controller;

import com.github.annotation.ApiAccessLog;
import com.github.bean.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api-access-test/api/")
public class ApiAccessController {

    @ApiAccessLog(enable = true, description = "测试接口")
    @PostMapping("/test")
    public ResponseEntity test(@RequestBody UserVO userVO) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello, " + userVO.getName());
        response.put("user", userVO);
        return ResponseEntity.ok(response);
    }
}