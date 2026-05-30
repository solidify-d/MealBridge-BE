package com.mealbridge.domain.registration.service.command;

import com.mealbridge.domain.registration.dto.PendingRegistrationDetailResDto;

import java.util.UUID;

public interface RegistrationApprovalCommandService {

	PendingRegistrationDetailResDto approve(UUID userId);

	PendingRegistrationDetailResDto reject(UUID userId, String reason);
}
