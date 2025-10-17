package com.womtech.security;

import com.womtech.util.CookieUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
@Order(10) // chạy sau các filter khác nếu có
public class RoleGuardFilter extends OncePerRequestFilter {

	private static final String CK_AT = "AT";

	private final JwtService jwtService;
	private final TokenRevokeService tokenRevokeService;

	public RoleGuardFilter(JwtService jwtService, TokenRevokeService tokenRevokeService) {
		this.jwtService = jwtService;
		this.tokenRevokeService = tokenRevokeService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String path = request.getRequestURI();

		// Bỏ qua các path công khai
		if (isPublic(path)) {
			filterChain.doFilter(request, response);
			return;
		}

		// Nếu yêu cầu role đặc biệt thì kiểm tra
		String requiredRole = requiredRoleFor(path);
		if (requiredRole == null) {
			filterChain.doFilter(request, response);
			return;
		}

		// Lấy roles từ JWT (ưu tiên) hoặc session fallback
		Set<String> roles = extractRoles(request);

		if (roles.contains(requiredRole)) {
			filterChain.doFilter(request, response);
		} else {
			// Nếu chưa đăng nhập → chuyển về login, nếu đã đăng nhập nhưng thiếu quyền →
			// 403
			if (roles.isEmpty()) {
				// chưa auth
				response.sendRedirect("/auth/login");
			} else {
				// có auth nhưng thiếu quyền
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				response.sendRedirect("/error/403");
			}
		}
	}

	private boolean isPublic(String path) {
		// Nơi bạn cho phép truy cập tự do
		return path.startsWith("/auth") || path.startsWith("/assets") || path.startsWith("/css")
				|| path.startsWith("/js") || path.equals("/") || path.startsWith("/product")
				|| path.startsWith("/category");
	}

	private String requiredRoleFor(String path) {
		if (path.startsWith("/admin"))
			return "ADMIN";
		if (path.startsWith("/vendor"))
			return "VENDOR";
		if (path.startsWith("/shipper"))
			return "SHIPPER";
		return null; // không yêu cầu role cụ thể
	}

	@SuppressWarnings("unchecked")
	private Set<String> extractRoles(HttpServletRequest request) {
		// 1) JWT trong cookie AT
		Cookie at = CookieUtil.get(request, CK_AT);
		if (at != null && jwtService.isValidAccess(at.getValue()) && !tokenRevokeService.isRevoked(at.getValue())) {
			List<String> r = jwtService.getRoles(at.getValue()); // <--- cần method này
			if (r != null)
				return toUpperSet(r);
		}

		// 2) Fallback session
		HttpSession session = request.getSession(false);
		if (session != null) {
			Object obj = session.getAttribute("CURRENT_ROLES");
			if (obj instanceof List<?>) {
				return toUpperSet((List<String>) obj);
			}
		}
		return Collections.emptySet();
	}

	private Set<String> toUpperSet(List<String> roles) {
		Set<String> rs = new HashSet<>();
		for (String s : roles) {
			if (s != null)
				rs.add(s.toUpperCase(Locale.ROOT));
		}
		return rs;
	}
}
