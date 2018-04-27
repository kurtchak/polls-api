package org.blackbell.polls.domain.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonView;
import org.blackbell.polls.domain.api.Views;
import org.blackbell.polls.domain.model.common.NamedEntity;
import org.blackbell.polls.source.Source;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Ján Korčák on 6.3.2017.
 * email: korcak@esten.sk
 */
@Entity
public class Town extends NamedEntity {

    @Enumerated(EnumType.STRING)
    private Source source;

    @JsonView(value = {Views.Towns.class})
    @JsonFormat(pattern = "dd.MM.yyyy HH:mm:ss")
    @Temporal(TemporalType.DATE)
    private Date lastSyncDate;

    @JsonView(value = {Views.Towns.class, Views.Club.class})
    public String getRef() {
        return ref;
    }

    @JsonView(value = {Views.Towns.class, Views.Club.class})
    public String getName() {
        return name;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Date getLastSyncDate() {
        return lastSyncDate;
    }

    public void setLastSyncDate(Date lastSyncDate) {
        this.lastSyncDate = lastSyncDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Town)) return false;

        Town town = (Town) o;

        return getId() == town.getId();

    }

    @Override
    public int hashCode() {
        return (int) (getId() ^ (getId() >>> 32));
    }

    @Override
    public String toString() {
        return "Town{" +
                "id=" + id +
                ", ref='" + ref + '\'' +
                ", name='" + name + '\'' +
                ", source=" + source +
                ", lastSyncDate=" + lastSyncDate +
                '}';
    }
}
