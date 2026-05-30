package com.mealbridge.domain.organization;

import com.mealbridge.domain.common.InstitutionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "organizations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Organization {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 200)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private OrganizationType type;

	@Column(nullable = false, length = 500)
	private String address;

	@Column(name = "organization_phone", nullable = false, length = 32)
	private String organizationPhone;

	@Column(name = "service_area", length = 200)
	private String serviceArea;

	@Column(name = "recipient_capacity")
	private Integer recipientCapacity;

	@Enumerated(EnumType.STRING)
	@Column(name = "target_demographic", length = 32)
	private TargetDemographic targetDemographic;

	@Column(name = "registration_number", length = 64)
	private String registrationNumber;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private InstitutionStatus status;

	@Builder
	public Organization(String name, OrganizationType type, String address, String organizationPhone,
			String serviceArea, Integer recipientCapacity, TargetDemographic targetDemographic,
			String registrationNumber, InstitutionStatus status) {
		this.name = name;
		this.type = type;
		this.address = address;
		this.organizationPhone = organizationPhone;
		this.serviceArea = serviceArea;
		this.recipientCapacity = recipientCapacity;
		this.targetDemographic = targetDemographic;
		this.registrationNumber = registrationNumber;
		this.status = status;
	}

	public void updateStatus(InstitutionStatus status) {
		this.status = status;
	}
}
