package com.mealbridge.common.auth.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationEmailSender {

	private final JavaMailSender mailSender;
	private final EmailVerificationProperties properties;

	public void sendCode(String toEmail, String code) {
		if (!properties.enabled()) {
			log.info("[email-verification] Mail disabled. Code for {}: {}", toEmail, code);
			return;
		}

		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(formatFrom());
			helper.setTo(toEmail);
			helper.setSubject(VerificationEmailTemplate.subject());
			String plain = VerificationEmailTemplate.plainText(code, properties.codeTtlMinutes());
			String html = VerificationEmailTemplate.html(code, properties.codeTtlMinutes());
			helper.setText(plain, html);
			mailSender.send(message);
			log.info("[email-verification] Sent HTML verification code to {}", toEmail);
		} catch (MessagingException e) {
			throw new IllegalStateException("Failed to send verification email", e);
		}
	}

	private String formatFrom() {
		if (properties.fromAddress() == null || properties.fromAddress().isBlank()) {
			return properties.fromName();
		}
		return "%s <%s>".formatted(properties.fromName(), properties.fromAddress());
	}
}
