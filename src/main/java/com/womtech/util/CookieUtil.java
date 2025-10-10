package com.womtech.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class CookieUtil {

	private CookieUtil() {

	}

	public static void add(HttpServletResponse response, String name, String value, int maxAgeSec, boolean httpOnly,
			boolean secure, String sameSite) {

		Cookie cookie = new Cookie(name, value);
		cookie.setPath("/");
		cookie.setMaxAge(maxAgeSec);
		cookie.setHttpOnly(httpOnly);
		cookie.setSecure(secure);

		response.addCookie(cookie);

		if (sameSite != null && !sameSite.isBlank()) {
			String headerValue = buildHeaderValue(name, value, maxAgeSec, httpOnly, secure, sameSite);
			response.addHeader("Set-Cookie", headerValue);
		}
	}

	public static Cookie get(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null)
			return null;
		for (Cookie c : cookies) {
			if (name.equals(c.getName()))
				return c;
		}
		return null;
	}

	public static void delete(HttpServletResponse response, String name) {
		// Xóa cookie với nhiều cách khác nhau để đảm bảo browser nhận diện
		
		// 1. Xóa với path "/"
		Cookie cookie1 = new Cookie(name, "");
		cookie1.setPath("/");
		cookie1.setMaxAge(0);
		response.addCookie(cookie1);

		// 2. Xóa với domain "localhost"
		Cookie cookie2 = new Cookie(name, "");
		cookie2.setPath("/");
		cookie2.setMaxAge(0);
		cookie2.setDomain("localhost");
		response.addCookie(cookie2);

		// 3. Xóa với domain ".localhost" (subdomain)
		Cookie cookie3 = new Cookie(name, "");
		cookie3.setPath("/");
		cookie3.setMaxAge(0);
		cookie3.setDomain(".localhost");
		response.addCookie(cookie3);

		// 4. Thêm headers Set-Cookie với nhiều format khác nhau
		response.addHeader("Set-Cookie", name + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict");
		response.addHeader("Set-Cookie", name + "=; Path=/; Max-Age=0; Domain=localhost; HttpOnly; SameSite=Strict");
		response.addHeader("Set-Cookie", name + "=; Path=/; Max-Age=0; Domain=.localhost; HttpOnly; SameSite=Strict");
		
		// 5. Thêm với Expires header (cho browser cũ)
		response.addHeader("Set-Cookie", name + "=; Path=/; Expires=Thu, 01 Jan 1970 00:00:00 GMT; HttpOnly");
	}

	private static String buildHeaderValue(String name, String value, int maxAgeSec, boolean httpOnly, boolean secure,
			String sameSite) {
		StringBuilder sb = new StringBuilder();
		sb.append(name).append('=').append(value).append("; Path=/").append("; Max-Age=").append(maxAgeSec);

		if (httpOnly)
			sb.append("; HttpOnly");
		if (secure)
			sb.append("; Secure");
		sb.append("; SameSite=").append(sameSite);

		return sb.toString();
	}
}
