package com.mealbridge.domain.platformuser.service.command;

import com.mealbridge.domain.platformuser.dto.PlatformUserSummaryResDto;
import com.mealbridge.domain.user.entity.UserStatus;

import java.util.UUID;

public interface PlatformUserCommandService {

	PlatformUserSummaryResDto updateStatus(UUID userId, UserStatus status, String reason);
}
