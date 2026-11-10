package com.mealbridge.domain.registration.dto;

import jakarta.validation.constraints.Size;

public record RejectRegistrationReq(@Size(max = 500) String reason) {
}
