package com.mealbridge.domain.admin.service.query;

import com.mealbridge.domain.admin.dto.AdminUserResDto;

import java.util.List;
import java.util.UUID;

public interface AdminUserQueryService {

	List<AdminUserResDto> listAdmins();

	AdminUserResDto getMe(UUID actorId);
}
