package org.blackbell.polls.domain.model.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Created by kurtcha on 18.4.2018.
 */
@MappedSuperclass
public class EntityWithReference extends BaseEntity {

    @Column(unique = true)
    protected String ref;

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }
}
