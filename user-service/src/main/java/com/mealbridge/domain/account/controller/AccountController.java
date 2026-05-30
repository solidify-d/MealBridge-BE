package com.mealbridge.domain.account.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.account.dto.UpdateMyAccountReq;
import com.mealbridge.domain.account.service.AccountProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

	private final AuthHeaders authHeaders;
	private final AccountProfileService accountProfileService;

	@GetMapping("/me")
	public ResponseEntity<?> getMyProfile(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {
		var actor = authHeaders.requireActiveActor(userId, role);
		return ApiResponse.ok(accountProfileService.getProfile(actor.getId()), "Account profile retrieved");
	}

	@PutMapping("/me")
	public ResponseEntity<?> updateMyProfile(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestBody UpdateMyAccountReq request) {
		var actor = authHeaders.requireActiveActor(userId, role);
		return ApiResponse.ok(accountProfileService.updateProfile(actor.getId(), request), "Account profile updated");
	}

	@PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> uploadProfileImage(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestPart("file") MultipartFile file) {
		var actor = authHeaders.requireActiveActor(userId, role);
		var res = accountProfileService.uploadProfileImage(actor.getId(), file);
		return ApiResponse.ok(res, "Profile image uploaded");
	}

	@DeleteMapping("/me/profile-image")
	public ResponseEntity<?> deleteProfileImage(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {
		var actor = authHeaders.requireActiveActor(userId, role);
		accountProfileService.deleteProfileImage(actor.getId());
		return ApiResponse.ok(null, "Profile image deleted");
	}

	@DeleteMapping("/me")
	public ResponseEntity<?> deleteMyAccount(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {
		var actor = authHeaders.requireActiveActor(userId, role);
		accountProfileService.deleteAccount(actor.getId());
		return ApiResponse.ok(null, "Account deleted");
	}
}
