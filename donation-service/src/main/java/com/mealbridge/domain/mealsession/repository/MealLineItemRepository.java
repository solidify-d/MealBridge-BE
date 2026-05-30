package com.mealbridge.domain.mealsession.repository;

import com.mealbridge.domain.mealsession.entity.MealLineItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MealLineItemRepository extends JpaRepository<MealLineItem, UUID> {

	List<MealLineItem> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

	List<MealLineItem> findBySessionIdInOrderBySessionIdAscCreatedAtAsc(Collection<UUID> sessionIds);

	void deleteBySessionId(UUID sessionId);

	void deleteBySessionIdIn(Collection<UUID> sessionIds);
}
