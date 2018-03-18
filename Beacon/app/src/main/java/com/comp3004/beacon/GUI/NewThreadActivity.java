package com.comp3004.beacon.GUI;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.comp3004.beacon.BeaconDatabase;
import com.comp3004.beacon.Chat;
import com.comp3004.beacon.Message;
import com.comp3004.beacon.R;
import com.comp3004.beacon.User.BeaconUser;
import com.comp3004.beacon.User.CurrentBeaconUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;

public class NewThreadActivity extends GenericActivity {
    ArrayList<Chat> chats = new ArrayList<>();
    ThreadAdapter threadAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setTitle("Messages");

        threadAdapter = new ThreadAdapter();
        final ListView listView = (ListView) findViewById(R.id.generic_listview);
        listView.setAdapter(threadAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(NewThreadActivity.this, NewChatActivity.class);
                intent.putExtra("chat", chats.get(position));
                startActivity(intent);
            }
        });

        BeaconDatabase.getChats().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                threadAdapter.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Chat chat = snapshot.getValue(Chat.class);

                    if (chat.getMembers().get(CurrentBeaconUser.getInstance().getUserId()) != null) {
                        threadAdapter.add(chat);
                    }
                }
                threadAdapter.notifyDataSetChanged();
                showProgressBar(false);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private class ThreadAdapter extends ArrayAdapter<Chat> {
        public ThreadAdapter() {
            super(NewThreadActivity.this, 0, chats);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder = new ViewHolder();
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.thread_item, parent, false);

                viewHolder.participants = (TextView) convertView.findViewById(R.id.chat_name);
                viewHolder.lastMessage = (TextView) convertView.findViewById(R.id.chat_last_message);
                viewHolder.timeSent = (TextView) convertView.findViewById(R.id.chat_date);
                viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            Chat chat = getItem(position);
            String members = "";
            for (String key : chat.getMembers().keySet()) {
                if (!key.equals(CurrentBeaconUser.getInstance().getUserId())) {
                    members += chat.getMembers().get(key) + ", ";
                }
            }
            Message lastMessage = chat.getMessages().get(chat.getMessages().size() - 1);
            viewHolder.participants.setText(members.substring(0, members.length() - 2));
            if (lastMessage.getFrom().equals(CurrentBeaconUser.getInstance().getUserId())) {
                viewHolder.lastMessage.setText("You: " + lastMessage.getBody());
            } else {
                viewHolder.lastMessage.setText(chat.getMembers().get(lastMessage.getFrom()) + ": " + lastMessage.getBody());
            }
            if (!lastMessage.getRead()) {
                viewHolder.lastMessage.setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                viewHolder.lastMessage.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
            }
            //convert this to readable time
            viewHolder.timeSent.setText(getElapsedTime(new Date().getTime() - lastMessage.getDateSent()));
            return convertView;
        }
    }

    private static class ViewHolder {
        TextView participants, lastMessage, timeSent;
        ImageView icon;
    }

    public static String getElapsedTime(long milliseconds) {
        long elapsedTime = milliseconds / 1000;
        long[] times = new long[4];
        times[3] = elapsedTime; //seconds
        times[2] = times[3] / 60; //minutes
        times[1] = times[2] / 60; //hours
        times[0] = times[1] / 24; //days
        String results = "";
        for (int i = 0; i < times.length; i++) {
            if (times[i] > 0) {

                if (i == 0) results += times[i] + " day";
                else if (i == 1) results += times[i] + " hour";
                else if (i == 2) results += times[i] + " minute";
                else results += times[i] + " second";

                if (times[i] != 1) results += "s";

                results += " ago";
                break;
            }
        }

        return results;
    }
}
