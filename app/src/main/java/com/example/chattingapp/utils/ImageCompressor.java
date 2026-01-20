package com.example.chattingapp.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ImageCompressor {
    
    private static final int MAX_SIZE_BYTES = 4 * 1024 * 1024; // 4MB
    
    /**
     * Compresses an image to be under 4MB
     * @param imagePath Path to the original image
     * @return Compressed image as byte array
     */
    public static byte[] compressToMaxSize(String imagePath) {
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        if (bitmap == null) return null;
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 100;
        
        // Start with PNG, if too large, switch to JPEG with decreasing quality
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        
        while (baos.toByteArray().length > MAX_SIZE_BYTES && quality > 10) {
            baos.reset();
            quality -= 10;
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        }
        
        // If still too large, scale down the image
        if (baos.toByteArray().length > MAX_SIZE_BYTES) {
            float scale = (float) Math.sqrt((double) MAX_SIZE_BYTES / baos.toByteArray().length);
            int newWidth = (int) (bitmap.getWidth() * scale);
            int newHeight = (int) (bitmap.getHeight() * scale);
            
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            baos.reset();
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            scaledBitmap.recycle();
        }
        
        bitmap.recycle();
        return baos.toByteArray();
    }
    
    /**
     * Checks if file is valid image type (JPEG/PNG only)
     */
    public static boolean isValidImageType(String mimeType) {
        return "image/jpeg".equals(mimeType) || "image/png".equals(mimeType);
    }
    
    /**
     * Checks if file is a video (not allowed)
     */
    public static boolean isVideo(String mimeType) {
        return mimeType != null && mimeType.startsWith("video/");
    }
}
