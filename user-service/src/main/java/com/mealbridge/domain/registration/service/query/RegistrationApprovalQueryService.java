package com.mealbridge.domain.registration.service.query;

import com.mealbridge.domain.registration.dto.PendingRegistrationDetailResDto;
import com.mealbridge.domain.registration.dto.PendingRegistrationSummaryResDto;

import java.util.List;
import java.util.UUID;

public interface RegistrationApprovalQueryService {

	List<PendingRegistrationSummaryResDto> listPending();

	PendingRegistrationDetailResDto getDetail(UUID userId);

	PendingRegistrationDetailResDto getDetailByUserId(UUID userId);
}
