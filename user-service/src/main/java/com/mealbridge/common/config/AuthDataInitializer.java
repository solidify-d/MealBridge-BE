package com.mealbridge.common.config;

import com.mealbridge.domain.user.Role;
import com.mealbridge.domain.user.User;
import com.mealbridge.domain.user.UserRepository;
import com.mealbridge.domain.user.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthDataInitializer implements ApplicationRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${mealbridge.auth.seed-enabled:false}")
	private boolean seedEnabled;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!seedEnabled) {
			return;
		}

		UUID orgId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		UUID donorInstitutionId = UUID.fromString("00000000-0000-0000-0000-000000000002");

		seedUser("super@mealbridge.local", "Super Admin", Role.SUPER_ADMIN, null, null, null);
		seedUser("admin@mealbridge.local", "Sub Admin", Role.ADMIN, null, null,
				findUserIdByEmail("super@mealbridge.local"));
		seedUser("org@mealbridge.local", "Demo Organization", Role.ORGANIZATION, orgId, null, null);
		seedUser("donor@mealbridge.local", "Demo Donor Institution", Role.DONOR, null, donorInstitutionId, null);

		log.info("Auth seed users ready (password: mealbridge123!)");
	}

	private UUID findUserIdByEmail(String email) {
		return userRepository.findByEmail(email).map(User::getId).orElse(null);
	}

	private void seedUser(String email, String name, Role role, UUID organizationId, UUID donorInstitutionId, UUID createdById) {
		if (userRepository.findByEmail(email).isPresent()) {
			return;
		}
		User user = User.builder()
				.email(email)
				.password(passwordEncoder.encode("mealbridge123!"))
				.name(name)
				.role(role)
				.status(UserStatus.ACTIVE)
				.organizationId(organizationId)
				.donorInstitutionId(donorInstitutionId)
				.createdById(createdById)
				.build();
		userRepository.save(user);
	}
}
