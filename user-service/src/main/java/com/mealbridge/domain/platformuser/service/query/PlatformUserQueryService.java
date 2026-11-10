package com.mealbridge.domain.platformuser.service.query;

import com.mealbridge.domain.platformuser.dto.PlatformUserDetailResDto;
import com.mealbridge.domain.platformuser.dto.PlatformUserSummaryResDto;
import com.mealbridge.domain.user.entity.Role;
import com.mealbridge.domain.user.entity.UserStatus;

import java.util.List;
import java.util.UUID;

public interface PlatformUserQueryService {

	List<PlatformUserSummaryResDto> listAll(Role role, UserStatus status);

	PlatformUserDetailResDto getDetail(UUID userId);
}
