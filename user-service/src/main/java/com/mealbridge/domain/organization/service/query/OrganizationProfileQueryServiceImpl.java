package com.mealbridge.domain.organization.service.query;

import com.mealbridge.domain.organization.dto.OrganizationProfileResDto;
import com.mealbridge.domain.organization.repository.OrganizationRepository;
import com.mealbridge.domain.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationProfileQueryServiceImpl implements OrganizationProfileQueryService {

	private final UserRepository userRepository;
	private final OrganizationRepository organizationRepository;

	@Override
	public OrganizationProfileResDto getProfile(UUID userId) {
		var user = userRepository.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("User not found"));
		if (user.getOrganizationId() == null) {
			throw new EntityNotFoundException("Organization not linked to user");
		}
		var org = organizationRepository.findById(user.getOrganizationId())
				.orElseThrow(() -> new EntityNotFoundException("Organization not found"));
		return new OrganizationProfileResDto(
				user.getId(),
				user.getEmail(),
				user.getName(),
				user.getPhone(),
				user.getStatus(),
				org.getId(),
				org.getName(),
				org.getType(),
				org.getAddress(),
				org.getOrganizationPhone(),
				org.getRecipientCapacity(),
				org.getTargetDemographic(),
				org.getStatus()
		);
	}
}
