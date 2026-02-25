package com.api.chatstack.config;

import com.chatstack.dto.Session;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import ua_parser.Client;
import ua_parser.Parser;

import java.io.IOException;

@Component
@RequestScope
@Getter
public class ClientRequestContext {
    private final String userAgent;
    private final String clientIp;

    public ClientRequestContext(HttpServletRequest request) {
        this.clientIp = extractClientIp(request);
        this.userAgent = request.getHeader("User-Agent");
    }

    public Session.DeviceTypeEnum getDeviceType() {
        if (userAgent == null) {
            return Session.DeviceTypeEnum.UNKNOWN;
        } else if (userAgent.contains("Mobile") || userAgent.contains("Android") || userAgent.contains("iPhone")) {
            return Session.DeviceTypeEnum.MOBILE;
        } else if (userAgent.contains("Tablet") || userAgent.contains("iPad")) {
            return Session.DeviceTypeEnum.TABLET;
        } else {
            return Session.DeviceTypeEnum.DESKTOP;
        }
    }

    public String extractDeviceName(String userAgent) {
        return new Parser().parse(userAgent).device.family;
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
