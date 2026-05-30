package com.mealbridge.common.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SUPER_ADMIN only (NylonMusk {@code ExchangeOnly} pattern).
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('SUPER_ADMIN') and principal != null and @userAccessGuard.isActiveUser(principal.userId())")
public @interface SuperAdminOnly {
}
