package org.hibernate.jpamodelgen.test.circulartypevariable;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;


@MappedSuperclass
@Access( AccessType.PROPERTY )
@Immutable
public class RoleAccess<
		TRoleAccess extends RoleAccess<TRoleAccess,TUser>,
		TUser extends User<TUser,TRoleAccess>> {

	private Long id;

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
}