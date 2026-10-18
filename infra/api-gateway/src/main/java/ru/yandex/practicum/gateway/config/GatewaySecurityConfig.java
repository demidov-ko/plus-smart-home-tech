package ru.yandex.practicum.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewaySecurityProperties.class)
@RequiredArgsConstructor
public class GatewaySecurityConfig {

    private final GatewaySecurityProperties securityProperties;

    //  хеширование паролей
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        List<UserDetails> users = securityProperties.getUsers().stream()
                .map(u -> User.withUsername(u.getUsername())
                        .password(passwordEncoder.encode(u.getPassword()))
                        .roles(u.getRoles().toArray(new String[0]))
                        .build())
                .toList();

        // MapReactiveUserDetailsService хранит пользователей в ConcurrentHashMap и умеет работать асинхронно
        return new MapReactiveUserDetailsService(users);
    }

    // Возвращает источник конфигурации CORS, который Spring Security будет исп. для проверки запросов из браузера
    @Bean
    CorsConfigurationSource corsConfigurationSource(GlobalCorsProperties globalCorsProperties) {
        // Берёт глобальные CORS-настройки из GlobalCorsProperties (в api-gateway.yml)
        // Регистрирует их в UrlBasedCorsConfigurationSource
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        globalCorsProperties.getCorsConfigurations()
                .forEach(source::registerCorsConfiguration);
        return source;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // отключает CSRF
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                // отключает форму входа (логин-страница)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                // включает HTTP Basic Auth (логин/пароль в заголовке Authorization: Basic ...)
                // Это то, что будет проверять учётные данные
                .httpBasic(withDefaults())
                // подключает CORS-конфигурацию
                .cors(withDefaults())
                // NoOp означает, что контекст безопасности не сохраняется между запросами.
                // Каждый запрос проверяется заново
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                // Правила авторизации
                .authorizeExchange(exchanges -> exchanges
                        // Preflight - всегда открыт, иначе браузер не дойдёт до реального запроса
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // открывает доступ к документации Swagger и OpenAPI для всех
                        .pathMatchers(
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/*/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        // Публичный каталог/остатки - только чтение
                        .pathMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/inventory/**").permitAll()
                        // Доступ к списку всех заказов (GET /api/orders) разрешён только роли ADMIN
                        .pathMatchers(HttpMethod.GET, "/api/orders").hasRole("ADMIN")
                        // сюда долетают только изменяющие методы (POST/PUT/PATCH/DELETE)
                        .pathMatchers("/api/products/**").hasRole("ADMIN")
                        .pathMatchers("/api/categories/**").hasRole("ADMIN")
                        .pathMatchers("/api/inventory/**").hasRole("ADMIN")
                        // Создание заказа и просмотр своих заказов - только для роли USER
                        .pathMatchers(HttpMethod.POST, "/api/orders/**").hasRole("USER")
                        .pathMatchers(HttpMethod.GET, "/api/orders/**").hasRole("USER")
                        // Всё остальное закрыто - даже для ADMIN, если маршрут не
                        // попал ни в одно явное правило выше
                        .anyExchange().denyAll()
                )
                .build();
    }
}
