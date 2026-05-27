package com.example.skillstat.models;

public class ChatMessage {
    private String text;
    private boolean sent;
    private long timestamp;

    public ChatMessage() {
        // Required for Firebase
    }

    public ChatMessage(String text, boolean sent, long timestamp) {
        this.text = text;
        this.sent = sent;
        this.timestamp = timestamp;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
