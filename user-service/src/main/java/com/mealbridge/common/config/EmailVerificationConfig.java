package com.mealbridge.common.config;

import com.mealbridge.common.auth.email.EmailVerificationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EmailVerificationProperties.class)
public class EmailVerificationConfig {
}
