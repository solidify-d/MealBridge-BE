package com.mealbridge.domain.admin.service.command;

import com.mealbridge.domain.admin.dto.AdminUserResDto;
import com.mealbridge.domain.admin.dto.CreateAdminReq;
import com.mealbridge.domain.user.entity.UserStatus;

import java.util.UUID;

public interface AdminUserCommandService {

	AdminUserResDto createAdmin(CreateAdminReq request, UUID createdById);

	AdminUserResDto updateStatus(UUID id, UserStatus status);
}
