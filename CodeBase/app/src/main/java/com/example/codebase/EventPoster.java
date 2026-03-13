package com.example.codebase;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Represents an event poster image stored in a Base64-encoded format.
 *
 * <p>This class handles the storage and decoding of event poster images.
 * Images are encoded as Base64 strings to allow storage in Firestore,
 * which does not natively support binary data.</p>
 */
public class EventPoster {

    /**
     * The Base64-encoded string representation of the poster image.
     * Stored as text to enable compatibility with Firestore's data model.
     */
    private String posterImageBase64;

    /**
     * Default no-argument constructor required for Firestore deserialization.
     */
    public EventPoster() {}

    /**
     * Constructs an {@code EventPoster} with the specified Base64-encoded image string.
     *
     * @param posterImageBase64 the Base64-encoded string representation of the poster image;
     *                          must not be {@code null} or empty
     */
    public EventPoster(String posterImageBase64) {
        this.posterImageBase64 = posterImageBase64;
    }

    /**
     * Decodes a Base64-encoded image string into an Android {@link Bitmap}.
     *
     * <p>This static utility method converts a Base64 string back into a
     * {@link Bitmap} object suitable for display in an {@code ImageView}
     * or other UI components.</p>
     *
     * @param base64Image the Base64-encoded string to decode; must not be {@code null}
     * @return a {@link Bitmap} decoded from the provided Base64 string,
     *         or {@code null} if decoding fails
     */
    public static Bitmap decodeImage(String base64Image) {
        byte[] bytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * Returns the Base64-encoded string representation of the poster image.
     *
     * @return the Base64-encoded poster image string, or {@code null} if not set
     */
    public String getPosterImageBase64() {
        return posterImageBase64;
    }

    /**
     * Sets the Base64-encoded string representation of the poster image.
     *
     * @param posterImageBase64 the Base64-encoded string to set;
     *                          must not be {@code null} or empty
     */
    public void setPosterImageBase64(String posterImageBase64) {
        this.posterImageBase64 = posterImageBase64;
    }
}