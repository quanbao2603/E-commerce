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
				.securityContext(sc -> sc.requireExplicitSave(true))

				.authorizeHttpRequests(auth -> auth
						// Static resources
						.requestMatchers("/css/**", "/js/**", "/img/**", "/images/**", "/static/**", "/webjars/**",
								"/favicon.ico")
						.permitAll().requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()

						// Public pages
						.requestMatchers("/", "/auth/**", "/products/**", "/about", "/contact", "/error").permitAll()

						// === WebSocket handshake (SockJS tạo thêm các path con) ===
						// User chat page + WS endpoint
						.requestMatchers("/user/chat").authenticated().requestMatchers("/user/chat-ws/**")
						.authenticated()

						// Vendor chat page + WS endpoint (cần role VENDOR)
						.requestMatchers("/vendor/chat").hasRole("VENDOR").requestMatchers("/vendor/chat-ws/**")
						.hasRole("VENDOR")

						// REST tạo/lấy chatId (nếu bạn dùng API /api/chat như đã thiết kế)
						.requestMatchers("/api/chat/**").authenticated()

						// Khu vực khác
						.requestMatchers("/admin/**").hasRole("ADMIN").requestMatchers("/vendor/**").hasRole("VENDOR")
						.requestMatchers("/shipper/**").hasRole("SHIPPER")

						// Cuối cùng
						.anyRequest().permitAll())

				// JWT filter cho mọi request (bao gồm WS handshake)
				.addFilterBefore(new JwtAuthFilter(jwtService, revokeService),
						org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)

				.formLogin(form -> form.disable()).logout(logout -> logout.disable()).httpBasic(h -> h.disable());

		return http.build();
	}
}
