package com.example.chattingapp.repository;

import android.os.Handler;
import android.os.Looper;

import com.example.chattingapp.model.Message;
import com.example.chattingapp.services.MessageDto;
import com.example.chattingapp.services.SupabaseService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatRepository {

    public interface ChatCallback {
        void onMessagesReceived(List<Message> messages);
        void onNewMessage(Message message);
        void onError(String error);
    }

    private ChatCallback callback;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isSubscribed = false;

    public void init(ChatCallback callback) {
        this.callback = callback;
        SupabaseService.INSTANCE.init();
    }

    public void loadGlobalMessages() {
        SupabaseService.INSTANCE.fetchGlobalMessages(new SupabaseService.MessageCallback() {
            @Override
            public void onMessagesReceived(List<MessageDto> messageDtos) {
                List<Message> messages = new ArrayList<>();
                for (MessageDto dto : messageDtos) {
                    messages.add(mapToDomain(dto));
                }
                
                mainHandler.post(() -> {
                   if (callback != null) callback.onMessagesReceived(messages);
                });
            }

            @Override
            public void onError(String error) {
                mainHandler.post(() -> {
                   if (callback != null) callback.onError(error);
                });
            }

            @Override
            public void onNewMessage(MessageDto message) {
                // Not used in fetch
            }
        });
    }

    public void subscribeToRealtime() {
        if (isSubscribed) return;
        isSubscribed = true;
        
        SupabaseService.INSTANCE.subscribeToGlobalMessages(new SupabaseService.MessageCallback() {
            @Override
            public void onMessagesReceived(List<MessageDto> messages) {
                // Not used in subscribe
            }

            @Override
            public void onError(String error) {
                 mainHandler.post(() -> {
                   if (callback != null) callback.onError(error);
                });
            }

            @Override
            public void onNewMessage(MessageDto dto) {
                Message message = mapToDomain(dto);
                mainHandler.post(() -> {
                   if (callback != null) callback.onNewMessage(message);
                });
            }
        });
    }

    public void sendGlobalMessage(String senderId, String senderName, String senderAvatarUrl, String text, String type, String fileUrl, long duration) {
        SupabaseService.INSTANCE.sendGlobalMessage(senderId, senderName, senderAvatarUrl, text, type, fileUrl, duration);
    }

    private Message mapToDomain(MessageDto dto) {
        Message m = new Message();
        m.setId(dto.getId());
        m.setSenderId(dto.getSender_id());
        m.setSenderName(dto.getSender_name());
        m.setSenderAvatarUrl(dto.getSender_avatar_url());
        m.setMessage(dto.getMessage());
        m.setType(dto.getType());
        m.setFileUrl(dto.getFile_url());
        if (dto.getDuration() != null) {
            m.setDuration(dto.getDuration());
        }
        
        // Parse date
        try {
            // ISO 8601 parsing might be needed
            // For now, if string parsing fails, use current date as fallback or specialized parser
            // Simple approach: Supabase sends ISO string. 
            // We can leave timestamp null and adapter can show "Now" or we parse it
             if (dto.getCreated_at() != null) {
                  // ISO 8601 Parser (Basic)
                  // m.setTimestamp(...)
                  // Often specific parser needed for varying precisions
             }
        } catch (Exception e) {}

        // Fallback
        if (m.getTimestamp() == null) {
             m.setTimestamp(new java.util.Date());
        }
        
        return m;
    }
}
