package org.blackbell.polls;

/**
 * Created by Ján Korčák on 18.2.2017.
 * email: korcak@esten.sk
 */

import org.blackbell.polls.data.repositories.CouncilMemberRepository;
import org.blackbell.polls.data.repositories.SeasonRepository;
import org.blackbell.polls.data.repositories.TownRepository;
import org.blackbell.polls.meetings.model.CouncilMember;
import org.blackbell.polls.meetings.model.Season;
import org.blackbell.polls.meetings.model.Town;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;

@SpringBootApplication
public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public List<Town> loadTowns(TownRepository repository) {
        List<Town> towns = repository.findAll();
        DataContext.addTowns(towns);
        return towns;
    }

    @Bean
    public Map<Season, Map<String, CouncilMember>> loadMembers(CouncilMemberRepository repository) {
        for (CouncilMember cm : repository.findAll()) {
            DataContext.addMember(cm.getSeason(), cm);
        }
        return DataContext.getAllMembersMap();
    }
}