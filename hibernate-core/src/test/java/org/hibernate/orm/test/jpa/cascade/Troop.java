/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

//$Id$
package org.hibernate.orm.test.jpa.cascade;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import org.hibernate.annotations.OrderBy;

/**
 * @author Emmanuel Bernard
 */
@Entity
public class Troop implements Serializable {
	private Integer id;
	private String name;
	private Set<Soldier> soldiers;

	@OneToMany(mappedBy = "troop", cascade = {CascadeType.ALL}, fetch = FetchType.LAZY)
	@OrderBy("name desc")
	@org.hibernate.annotations.Cascade({org.hibernate.annotations.CascadeType.DELETE_ORPHAN})
	public Set<Soldier> getSoldiers() {
		return soldiers;
	}

	public void setSoldiers(Set<Soldier> soldiers) {
		this.soldiers = soldiers;
	}

	@Id
	@GeneratedValue
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void addSoldier(Soldier s) {
		if ( soldiers == null ) soldiers = new HashSet<Soldier>();
		soldiers.add( s );
		s.setTroop( this );
	}
}
