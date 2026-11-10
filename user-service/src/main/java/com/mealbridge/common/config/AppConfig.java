package com.mealbridge.common.config;

import com.mealbridge.common.auth.config.AuthCookieProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
		AuthCookieProperties.class,
		SupportProperties.class,
		ProfileImageCacheProperties.class
})
public class AppConfig {
}
