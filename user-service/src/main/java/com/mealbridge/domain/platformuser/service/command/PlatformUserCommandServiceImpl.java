package com.mealbridge.domain.platformuser.service.command;

import com.mealbridge.domain.platformuser.dto.PlatformUserSummaryResDto;
import com.mealbridge.domain.user.entity.Role;
import com.mealbridge.domain.user.service.UserStatusUpdateSupport;
import com.mealbridge.domain.user.entity.User;
import com.mealbridge.domain.user.entity.UserStatus;
import com.mealbridge.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PlatformUserCommandServiceImpl implements PlatformUserCommandService {

	private final UserRepository userRepository;

	@Override
	public PlatformUserSummaryResDto updateStatus(UUID userId, UserStatus status, String reason) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("User not found"));

		if (user.getRole() != Role.ORGANIZATION && user.getRole() != Role.DONOR) {
			throw new IllegalArgumentException("Status can only be updated for organization or donor accounts");
		}
		if (status != UserStatus.ACTIVE && status != UserStatus.SUSPENDED) {
			throw new IllegalArgumentException("Only ACTIVE or SUSPENDED status is allowed");
		}
		if (user.getStatus() == UserStatus.PENDING || user.getStatus() == UserStatus.REJECTED) {
			throw new IllegalStateException("Use registration approval for pending or rejected accounts");
		}

		UserStatusUpdateSupport.applyStatus(user, status, reason);
		return new PlatformUserSummaryResDto(
				user.getId(),
				user.getEmail(),
				user.getName(),
				user.getPhone(),
				user.getRole(),
				user.getStatus(),
				null,
				user.getCreatedAt());
	}
}
