package com.comp3004.beacon;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.comp3004.beacon.GUI.ChatActivity;
import com.comp3004.beacon.GUI.NewChatActivity;
import com.comp3004.beacon.User.CurrentBeaconUser;
import com.facebook.Profile;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MessageService extends Service {
    public MessageService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Log.e("MESSAGE_SERVICE", "CREATE");
        FirebaseDatabase.getInstance().getReference("chats").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                final Chat chat = dataSnapshot.getValue(Chat.class);
                if (chat.getMembers().get(CurrentBeaconUser.getInstance().getUserId()) != null) {
                    //get beacon user that sent the message and display their name?

                    Intent resultIntent = new Intent(MessageService.this, NewChatActivity.class);
                    resultIntent.putExtra("chat", chat);
                    PendingIntent resultPendingIntent =
                            PendingIntent.getActivity(
                                    MessageService.this,
                                    0,
                                    resultIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );


                    Notification n = new Notification.Builder(getApplicationContext())
                            .setContentIntent(resultPendingIntent)
                            .setContentTitle(chat.getMembers().get(chat.getLastMessage().getFrom()))
                            .setTicker("New Message")
                            .setContentText(chat.getLastMessage().getBody())
                            .setSmallIcon(R.mipmap.ic_launcher_2)
                            .setAutoCancel(true).setContentIntent(resultPendingIntent)
                            .setPriority(Notification.PRIORITY_MAX)
                            .setVibrate(new long[]{0, 100, 100, 100}).build();


                    NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    notificationManager.notify(0, n);


                }
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
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("MESSAGE_SERVICE", "START");
        return START_STICKY;
    }
}
