package com.mealbridge.domain.donation.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.donation.dto.AcceptDonationReq;
import com.mealbridge.domain.donation.dto.CreateDonationReq;
import com.mealbridge.domain.donation.dto.DonationResDto;
import com.mealbridge.domain.donation.dto.ManualDonationReq;
import com.mealbridge.domain.donation.dto.OrganizationDonationActionReq;
import com.mealbridge.domain.donation.service.DonationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/donations")
@RequiredArgsConstructor
public class DonationController {

	private final AuthHeaders authHeaders;
	private final DonationService donationService;

	@PostMapping(consumes = "multipart/form-data")
	public ResponseEntity<?> create(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestPart("data") CreateDonationReq request,
			@RequestPart(value = "beforeImage", required = false) MultipartFile beforeImage,
			@RequestPart("afterImage") MultipartFile afterImage) {

		UUID actorId = authHeaders.requireDonor(userId, role);
		DonationResDto created = donationService.create(actorId, request, beforeImage, afterImage);
		return ApiResponse.created(created, "Donation registered");
	}

	@PostMapping(value = "/manual", consumes = "multipart/form-data")
	public ResponseEntity<?> createManual(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestPart("data") ManualDonationReq request,
			@RequestPart(value = "beforeImage", required = false) MultipartFile beforeImage,
			@RequestPart(value = "afterImage", required = false) MultipartFile afterImage) {

		UUID actorId = authHeaders.requireDonor(userId, role);
		DonationResDto created = donationService.createManual(actorId, request, beforeImage, afterImage);
		return ApiResponse.created(created, "Surplus registered manually");
	}

	@GetMapping("/me")
	public ResponseEntity<?> listMine(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {

		UUID actorId = authHeaders.requireDonor(userId, role);
		List<DonationResDto> list = donationService.listMine(actorId);
		return ApiResponse.ok(list, "Donations retrieved");
	}

	@GetMapping("/available")
	public ResponseEntity<?> listAvailable(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {

		authHeaders.requireOrganization(userId, role);
		List<DonationResDto> list = donationService.listAvailableForOrganization();
		return ApiResponse.ok(list, "Available donations retrieved");
	}

	@GetMapping("/{id}")
	public ResponseEntity<?> getById(
			@PathVariable UUID id,
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {

		UUID actorId = authHeaders.requireUserId(userId);
		String actorRole = authHeaders.requireRole(role);
		if (!actorRole.equalsIgnoreCase("ORGANIZATION") && !actorRole.equalsIgnoreCase("DONOR")) {
			throw new SecurityException("ORGANIZATION or DONOR role is required");
		}
		DonationResDto donation = donationService.getById(id, actorId, actorRole);
		return ApiResponse.ok(donation, "Donation retrieved");
	}

	@PostMapping("/{id}/accept")
	public ResponseEntity<?> accept(
			@PathVariable UUID id,
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestBody AcceptDonationReq request) {

		UUID actorId = authHeaders.requireOrganization(userId, role);
		DonationResDto accepted = donationService.accept(id, actorId, request);
		return ApiResponse.ok(accepted, "Donation accepted");
	}

	@PostMapping("/{id}/in-transit")
	public ResponseEntity<?> markInTransit(
			@PathVariable UUID id,
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestBody OrganizationDonationActionReq request) {

		UUID actorId = authHeaders.requireOrganization(userId, role);
		DonationResDto updated = donationService.markInTransit(id, actorId, request.organizationId());
		return ApiResponse.ok(updated, "Donation marked in transit");
	}

	@PostMapping("/{id}/complete")
	public ResponseEntity<?> complete(
			@PathVariable UUID id,
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestBody OrganizationDonationActionReq request) {

		UUID actorId = authHeaders.requireOrganization(userId, role);
		DonationResDto updated = donationService.complete(id, actorId, request.organizationId());
		return ApiResponse.ok(updated, "Donation pickup completed");
	}

	@PostMapping("/{id}/release")
	public ResponseEntity<?> release(
			@PathVariable UUID id,
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestBody OrganizationDonationActionReq request) {

		UUID actorId = authHeaders.requireOrganization(userId, role);
		DonationResDto updated = donationService.release(id, actorId, request.organizationId());
		return ApiResponse.ok(updated, "Donation released back to available pool");
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<?> cancel(
			@PathVariable UUID id,
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role) {

		UUID actorId = authHeaders.requireDonor(userId, role);
		donationService.cancel(id, actorId);
		return ApiResponse.ok(null, "Donation cancelled");
	}
}
