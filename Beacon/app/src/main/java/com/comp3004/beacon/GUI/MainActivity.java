package com.comp3004.beacon.GUI;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.comp3004.beacon.FirebaseServices.DatabaseManager;
import com.comp3004.beacon.Networking.MessageSenderHandler;
import com.comp3004.beacon.User.BeaconUser;
import com.comp3004.beacon.R;
import com.comp3004.beacon.User.CurrentBeaconUser;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    private SupportMapFragment mapFragment;
    private GoogleMap map;

    private static final String TAG = "MainActivity";
    private String mUsername;
    private String mPhotoUrl;
    private SharedPreferences mSharedPreferences;
    public static final String ANONYMOUS = "anonymous";
    public static final String LOCATION_MESSAGE_CHILD = "locations";

    // Firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    private GoogleApiClient mGoogleApiClient;
    private FirebaseAnalytics mFirebaseAnalytics;
    private BeaconUser currentBeaconUser;
    private MessageSenderHandler messageHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final String username = "julian";

        FirebaseMessaging.getInstance().subscribeToTopic("user_"+username);


        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Set default username is anonymous.
        mUsername = ANONYMOUS;

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        currentBeaconUser = new CurrentBeaconUser(mFirebaseUser, FirebaseInstanceId.getInstance());
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        messageHandler = MessageSenderHandler.getInstance();
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        } else {
            mUsername = mFirebaseUser.getDisplayName();
            if (mFirebaseUser.getPhotoUrl() != null) {
                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
                Toast.makeText(MainActivity.this, getString(R.string.login_greeting) + " " + currentBeaconUser.getDisplayName(),
                        Toast.LENGTH_SHORT).show();
            }
        }

        MessageSenderHandler.getInstance().sendRegisterUserMessage();

        System.out.println("FRIENDS: " + DatabaseManager.getInstance().getCurrentUsersFriends());

        // Define Firebase Remote Config Settings.
        FirebaseRemoteConfigSettings firebaseRemoteConfigSettings =
                new FirebaseRemoteConfigSettings.Builder()
                        .setDeveloperModeEnabled(true)
                        .build();

        //GUI
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        Button FriendListButton = (Button) findViewById(R.id.friend_list_test);


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                messageHandler.sendBeaconInvitation(username);

            }
        });
    }


    public void fetchConfig() {
        long cacheExpiration = 3600; // 1 hour in seconds
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings()
                .isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Make the fetched config available via
                        // FirebaseRemoteConfig get<type> calls.
                        mFirebaseRemoteConfig.activateFetched();

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // There has been an error fetching the config
                        Log.w(TAG, "Error fetching config: " +
                                e.getMessage());
                    }
                });

        //MessageSenderHandler.getInstance().sendRegisterUserMessage();

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }


    public void openArrow(View v) {
        startActivity(new Intent(this, ArrowActivity.class));
    }

    public void openMap(View v) {
        startActivity(new Intent(this, MapsActivity.class));
    }
}
