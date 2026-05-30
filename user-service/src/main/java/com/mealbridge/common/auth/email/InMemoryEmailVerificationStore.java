package com.mealbridge.common.auth.email;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("test")
public class InMemoryEmailVerificationStore implements EmailVerificationStore {

	private final Map<String, String> codes = new ConcurrentHashMap<>();
	private final Map<String, Instant> codeExpiry = new ConcurrentHashMap<>();
	private final Map<String, Instant> verifiedExpiry = new ConcurrentHashMap<>();
	private final Map<String, Instant> sendLockExpiry = new ConcurrentHashMap<>();

	@Override
	public void saveCode(String email, String code, Duration ttl) {
		String key = normalize(email);
		codes.put(key, code);
		codeExpiry.put(key, Instant.now().plus(ttl));
	}

	@Override
	public Optional<String> getCode(String email) {
		String key = normalize(email);
		if (isExpired(codeExpiry.get(key))) {
			codes.remove(key);
			codeExpiry.remove(key);
			return Optional.empty();
		}
		return Optional.ofNullable(codes.get(key));
	}

	@Override
	public void deleteCode(String email) {
		String key = normalize(email);
		codes.remove(key);
		codeExpiry.remove(key);
	}

	@Override
	public void markVerified(String email, Duration ttl) {
		verifiedExpiry.put(normalize(email), Instant.now().plus(ttl));
	}

	@Override
	public boolean isVerified(String email) {
		String key = normalize(email);
		Instant expiry = verifiedExpiry.get(key);
		if (expiry == null || isExpired(expiry)) {
			verifiedExpiry.remove(key);
			return false;
		}
		return true;
	}

	@Override
	public void deleteVerified(String email) {
		verifiedExpiry.remove(normalize(email));
	}

	@Override
	public boolean tryAcquireSendLock(String email, Duration cooldown) {
		String key = normalize(email);
		Instant existing = sendLockExpiry.get(key);
		if (existing != null && !isExpired(existing)) {
			return false;
		}
		sendLockExpiry.put(key, Instant.now().plus(cooldown));
		return true;
	}

	private boolean isExpired(Instant expiry) {
		return expiry == null || Instant.now().isAfter(expiry);
	}

	private String normalize(String email) {
		return email.trim().toLowerCase();
	}
}
