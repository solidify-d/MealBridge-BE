package com.mealbridge.domain.admin.service.query;

import com.mealbridge.domain.admin.dto.AdminUserResDto;
import com.mealbridge.domain.user.entity.Role;
import com.mealbridge.domain.user.entity.User;
import com.mealbridge.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserQueryServiceImpl implements AdminUserQueryService {

	private final UserRepository userRepository;

	@Override
	public List<AdminUserResDto> listAdmins() {
		return userRepository.findByRole(Role.ADMIN).stream()
				.map(this::toResponse)
				.toList();
	}

	@Override
	public AdminUserResDto getById(UUID id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("User not found"));
		if (user.getRole() != Role.ADMIN) {
			throw new IllegalArgumentException("User is not a sub-admin");
		}
		return toResponse(user);
	}

	@Override
	public AdminUserResDto getMe(UUID actorId) {
		User user = userRepository.findById(actorId)
				.orElseThrow(() -> new EntityNotFoundException("User not found"));
		return toResponse(user);
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
				user.getSuspendReason(),
				user.getCreatedById()
		);
	}
}
