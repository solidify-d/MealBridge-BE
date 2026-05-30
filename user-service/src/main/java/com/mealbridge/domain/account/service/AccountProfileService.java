package com.mealbridge.domain.account.service;

import com.mealbridge.domain.account.dto.MyAccountProfileResDto;
import com.mealbridge.domain.account.dto.ProfileImageUploadResDto;
import com.mealbridge.domain.account.dto.UpdateMyAccountReq;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface AccountProfileService {

	MyAccountProfileResDto getProfile(UUID userId);

	MyAccountProfileResDto updateProfile(UUID userId, UpdateMyAccountReq request);

	ProfileImageUploadResDto uploadProfileImage(UUID userId, MultipartFile file);

	void deleteProfileImage(UUID userId);

	void deleteAccount(UUID userId);
}
