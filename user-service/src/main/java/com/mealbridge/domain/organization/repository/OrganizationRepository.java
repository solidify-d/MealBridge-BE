package com.mealbridge.domain.organization.repository;

import com.mealbridge.domain.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
}
