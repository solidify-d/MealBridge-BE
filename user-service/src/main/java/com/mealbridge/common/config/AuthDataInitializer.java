package com.mealbridge.common.config;

import com.mealbridge.domain.common.entity.InstitutionStatus;
import com.mealbridge.domain.donor.entity.DonorInstitution;
import com.mealbridge.domain.donor.entity.DonorInstitutionType;
import com.mealbridge.domain.donor.repository.DonorInstitutionRepository;
import com.mealbridge.domain.organization.entity.Organization;
import com.mealbridge.domain.organization.entity.OrganizationType;
import com.mealbridge.domain.organization.entity.TargetDemographic;
import com.mealbridge.domain.organization.repository.OrganizationRepository;
import com.mealbridge.domain.user.entity.Role;
import com.mealbridge.domain.user.entity.User;
import com.mealbridge.domain.user.entity.UserStatus;
import com.mealbridge.domain.user.repository.UserRepository;
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
	private final OrganizationRepository organizationRepository;
	private final DonorInstitutionRepository donorInstitutionRepository;
	private final PasswordEncoder passwordEncoder;

	@Value("${mealbridge.auth.seed-enabled:false}")
	private boolean seedEnabled;

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!seedEnabled) {
			return;
		}

		Organization demoOrg = seedOrganization();
		DonorInstitution demoDonor = seedDonorInstitution();

		seedUser("super@mealbridge.local", "Super Admin", "01000000001", Role.SUPER_ADMIN,
				null, null, null, null, null);
		seedUser("admin@mealbridge.local", "Sub Admin", "01000000002", Role.ADMIN,
				null, null, findUserIdByEmail("super@mealbridge.local"), "Operations", null);
		seedUser("org@mealbridge.local", "Demo Organization Contact", "01000000003", Role.ORGANIZATION,
				demoOrg.getId(), null, null, null, null);
		seedUser("donor@mealbridge.local", "Demo Donor Contact", "01000000004", Role.DONOR,
				null, demoDonor.getId(), null, null, null);

		log.info("Auth seed users ready (password: mealbridge123!)");
	}

	private Organization seedOrganization() {
		if (organizationRepository.count() > 0) {
			return organizationRepository.findAll().get(0);
		}
		Organization organization = Organization.builder()
				.name("Demo Welfare Center")
				.type(OrganizationType.WELFARE_CENTER)
				.address("Seoul, Demo District 1")
				.organizationPhone("0212345678")
				.serviceArea("Seoul Metro")
				.recipientCapacity(50)
				.targetDemographic(TargetDemographic.GENERAL)
				.status(InstitutionStatus.ACTIVE)
				.build();
		return organizationRepository.save(organization);
	}

	private DonorInstitution seedDonorInstitution() {
		if (donorInstitutionRepository.count() > 0) {
			return donorInstitutionRepository.findAll().get(0);
		}
		DonorInstitution institution = DonorInstitution.builder()
				.name("Demo Military Unit")
				.type(DonorInstitutionType.MILITARY)
				.address("Gyeonggi, Demo Base")
				.institutionPhone("0312345678")
				.description("Seed donor institution for local testing")
				.unitIdentifier("DEMO-UNIT-01")
				.status(InstitutionStatus.ACTIVE)
				.build();
		return donorInstitutionRepository.save(institution);
	}

	private UUID findUserIdByEmail(String email) {
		return userRepository.findByEmail(email).map(User::getId).orElse(null);
	}

	private void seedUser(String email, String name, String phone, Role role,
			UUID organizationId, UUID donorInstitutionId, UUID createdById,
			String department, String memo) {
		if (userRepository.findByEmail(email).isPresent()) {
			return;
		}
		User user = User.builder()
				.email(email)
				.password(passwordEncoder.encode("mealbridge123!"))
				.name(name)
				.phone(phone)
				.department(department)
				.memo(memo)
				.role(role)
				.status(UserStatus.ACTIVE)
				.emailVerified(true)
				.organizationId(organizationId)
				.donorInstitutionId(donorInstitutionId)
				.createdById(createdById)
				.build();
		userRepository.save(user);
	}
}
