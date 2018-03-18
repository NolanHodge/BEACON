package com.comp3004.beacon.GUI;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.comp3004.beacon.BeaconDatabase;
import com.comp3004.beacon.Chat;
import com.comp3004.beacon.LocationManagement.LocationService;
import com.comp3004.beacon.Message;
import com.comp3004.beacon.R;
import com.comp3004.beacon.User.BeaconUser;
import com.comp3004.beacon.User.CurrentBeaconUser;
import com.facebook.Profile;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class NewChatActivity extends AppCompatActivity {
    Chat chat;
    ArrayList<Message> messages = new ArrayList<>();
    ArrayList<BeaconUser> members = new ArrayList<>();
    HashMap<String, Bitmap> bitmaps = new HashMap<>();
    ListView chatListView;
    EditText chatTextbox;
    ChatAdapter adapter;
    String names = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.chat_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);


        chat = getIntent().getParcelableExtra("chat");

        chatListView = (ListView) findViewById(R.id.chatListView);
        chatTextbox = (EditText) findViewById(R.id.chatEditText);
        chatListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
        BeaconDatabase.getUsers().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    BeaconUser beaconUser = snapshot.getValue(BeaconUser.class);

                    if (chat.getMembers().get(beaconUser.getUserId()) != null &&
                            !beaconUser.getUserId().equals(CurrentBeaconUser.getInstance().getUserId())) {
                        names += beaconUser.getDisplayName() + ", ";
                        members.add(beaconUser);

                    }
                }
                members.add(CurrentBeaconUser.getInstance());
                toolbar.setTitle(names.substring(0, names.length() - 2));
                new getMultipleImages().execute(members);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public void sendMessage(View view) {
        if (chatTextbox.getText().length() < 1) {
            return;
        }
        Message message = new Message();
        message.setFrom(CurrentBeaconUser.getInstance().getUserId());
        message.setDateSent(new Date().getTime());
        message.setBody(chatTextbox.getText().toString());
        message.setRead(false);

        ArrayList temp = chat.getMessages();
        temp.add(message);
        chat.setMessages(temp);

        chatTextbox.setText("");
        BeaconDatabase.getChat(chat.getKey()).setValue(chat);
    }

    private class ChatAdapter extends ArrayAdapter<Message> {
        public ChatAdapter() {
            super(NewChatActivity.this, 0, messages);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.chat_left, parent, false);

                viewHolder.message = (TextView) convertView.findViewById(R.id.text1);
                viewHolder.leftIcon = (ImageView) convertView.findViewById(R.id.user_icon);
                viewHolder.rightIcon = (ImageView) convertView.findViewById(R.id.other_icon);
                viewHolder.layout = convertView.findViewById(R.id.chat_container);
                convertView.setTag(viewHolder);

            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            if (position == 0) {
                if (getItem(position).getFrom().equals(CurrentBeaconUser.getInstance().getUserId())) {
                    if (bitmaps.get(CurrentBeaconUser.getInstance().getUserId()) != null)
                        viewHolder.rightIcon.setImageBitmap(bitmaps.get(CurrentBeaconUser.getInstance().getUserId()));
                    viewHolder.rightIcon.setVisibility(View.VISIBLE);
                    viewHolder.leftIcon.setVisibility(View.GONE);
                } else {
                    if (bitmaps.get(getItem(position).getFrom()) != null)
                        viewHolder.leftIcon.setImageBitmap(bitmaps.get(getItem(position).getFrom()));
                    viewHolder.rightIcon.setVisibility(View.GONE);
                    viewHolder.leftIcon.setVisibility(View.VISIBLE);
                }
            } else {
                //if message is from the same person, don't show the icon
                if (getItem(position).getFrom().equals(getItem(position - 1).getFrom())) {
                    viewHolder.layout.setPadding(0, 2, 0, 0);
                    if (getItem(position).getFrom().equals(CurrentBeaconUser.getInstance().getUserId())) {
                        viewHolder.rightIcon.setVisibility(View.INVISIBLE);
                        viewHolder.leftIcon.setVisibility(View.GONE);
                    } else {
                        viewHolder.leftIcon.setVisibility(View.INVISIBLE);
                        viewHolder.rightIcon.setVisibility(View.GONE);
                    }
                } else {
                    viewHolder.layout.setPadding(0, 10, 0, 0);
                    if (getItem(position).getFrom().equals(CurrentBeaconUser.getInstance().getUserId())) {
                        if (bitmaps.get(CurrentBeaconUser.getInstance().getUserId()) != null)
                            viewHolder.rightIcon.setImageBitmap(bitmaps.get(CurrentBeaconUser.getInstance().getUserId()));
                        viewHolder.rightIcon.setVisibility(View.VISIBLE);
                        viewHolder.leftIcon.setVisibility(View.GONE);
                    } else {
                        if (bitmaps.get(getItem(position).getFrom()) != null)
                            viewHolder.leftIcon.setImageBitmap(bitmaps.get(getItem(position).getFrom()));
                        viewHolder.rightIcon.setVisibility(View.GONE);
                        viewHolder.leftIcon.setVisibility(View.VISIBLE);
                    }
                }
            }

            viewHolder.message.setText(getItem(position).getBody());

            return convertView;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.menu_request_beacon) {
            //MessageSenderHandler.getInstance().sendLocationRequest(otherChatParticipantId);
            return true;
        } else if (item.getItemId() == R.id.menu_send_beacon) {
            final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            LocationService locationService = new LocationService() {
                @Override
                public void onLocationChanged(Location location) {
                    try {
                        locationManager.removeUpdates(this);
                    } catch (SecurityException e) {
                    }


                    //MessageSenderHandler.getInstance().sendBeaconRequest(otherChatParticipantId, MyLocationManager.getInstance().getMyLocation());
                }
            };
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, locationService);
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_chat, menu);
        return super.onCreateOptionsMenu(menu);
    }

    private static class ViewHolder {
        public TextView message;
        public ImageView leftIcon;
        public ImageView rightIcon;
        public View layout;
    }

    private class getMultipleImages extends AsyncTask<ArrayList<BeaconUser>, Void, HashMap<String, Bitmap>> {
        @Override
        protected HashMap<String, Bitmap> doInBackground(ArrayList<BeaconUser>... params) {
            HashMap<String, Bitmap> icons = new HashMap<>();
            for (BeaconUser u : params[0]) {
                try {
                    InputStream in = new java.net.URL(u.getPhotoUrl()).openStream();
                    icons.put(u.getUserId(), BitmapFactory.decodeStream(in));
                    in.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return icons;
        }

        @Override
        protected void onPostExecute(HashMap<String, Bitmap> stringBitmapHashMap) {
            bitmaps = stringBitmapHashMap;
            System.out.println(bitmaps);
            adapter = new ChatAdapter();
            chatListView.setAdapter(adapter);

            BeaconDatabase.getChatMessages(chat.getKey()).addChildEventListener(new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    Message message = dataSnapshot.getValue(Message.class);
                    adapter.add(message);
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {

                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }
}
