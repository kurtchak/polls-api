package org.blackbell.polls.domain.model.common;

import javax.persistence.MappedSuperclass;

/**
 * Created by kurtcha on 18.4.2018.
 */
@MappedSuperclass
public class NamedEntity extends EntityWithReference {

    protected String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
