// src/main/java/com/github/config/WebSocketInitializer.java
package com.github.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import javax.annotation.PostConstruct;

@Configuration
public class WebSocketInitializer {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostConstruct
    public void init() {
        WebSocketLogAppender.setMessagingTemplate(messagingTemplate);
    }
}