package com.mealbridge.domain.organization.entity;

import com.mealbridge.common.domain.BaseIdAndTimeEntity;
import com.mealbridge.domain.common.entity.InstitutionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "organizations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE organizations SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Organization extends BaseIdAndTimeEntity {

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

	public void updateProfile(String name, String address, String organizationPhone, String serviceArea,
			Integer recipientCapacity, TargetDemographic targetDemographic, String registrationNumber) {
		if (name != null && !name.isBlank()) {
			this.name = name;
		}
		if (address != null && !address.isBlank()) {
			this.address = address;
		}
		if (organizationPhone != null && !organizationPhone.isBlank()) {
			this.organizationPhone = organizationPhone;
		}
		this.serviceArea = serviceArea;
		this.recipientCapacity = recipientCapacity;
		this.targetDemographic = targetDemographic;
		this.registrationNumber = registrationNumber;
	}
}
