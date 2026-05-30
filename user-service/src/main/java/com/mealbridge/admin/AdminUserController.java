package com.mealbridge.admin;

import com.mealbridge.admin.dto.AdminUserResponse;
import com.mealbridge.admin.dto.CreateAdminRequest;
import com.mealbridge.admin.dto.UpdateAdminStatusRequest;
import com.mealbridge.common.api.ApiResponse;
import com.mealbridge.common.security.CustomUserPrincipal;
import com.mealbridge.common.security.AdminOnly;
import com.mealbridge.common.security.SuperAdminOnly;
import com.mealbridge.domain.user.Role;
import com.mealbridge.domain.user.User;
import com.mealbridge.domain.user.UserRepository;
import com.mealbridge.domain.user.UserStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@GetMapping
	@SuperAdminOnly
	public ResponseEntity<?> listAdmins() {
		List<AdminUserResponse> admins = userRepository.findByRole(Role.ADMIN).stream()
				.map(this::toResponse)
				.toList();
		return ApiResponse.ok(admins, "Admin users retrieved");
	}

	@PostMapping
	@SuperAdminOnly
	public ResponseEntity<?> createAdmin(
			@Valid @RequestBody CreateAdminRequest request,
			@AuthenticationPrincipal CustomUserPrincipal principal) {

		if (userRepository.findByEmail(request.email()).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
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
				.createdById(principal.userId())
				.build();

		userRepository.save(admin);
		return ApiResponse.ok(toResponse(admin), "Admin user created");
	}

	@PatchMapping("/{id}/status")
	@SuperAdminOnly
	public ResponseEntity<?> updateStatus(
			@PathVariable UUID id,
			@Valid @RequestBody UpdateAdminStatusRequest request) {

		User admin = userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found"));

		if (admin.getRole() != Role.ADMIN) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User is not a sub-admin");
		}

		admin.updateStatus(request.status());
		return ApiResponse.ok(toResponse(admin), "Admin status updated");
	}

	@GetMapping("/me")
	@AdminOnly
	public ResponseEntity<?> me(@AuthenticationPrincipal CustomUserPrincipal principal) {
		User user = userRepository.findById(principal.userId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		return ApiResponse.ok(toResponse(user), "Current admin profile");
	}

	private AdminUserResponse toResponse(User user) {
		return new AdminUserResponse(
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
