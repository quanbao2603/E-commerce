package com.womtech.security;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
						// Cho phép truy cập tất cả static resources
						.requestMatchers("/css/**", "/js/**", "/img/**", "/images/**", "/static/**", "/webjars/**", "/favicon.ico")
						.permitAll()
						.requestMatchers(PathRequest.toStaticResources().atCommonLocations())
						.permitAll()
						.requestMatchers("/", "/auth/**", "/products/**", "/about", "/contact", "/error").permitAll()
						.requestMatchers("/user/**").authenticated().requestMatchers("/admin/**").hasRole("ADMIN")
						.requestMatchers("/vendor/**").hasRole("VENDOR").requestMatchers("/shipper/**")
						.hasRole("SHIPPER").anyRequest().authenticated())
				.addFilterBefore(new JwtAuthFilter(jwtService, revokeService),
						org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
				.formLogin(form -> form.disable()).logout(logout -> logout.disable()).httpBasic(h -> h.disable());
		return http.build();
	}
}
