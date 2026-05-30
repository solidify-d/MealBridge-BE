package com.mealbridge.domain.donation.controller;

import com.mealbridge.common.apiResponse.ApiResponse;
import com.mealbridge.domain.donation.service.DonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/donations")
@RequiredArgsConstructor
public class InternalDonationController {

	private final DonationService donationService;

	@GetMapping("/{id}")
	public ResponseEntity<?> getById(@PathVariable UUID id) {
		return ApiResponse.ok(donationService.getInternalById(id), "Donation retrieved");
	}

	@GetMapping("/registered")
	public ResponseEntity<?> listRegistered() {
		return ApiResponse.ok(donationService.listRegisteredInternal(), "Registered donations retrieved");
	}
}
