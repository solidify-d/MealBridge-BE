package com.mealbridge.common.s3;

import com.mealbridge.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@RequiredArgsConstructor
public class ProfileImageStorage {

	private final ObjectProvider<S3Manager> s3ManagerProvider;

	public String upload(User user, MultipartFile file) {
		S3Manager s3Manager = requireS3();
		ProfileImageValidator.validate(file);
		String prefix = MealConnectS3Paths.profileImage(user.getRole(), user.getId());
		return s3Manager.upload(file, prefix);
	}

	public void deleteQuietly(String fileUrl) {
		if (fileUrl == null || fileUrl.isBlank()) {
			return;
		}
		try {
			requireS3().delete(fileUrl);
		} catch (Exception ignored) {
			// best-effort cleanup (same as community forum posts)
		}
	}

	public void delete(String fileUrl) {
		if (fileUrl == null || fileUrl.isBlank()) {
			return;
		}
		requireS3().delete(fileUrl);
	}

	private S3Manager requireS3() {
		S3Manager s3Manager = s3ManagerProvider.getIfAvailable();
		if (s3Manager == null) {
			throw new IllegalStateException("S3 is not enabled. Set cloud.aws.s3.enabled=true and configure credentials.");
		}
		return s3Manager;
	}

}
