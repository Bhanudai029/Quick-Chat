package com.example.chattingapp.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.chattingapp.adapter.ChatAdapter;
import com.example.chattingapp.databinding.FragmentChatBinding;
import com.example.chattingapp.model.Message;
import com.example.chattingapp.repository.ChatRepository;
import com.example.chattingapp.repository.StorageRepository;
import com.example.chattingapp.utils.MediaValidator;
import com.example.chattingapp.utils.NetworkHelper;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import com.example.chattingapp.utils.UserManager;

public class ChatFragment extends Fragment implements ChatRepository.ChatCallback {

    private FragmentChatBinding binding;
    private ChatAdapter adapter;
    private ChatRepository chatRepository;
    private StorageRepository storageRepository;
    private UserManager userManager;
    
    private String currentUserId;
    private String currentUserName;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> audioPickerLauncher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        userManager = new UserManager(requireContext());
        currentUserId = userManager.getUserId();
        currentUserName = userManager.getUserName();
        
        if (currentUserName == null || currentUserName.isEmpty()) {
            currentUserName = "Anonymous";
        }
        
        chatRepository = new ChatRepository();
        storageRepository = new StorageRepository();
        
        // Image picker result
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        handleImageSelected(imageUri);
                    }
                }
            }
        );

        // Audio picker result
        audioPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri audioUri = result.getData().getData();
                    if (audioUri != null) {
                        handleAudioSelected(audioUri);
                    }
                }
            }
        );
    }

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        setupRecyclerView();
        setupInputBar();
        setupNetworkListener();
        
        chatRepository.init(this);
        chatRepository.loadGlobalMessages();
        chatRepository.subscribeToRealtime();

        return root;
    }
    
    private void setupRecyclerView() {
        adapter = new ChatAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        binding.recyclerViewChat.setLayoutManager(layoutManager);
        binding.recyclerViewChat.setAdapter(adapter);
    }
    
    private void setupInputBar() {
        binding.btnSend.setOnClickListener(v -> {
            String text = binding.editMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                sendTextMessage(text);
                binding.editMessage.setText("");
            }
        });
        
        binding.btnImage.setOnClickListener(v -> {
            openImagePicker();
        });

        binding.btnAudio.setOnClickListener(v -> {
            openAudioPicker();
        });
    }
    
    private void setupNetworkListener() {
        NetworkHelper.getInstance().setNetworkCallback(isConnected -> {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    // Update connection UI status if needed
                });
            }
        });
    }
    
    private void sendTextMessage(String text) {
        // Optimistic update not needed for global chat usually, but good for UX.
        // However, since we listen to realtime, we might get duplicate if we add locally too unless we check IDs.
        // For simplicity, let's wait for realtime echo or add locally and dedupe?
        // Simplest: Send to backend, backend triggers realtime, we show it. 
        // To avoid lag, we can add locally as "Sending..." but simpler here:
        
        // Since we don't have a backend Profile User table link yet, we are not sending avatar URL here explicitly unless we store it in prefs.
        // But requested output logic needs it.
        // For now, let's assume no URL is sent, so default avatar drawable will be used by receivers.
        // If we want persistence, we'd need to upload profile image to storage and save URL in prefs.
        chatRepository.sendGlobalMessage(currentUserId, currentUserName, null, text, "text", null, 0);
    }
    
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Image"));
    }

    private void openAudioPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        audioPickerLauncher.launch(Intent.createChooser(intent, "Select Audio"));
    }

    private void handleAudioSelected(Uri audioUri) {
         try {
            long durationMillis = 0;
            try {
                // Get duration
                android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                retriever.setDataSource(getContext(), audioUri);
                String time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (time != null) {
                    durationMillis = Long.parseLong(time);
                }
                retriever.release();
            } catch (Exception e) {
                e.printStackTrace(); // Log but don't stop upload
            }
            
            final long finalDuration = durationMillis;

            // read bytes
            java.io.InputStream iStream = requireContext().getContentResolver().openInputStream(audioUri);
            byte[] inputData = com.example.chattingapp.services.SupabaseService.INSTANCE.readBytes(iStream);
            
            String fileName = "audio_" + UUID.randomUUID().toString() + ".mp3";
            Toast.makeText(getContext(), "Uploading audio... (" + (finalDuration/1000) + "s)", Toast.LENGTH_SHORT).show();
            
            storageRepository.uploadAudio(inputData, fileName, new StorageRepository.UploadCallback() {
                @Override
                public void onSuccess(String fileUrl) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            chatRepository.sendGlobalMessage(currentUserId, currentUserName, null, "Sent an audio message", "audio", fileUrl, finalDuration);
                            Toast.makeText(getContext(), "Audio sent!", Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void onError(Exception e) {
                     if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
            
         } catch(Exception e) {
             e.printStackTrace();
             Toast.makeText(getContext(), "Failed to read file", Toast.LENGTH_SHORT).show();
         }
    }
    
    private void handleImageSelected(Uri imageUri) {
        String mimeType = getContext().getContentResolver().getType(imageUri);
        if (!MediaValidator.isAllowed(mimeType)) {
            Toast.makeText(getContext(), MediaValidator.getBlockedMessage(mimeType), Toast.LENGTH_LONG).show();
            return;
        }
        
        // TODO: Implement Storage Upload then send message
        Toast.makeText(getContext(), "Image upload coming soon!", Toast.LENGTH_SHORT).show();
    }
    
    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            binding.recyclerViewChat.smoothScrollToPosition(adapter.getItemCount() - 1);
        }
    }
    
    private void updateEmptyState() {
        if (adapter.getItemCount() > 0) {
            binding.emptyState.setVisibility(View.GONE);
            binding.recyclerViewChat.setVisibility(View.VISIBLE);
        } else {
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.recyclerViewChat.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null) {
            adapter.releaseAudio();
        }
        binding = null;
    }

    // ChatCallback Implementation

    @Override
    public void onMessagesReceived(java.util.List<Message> messages) {
        // Flag messages as sent by me based on ID
        for (Message m : messages) {
            m.setSentByMe(currentUserId.equals(m.getSenderId()));
        }
        
        adapter.setMessages(messages);
        updateEmptyState();
        scrollToBottom();
    }

    @Override
    public void onNewMessage(Message message) {
        message.setSentByMe(currentUserId.equals(message.getSenderId()));
        adapter.addMessage(message);
        updateEmptyState();
        scrollToBottom();
    }

    @Override
    public void onError(String error) {
        Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
    }
}
