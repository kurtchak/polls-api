package org.blackbell.polls.meetings.model;

import java.util.Map;

/**
 * Created by Ján Korčák on 19.2.2017.
 * email: korcak@esten.sk
 */
public class Agenda {

    private Map<Integer, AgendaItem> items;

    public Map<Integer, AgendaItem> getItems() {
        return items;
    }

    public void setItems(Map<Integer, AgendaItem> items) {
        this.items = items;
    }
}
