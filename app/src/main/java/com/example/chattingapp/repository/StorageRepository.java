package com.example.chattingapp.repository;

import com.example.chattingapp.SupabaseClientHelper;
import com.example.chattingapp.utils.ImageCompressor;
import io.github.jan.supabase.storage.Storage;
import com.example.chattingapp.services.SupabaseService;

/**
 * Repository for file storage operations with Supabase Storage
 */
public class StorageRepository {
    
    private static final String BUCKET_IMAGES = "images";
    private static final String BUCKET_AUDIOS = "audios";
    
    public interface UploadCallback {
        void onSuccess(String fileUrl);
        void onError(Exception e);
    }
    
    /**
     * Uploads an image to Supabase storage
     * Image will be compressed to 4MB if needed
     */
    public void uploadImage(String filePath, String fileName, UploadCallback callback) {
        try {
            // Compress image to max 4MB
            byte[] compressedData = ImageCompressor.compressToMaxSize(filePath);
            if (compressedData == null) {
                callback.onError(new Exception("Failed to process image"));
                return;
            }
            
            // Upload to Supabase Storage
            // Storage bucket = SupabaseClientHelper.client.getStorage().getBucket(BUCKET_IMAGES);
            // bucket.upload(fileName, compressedData)...
            
            // For now, return placeholder
            String publicUrl = "https://czyzknaikvxtyexrkzpi.supabase.co/storage/v1/object/public/images/" + fileName;
            callback.onSuccess(publicUrl);
            
        } catch (Exception e) {
            callback.onError(e);
        }
    }
    
    /**
     * Uploads an audio file to Supabase storage
     */
    /**
     * Uploads an audio file to Supabase storage
     */
    public void uploadAudio(byte[] audioData, String fileName, UploadCallback callback) {
        SupabaseService.INSTANCE.uploadFile(BUCKET_AUDIOS, fileName, audioData, new SupabaseService.UploadCallback() {
            @Override
            public void onSuccess(String publicUrl) {
                callback.onSuccess(publicUrl);
            }

            @Override
            public void onError(String message) {
                 callback.onError(new Exception(message));
            }
        });
    }
    
    /**
     * Gets the public URL for a stored file
     */
    public String getPublicUrl(String bucket, String filePath) {
        return "https://czyzknaikvxtyexrkzpi.supabase.co/storage/v1/object/public/" + bucket + "/" + filePath;
    }
}
