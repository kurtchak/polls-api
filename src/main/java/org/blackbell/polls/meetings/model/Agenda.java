package org.blackbell.polls.meetings.model;

import javax.persistence.Entity;
import java.util.Map;

/**
 * Created by Ján Korčák on 19.2.2017.
 * email: korcak@esten.sk
 */
@Entity
public class Agenda {

    private Map<Integer, AgendaItem> items;

    public Map<Integer, AgendaItem> getItems() {
        return items;
    }

    public void setItems(Map<Integer, AgendaItem> items) {
        this.items = items;
    }
}
