/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * @author Steve Ebersole
 */
@Entity(name = "OrderSupplemental2")
@Table(name = "order_supp2")
public class OrderSupplemental2 {
	private Integer oid;
	private Integer receivablesId;

	private Order order;

	public OrderSupplemental2() {
	}

	public OrderSupplemental2(Integer oid, Integer receivablesId) {
		this.oid = oid;
		this.receivablesId = receivablesId;
	}

	@Id
	@Column(name = "oid")
	public Integer getOid() {
		return oid;
	}

	public void setOid(Integer oid) {
		this.oid = oid;
	}

	public Integer getReceivablesId() {
		return receivablesId;
	}

	public void setReceivablesId(Integer receivablesId) {
		this.receivablesId = receivablesId;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@MapsId
	public Order getOrder() {
		return order;
	}

	public void setOrder(Order order) {
		this.order = order;
	}
}
