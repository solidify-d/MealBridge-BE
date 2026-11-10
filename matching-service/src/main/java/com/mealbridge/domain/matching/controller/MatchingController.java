package com.mealbridge.domain.matching.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.common.auth.AuthHeaders;
import com.mealbridge.domain.matching.dto.AcceptAssignmentReq;
import com.mealbridge.domain.matching.dto.CancelInTransitReq;
import com.mealbridge.domain.matching.dto.ConfirmPickupPlanReq;
import com.mealbridge.domain.matching.dto.OptimizeRouteReq;
import com.mealbridge.domain.matching.dto.SuggestPickupPlanReq;
import com.mealbridge.domain.matching.service.PickupPlanService;
import com.mealbridge.domain.matching.service.PickupAssignmentService;
import com.mealbridge.domain.matching.service.PickupTrackingService;
import com.mealbridge.domain.matching.service.RecommendationService;
import com.mealbridge.domain.matching.service.RouteOptimizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/matching")
@RequiredArgsConstructor
public class MatchingController {

	private final AuthHeaders authHeaders;
	private final RecommendationService recommendationService;
	private final PickupAssignmentService pickupAssignmentService;
	private final RouteOptimizationService routeOptimizationService;
	private final PickupPlanService pickupPlanService;
	private final PickupTrackingService pickupTrackingService;

	@GetMapping("/recommendations")
	public ResponseEntity<?> recommendForDonation(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam UUID donationId) {
		authHeaders.requireDonor(userId, role);
		return ApiResponse.ok(
				recommendationService.recommendOrganizationsForDonation(donationId),
				"Organization recommendations retrieved");
	}

	@GetMapping("/recommendations/for-organization")
	public ResponseEntity<?> recommendForOrganization(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam UUID organizationId) {
		authHeaders.requireOrganization(userId, role);
		return ApiResponse.ok(
				recommendationService.recommendDonationsForOrganization(organizationId),
				"Donation recommendations retrieved");
	}

	@PostMapping("/assignments/accept")
	public ResponseEntity<?> acceptAssignment(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestBody AcceptAssignmentReq request) {
		UUID actorUserId = authHeaders.requireOrganization(userId, role);
		return ApiResponse.ok(
				pickupAssignmentService.accept(actorUserId, request),
				"Pickup assignment accepted");
	}

	@GetMapping("/assignments/accepted")
	public ResponseEntity<?> listAcceptedAssignments(@RequestParam UUID organizationId) {
		return ApiResponse.ok(
				pickupAssignmentService.listAccepted(organizationId),
				"Accepted assignments retrieved");
	}

	@PostMapping("/routes/optimize")
	public ResponseEntity<?> optimizeRoute(@Valid @RequestBody OptimizeRouteReq request) {
		return ApiResponse.ok(
				routeOptimizationService.optimize(request.organizationId(), request.donationIds()),
				"Route optimized");
	}

	@PostMapping("/pickup-plan/suggest")
	public ResponseEntity<?> suggestPickupPlan(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestBody SuggestPickupPlanReq request) {
		authHeaders.requireOrganization(userId, role);
		return ApiResponse.ok(pickupPlanService.suggest(request), "Pickup plan suggested");
	}

	@PostMapping("/pickup-plan/confirm")
	public ResponseEntity<?> confirmPickupPlan(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestBody ConfirmPickupPlanReq request) {
		UUID actorUserId = authHeaders.requireOrganization(userId, role);
		return ApiResponse.ok(
				pickupPlanService.confirm(actorUserId, request),
				"Pickup plan confirmed");
	}

	@PostMapping("/pickup/{routePlanId}/start-transit")
	public ResponseEntity<?> startTransit(
			@PathVariable UUID routePlanId,
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam UUID organizationId) {
		UUID actorUserId = authHeaders.requireOrganization(userId, role);
		pickupTrackingService.startTransit(routePlanId, organizationId, actorUserId);
		return ApiResponse.ok(null, "Pickup started");
	}

	@PostMapping("/pickup/{routePlanId}/complete")
	public ResponseEntity<?> completeRoute(
			@PathVariable UUID routePlanId,
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam UUID organizationId) {
		UUID actorUserId = authHeaders.requireOrganization(userId, role);
		pickupTrackingService.completeRoute(routePlanId, organizationId, actorUserId);
		return ApiResponse.ok(null, "Pickup route completed");
	}

	@PostMapping("/pickup/assignments/{assignmentId}/cancel-in-transit")
	public ResponseEntity<?> cancelInTransit(
			@PathVariable UUID assignmentId,
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@Valid @RequestBody CancelInTransitReq request) {
		UUID actorUserId = authHeaders.requireOrganization(userId, role);
		pickupTrackingService.cancelInTransit(
				assignmentId, request.organizationId(), actorUserId, request.reason());
		return ApiResponse.ok(null, "Pickup cancelled");
	}

	@GetMapping("/pickup/active")
	public ResponseEntity<?> listActivePickups(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam UUID organizationId) {
		authHeaders.requireOrganization(userId, role);
		return ApiResponse.ok(
				pickupTrackingService.listActive(organizationId),
				"Active pickups retrieved");
	}

	@GetMapping("/pickup/history")
	public ResponseEntity<?> listPickupHistory(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam UUID organizationId) {
		authHeaders.requireOrganization(userId, role);
		return ApiResponse.ok(
				pickupTrackingService.listHistory(organizationId),
				"Pickup history retrieved");
	}

	@GetMapping("/pickup/by-donation")
	public ResponseEntity<?> getDonorPickupDetail(
			@RequestHeader(value = "X-User-Id", required = false) String userId,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestParam UUID donationId) {
		UUID actorUserId = authHeaders.requireDonor(userId, role);
		return ApiResponse.ok(
				pickupTrackingService.getDonorPickupDetail(donationId, actorUserId),
				"Donor pickup detail retrieved");
	}
}
