package com.womtech.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
//                // Cho phép truy cập không cần đăng nhập
//                .requestMatchers("/", "/home", "/register", "/profile", "/user/index", "/css/**", "/js/**", "/images/**").permitAll()
//                // Các trang admin yêu cầu đăng nhập
//                .requestMatchers("/admin/**").authenticated()

                .anyRequest().permitAll()
            )
            // Cấu hình form login
            .formLogin(form -> form
                .loginPage("/auth/login")       // Đường dẫn tới trang login của bạn
                .permitAll()
            )
            // Cho phép logout
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );

        return http.build();
    }
}
