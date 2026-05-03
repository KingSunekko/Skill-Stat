package com.example.skillstat;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder> {

    private final List<String> avatars;
    private int selectedPosition = 0;
    private final OnAvatarClickListener listener;

    public interface OnAvatarClickListener {
        void onAvatarClick(int position);
    }

    public AvatarAdapter(List<String> avatars, OnAvatarClickListener listener) {
        this.avatars = avatars;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avatar, parent, false);
        return new AvatarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        holder.tvEmoji.setText(avatars.get(position));
        
        // Reset translation before applying new state to avoid cumulative issues during recycling
        holder.container.setTranslationY(0f);
        holder.container.setTranslationZ(0f);

        // Handle selection UI and persistent "hover" elevation
        if (selectedPosition == position) {
            holder.container.setBackgroundResource(R.drawable.shape_avatar_selected);
            // Animate to elevated state
            AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(holder.itemView.getContext(), R.animator.avatar_selected_anim);
            set.setTarget(holder.container);
            set.start();
        } else {
            holder.container.setBackgroundResource(R.drawable.shape_avatar_bg);
            // Animate back to default state
            AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(holder.itemView.getContext(), R.animator.avatar_deselected_anim);
            set.setTarget(holder.container);
            set.start();
        }

        holder.itemView.setOnClickListener(v -> {
            if (selectedPosition == holder.getAdapterPosition()) return;

            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onAvatarClick(selectedPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return avatars.size();
    }

    static class AvatarViewHolder extends RecyclerView.ViewHolder {
        TextView tvEmoji;
        View container;

        AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmoji = itemView.findViewById(R.id.tv_avatar_emoji);
            container = itemView.findViewById(R.id.avatar_container);
        }
    }
}