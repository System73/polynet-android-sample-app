package com.system73.polynet.android.sample.model;

public class Channel {

    private Integer id;
    private int channelId;
    private String manifestUrl;
    private String backendUrl;
    private String backendMetricsUrl;
    private String stunServerUrl;

    public void setId(int id) {
        this.id = id;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
    }

    public void setManifestUrl(String manifestUrl) {
        this.manifestUrl = manifestUrl;
    }

    public void setBackendUrl(String backendUrl) {
        this.backendUrl = backendUrl;
    }

    public void setBackendMetricsUrl(String backendMetricsUrl) {
        this.backendMetricsUrl = backendMetricsUrl;
    }

    public void setStunServerUrl(String stunServerUrl) {
        this.stunServerUrl = stunServerUrl;
    }

    public Integer getId() {
        return id;
    }

    public int getChannelId() {
        return channelId;
    }

    public String getManifestUrl() {
        return manifestUrl;
    }

    public String getBackendUrl() {
        return backendUrl;
    }

    public String getBackendMetricsUrl() {
        return backendMetricsUrl;
    }

    public String getStunServerUrl() {
        return stunServerUrl;
    }


}
