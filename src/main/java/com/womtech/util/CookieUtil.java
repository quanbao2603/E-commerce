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
		Cookie cookie = new Cookie(name, "");
		cookie.setPath("/");
		cookie.setMaxAge(0);
		response.addCookie(cookie);

		response.addHeader("Set-Cookie", name + "=; Path=/; Max-Age=0");
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
