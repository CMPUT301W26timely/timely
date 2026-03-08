package com.example.codebase;

public class EventPoster {
    private String posterImageUrl;
    private String qrCodeUrl;

    public EventPoster() {}

    public EventPoster(String posterImageUrl, String qrCodeUrl) {
        this.posterImageUrl = posterImageUrl;
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getPosterImageUrl() {
        return posterImageUrl;
    }

    public void setPosterImageUrl(String posterImageUrl) {
        this.posterImageUrl = posterImageUrl;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }
}
