package com.mealbridge.domain.procurement.dto;

import java.util.List;

public record ProcurementAdviseResDto(
		String summary,
		List<String> highlights,
		List<String> cautions,
		boolean mock) {
}
