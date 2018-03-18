package com.comp3004.beacon;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;

public class Chat implements Parcelable {
    String key;
    ArrayList<Message> messages = new ArrayList<>();
    HashMap<String, String> members = new HashMap<>();

    public Chat() {

    }

    protected Chat(Parcel in) {
        key = in.readString();
        in.readTypedList(messages, Message.CREATOR);
        int x = in.readInt();
        String[] keys = new String[x];
        String[] values = new String[x];
        in.readStringArray(keys);
        in.readStringArray(values);
        members = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            members.put(keys[i], values[i]);
        }
    }

    public static final Creator<Chat> CREATOR = new Creator<Chat>() {
        @Override
        public Chat createFromParcel(Parcel in) {
            return new Chat(in);
        }

        @Override
        public Chat[] newArray(int size) {
            return new Chat[size];
        }
    };

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public ArrayList<Message> getMessages() {
        return messages;
    }

    public HashMap<String, String> getMembers() {
        return members;
    }

    public void setMembers(HashMap<String, String> members) {
        this.members = members;
    }

    public void setMessages(ArrayList<Message> messages) {
        this.messages = messages;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(key);
        parcel.writeTypedList(messages);
        String[] keys = members.keySet().toArray(new String[members.size()]);
        String[] values = new String[members.size()];
        for (int x = 0; x < keys.length; x++) {
            values[x] = members.get(keys[x]);
        }
        parcel.writeInt(members.size());
        parcel.writeStringArray(keys);
        parcel.writeStringArray(values);
    }

    public Message getLastMessage() {
        if (messages.size() > 1)
            return messages.get(messages.size() - 1);
        return null;
    }
}
