package com.mealbridge.domain.donor;

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
@Table(name = "donor_institutions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DonorInstitution {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

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
}
