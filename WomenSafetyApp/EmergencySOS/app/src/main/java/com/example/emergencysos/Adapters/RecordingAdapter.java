package com.example.emergencysos.Adapters;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencysos.Models.RecordingModel;
import com.example.emergencysos.R;

import java.io.IOException;
import java.util.List;

public class RecordingAdapter extends RecyclerView.Adapter<RecordingAdapter.ViewHolder> {

    private Context context;
    private List<RecordingModel> recordingList;
    private MediaPlayer mediaPlayer;

    public RecordingAdapter(Context context, List<RecordingModel> recordingList) {
        this.context = context;
        this.recordingList = recordingList;
    }

    @NonNull
    @Override
    public RecordingAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recording_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordingAdapter.ViewHolder holder, int position) {
        RecordingModel model = recordingList.get(position);
        holder.tvFileName.setText(model.fileName);
        holder.tvDuration.setText(model.duration);

        holder.btnPlay.setOnClickListener(v -> {
            playAudio(model.downloadUrl);
        });

        holder.btnDelete.setOnClickListener(v -> {
            context.getSharedPreferences("deleted_recordings", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(model.downloadUrl, true)
                    .apply();

            recordingList.remove(position);
            notifyItemRemoved(position);
            Toast.makeText(context, "Removed from this device only", Toast.LENGTH_SHORT).show();
        });

    }

    private void playAudio(String url) {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );
            mediaPlayer.setDataSource(context, Uri.parse(url));
            mediaPlayer.prepare();
            mediaPlayer.start();

            Toast.makeText(context, "Playing audio...", Toast.LENGTH_SHORT).show();

            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
                Toast.makeText(context, "Playback finished", Toast.LENGTH_SHORT).show();
            });

        } catch (IOException e) {
            Toast.makeText(context, "Failed to play: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int getItemCount() {
        return recordingList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFileName, tvDuration;
        ImageView btnPlay, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            btnPlay = itemView.findViewById(R.id.btnPlay);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
