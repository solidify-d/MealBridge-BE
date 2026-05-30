package com.mealbridge.common.auth.email;

import com.mealbridge.common.exception.DuplicateResourceException;
import com.mealbridge.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

	private final EmailVerificationStore store;
	private final VerificationEmailSender emailSender;
	private final EmailVerificationProperties properties;
	private final UserRepository userRepository;
	private final SecureRandom random = new SecureRandom();

	public void sendVerificationCode(String email) {
		String normalized = normalize(email);
		ensureEmailAvailable(normalized);
		ensureSendAllowed(normalized);

		String code = resolveCode();
		store.saveCode(normalized, code, Duration.ofMinutes(properties.codeTtlMinutes()));
		emailSender.sendCode(normalized, code);
	}

	public void confirmVerificationCode(String email, String code) {
		String normalized = normalize(email);
		String expected = store.getCode(normalized)
				.orElseThrow(() -> new IllegalArgumentException("Verification code expired or not found. Request a new code."));

		if (!expected.equals(code.trim())) {
			throw new IllegalArgumentException("Invalid verification code");
		}

		store.deleteCode(normalized);
		store.markVerified(normalized, Duration.ofMinutes(properties.verifiedTtlMinutes()));
	}

	public void requireVerifiedForRegistration(String email) {
		if (!properties.enabled()) {
			return;
		}
		String normalized = normalize(email);
		if (!store.isVerified(normalized)) {
			throw new IllegalStateException("Email is not verified. Complete email verification before registering.");
		}
		store.deleteVerified(normalized);
	}

	private void ensureEmailAvailable(String email) {
		if (userRepository.findByEmail(email).isPresent()) {
			throw new DuplicateResourceException("Email already exists");
		}
	}

	private void ensureSendAllowed(String email) {
		if (!store.tryAcquireSendLock(email, Duration.ofSeconds(properties.resendCooldownSeconds()))) {
			throw new IllegalStateException("Please wait before requesting another verification code");
		}
	}

	private String resolveCode() {
		if (properties.devFixedCode() != null && !properties.devFixedCode().isBlank()) {
			return properties.devFixedCode().trim();
		}
		int bound = (int) Math.pow(10, properties.codeLength());
		int value = random.nextInt(bound / 10, bound);
		return String.valueOf(value);
	}

	private String normalize(String email) {
		return email.trim().toLowerCase();
	}
}
