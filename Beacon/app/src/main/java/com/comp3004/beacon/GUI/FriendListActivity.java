package com.comp3004.beacon.GUI;

import android.Manifest;

import android.app.SearchManager;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.location.Location;
import android.location.LocationManager;

import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;

import android.os.Bundle;

import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.comp3004.beacon.BeaconDatabase;
import com.comp3004.beacon.Chat;
import com.comp3004.beacon.FirebaseServices.DatabaseManager;
import com.comp3004.beacon.LocationManagement.LocationService;
import com.comp3004.beacon.LocationManagement.MyLocationManager;
import com.comp3004.beacon.Message;
import com.comp3004.beacon.Networking.MailBox;
import com.comp3004.beacon.Networking.MessageSenderHandler;
import com.comp3004.beacon.R;
import com.comp3004.beacon.User.Beacon;
import com.comp3004.beacon.User.BeaconUser;
import com.comp3004.beacon.User.CurrentBeaconUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class FriendListActivity extends AppCompatActivity {

    ArrayList<BeaconUser> friendsList;
    ArrayList<String> userNames;
    ListView friendsListView;
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_list);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.friend_list_toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        DatabaseManager.getInstance().subscribeToMessageThread();
        friendsList = new ArrayList<BeaconUser>();
        userNames = new ArrayList<String>();
        friendsListView = (ListView) findViewById(R.id.friendListView);
        friendsListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.menu_multiple_select, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.send_beacon:

                        SparseBooleanArray sparseBooleanArray = friendsListView.getCheckedItemPositions();
                        for (int i = 0; i < friendsList.size(); i++) {
                            if (sparseBooleanArray.get(i)) {
                                final BeaconUser user = friendsList.get(i);
                                final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                                LocationService locationService = new LocationService() {
                                    @Override
                                    public void onLocationChanged(Location location) {
                                        try {
                                            locationManager.removeUpdates(this);
                                        } catch (SecurityException e) {
                                        }

                                        MessageSenderHandler.getInstance().sendBeaconRequest(user.getUserId(), MyLocationManager.getInstance().getMyLocation());
                                    }
                                };
                                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, locationService);

                                System.out.println(user.getDisplayName());
                            }
                        }
                        mode.finish(); // Action picked, so close the CAB
                        return true;
                    default:
                        return false;
                }
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

            }

        });
        FloatingActionButton messageButton = (FloatingActionButton) findViewById(R.id.message_friends_button);
        context = this;
        //Add friends to list for GUI
        if (CurrentBeaconUser.getInstance().getFriends() != null) {
            for (Object key : CurrentBeaconUser.getInstance().getFriends().keySet()) {
                BeaconUser beaconUser = (BeaconUser) CurrentBeaconUser.getInstance().getFriends().get(key);
                if (!beaconUser.getUserId().equals(CurrentBeaconUser.getInstance().getUserId())) {
                    friendsList.add(beaconUser);
                    userNames.add(beaconUser.getDisplayName());
                }
            }
        }

        messageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FriendListActivity.this, ChatMessageThreadsActivity.class);
                startActivity(intent);
            }
        });

        populateFriendsListView();
        registerFriendsListviewCallback();

    }

    private void populateFriendsListView() {
        FriendAdapter adapter = new FriendAdapter(FriendListActivity.this, friendsList);

        friendsListView.setAdapter(adapter);
    }

    private void registerFriendsListviewCallback() {
        friendsListView = (ListView) findViewById(R.id.friendListView);
        final Context context = this;

        friendsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BeaconUser selectedBeaconUser = friendsList.get(position);
                showBeaconOptionDialog(selectedBeaconUser, position);
            }
        });
    }

    public void showBeaconOptionDialog(BeaconUser beaconUser, final int userIndex) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialogTheme);
        final Context context = this;
        builder.setTitle("Send " + beaconUser.getDisplayName())
                .setItems(new String[]{"Beacon", "Message", "Request a Beacon", "Cancel"}, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                final LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                                LocationService locationService = new LocationService() {
                                    @Override
                                    public void onLocationChanged(Location location) {
                                        try {
                                            locationManager.removeUpdates(this);
                                        } catch (SecurityException e) {
                                        }


                                        MessageSenderHandler.getInstance().sendBeaconRequest(friendsList.get(userIndex).getUserId(), MyLocationManager.getInstance().getMyLocation());
                                    }
                                };
                                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    return;
                                }
                                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 10, locationService);

                                break;
                            case 1:
                                final HashMap<String, String> members = new HashMap<>();
                                members.put(friendsList.get(userIndex).getUserId(), friendsList.get(userIndex).getDisplayName());
                                members.put(CurrentBeaconUser.getInstance().getUserId(), CurrentBeaconUser.getInstance().getDisplayName());

                                BeaconDatabase.getChats().addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        Intent intent = new Intent(FriendListActivity.this, NewChatActivity.class);
                                        //if there already exists a chat load that one instead of a new chat
                                        boolean keepGoing = true;
                                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                            Chat chat = snapshot.getValue(Chat.class);
                                            if (chat.getMembers().equals(members)) {
                                                intent.putExtra("chat", chat);
                                                startActivity(intent);
                                                keepGoing = false;
                                                break;
                                            }
                                        }
                                        if (keepGoing) {
                                            Chat chat = new Chat();
                                            chat.setMessages(new ArrayList<Message>());
                                            chat.setMembers(members);

                                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("chats").push();
                                            chat.setKey(ref.getKey());
                                            ref.setValue(chat);
                                            intent.putExtra("chat", chat);
                                            startActivity(intent);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });



/*
                                String chatId = CurrentBeaconUser.getInstance().getUserId() + "_" + friendsList.get(userIndex).getUserId();
                                if (!MailBox.getInstance().getInbox().containsKey(chatId)) {
                                    MailBox.getInstance().initializeThread(chatId);
                                    MessageSenderHandler.getInstance().sendMessage(friendsList.get(userIndex).getUserId(), "Has started a chat");
                                }
                                Intent intent = new Intent(FriendListActivity.this, ChatActivity.class);
                                intent.putExtra("CHAT_ID", CurrentBeaconUser.getInstance().getUserId() + "_" + friendsList.get(userIndex).getUserId());
                                intent.putExtra("CHAT_PARTICIPANT", friendsList.get(userIndex).getUserId());
                                intent.putExtra("CHAT_WITH", friendsListView.getItemAtPosition(userIndex).toString());
                                startActivity(intent);*/
                                break;
                            case 2:
                                MessageSenderHandler.getInstance().sendLocationRequest(friendsList.get(userIndex).getUserId());
                            case 3:
                                break;
                        }
                    }
                });
        builder.create().show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_friends, menu);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.user_searchView));
        SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);

        ComponentName componentName = new ComponentName(this, UserSearchActivity.class);
        searchView.setSearchableInfo(searchManager.getSearchableInfo(componentName));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_multiple_select, menu);
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.chat_threads) {
            Intent intent = new Intent(FriendListActivity.this, NewThreadActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
