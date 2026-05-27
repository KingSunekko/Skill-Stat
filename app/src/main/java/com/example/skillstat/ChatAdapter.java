package com.example.skillstat;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.skillstat.models.Message;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("h:mm a", Locale.US);

    private List<Message> messageList;
    private String currentUid;
    private String receiverAvatar;

    public ChatAdapter(List<Message> messageList, String receiverAvatar) {
        this.messageList = messageList;
        this.receiverAvatar = receiverAvatar;
        this.currentUid = FirebaseAuth.getInstance().getUid();
    }

    @Override
    public int getItemViewType(int position) {
        if (messageList.get(position).getSenderId().equals(currentUid)) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == TYPE_SENT) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_sent, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        Context context = holder.itemView.getContext();
        
        // Image Handling
        if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
            holder.ivMessageImage.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(message.getImageUrl())
                    .centerCrop()
                    .into(holder.ivMessageImage);
        } else {
            holder.ivMessageImage.setVisibility(View.GONE);
        }

        // Text Handling
        if (message.getMessage() != null && !message.getMessage().isEmpty()) {
            holder.tvMessage.setVisibility(View.VISIBLE);
            holder.tvMessage.setText(message.getMessage());
        } else {
            holder.tvMessage.setVisibility(View.GONE);
        }
        
        holder.tvTime.setText(TIME_FORMAT.format(new Date(message.getTimestamp())));

        if (getItemViewType(position) == TYPE_RECEIVED && holder.ivAvatar != null) {
            int resId = context.getResources().getIdentifier(receiverAvatar, "drawable", context.getPackageName());
            if (resId != 0) {
                holder.ivAvatar.setImageResource(resId);
            } else {
                holder.ivAvatar.setImageResource(R.drawable.prof1); // Fallback
            }
        }

        if (holder.tvStatus != null) {
            if (message.isSeen()) {
                holder.tvStatus.setText("✓✓");
                holder.tvStatus.setTextColor(Color.WHITE);
            } else {
                holder.tvStatus.setText("✓");
                holder.tvStatus.setTextColor(Color.parseColor("#B0B0B0"));
            }
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvStatus;
        ImageView ivMessageImage, ivAvatar;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvStatus = itemView.findViewById(R.id.tv_status);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            ivMessageImage = itemView.findViewById(R.id.iv_message_image);
        }
    }
}
