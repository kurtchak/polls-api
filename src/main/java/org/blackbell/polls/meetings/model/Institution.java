package org.blackbell.polls.meetings.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.meetings.model.common.BaseEntity;

import javax.persistence.*;

/**
 * Created by Ján Korčák on 23.3.2018.
 * email: korcak@esten.sk
 */
@Entity
public class Institution extends BaseEntity {

    @JsonView(value = {})
    @Column(unique = true)
    private String ref;

    @Enumerated(EnumType.STRING)
    private InstitutionType type;

    @JsonView(value = {})
    private String name;

    @JsonView(value = {})
    private String description;

    public Institution() {}

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public InstitutionType getType() {
        return type;
    }

    public void setType(InstitutionType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
