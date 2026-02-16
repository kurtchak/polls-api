package org.blackbell.polls.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "dm.api")
public class DmApiProperties {

    private String baseUrl = "https://www.digitalnemesto.sk/DmApi/";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getCitiesUrl() {
        return baseUrl + "Obce/";
    }

    public String getMeetingsUrl(String city, String institution, String season) {
        return baseUrl + "GetDZZasadnutie/" + institution + "/mesto-" + city
                + "?VolebneObdobie=" + season + "&format=json";
    }

    public String getCommissionMeetingsUrl(String city, String season) {
        return baseUrl + "GetDZKomisie/mesto-" + city
                + "?VolebneObdobie=" + season + "&format=json";
    }

    public String getMeetingDetailUrl(String meetingId) {
        return baseUrl + "GetDZZaKoDet/" + meetingId + "?format=json";
    }

    public String getPollDetailUrl(String agendaItemId, String pollRoute) {
        return baseUrl + "GetDZHlas/" + agendaItemId + "/" + pollRoute + "?format=json";
    }

    public String getSeasonsUrl(String city) {
        return baseUrl + "GetDZVolebneObdobie/mesto-" + city + "?format=json";
    }
}
