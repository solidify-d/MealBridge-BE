package com.mealbridge.common.config;

import com.mealbridge.common.security.GatewayHeaderAuthFilter;
import com.mealbridge.common.security.PublicApiPathMatcher;
import com.mealbridge.common.security.RestAccessDeniedHandler;
import com.mealbridge.common.security.RestAuthenticationEntryPoint;
import com.mealbridge.common.security.UserActiveGuardFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * NylonMusk mkx-platform style: coarse path rules + {@code @PreAuthorize} on controllers.
 * Fine-grained roles (SUPER_ADMIN-only registration approval) live on annotations, not here.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final GatewayHeaderAuthFilter gatewayHeaderAuthFilter;
	private final UserActiveGuardFilter userActiveGuardFilter;
	private final RestAuthenticationEntryPoint authenticationEntryPoint;
	private final RestAccessDeniedHandler accessDeniedHandler;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(new PublicApiPathMatcher()).permitAll()
						.requestMatchers("/error").permitAll()
						.requestMatchers("/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN")
						.anyRequest().authenticated()
				)
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler)
				)
				.addFilterBefore(gatewayHeaderAuthFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(userActiveGuardFilter, GatewayHeaderAuthFilter.class);

		return http.build();
	}
}
