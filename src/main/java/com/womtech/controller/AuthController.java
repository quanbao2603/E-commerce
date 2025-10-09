package com.womtech.controller;

import com.womtech.dto.request.auth.LoginRequest;
import com.womtech.dto.request.auth.RegisterRequest;
import com.womtech.dto.response.auth.LoginResponse;
import com.womtech.dto.response.auth.RegisterResponse;
import com.womtech.security.JwtService;
import com.womtech.security.TokenRevokeService;
import com.womtech.service.UserService;
import com.womtech.util.CookieUtil;
import com.womtech.util.EmailUtil;
import com.womtech.util.PasswordUtil;
import com.womtech.util.RememberMeUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final UserService userService;
	private final EmailUtil emailUtil;

	// JWT services
	private final JwtService jwtService;
	private final TokenRevokeService tokenRevokeService;

	private static final String CK_REMEMBER = "WOM_REMEMBER";
	private static final int REMEMBER_TTL = RememberMeUtil.REMEMBER_ME_TTL;

	// Cookie JWT
	private static final String CK_AT = "AT";
	private static final String CK_RT = "RT";
	private static final int ACCESS_TTL_SEC = 15 * 60;
	private static final int REFRESH_TTL_SEC = 7 * 24 * 3600;

	private static final String SK_REG_PENDING = "REG_PENDING_REQ";
	private static final String SK_REG_EMAIL_MASK = "REG_EMAIL_MASK";
	private static final String SK_OTP_CODE = "REG_OTP_CODE";
	private static final String SK_OTP_EXPIRE = "REG_OTP_EXPIRE";
	private static final String SK_OTP_LAST_SENT = "REG_OTP_LAST_SENT";
	private static final int OTP_TTL_MINUTES = 10;
	private static final int RESEND_COOLDOWN_SECONDS = 60;

	@GetMapping("/login")
	public String showLogin(HttpServletRequest request, HttpSession session) {
		if (session.getAttribute("CURRENT_USER_ID") != null) {
			return "redirect:/";
		}
		Cookie ck = CookieUtil.get(request, CK_REMEMBER);
		if (ck != null) {
			String userId = RememberMeUtil.verifyToken(ck.getValue());
			if (userId != null) {
				userService.findById(userId).ifPresent(u -> {
					HttpSession s = request.getSession(true);
					s.setAttribute("CURRENT_USER_ID", u.getUserID());
					s.setAttribute("CURRENT_USERNAME", u.getUsername());
				});
				return "redirect:/";
			}
		}
		return "auth/login";
	}

	@PostMapping("/login")
	public String doLogin(@RequestParam("username") String username, @RequestParam("password") String password,
			@RequestParam(value = "rememberMe", required = false) Boolean rememberMe, HttpSession session,
			HttpServletResponse response, RedirectAttributes ra, Model model) {
		if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
			model.addAttribute("error", "Vui lòng nhập đầy đủ thông tin.");
			return "auth/login";
		}

		LoginResponse res = userService.login(LoginRequest.builder().username(username).password(password).build());

		if (!"Login successful!".equalsIgnoreCase(res.getMessage())) {
			model.addAttribute("error", res.getMessage());
			return "auth/login";
		}

		// Session (render MVC)
		session.setAttribute("CURRENT_USER_ID", res.getUserID());
		session.setAttribute("CURRENT_USERNAME", res.getUsername());

		// ⭐ JWT: phát hành & set cookie HttpOnly (secure=false khi dev HTTP)
		// TODO: thay Collections.emptyList() bằng danh sách roles thật khi có
		String at = jwtService.generateAccessToken(res.getUserID(), res.getUsername(), Collections.emptyList());
		String rt = jwtService.generateRefreshToken(res.getUserID());
		CookieUtil.add(response, CK_AT, at, ACCESS_TTL_SEC, true, false, "Strict");
		CookieUtil.add(response, CK_RT, rt, REFRESH_TTL_SEC, true, false, "Strict");

		// (Tuỳ chọn) remember-me cũ
		if (Boolean.TRUE.equals(rememberMe)) {
			String token = RememberMeUtil.generateToken(res.getUserID());
			CookieUtil.add(response, CK_REMEMBER, token, REMEMBER_TTL, true, false, "Lax");
		}

		ra.addFlashAttribute("success", "Đăng nhập thành công!");
		String redirect = (res.getRedirectUrl() != null && !res.getRedirectUrl().isBlank()) ? res.getRedirectUrl()
				: "/";
		return "redirect:" + redirect;
	}

	@GetMapping("/register")
	public String showRegister() {
		return "auth/register";
	}

	@PostMapping("/register")
	public String doRegister(@RequestParam("email") String email, @RequestParam("username") String username,
			@RequestParam("password") String password, @RequestParam("confirmPassword") String confirmPassword,
			RedirectAttributes ra, Model model, HttpSession session) {

		// Validate cơ bản
		if (!StringUtils.hasText(email) || !StringUtils.hasText(username) || !StringUtils.hasText(password)
				|| !StringUtils.hasText(confirmPassword)) {
			model.addAttribute("error", "Vui lòng nhập đầy đủ Email, Username, Mật khẩu và Xác nhận mật khẩu.");
			return "auth/register";
		}
		if (username.length() < 4) {
			model.addAttribute("error", "Tên đăng nhập phải có ít nhất 4 ký tự.");
			return "auth/register";
		}
		if (password.length() < 6) {
			model.addAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự.");
			return "auth/register";
		}
		if (!password.equals(confirmPassword)) {
			model.addAttribute("error", "Xác nhận mật khẩu không khớp.");
			return "auth/register";
		}
		if (userService.existsByEmail(email)) {
			model.addAttribute("error", "Email đã được đăng ký.");
			return "auth/register";
		}
		if (userService.existsByUsername(username)) {
			model.addAttribute("error", "Tên đăng nhập đã tồn tại.");
			return "auth/register";
		}

		// ❌ KHÔNG hash tại Controller
		// String hashed = PasswordUtil.encode(password);

		// ✅ Truyền RAW password cho Service xử lý encode
		RegisterRequest pending = RegisterRequest.builder().email(email).username(username).password(password) // raw
				.build();

		// Lưu pending + info OTP vào session cho bước verify
		session.setAttribute(SK_REG_PENDING, pending);
		session.setAttribute(SK_REG_EMAIL_MASK, maskEmail(email));

		String otp = generateOtp();
		Instant expireAt = Instant.now().plus(Duration.ofMinutes(OTP_TTL_MINUTES));
		session.setAttribute(SK_OTP_CODE, otp);
		session.setAttribute(SK_OTP_EXPIRE, expireAt);
		session.setAttribute(SK_OTP_LAST_SENT, Instant.now());

		try {
			emailUtil.sendVerifyOtp(email, username, otp, OTP_TTL_MINUTES);
		} catch (Exception e) {
			clearOtpSession(session);
			model.addAttribute("error", "Không thể gửi email OTP. Vui lòng thử lại.");
			return "auth/register";
		}

		ra.addFlashAttribute("success", "Mã OTP đã được gửi tới email của bạn.");
		return "redirect:/auth/verify-otp";
	}

	@GetMapping("/verify-otp")
	public String showVerifyOtp(Model model, HttpSession session, RedirectAttributes ra) {
		RegisterRequest pending = (RegisterRequest) session.getAttribute(SK_REG_PENDING);
		if (pending == null) {
			ra.addFlashAttribute("error", "Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại.");
			return "redirect:/auth/register";
		}

		Instant exp = (Instant) session.getAttribute(SK_OTP_EXPIRE);
		long remain = Math.max(0,
				Duration.between(Instant.now(), Objects.requireNonNullElse(exp, Instant.now())).getSeconds());
		model.addAttribute("maskedEmail", (String) session.getAttribute(SK_REG_EMAIL_MASK));
		model.addAttribute("remainingSeconds", Math.min(remain, RESEND_COOLDOWN_SECONDS));
		return "auth/verify-otp";
	}

	@PostMapping("/verify-otp")
	public String doVerifyOtp(@RequestParam("otp") String otp, HttpSession session, RedirectAttributes ra) {
		RegisterRequest pending = (RegisterRequest) session.getAttribute(SK_REG_PENDING);
		String code = (String) session.getAttribute(SK_OTP_CODE);
		Instant expireAt = (Instant) session.getAttribute(SK_OTP_EXPIRE);

		if (pending == null || code == null || expireAt == null) {
			ra.addFlashAttribute("error", "Phiên OTP không hợp lệ hoặc đã hết hạn.");
			return "redirect:/auth/register";
		}
		if (Instant.now().isAfter(expireAt)) {
			ra.addFlashAttribute("error", "Mã OTP đã hết hạn. Vui lòng gửi lại mã.");
			return "redirect:/auth/verify-otp";
		}
		if (!otp.equals(code)) {
			ra.addFlashAttribute("error", "Mã OTP không đúng. Vui lòng kiểm tra lại.");
			return "redirect:/auth/verify-otp";
		}

		RegisterResponse res = userService.register(pending);
		if (!"Register successful!".equalsIgnoreCase(res.getMessage())) {
			ra.addFlashAttribute("error", "Không thể tạo tài khoản: " + res.getMessage());
			return "redirect:/auth/register";
		}

		String userId = res.getUserID();
		String username = res.getUsername();

		if (userId == null || username == null) {
			userService.findByEmail(pending.getEmail()).ifPresent(u -> {
				session.setAttribute("CURRENT_USER_ID", u.getUserID());
				session.setAttribute("CURRENT_USERNAME", u.getUsername());
			});
		} else {
			session.setAttribute("CURRENT_USER_ID", userId);
			session.setAttribute("CURRENT_USERNAME", username);
		}

		clearOtpSession(session);
		ra.addFlashAttribute("success", "Đăng ký thành công! Bạn có thể đăng nhập ngay.");
		return "redirect:/";
	}

	@PostMapping("/resend-otp")
	public String resendOtp(HttpSession session, RedirectAttributes ra) {
		RegisterRequest pending = (RegisterRequest) session.getAttribute(SK_REG_PENDING);
		Instant last = (Instant) session.getAttribute(SK_OTP_LAST_SENT);
		if (pending == null) {
			ra.addFlashAttribute("error", "Phiên đăng ký đã hết hạn.");
			return "redirect:/auth/register";
		}
		if (last != null) {
			long diff = Duration.between(last, Instant.now()).getSeconds();
			if (diff < RESEND_COOLDOWN_SECONDS) {
				ra.addFlashAttribute("error",
						"Vui lòng chờ " + (RESEND_COOLDOWN_SECONDS - diff) + " giây để gửi lại OTP.");
				return "redirect:/auth/verify-otp";
			}
		}

		String otp = generateOtp();
		Instant exp = Instant.now().plus(Duration.ofMinutes(OTP_TTL_MINUTES));
		session.setAttribute(SK_OTP_CODE, otp);
		session.setAttribute(SK_OTP_EXPIRE, exp);
		session.setAttribute(SK_OTP_LAST_SENT, Instant.now());

		try {
			emailUtil.sendVerifyOtp(pending.getEmail(), pending.getUsername(), otp, OTP_TTL_MINUTES);
		} catch (Exception e) {
			ra.addFlashAttribute("error", "Không thể gửi lại OTP. Vui lòng thử sau.");
			return "redirect:/auth/verify-otp";
		}

		ra.addFlashAttribute("success", "Đã gửi lại mã OTP.");
		return "redirect:/auth/verify-otp";
	}

	// Refresh Access Token (cookie RT -> cookie AT mới)
	@PostMapping("/refresh-token")
	@ResponseBody
	public Object refreshToken(HttpServletRequest request, HttpServletResponse response) {
		Cookie rtCk = CookieUtil.get(request, CK_RT);
		if (rtCk == null)
			return Map.of("ok", false, "message", "Missing refresh token");

		String rt = rtCk.getValue();
		if (!jwtService.isValidRefresh(rt) || tokenRevokeService.isRevoked(rt)) {
			return Map.of("ok", false, "message", "Refresh token invalid or revoked");
		}

		String userId = jwtService.getUserId(rt);
		var userOpt = userService.findById(userId);
		if (userOpt.isEmpty())
			return Map.of("ok", false, "message", "User not found");

		var user = userOpt.get();
		// TODO: thay Collections.emptyList() bằng roles thật khi có
		String newAT = jwtService.generateAccessToken(user.getUserID(), user.getUsername(), Collections.emptyList());
		CookieUtil.add(response, CK_AT, newAT, ACCESS_TTL_SEC, true, false, "Strict");

		// (Tuỳ chọn) rotate refresh:
		// String newRT = jwtService.generateRefreshToken(user.getUserID());
		// tokenRevokeService.revoke(rt, jwtService.getExpiry(rt));
		// CookieUtil.add(response, CK_RT, newRT, REFRESH_TTL_SEC, true, false,
		// "Strict");

		return Map.of("ok", true);
	}

	// me (demo đọc theo JWT; fallback session)
	@GetMapping("/me")
	@ResponseBody
	public Object me(HttpServletRequest request, HttpSession session) {
		Cookie at = CookieUtil.get(request, CK_AT);
		if (at != null && jwtService.isValidAccess(at.getValue()) && !tokenRevokeService.isRevoked(at.getValue())) {
			String userId = jwtService.getUserId(at.getValue());
			var userOpt = userService.findById(userId);
			if (userOpt.isEmpty())
				return Map.of("ok", false);
			var u = userOpt.get();
			return Map.of("by", "jwt", "userID", u.getUserID(), "username", u.getUsername()
			// "roles": thêm khi bạn có roles thực tế
			);
		}
		if (session.getAttribute("CURRENT_USER_ID") != null) {
			return Map.of("by", "session", "userID", session.getAttribute("CURRENT_USER_ID"), "username",
					session.getAttribute("CURRENT_USERNAME"));
		}
		return Map.of("ok", false, "message", "No auth");
	}

	@PostMapping("/logout")
	public String doLogout(HttpServletRequest request, HttpServletResponse response, HttpSession session,
			RedirectAttributes ra) {
		// Revoke AT/RT nếu có
		Cookie at = CookieUtil.get(request, CK_AT);
		Cookie rt = CookieUtil.get(request, CK_RT);
		if (at != null)
			tokenRevokeService.revoke(at.getValue(), jwtService.getExpiry(at.getValue()));
		if (rt != null)
			tokenRevokeService.revoke(rt.getValue(), jwtService.getExpiry(rt.getValue()));

		CookieUtil.delete(response, CK_AT);
		CookieUtil.delete(response, CK_RT);
		CookieUtil.delete(response, CK_REMEMBER);

		session.invalidate();
		ra.addFlashAttribute("success", "Bạn đã đăng xuất.");
		return "redirect:/auth/login";
	}

	@GetMapping("/logout")
	public String doLogoutGet(HttpServletRequest request, HttpServletResponse response, HttpSession session,
			RedirectAttributes ra) {
		Cookie at = CookieUtil.get(request, CK_AT);
		Cookie rt = CookieUtil.get(request, CK_RT);
		if (at != null)
			tokenRevokeService.revoke(at.getValue(), jwtService.getExpiry(at.getValue()));
		if (rt != null)
			tokenRevokeService.revoke(rt.getValue(), jwtService.getExpiry(rt.getValue()));

		CookieUtil.delete(response, CK_AT);
		CookieUtil.delete(response, CK_RT);
		CookieUtil.delete(response, CK_REMEMBER);

		session.invalidate();
		ra.addFlashAttribute("success", "Bạn đã đăng xuất.");
		return "redirect:/auth/login";
	}

	private static String generateOtp() {
		return String.format("%06d", new Random().nextInt(1_000_000));
	}

	private static String maskEmail(String email) {
		if (email == null || !email.contains("@"))
			return "email của bạn";
		String[] parts = email.split("@", 2);
		String local = parts[0];
		if (local.length() <= 2)
			return local.charAt(0) + "***@" + parts[1];
		return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + parts[1];
	}

	private static void clearOtpSession(HttpSession session) {
		session.removeAttribute(SK_REG_PENDING);
		session.removeAttribute(SK_REG_EMAIL_MASK);
		session.removeAttribute(SK_OTP_CODE);
		session.removeAttribute(SK_OTP_EXPIRE);
		session.removeAttribute(SK_OTP_LAST_SENT);
	}
}
