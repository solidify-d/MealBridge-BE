package com.mealbridge.common.s3;

import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

public final class DonationImageValidator {

	private static final long MAX_BYTES = 10L * 1024 * 1024;

	private DonationImageValidator() {
	}

	public static void validate(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Image file is required");
		}
		String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
		if (!contentType.startsWith("image/")) {
			throw new IllegalArgumentException("Only image files are allowed");
		}
		if (file.getSize() > MAX_BYTES) {
			throw new IllegalArgumentException("Image must be 10MB or smaller");
		}
	}
}
