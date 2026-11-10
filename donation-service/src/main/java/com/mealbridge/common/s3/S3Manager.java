package com.mealbridge.common.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "cloud.aws.s3", name = "enabled", havingValue = "true")
public class S3Manager {

	private final S3Client s3Client;

	@Value("${cloud.aws.s3.bucket}")
	private String bucket;

	public String upload(MultipartFile file, String prefix) {
		String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
		String key = String.format("%s/%s-%s", prefix, UUID.randomUUID(), originalName);

		try {
			PutObjectRequest request = PutObjectRequest.builder()
					.bucket(bucket)
					.key(key)
					.contentType(file.getContentType())
					.build();

			s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
			return s3Client.utilities().getUrl(builder -> builder.bucket(bucket).key(key)).toExternalForm();
		} catch (S3Exception e) {
			if (e.statusCode() == 403) {
				throw new IllegalArgumentException(
						"S3 access denied for bucket '%s'. Check IAM s3:PutObject on MealConnect/*".formatted(bucket),
						e);
			}
			throw new IllegalArgumentException("S3 upload failed: " + e.awsErrorDetails().errorMessage(), e);
		} catch (IOException e) {
			throw new IllegalArgumentException("Image upload failed", e);
		}
	}

	public void deleteQuietly(String fileUrl) {
		if (fileUrl == null || fileUrl.isBlank()) {
			return;
		}
		try {
			delete(fileUrl);
		} catch (Exception ignored) {
		}
	}

	public void delete(String fileUrl) {
		String key = extractKeyFromUrl(fileUrl);
		DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
				.bucket(bucket)
				.key(key)
				.build();
		s3Client.deleteObject(deleteRequest);
	}

	private String extractKeyFromUrl(String fileUrl) {
		int index = fileUrl.indexOf(".amazonaws.com/");
		if (index == -1) {
			throw new IllegalArgumentException("Invalid S3 URL format");
		}
		String encodedKey = fileUrl.substring(index + ".amazonaws.com/".length());
		return URLDecoder.decode(encodedKey, StandardCharsets.UTF_8);
	}
}
