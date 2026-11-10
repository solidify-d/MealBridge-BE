package com.mealbridge.common.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DonationImageStorage {

	private final ObjectProvider<S3Manager> s3ManagerProvider;

	public String upload(UUID donorInstitutionId, UUID donationId, MultipartFile file) {
		S3Manager s3Manager = requireS3();
		DonationImageValidator.validate(file);
		String prefix = MealConnectS3Paths.donation(donorInstitutionId, donationId);
		return s3Manager.upload(file, prefix);
	}

	public void deleteQuietly(String fileUrl) {
		S3Manager s3Manager = s3ManagerProvider.getIfAvailable();
		if (s3Manager != null) {
			s3Manager.deleteQuietly(fileUrl);
		}
	}

	private S3Manager requireS3() {
		S3Manager s3Manager = s3ManagerProvider.getIfAvailable();
		if (s3Manager == null) {
			throw new IllegalStateException("S3 is not enabled. Set cloud.aws.s3.enabled=true in application-local.yml");
		}
		return s3Manager;
	}
}
