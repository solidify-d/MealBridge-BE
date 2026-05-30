package com.mealbridge.domain.donation.repository;

import com.mealbridge.domain.donation.entity.Donation;
import com.mealbridge.domain.donation.entity.DonationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DonationRepository extends JpaRepository<Donation, UUID> {

	List<Donation> findByRegisteredByUserIdOrderByCreatedAtDesc(UUID registeredByUserId);

	List<Donation> findByStatusOrderByCreatedAtDesc(DonationStatus status);
}
