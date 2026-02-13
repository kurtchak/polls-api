package org.blackbell.polls.domain.model.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Created by kurtcha on 18.4.2018.
 */
@MappedSuperclass
public class NamedEntity extends EntityWithReference {

    @Column(length = 1000)
    protected String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NamedEntity)) return false;

        NamedEntity that = (NamedEntity) o;

        return getName() != null ? getName().equals(that.getName()) : that.getName() == null;
    }

    @Override
    public int hashCode() {
        return getName() != null ? getName().hashCode() : 0;
    }
}
