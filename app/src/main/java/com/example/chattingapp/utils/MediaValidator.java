package com.example.chattingapp.utils;

/**
 * Media validation utility - enforces allowed file types
 */
public class MediaValidator {
    
    // Allowed MIME types
    private static final String[] ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png"};
    private static final String[] ALLOWED_AUDIO_TYPES = {"audio/mpeg", "audio/mp3", "audio/wav", "audio/ogg", "audio/aac"};
    
    // Video is NOT allowed
    private static final String VIDEO_PREFIX = "video/";
    
    /**
     * Validates if the media type is allowed
     * @param mimeType The MIME type of the file
     * @return true if allowed, false otherwise
     */
    public static boolean isAllowed(String mimeType) {
        if (mimeType == null) return false;
        
        // Block all video types
        if (mimeType.startsWith(VIDEO_PREFIX)) {
            return false;
        }
        
        // Check if image type
        for (String allowed : ALLOWED_IMAGE_TYPES) {
            if (allowed.equals(mimeType)) return true;
        }
        
        // Check if audio type
        for (String allowed : ALLOWED_AUDIO_TYPES) {
            if (allowed.equals(mimeType)) return true;
        }
        
        return false;
    }
    
    /**
     * Checks if MIME type is an image
     */
    public static boolean isImage(String mimeType) {
        if (mimeType == null) return false;
        for (String allowed : ALLOWED_IMAGE_TYPES) {
            if (allowed.equals(mimeType)) return true;
        }
        return false;
    }
    
    /**
     * Checks if MIME type is audio
     */
    public static boolean isAudio(String mimeType) {
        if (mimeType == null) return false;
        for (String allowed : ALLOWED_AUDIO_TYPES) {
            if (allowed.equals(mimeType)) return true;
        }
        return false;
    }
    
    /**
     * Returns error message for blocked content
     */
    public static String getBlockedMessage(String mimeType) {
        if (mimeType != null && mimeType.startsWith(VIDEO_PREFIX)) {
            return "Video files are not allowed in this app.";
        }
        return "This file type is not supported. Only images (JPEG/PNG) and audio files are allowed.";
    }
}
