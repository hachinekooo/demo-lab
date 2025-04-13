// src/main/java/com/github/config/WebSocketLogAppender.java
package com.github.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public class WebSocketLogAppender extends AppenderBase<ILoggingEvent> {
    private static SimpMessagingTemplate messagingTemplate;

    public static void setMessagingTemplate(SimpMessagingTemplate messagingTemplate) {
        WebSocketLogAppender.messagingTemplate = messagingTemplate;
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (messagingTemplate != null) {
            String message = event.getFormattedMessage();
            messagingTemplate.convertAndSend("/topic/logs", message);
        }
    }
}