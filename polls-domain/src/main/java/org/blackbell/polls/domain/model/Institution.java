package org.blackbell.polls.domain.model;

import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.model.common.NamedEntity;
import org.blackbell.polls.domain.model.enums.InstitutionType;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Created by Ján Korčák on 23.3.2018.
 * email: korcak@esten.sk
 */
@Entity
public class Institution extends NamedEntity {

    @Enumerated(EnumType.STRING)
    private InstitutionType type;

    @JsonView(value = {})
    private String description;

    public Institution() {}

    public String getRef() {
        return ref;
    }

    public String getName() {
        return name;
    }

    public InstitutionType getType() {
        return type;
    }

    public void setType(InstitutionType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Institution)) return false;

        Institution that = (Institution) o;

        return id == that.id;

    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public String toString() {
        return "Institution{" +
                "id=" + id +
                ", ref='" + ref + '\'' +
                ", type=" + type +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
