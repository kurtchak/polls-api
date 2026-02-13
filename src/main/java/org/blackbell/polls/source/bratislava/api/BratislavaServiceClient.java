package org.blackbell.polls.source.bratislava.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BratislavaServiceClient {

    private static final Logger log = LoggerFactory.getLogger(BratislavaServiceClient.class);

    private static final String ARCGIS_QUERY_URL =
            "https://services8.arcgis.com/pRlN1m0su5BYaFAS/arcgis/rest/services/" +
            "Hlasovania_poslancov_a_poslank%C3%BD%C5%88_Mestsk%C3%A9ho_zastupite%C4%BEstva_od_11_12_2014/" +
            "FeatureServer/0/query";

    private static final int PAGE_SIZE = 2000;

    private static final RestTemplate restTemplate = new RestTemplate();

    public static List<ArcGisVoteRecord> queryDistinctPeriods() throws Exception {
        String url = ARCGIS_QUERY_URL +
                "?where=1%3D1" +
                "&outFields=" + encode("Volebné_obdobie") +
                "&returnDistinctValues=true&f=json";
        log.info("Querying distinct electoral periods: {}", url);
        return queryAll(url);
    }

    public static List<ArcGisVoteRecord> queryDistinctDatesForPeriod(int period) throws Exception {
        String url = ARCGIS_QUERY_URL +
                "?where=" + encode("Volebné_obdobie=" + period) +
                "&outFields=" + encode("Dátum") +
                "&returnDistinctValues=true&f=json";
        log.info("Querying distinct dates for period {}: {}", period, url);
        return queryAll(url);
    }

    public static List<ArcGisVoteRecord> queryRecordsForDate(int period, String date) throws Exception {
        String where = encode("Volebné_obdobie=" + period + " AND Dátum='" + date + "'");
        String url = ARCGIS_QUERY_URL +
                "?where=" + where +
                "&outFields=*&f=json";
        log.info("Querying records for period {} date {}", period, date);
        return queryAllPaginated(url);
    }

    public static List<ArcGisVoteRecord> queryRecordsForPoll(int period, String date, int pollSequence) throws Exception {
        String where = encode("Volebné_obdobie=" + period +
                " AND Dátum='" + date + "'" +
                " AND Poradie_hlasovania=" + pollSequence);
        String url = ARCGIS_QUERY_URL +
                "?where=" + where +
                "&outFields=*&f=json";
        log.info("Querying poll records: period={} date={} poll={}", period, date, pollSequence);
        return queryAll(url);
    }

    public static List<ArcGisVoteRecord> queryDistinctMembersForPeriod(int period) throws Exception {
        String url = ARCGIS_QUERY_URL +
                "?where=" + encode("Volebné_obdobie=" + period) +
                "&outFields=" + encode("Meno,Priezvisko,Poslanecký_klub") +
                "&returnDistinctValues=true&f=json";
        log.info("Querying distinct members for period {}", period);
        return queryAll(url);
    }

    private static List<ArcGisVoteRecord> queryAll(String url) throws Exception {
        ArcGisQueryResponse response = restTemplate.getForObject(url, ArcGisQueryResponse.class);
        if (response == null || response.getFeatures() == null) {
            return new ArrayList<>();
        }
        return response.getFeatures().stream()
                .map(ArcGisFeature::getAttributes)
                .toList();
    }

    private static List<ArcGisVoteRecord> queryAllPaginated(String baseUrl) throws Exception {
        List<ArcGisVoteRecord> allRecords = new ArrayList<>();
        int offset = 0;

        while (true) {
            String url = baseUrl + "&resultOffset=" + offset + "&resultRecordCount=" + PAGE_SIZE;
            ArcGisQueryResponse response = restTemplate.getForObject(url, ArcGisQueryResponse.class);
            if (response == null || response.getFeatures() == null || response.getFeatures().isEmpty()) {
                break;
            }
            for (ArcGisFeature feature : response.getFeatures()) {
                allRecords.add(feature.getAttributes());
            }
            if (!response.isExceededTransferLimit()) {
                break;
            }
            offset += PAGE_SIZE;
            log.debug("Pagination: fetched {} records so far, offset={}", allRecords.size(), offset);
        }

        log.info("Total records fetched: {}", allRecords.size());
        return allRecords;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
