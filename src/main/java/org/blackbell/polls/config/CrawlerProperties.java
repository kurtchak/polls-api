package org.blackbell.polls.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "crawler")
public class CrawlerProperties {

    private String presovMembersUrl = "https://www.presov.sk/poslanci-msz.html";
    private String popradMembersUrl = "https://www.poprad.sk/kontakty?SearchModel.SearchText=&SearchModel.Department=Poslanci_MsZ";
    private String trnavaMembersUrl = "https://www.trnava.sk/stranka/mestske-zastupitelstvo";
    private String trnavaMeetingsUrl = "https://www.trnava.sk/zasadnutia/msz";
    private String trnavaMeetingDetailUrl = "https://www.trnava.sk/zasadnutie/{id}";
    private String kosiceMembersUrl = "https://www.kosice.sk/mesto/samosprava/mestske-zastupitelstvo/{season}/poslanci";
    private String kosiceMeetingsUrl = "https://www.kosice.sk/mesto/samosprava/mestske-zastupitelstvo/{season}";
    private String kosiceMemberDetailUrl = "https://www.kosice.sk/mesto/samosprava/mestske-zastupitelstvo/{season}/poslanec/{slug}";
    private String nitraMembersUrl = "https://nitra.sk/mestske-zastupitelstvo/";
    private String bbMembersUrl = "https://www.banskabystrica.sk/poslanci/?per_page=72";
    private String trencinMembersUrl = "https://trencin.sk/samosprava/mestske-zastupitelstvo/poslankyne-a-poslanci-trencina/";
    private int timeoutMs = 30000;

    public String getPresovMembersUrl() {
        return presovMembersUrl;
    }

    public void setPresovMembersUrl(String presovMembersUrl) {
        this.presovMembersUrl = presovMembersUrl;
    }

    public String getPopradMembersUrl() {
        return popradMembersUrl;
    }

    public void setPopradMembersUrl(String popradMembersUrl) {
        this.popradMembersUrl = popradMembersUrl;
    }

    public String getTrnavaMembersUrl() {
        return trnavaMembersUrl;
    }

    public void setTrnavaMembersUrl(String trnavaMembersUrl) {
        this.trnavaMembersUrl = trnavaMembersUrl;
    }

    public String getTrnavaMeetingsUrl() {
        return trnavaMeetingsUrl;
    }

    public void setTrnavaMeetingsUrl(String trnavaMeetingsUrl) {
        this.trnavaMeetingsUrl = trnavaMeetingsUrl;
    }

    public String getTrnavaMeetingDetailUrl() {
        return trnavaMeetingDetailUrl;
    }

    public void setTrnavaMeetingDetailUrl(String trnavaMeetingDetailUrl) {
        this.trnavaMeetingDetailUrl = trnavaMeetingDetailUrl;
    }

    public String getKosiceMembersUrl() {
        return kosiceMembersUrl;
    }

    public void setKosiceMembersUrl(String kosiceMembersUrl) {
        this.kosiceMembersUrl = kosiceMembersUrl;
    }

    public String getKosiceMeetingsUrl() {
        return kosiceMeetingsUrl;
    }

    public void setKosiceMeetingsUrl(String kosiceMeetingsUrl) {
        this.kosiceMeetingsUrl = kosiceMeetingsUrl;
    }

    public String getKosiceMemberDetailUrl() {
        return kosiceMemberDetailUrl;
    }

    public void setKosiceMemberDetailUrl(String kosiceMemberDetailUrl) {
        this.kosiceMemberDetailUrl = kosiceMemberDetailUrl;
    }

    public String getNitraMembersUrl() {
        return nitraMembersUrl;
    }

    public void setNitraMembersUrl(String nitraMembersUrl) {
        this.nitraMembersUrl = nitraMembersUrl;
    }

    public String getBbMembersUrl() {
        return bbMembersUrl;
    }

    public void setBbMembersUrl(String bbMembersUrl) {
        this.bbMembersUrl = bbMembersUrl;
    }

    public String getTrencinMembersUrl() {
        return trencinMembersUrl;
    }

    public void setTrencinMembersUrl(String trencinMembersUrl) {
        this.trencinMembersUrl = trencinMembersUrl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
