package com.mealbridge.domain.donor.entity;

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
@Table(name = "donor_institutions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE donor_institutions SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class DonorInstitution extends BaseIdAndTimeEntity {

	@Column(nullable = false, length = 200)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private DonorInstitutionType type;

	@Column(nullable = false, length = 500)
	private String address;

	@Column(name = "institution_phone", nullable = false, length = 32)
	private String institutionPhone;

	@Column(length = 1000)
	private String description;

	@Column(name = "business_registration_number", length = 64)
	private String businessRegistrationNumber;

	@Column(name = "unit_identifier", length = 64)
	private String unitIdentifier;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private InstitutionStatus status;

	@Builder
	public DonorInstitution(String name, DonorInstitutionType type, String address, String institutionPhone,
			String description, String businessRegistrationNumber, String unitIdentifier,
			InstitutionStatus status) {
		this.name = name;
		this.type = type;
		this.address = address;
		this.institutionPhone = institutionPhone;
		this.description = description;
		this.businessRegistrationNumber = businessRegistrationNumber;
		this.unitIdentifier = unitIdentifier;
		this.status = status;
	}

	public void updateStatus(InstitutionStatus status) {
		this.status = status;
	}

	public void updateProfile(String name, String address, String institutionPhone, String description,
			String businessRegistrationNumber, String unitIdentifier) {
		if (name != null && !name.isBlank()) {
			this.name = name;
		}
		if (address != null && !address.isBlank()) {
			this.address = address;
		}
		if (institutionPhone != null && !institutionPhone.isBlank()) {
			this.institutionPhone = institutionPhone;
		}
		this.description = description;
		this.businessRegistrationNumber = businessRegistrationNumber;
		this.unitIdentifier = unitIdentifier;
	}
}
