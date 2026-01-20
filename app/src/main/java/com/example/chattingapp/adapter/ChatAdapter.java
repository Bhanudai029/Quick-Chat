package com.example.chattingapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.SeekBar;
import android.os.Handler;
import android.os.Looper;
import java.util.Locale;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.chattingapp.R;
import com.example.chattingapp.model.Message;
import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messages = new ArrayList<>();

    public void setMessages(List<Message> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    public void addMessage(Message message) {
        this.messages.add(message);
        notifyItemInserted(messages.size() - 1);
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        return message.isSentByMe() ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        if (holder.getItemViewType() == VIEW_TYPE_SENT) {
            ((SentMessageHolder) holder).bind(message);
        } else {
            ((ReceivedMessageHolder) holder).bind(message);
        }
    }

    // Static media player
    private static android.media.MediaPlayer mediaPlayer;
    private static String currentPlayingUrl = "";
    private static Handler audioHandler = new Handler(Looper.getMainLooper());
    private static Runnable audioRunnable;
    
    // UI references for current playing item
    private SeekBar currentSeekBar;
    private TextView currentTimer;
    private ImageView currentPlayBtn;

    private void formatTime(TextView textView, int current, int total) {
        String currStr = String.format(Locale.getDefault(), "%02d:%02d", current / 60000, (current % 60000) / 1000);
        String totalStr = String.format(Locale.getDefault(), "%02d:%02d", total / 60000, (total % 60000) / 1000);
        textView.setText(currStr + " / " + totalStr);
    }

    public void releaseAudio() {
        if (mediaPlayer != null) {
             try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
             } catch(Exception e) {}
             mediaPlayer.release();
             mediaPlayer = null;
        }
        if (audioRunnable != null) {
            audioHandler.removeCallbacks(audioRunnable);
        }
        if (currentPlayBtn != null) {
            currentPlayBtn.setImageResource(android.R.drawable.ic_media_play);
        }
        currentPlayingUrl = "";
        currentSeekBar = null;
        currentTimer = null;
        currentPlayBtn = null;
    }

    private static boolean isSeeking = false;

    private void playAudio(String url, SeekBar seekBar, TextView timerText, ImageView playBtn) {
        // If clicking same audio which is playing, pause/stop
        if (url.equals(currentPlayingUrl) && mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                playBtn.setImageResource(android.R.drawable.ic_media_play);
            } else {
                mediaPlayer.start();
                playBtn.setImageResource(android.R.drawable.ic_media_pause);
                updateSeekBar();
            }
            return;
        }

        // New audio
        releaseAudio();
        
        currentSeekBar = seekBar;
        currentTimer = timerText;
        currentPlayBtn = playBtn;
        currentPlayingUrl = url;

        // Setup SeekBar listener for user seeking
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    formatTime(timerText, progress, mediaPlayer.getDuration());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                }
                isSeeking = false;
            }
        });

        try {
            mediaPlayer = new android.media.MediaPlayer();
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                playBtn.setImageResource(android.R.drawable.ic_media_pause);
                if (currentSeekBar != null) {
                    currentSeekBar.setMax(mp.getDuration());
                }
                updateSeekBar();
            });
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                // Handle error, reset UI
                playBtn.setImageResource(android.R.drawable.ic_media_play);
                if (currentSeekBar != null) currentSeekBar.setProgress(0);
                if (currentTimer != null) currentTimer.setText("00:00");
                releaseAudio();
                return true; // Error handled
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                playBtn.setImageResource(android.R.drawable.ic_media_play);
                if (currentSeekBar != null) currentSeekBar.setProgress(0);
                // Keep the socket open or release? Let's release to save resources
                // releaseAudio(); // Or just reset UI
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateSeekBar() {
        if (mediaPlayer == null || !mediaPlayer.isPlaying()) return;
        
        audioRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    if (!isSeeking) {
                        int currentPosition = mediaPlayer.getCurrentPosition();
                        int totalDuration = mediaPlayer.getDuration();
                        
                        if (currentSeekBar != null) {
                            currentSeekBar.setProgress(currentPosition);
                        }
                        if (currentTimer != null) {
                            formatTime(currentTimer, currentPosition, totalDuration);
                        }
                    }
                    audioHandler.postDelayed(this, 100);
                }
            }
        };
        audioHandler.post(audioRunnable);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }
    
    // ... getItemCount

    class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView textBody;
        ImageView imageBody;
        View audioLayout;
        ImageView btnPlayAudio;
        SeekBar audioSeekBar;
        TextView audioTimer;

        SentMessageHolder(View itemView) {
            super(itemView);
            textBody = itemView.findViewById(R.id.text_message_body);
            imageBody = itemView.findViewById(R.id.image_message_body);
            audioLayout = itemView.findViewById(R.id.audio_layout);
            btnPlayAudio = itemView.findViewById(R.id.btn_play_audio);
            audioSeekBar = itemView.findViewById(R.id.audio_seekbar);
            audioTimer = itemView.findViewById(R.id.audio_timer);
        }

        void bind(Message message) {
            textBody.setVisibility(View.GONE);
            imageBody.setVisibility(View.GONE);
            audioLayout.setVisibility(View.GONE);

            if ("image".equals(message.getType())) {
                imageBody.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext()).load(message.getFileUrl()).into(imageBody);
            } else if ("audio".equals(message.getType())) {
                audioLayout.setVisibility(View.VISIBLE);
                // Reset UI state
                btnPlayAudio.setImageResource(android.R.drawable.ic_media_play);
                audioSeekBar.setProgress(0);
                
                if (message.getDuration() > 0) {
                     long durationSec = message.getDuration() / 1000;
                     String totalStr = String.format(Locale.getDefault(), "%02d:%02d", durationSec / 60, durationSec % 60);
                     audioTimer.setText("00:00 / " + totalStr);
                } else {
                    audioTimer.setText("00:00");
                    // Fetch duration from URL asynchronously
                    if (message.getFileUrl() != null) {
                        final TextView timer = audioTimer;
                        new Thread(() -> {
                            try {
                                android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                                retriever.setDataSource(message.getFileUrl(), new java.util.HashMap<>());
                                String time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                                retriever.release();
                                if (time != null) {
                                    long durationMs = Long.parseLong(time);
                                    long durationSec = durationMs / 1000;
                                    String totalStr = String.format(Locale.getDefault(), "%02d:%02d", durationSec / 60, durationSec % 60);
                                    timer.post(() -> timer.setText("00:00 / " + totalStr));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                }
                
                // If this specific message is currently playing, update UI to match state
                if (message.getFileUrl() != null && message.getFileUrl().equals(currentPlayingUrl)) {
                     if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                         btnPlayAudio.setImageResource(android.R.drawable.ic_media_pause);
                         // Re-link current references to this bound view holder (handling recycling)
                         currentSeekBar = audioSeekBar;
                         currentTimer = audioTimer;
                         currentPlayBtn = btnPlayAudio;
                     }
                }

                btnPlayAudio.setOnClickListener(v -> playAudio(message.getFileUrl(), audioSeekBar, audioTimer, btnPlayAudio));
            } else {
                textBody.setVisibility(View.VISIBLE);
                textBody.setText(message.getMessage());
            }
        }
    }

    class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView textBody;
        ImageView imageBody;
        ImageView avatar;
        View audioLayout;
        ImageView btnPlayAudio;
        SeekBar audioSeekBar;
        TextView audioTimer;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            textBody = itemView.findViewById(R.id.text_message_body);
            imageBody = itemView.findViewById(R.id.image_message_body);
            avatar = itemView.findViewById(R.id.img_sender_avatar);
            audioLayout = itemView.findViewById(R.id.audio_layout);
            btnPlayAudio = itemView.findViewById(R.id.btn_play_audio);
            audioSeekBar = itemView.findViewById(R.id.audio_seekbar);
            audioTimer = itemView.findViewById(R.id.audio_timer);
        }

        void bind(Message message) {
             // Bind Sender Name
            TextView senderName = itemView.findViewById(R.id.text_sender_name);
            if (senderName != null) {
                if (message.getSenderName() != null && !message.getSenderName().isEmpty()) {
                    senderName.setText(message.getSenderName());
                    senderName.setVisibility(View.VISIBLE);
                } else {
                    senderName.setVisibility(View.GONE);
                }
            }

            // Bind Avatar
            if (avatar != null) {
                String avatarUrl = message.getSenderAvatarUrl();
                String name = message.getSenderName();
                
                if (avatarUrl != null && !avatarUrl.isEmpty()) {
                    Glide.with(itemView.getContext())
                        .load(avatarUrl)
                        .placeholder(new com.example.chattingapp.utils.AvatarDrawable(name))
                        .error(new com.example.chattingapp.utils.AvatarDrawable(name))
                        .into(avatar);
                } else {
                    avatar.setImageDrawable(new com.example.chattingapp.utils.AvatarDrawable(name));
                }
            }

            textBody.setVisibility(View.GONE);
            imageBody.setVisibility(View.GONE);
            audioLayout.setVisibility(View.GONE);

            if ("image".equals(message.getType())) {
                imageBody.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext()).load(message.getFileUrl()).into(imageBody);
            } else if ("audio".equals(message.getType())) {
                audioLayout.setVisibility(View.VISIBLE);
                // Reset UI
                btnPlayAudio.setImageResource(android.R.drawable.ic_media_play);
                audioSeekBar.setProgress(0);
                
                if (message.getDuration() > 0) {
                     long durationSec = message.getDuration() / 1000;
                     String totalStr = String.format(Locale.getDefault(), "%02d:%02d", durationSec / 60, durationSec % 60);
                     audioTimer.setText("00:00 / " + totalStr);
                } else {
                    audioTimer.setText("00:00");
                    // Fetch duration from URL asynchronously
                    if (message.getFileUrl() != null) {
                        final TextView timer = audioTimer;
                        new Thread(() -> {
                            try {
                                android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                                retriever.setDataSource(message.getFileUrl(), new java.util.HashMap<>());
                                String time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                                retriever.release();
                                if (time != null) {
                                    long durationMs = Long.parseLong(time);
                                    long durationSec = durationMs / 1000;
                                    String totalStr = String.format(Locale.getDefault(), "%02d:%02d", durationSec / 60, durationSec % 60);
                                    timer.post(() -> timer.setText("00:00 / " + totalStr));
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).start();
                    }
                }
                
                if (message.getFileUrl() != null && message.getFileUrl().equals(currentPlayingUrl)) {
                     if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                         btnPlayAudio.setImageResource(android.R.drawable.ic_media_pause);
                         currentSeekBar = audioSeekBar;
                         currentTimer = audioTimer;
                         currentPlayBtn = btnPlayAudio;
                     }
                }
                
                btnPlayAudio.setOnClickListener(v -> playAudio(message.getFileUrl(), audioSeekBar, audioTimer, btnPlayAudio));
            } else {
                textBody.setVisibility(View.VISIBLE);
                textBody.setText(message.getMessage());
            }
        }
    }
}
