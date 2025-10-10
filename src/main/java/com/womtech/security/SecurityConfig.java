package com.womtech.security;

import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
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
				.authorizeHttpRequests(auth -> auth.requestMatchers(PathRequest.toStaticResources().atCommonLocations())
						.permitAll().requestMatchers("/", "/auth/**", "/products/**", "/error").permitAll()
						.requestMatchers("/user/**").authenticated() // ✅ cho phép user đã login vào
						.requestMatchers("/admin/**").hasRole("ADMIN").requestMatchers("/vendor/**").hasRole("VENDOR")
						.requestMatchers("/shipper/**").hasRole("SHIPPER").anyRequest().authenticated())
				.addFilterBefore(new JwtAuthFilter(jwtService, revokeService),
						org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
				.formLogin(form -> form.disable())
				.logout(logout -> logout.logoutUrl("/auth/logout").logoutSuccessUrl("/auth/login?logout").permitAll())
				.httpBasic(Customizer.withDefaults());
		return http.build();
	}

}
