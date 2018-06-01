package com.system73.polynet.android.sample.model;

public class Channel {

    private Integer id;
    private String channelId;
    private String manifestUrl;
    private String apiKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public void setManifestUrl(String manifestUrl) {
        this.manifestUrl = manifestUrl;
    }

    public Integer getId() {
        return id;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getManifestUrl() {
        return manifestUrl;
    }



}
