package com.example.campuseventdiscoverysystem.chat;

/** One message in the assistant chat transcript. */
public class ChatMessage {

    public static final int TYPE_USER = 1;
    public static final int TYPE_BOT  = 2;

    public final int type;
    public final String text;

    public ChatMessage(int type, String text) {
        this.type = type;
        this.text = text;
    }
}
