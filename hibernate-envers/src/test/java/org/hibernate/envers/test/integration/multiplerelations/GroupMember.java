/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.multiplerelations;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.envers.Audited;

/**
 * @author Chris Cranford
 */
@Entity
@Audited
public class GroupMember {
    @Id
    @GeneratedValue(generator = "increment")
    @GenericGenerator(name = "increment", strategy = "increment")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "uniqueGroup_id", insertable = false, updatable = false)
    private UniqueGroup uniqueGroup;

    @ManyToMany(mappedBy = "members")
    private List<MultiGroup> multiGroups = new ArrayList<MultiGroup>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UniqueGroup getUniqueGroup() {
        return uniqueGroup;
    }

    public void setUniqueGroup(UniqueGroup uniqueGroup) {
        this.uniqueGroup = uniqueGroup;
    }

    public List<MultiGroup> getMultiGroups() {
        return multiGroups;
    }

    public void setMultiGroups(List<MultiGroup> multiGroups) {
        this.multiGroups = multiGroups;
    }

    public void addMultiGroup(MultiGroup multiGroup) {
        multiGroups.add( multiGroup );
    }

    @Override
    public String toString() {
        return "GroupMember [id=" + id
                + ", uniqueGroup=" + uniqueGroup
                + ", multiGroups.size=" + multiGroups.size()
                + "]";
    }
}
