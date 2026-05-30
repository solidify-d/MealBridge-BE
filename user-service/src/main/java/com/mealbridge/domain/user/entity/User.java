package com.mealbridge.domain.user.entity;

import com.mealbridge.common.domain.BaseIdAndTimeEntity;
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

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE users SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class User extends BaseIdAndTimeEntity {

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(nullable = false, length = 255)
	private String password;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 32)
	private String phone;

	@Column(length = 64)
	private String department;

	@Column(length = 500)
	private String memo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private Role role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private UserStatus status;

	@Column(name = "email_verified", nullable = false)
	private boolean emailVerified;

	@Column(name = "organization_id")
	private UUID organizationId;

	@Column(name = "donor_institution_id")
	private UUID donorInstitutionId;

	@Column(name = "created_by_id")
	private UUID createdById;

	@Builder
	public User(String email, String password, String name, String phone, String department, String memo,
			Role role, UserStatus status, boolean emailVerified, UUID organizationId, UUID donorInstitutionId,
			UUID createdById) {
		this.email = email;
		this.password = password;
		this.name = name;
		this.phone = phone;
		this.department = department;
		this.memo = memo;
		this.role = role;
		this.status = status;
		this.emailVerified = emailVerified;
		this.organizationId = organizationId;
		this.donorInstitutionId = donorInstitutionId;
		this.createdById = createdById;
	}

	public void markEmailVerified() {
		this.emailVerified = true;
	}

	public void updateStatus(UserStatus status) {
		this.status = status;
	}

	public void updateMemo(String memo) {
		this.memo = memo;
	}
}
