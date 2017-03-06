package org.blackbell.polls;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */

import org.blackbell.polls.context.ApplicationContext;
import org.blackbell.polls.meetings.model.*;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
public class MeetingsController {

    @RequestMapping("/{city}/{season}/meetings")
    public Collection<Meeting> meetings(@PathVariable(value="city") String city,
                                        @PathVariable(value="season") String season) {
        Application.checkLoaded(city);
        return ApplicationContext.getInstance().getMeetings(city, season);
    }

    @RequestMapping("/{city}/{season}/meeting/{order}")
    public Meeting meeting(@PathVariable(value="city") String city,
                           @PathVariable(value="season") String season,
                           @PathVariable(value="order") Integer order) {
        Application.checkLoaded(city);
        return ApplicationContext.getInstance().getMeeting(city, season, order);
    }

    @RequestMapping("/{city}/{season}/meeting/{order}/agenda")
    public Agenda agenda(@PathVariable(value="city") String city,
                         @PathVariable(value="season") String season,
                         @PathVariable(value="order") Integer order) {
        Application.checkLoaded(city);
        return ApplicationContext.getInstance().getAgenda(city, season, order);
    }

    @RequestMapping("/{city}/{season}/meeting/{order}/agenda/{item}")
    public AgendaItem agendaItem(@PathVariable(value="city") String city,
                                 @PathVariable(value="season") String season,
                                 @PathVariable(value="order") Integer order,
                                 @PathVariable(value="item") Integer item) {
        Application.checkLoaded(city);
        return ApplicationContext.getInstance().getAgendaItem(city, season, order, item);
    }

    @RequestMapping("/{city}/{season}/meeting/{order}/attachments")
    public Collection<MeetingAttachment> atachments(@PathVariable(value="city") String city,
                                              @PathVariable(value="season") String season,
                                              @PathVariable(value="order") Integer order) {
        Application.checkLoaded(city);
        return ApplicationContext.getInstance().getAttachments(city, season, order);
    }

    @RequestMapping("/{city}/{season}/meeting/{order}/attachment/{item}")
    public MeetingAttachment attachment(@PathVariable(value="city") String city,
                                 @PathVariable(value="season") String season,
                                 @PathVariable(value="order") Integer order,
                                 @PathVariable(value="item") Integer item) {
        Application.checkLoaded(city);
        return ApplicationContext.getInstance().getMeetingAttachment(city, season, order, item);
    }

    @RequestMapping("/{city}/{season}/members")
    public Collection<CouncilMember> members(@PathVariable(value="city") String city,
                                             @PathVariable(value="season") String season) {
        Application.checkLoaded(city);
        return ApplicationContext.getInstance().getMembers(city, season);
    }

}