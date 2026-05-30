package com.mealbridge.common.auth.email;

import java.time.Duration;
import java.util.Optional;

public interface EmailVerificationStore {

	void saveCode(String email, String code, Duration ttl);

	Optional<String> getCode(String email);

	void deleteCode(String email);

	void markVerified(String email, Duration ttl);

	boolean isVerified(String email);

	void deleteVerified(String email);

	boolean tryAcquireSendLock(String email, Duration cooldown);

}
