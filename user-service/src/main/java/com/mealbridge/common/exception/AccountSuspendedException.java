package com.mealbridge.common.exception;

import com.mealbridge.common.auth.dto.SuspendedAccountInfo;
import lombok.Getter;

@Getter
public class AccountSuspendedException extends RuntimeException {

	private final SuspendedAccountInfo info;

	public AccountSuspendedException(SuspendedAccountInfo info) {
		super("Account suspended");
		this.info = info;
	}
}
