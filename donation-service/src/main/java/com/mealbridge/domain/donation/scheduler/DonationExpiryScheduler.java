package com.mealbridge.domain.donation.scheduler;

import com.mealbridge.domain.donation.service.DonationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DonationExpiryScheduler {

	private final DonationService donationService;

	@Scheduled(fixedRate = 60_000)
	public void expireStaleSurplusListings() {
		int expired = donationService.expireStaleDonations();
		if (expired > 0) {
			log.info("Auto-expired {} surplus listing(s) past pickup deadline", expired);
		}
	}
}
