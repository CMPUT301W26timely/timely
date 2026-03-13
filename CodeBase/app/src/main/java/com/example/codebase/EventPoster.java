package com.example.codebase;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Represents the poster image for an {@link Event}.
 * <p>
 * The image is stored as a Base64-encoded string so that it can be persisted in Firestore without paying
 * </p>
 */
public class EventPoster {
    private String posterImageBase64; //raw image encoded as text so Firestore can store it.

    /**
     * Constructs an empty {@code EventPoster} with no image data.
     */
    public EventPoster() {}

    /**
     * Constructs an {@code EventPoster} with the given Base64-encoded image.
     *
     * @param posterImageBase64 the Base64-encoded string representation of the poster image
     */
    public EventPoster(String posterImageBase64) {
        this.posterImageBase64 = posterImageBase64;
    }

    /**
     * Decodes a Base64-encoded image string into an Android {@link Bitmap}.
     *
     * @param base64Image the Base64-encoded string to decode
     * @return a {@link Bitmap} representation of the decoded image
     */
    public static Bitmap decodeImage(String base64Image) {
        byte[] bytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * Returns the Base64-encoded string representation of the poster image.
     *
     * @return the Base64-encoded poster image string
     */
    public String getPosterImageBase64() {
        return posterImageBase64;
    }

    /**
     * Sets the Base64-encoded string representation of the poster image.
     *
     * @param posterImageBase64 the Base64-encoded poster image string to set
     */
    public void setPosterImageBase64(String posterImageBase64) {
        this.posterImageBase64 = posterImageBase64;
    }
}