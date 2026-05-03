package com.example.campuseventdiscoverysystem.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campuseventdiscoverysystem.R;
import com.example.campuseventdiscoverysystem.chat.ChatMessage;

import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<ChatMessageAdapter.VH> {

    private final List<ChatMessage> messages;

    public ChatMessageAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).type;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == ChatMessage.TYPE_USER)
                ? R.layout.item_chat_user
                : R.layout.item_chat_bot;
        View v = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.tvMessage.setText(messages.get(position).text);
    }

    @Override
    public int getItemCount() { return messages.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvMessage;
        VH(@NonNull View v) {
            super(v);
            tvMessage = v.findViewById(R.id.tvMessage);
        }
    }
}
