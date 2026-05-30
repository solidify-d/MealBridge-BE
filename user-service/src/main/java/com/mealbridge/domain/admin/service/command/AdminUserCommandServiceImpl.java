package com.mealbridge.domain.admin.service.command;

import com.mealbridge.common.exception.DuplicateResourceException;
import com.mealbridge.domain.admin.dto.AdminUserResDto;
import com.mealbridge.domain.admin.dto.CreateAdminReq;
import com.mealbridge.domain.user.entity.Role;
import com.mealbridge.domain.user.entity.User;
import com.mealbridge.domain.user.entity.UserStatus;
import com.mealbridge.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserCommandServiceImpl implements AdminUserCommandService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public AdminUserResDto createAdmin(CreateAdminReq request, UUID createdById) {
		if (userRepository.findByEmail(request.email()).isPresent()) {
			throw new DuplicateResourceException("Email already exists");
		}
		User admin = User.builder()
				.email(request.email())
				.password(passwordEncoder.encode(request.password()))
				.name(request.name())
				.phone(request.phone() != null ? request.phone() : "0000000000")
				.department(request.department())
				.memo(request.memo())
				.role(Role.ADMIN)
				.status(UserStatus.ACTIVE)
				.emailVerified(true)
				.createdById(createdById)
				.build();
		userRepository.save(admin);
		return toResponse(admin);
	}

	@Override
	public AdminUserResDto updateStatus(UUID id, UserStatus status) {
		User admin = userRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Admin not found"));
		if (admin.getRole() != Role.ADMIN) {
			throw new IllegalArgumentException("User is not a sub-admin");
		}
		admin.updateStatus(status);
		return toResponse(admin);
	}

	private AdminUserResDto toResponse(User user) {
		return new AdminUserResDto(
				user.getId(),
				user.getEmail(),
				user.getName(),
				user.getPhone(),
				user.getDepartment(),
				user.getMemo(),
				user.getStatus(),
				user.getCreatedById()
		);
	}
}
