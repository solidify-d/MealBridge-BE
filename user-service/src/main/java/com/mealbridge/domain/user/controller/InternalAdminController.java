package com.mealbridge.domain.user.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.domain.user.entity.Role;
import com.mealbridge.domain.user.entity.User;
import com.mealbridge.domain.user.entity.UserStatus;
import com.mealbridge.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/admins")
@RequiredArgsConstructor
public class InternalAdminController {

	private final UserRepository userRepository;

	@GetMapping("/active-user-ids")
	public ResponseEntity<?> activeAdminUserIds() {
		List<UUID> ids = userRepository
				.findByStatusAndRoleIn(UserStatus.ACTIVE, List.of(Role.ADMIN, Role.SUPER_ADMIN))
				.stream()
				.map(User::getId)
				.toList();
		return ApiResponse.ok(ids, "Active admin user ids retrieved");
	}
}
