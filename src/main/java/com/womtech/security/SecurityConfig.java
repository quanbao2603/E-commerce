package com.womtech.security;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	private final JwtService jwtService;
	private final TokenRevokeService revokeService;

	public SecurityConfig(JwtService jwtService, TokenRevokeService revokeService) {
		this.jwtService = jwtService;
		this.revokeService = revokeService;
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				// ✅ stateless + JwtAuthFilter: KHÔNG yêu cầu explicit save
				.securityContext(sc -> sc.requireExplicitSave(false))

				.authorizeHttpRequests(auth -> auth
						// 1) Static
						.requestMatchers("/css/**", "/js/**", "/img/**", "/images/**", "/static/**", "/webjars/**",
								"/favicon.ico")
						.permitAll().requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()

						// 2) Public pages
						.requestMatchers("/", "/auth/**", "/products/**", "/about", "/contact", "/error").permitAll()

						// 3) Đăng ký shop (OTP) — chỉ cần authenticated
						// Bao toàn bộ biến thể & ĐẶT TRƯỚC /vendor/**
						.requestMatchers("/vendor/register", "/vendor/register/", "/vendor/register/**",
								"/vendor/register/resend")
						.authenticated()

						// 4) WebSocket / chat
						.requestMatchers("/user/chat", "/user/chat-ws/**").authenticated()
						.requestMatchers("/vendor/chat", "/vendor/chat-ws/**").hasRole("VENDOR")

						// 5) REST cho chat
						.requestMatchers("/api/chat/**", "/api/chats/**").authenticated()

						// 6) Khu vực role-based
						.requestMatchers("/admin/**").hasRole("ADMIN").requestMatchers("/vendor/**").hasRole("VENDOR") // ⬅️
																														// để
																														// SAU
																														// /vendor/register*
						.requestMatchers("/shipper/**").hasRole("SHIPPER")

						// 7) Mặc định
						.anyRequest().permitAll())

				// Phân luồng lỗi: trả mã lỗi chuẩn (không redirect tới URL không có)
				.exceptionHandling(ex -> ex.authenticationEntryPoint((req, res, e) -> res.sendRedirect("/auth/login")) // chưa
																														// login
						.accessDeniedHandler((req, res, e) -> res.sendError(403)) // thiếu quyền
				)

				// JWT filter
				.addFilterBefore(new JwtAuthFilter(jwtService, revokeService),
						org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)

				.formLogin(form -> form.disable()).logout(logout -> logout.disable()).httpBasic(h -> h.disable());

		return http.build();
	}
}
