package com.womtech.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TokenRevokeService – quản lý danh sách JWT bị thu hồi (blacklist).
 * 
 * Phiên bản này chạy in-memory (dành cho môi trường dev hoặc local test). Khi
 * triển khai thực tế (deploy nhiều instance), bạn có thể viết thêm bản Redis
 * hoặc Database để lưu danh sách token bị revoke.
 */
@Service
public class TokenRevokeService {

	// Lưu token và thời điểm hết hạn (expiry) của nó.
	private final Map<String, Long> revokedTokens = new ConcurrentHashMap<>();

	/**
	 * Thu hồi token – thêm token vào danh sách blacklist cho đến khi hết hạn.
	 *
	 * @param token  chuỗi JWT cần thu hồi
	 * @param expiry thời điểm token hết hạn
	 */
	public void revoke(String token, Instant expiry) {
		if (token == null || token.isBlank())
			return;
		long expMillis = expiry != null ? expiry.toEpochMilli() : Instant.now().toEpochMilli();
		revokedTokens.put(token, expMillis);
	}

	/**
	 * Kiểm tra token có bị thu hồi không.
	 * 
	 * @param token chuỗi JWT
	 * @return true nếu token đã bị revoke và chưa hết hạn
	 */
	public boolean isRevoked(String token) {
		if (token == null || token.isBlank())
			return false;
		Long exp = revokedTokens.get(token);
		if (exp == null)
			return false;

		long now = Instant.now().toEpochMilli();
		if (exp <= now) {
			// token đã hết hạn tự nhiên, xóa cho gọn
			revokedTokens.remove(token);
			return false;
		}
		return true; // token đang bị thu hồi
	}

	/**
	 * Dọn dẹp token đã hết hạn khỏi danh sách blacklist. (Gọi định kỳ nếu muốn,
	 * không bắt buộc)
	 */
	public void purgeExpired() {
		long now = Instant.now().toEpochMilli();
		revokedTokens.entrySet().removeIf(e -> e.getValue() <= now);
	}
}
