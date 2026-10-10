package ru.yandex.practicum.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// Превращает внешнюю конфигурацию (app.security.users: ...) в список учебных
// пользователей. Логины/пароли/роли остаются в конфиге, а не в коде
@Getter
@Setter
@ConfigurationProperties(prefix = "app.security")
public class GatewaySecurityProperties {

    private List<SecurityUser> users = List.of();

    @Getter
    @Setter
    public static class SecurityUser {
        private String username;
        private String password;
        private List<String> roles = List.of();
    }
}
