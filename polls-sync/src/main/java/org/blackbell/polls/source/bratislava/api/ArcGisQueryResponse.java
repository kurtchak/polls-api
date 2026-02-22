package org.blackbell.polls.source.bratislava.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ArcGisQueryResponse {

    private List<ArcGisFeature> features;
    private boolean exceededTransferLimit;

    public List<ArcGisFeature> getFeatures() { return features; }
    public void setFeatures(List<ArcGisFeature> features) { this.features = features; }

    public boolean isExceededTransferLimit() { return exceededTransferLimit; }
    public void setExceededTransferLimit(boolean exceededTransferLimit) { this.exceededTransferLimit = exceededTransferLimit; }
}
