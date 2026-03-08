package com.example.codebase;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class EventPoster {
    private String posterImageBase64; //raw image encoded as text so Firestore can store it.

    public EventPoster() {}

    public EventPoster(String posterImageBase64) {
        this.posterImageBase64 = posterImageBase64;
    }

    public static Bitmap decodeImage(String base64Image) {
        byte[] bytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public String getPosterImageBase64() {
        return posterImageBase64;
    }

    public void setPosterImageBase64(String posterImageBase64) {
        this.posterImageBase64 = posterImageBase64;
    }
}