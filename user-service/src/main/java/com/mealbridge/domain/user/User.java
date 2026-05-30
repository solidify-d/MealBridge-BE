package com.mealbridge.domain.user;

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
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(nullable = false, length = 255)
	private String password;

	@Column(nullable = false, length = 100)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private UserStatus status;

	@Column(name = "organization_id")
	private UUID organizationId;

	@Column(name = "donor_institution_id")
	private UUID donorInstitutionId;

	@Column(name = "created_by_id")
	private UUID createdById;

	@Builder
	public User(String email, String password, String name, Role role, UserStatus status,
			UUID organizationId, UUID donorInstitutionId, UUID createdById) {
		this.email = email;
		this.password = password;
		this.name = name;
		this.role = role;
		this.status = status;
		this.organizationId = organizationId;
		this.donorInstitutionId = donorInstitutionId;
		this.createdById = createdById;
	}

	public void updateStatus(UserStatus status) {
		this.status = status;
	}
}
