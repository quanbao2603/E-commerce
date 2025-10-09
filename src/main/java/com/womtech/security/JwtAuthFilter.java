package com.womtech.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.springframework.security.core.context.SecurityContextHolder.getContext;

public class JwtAuthFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final TokenRevokeService revokeService;

	public JwtAuthFilter(JwtService jwtService, TokenRevokeService revokeService) {
		this.jwtService = jwtService;
		this.revokeService = revokeService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
			throws ServletException, IOException {

		if (getContext().getAuthentication() == null) {
			String at = readCookie(req, "AT"); // access token trong cookie
			if (at != null && jwtService.isValidAccess(at) && !revokeService.isRevoked(at)) {
				String userId = jwtService.getUserId(at);
				List<String> roles = jwtService.getRoles(at);
				if (roles == null)
					roles = List.of();

				Collection<SimpleGrantedAuthority> auths = new ArrayList<>();
				for (String r : roles) {
					if (r != null && !r.isBlank()) {
						auths.add(new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()));
					}
				}

				// 👉 Sử dụng UsernamePasswordAuthenticationToken: authenticated = true
				var authentication = new UsernamePasswordAuthenticationToken(userId, null, auths);
				authentication.setDetails(req);
				getContext().setAuthentication(authentication);
			}
		}

		chain.doFilter(req, res);
	}

	private static String readCookie(HttpServletRequest req, String name) {
		Cookie[] cookies = req.getCookies();
		if (cookies == null)
			return null;
		for (Cookie c : cookies) {
			if (name.equals(c.getName()))
				return c.getValue();
		}
		return null;
	}
}
