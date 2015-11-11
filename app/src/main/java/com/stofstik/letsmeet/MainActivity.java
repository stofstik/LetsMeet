package com.stofstik.letsmeet;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        DialogChangeUsername.NoticeDialogListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMarkerDragListener,
        com.google.android.gms.location.LocationListener,
        OnMapReadyCallback {

    private static final String LOG = "MainActivity";

    private static final String SERVER = "http://92.111.66.145:13000";

    public static final String SP_KEY_USERNAME = "SP_KEY_USERNAME";
    public static final String SP_KEY_USER_ID = "SP_KEY_USER_ID";
    public static final String SP_KEY_FIRST_RUN = "SP_KEY_FIRST_RUN";

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 0;

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private boolean mRequestingLocationUpdates = true;

    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor sharedPreferencesEditor;

    private FloatingActionButton bStartMeet;
    private int mLongAnimationDuration;

    private ResponseParser responseParser = new ResponseParser();

    private Lobby mCurrentLobby; // TODO test if this works everywhere!
    private String mUserId;
    private Double dLatitude, dLongitude;
    private Location mCurrentLocation;

    private MapFragment mapFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // init shared prefs
        sharedPreferences = getPreferences(MODE_PRIVATE);
        sharedPreferencesEditor = sharedPreferences.edit();

        // setup views
        bStartMeet = (FloatingActionButton) findViewById(R.id.b_start_meet);
        bStartMeet.setOnClickListener(this);
        // Retrieve and cache the system's default "short" animation time.
        mLongAnimationDuration = getResources().getInteger(
                android.R.integer.config_longAnimTime);

        // setup google API client
        buildGoogleApiClient();

        // setup location request
        createLocationRequest();

        // get a handle to the map fragment
        mapFragment = (MapFragment)
                getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // check if this is the first run
        if (sharedPreferences.getBoolean(SP_KEY_FIRST_RUN, true)) {
            new DialogChangeUsername().show(getSupportFragmentManager(), "DialogChangeUsername");
        }

        // receive intent
        Intent intent = getIntent();
        Log.d(LOG, "intent: " + intent);
        if (intent.getDataString() != null) {
            // User clicked a link caught by our intent filter
            Log.d(LOG, "intent: " + intent.getDataString());
            Uri uri = Uri.parse(intent.getDataString());
            joinMeet(uri.getLastPathSegment());
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        // if (!mResolvingError) {}
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    protected void buildGoogleApiClient() {
        // setup API client, connect in onStart()
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    protected void createLocationRequest() {
        // TODO test and tweak these settings
        // mLocationRequest contains quality of service params for requests to the FusedLocationProviderApi.
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(60000); // location updates from this app
        mLocationRequest.setFastestInterval(30000); // location updates from other apps as well
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        // check if we have access to location
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // yes we do! request location updates!
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
            Log.d(LOG, "started location updates");
        } else {
            // no we don't! show a system dialog asking for permission, catch response in
            // onRequestPermissionsResult
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
        Log.d(LOG, "stopped location updates");
    }

    private void addUserAndGetIdFromServer() {
        String username = sharedPreferences.getString(MainActivity.SP_KEY_USERNAME, "");
        if (username.isEmpty()) {
            Log.d(LOG, "addUserAndGetIdFromServer() invalid user name");
            Toast.makeText(this, "Invalid username", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri.Builder builder = Uri.parse(SERVER).buildUpon()
                .appendPath("adduser")
                .appendPath(username)
                .appendPath("some@email.com")
                .appendPath("" + dLatitude)
                .appendPath("" + dLongitude);

        final String url = builder.toString();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(LOG, response);
                try {
                    mUserId = responseParser.parseUserId(response);
                    sharedPreferencesEditor.putString(SP_KEY_USER_ID, mUserId);
                    sharedPreferencesEditor.apply();
                    Log.d(LOG, "got id " + mUserId + " from server : )");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(LOG, "addUserAndGetIdFromServer error: " + error.toString());
                Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void updateUserById(Location location) {
        if (hasErrors("updateUserById", location)) {
            return;
        }

        Uri.Builder builder = Uri.parse(SERVER).buildUpon()
                .appendPath("updateUser")
                .appendPath(sharedPreferences.getString(SP_KEY_USER_ID, ""))
                .appendPath(sharedPreferences.getString(SP_KEY_USERNAME, ""))
                .appendPath("some@email.com")
                .appendPath("" + location.getLatitude())
                .appendPath("" + location.getLongitude());
        final String url = builder.toString();
        Log.d(LOG, "updateUserById: " + url);

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(LOG, "updateUserById: " + response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(LOG, "updateUserById error: " + error.toString());
                Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void getUsersInLobbyAndUpdateMap() {
        // TODO check for more errors probably
        if (mCurrentLobby == null) {
            Log.d(LOG, "getUsersInLobbyAndUpdateMap: mCurrentLobby is null");
            return;
        }

        Uri.Builder builder = Uri.parse(SERVER).buildUpon()
                .appendPath("getlobby")
                .appendPath(mCurrentLobby.getName())
                .appendPath(sharedPreferences.getString(SP_KEY_USER_ID, ""));
        String url = builder.toString();
        Log.d(LOG, "getUsersInLobbyAndUpdateMap: " + url);
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(LOG, "getUsersInLobbyAndUpdateMap: " + response);
                mMap.clear();
                LatLngBounds.Builder usersBounds = new LatLngBounds.Builder();
                // ArrayList<User> users = responseParser.parseUsers(response);
                // boolean bCreator = responseParser.parseLobbyCreator(response, sharedPreferences.getString(SP_KEY_USER_ID, ""));
                Lobby lobby = responseParser.parseBla(response);
                boolean bCreator = lobby.getCreator().matches(sharedPreferences.getString(SP_KEY_USER_ID, ""));
                User[] users = lobby.getUsers();
                MarkerOptions options;
                LatLng userLatLng;
                for (User user : users) {
                    // create markers for all users
                    options = new MarkerOptions();
                    userLatLng = new LatLng(user.getLatitude(), user.getLongitude());
                    usersBounds.include(userLatLng);
                    options.title(user.getUsername());
                    options.position(userLatLng);
                    options.visible(true);
                    options.anchor(0.5f, 0.5f);
                    options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_person_mdpi));
                    mMap.addMarker(options);
                }
                // create center marker if users > 1
                if (users.length > 1) {
                    options = new MarkerOptions();
                    options.position(usersBounds.build().getCenter());
                    options.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_meeting_point_ldpi));
                    options.flat(true);
                    options.anchor(0.5f, 0.5f);
                    // if we are creator set the marker to be draggable
                    if (bCreator) {
                        options.draggable(true);
                        // TODO notify the user that he can drag meeting point
                    }
                    mMap.addMarker(options);
                }

                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(usersBounds.build(), 0));
                mMap.moveCamera(CameraUpdateFactory.zoomBy(-2));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(LOG, "getUsersInLobbyAndUpdateMap error: " + error.toString());
                Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void startMeet() {
        Log.d(LOG, "clicked lets meet!");
        if (hasErrors("startMeet()", mCurrentLocation)) {
            return;
        }
        // TODO keep current lobby in memory
        Uri.Builder builder = Uri.parse(SERVER).buildUpon()
                .appendPath("startlobby")
                .appendPath(sharedPreferences.getString(SP_KEY_USER_ID, ""))
                .appendPath("" + dLatitude)
                .appendPath("" + dLongitude);

        final String url = builder.toString();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(LOG, "startMeet response: " + response);
                fadeOutViewAnimation(bStartMeet);
                mCurrentLobby = responseParser.parseBla(response);
                Uri.Builder builder = new Uri.Builder();
                builder.scheme("http")
                        .authority("www.letsmeet.nl")
                        .appendPath("joinlobby")
                        .appendPath(mCurrentLobby.getName());
                String url = builder.toString();
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                // Add data to the intent, the receiving app will decide
                // what to do with it.
                intent.putExtra(Intent.EXTRA_SUBJECT, "Let's meet!");
                intent.putExtra(Intent.EXTRA_TEXT, url);

                startActivity(Intent.createChooser(intent, "Share link!"));
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(LOG, "startMeet error: " + error.toString());
                Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void joinMeet(String lobbyName) {
        Log.d(LOG, "joining meet: " + lobbyName);
        if (sharedPreferences.getString(SP_KEY_USER_ID, "").isEmpty()) {
            Log.d("joinMeet", "no user id!!!");
            return;
        }
        Uri.Builder builder = Uri.parse(SERVER).buildUpon()
                .appendPath("joinlobby")
                .appendPath(lobbyName)
                .appendPath(sharedPreferences.getString(SP_KEY_USER_ID, ""));

        String url = builder.toString();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(LOG, "joinMeet response: " + response);
                fadeOutViewAnimation(bStartMeet);
                mCurrentLobby = responseParser.parseBla(response);
                getUsersInLobbyAndUpdateMap();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(LOG, "joinMeet error: " + error.toString());
                Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    private void leaveMeet() {
        Log.d(LOG, "leaving meet: " + mCurrentLobby);
        if (mCurrentLobby == null) {
            Log.d("leaveMeet", "mCurrentLobby is null");
            return;
        }
        if (sharedPreferences.getString(SP_KEY_USER_ID, "").isEmpty()) {
            Log.d("leaveMeet", "no user id!!!");
            return;
        }
        Uri.Builder builder = Uri.parse(SERVER).buildUpon()
                .appendPath("exitlobby")
                .appendPath(mCurrentLobby.getName())
                .appendPath(sharedPreferences.getString(SP_KEY_USER_ID, ""));
        final String url = builder.toString();
        StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(LOG, "leaveMeet response: " + response);
                // successfully left meet
                mMap.clear();
                mCurrentLobby = null;
                fadeInViewAnimation(bStartMeet);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(LOG, "leaveMeet error: " + error.toString());
                Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        VolleySingleton.getInstance(this).addToRequestQueue(stringRequest);
    }

    /**
     * A helper method to check for errors
     *
     * @param location the current location to check for age and accuracy
     * @return true if any errors have been detected
     */
    private boolean hasErrors(String methodName, Location location) {
        boolean error = false;

        Log.d(LOG, "Method:   " + methodName);
        Log.d(LOG, "Username: " + sharedPreferences.getString(SP_KEY_USERNAME, ""));
        Log.d(LOG, "userId:   " + sharedPreferences.getString(SP_KEY_USER_ID, ""));
        Log.d(LOG, "Location: " + location);


        if (sharedPreferences.getString(SP_KEY_USERNAME, "").isEmpty()) {
            Log.d(LOG, methodName + " invalid username");
            Toast.makeText(this, "Invalid username", Toast.LENGTH_SHORT).show();
            error = true;
        }
        if (sharedPreferences.getString(SP_KEY_USER_ID, "").isEmpty()) {
            Log.d(LOG, methodName + " invalid userId");
            Toast.makeText(this, "Invalid userId", Toast.LENGTH_SHORT).show();
            error = true;
        }
        if (!isRecentLocation(location)) {
            Log.d(LOG, methodName + " location too old");
            Toast.makeText(this, "Need a more recent location", Toast.LENGTH_SHORT).show();
            error = true;
        }
        if (!isAccurateLocation(location)) {
            Log.d(LOG, methodName + " location not accurate");
            Toast.makeText(this, "Need a more accurate location", Toast.LENGTH_SHORT).show();
            error = true;
        }
        return error;
    }


    private boolean isRecentLocation(Location location) {
        if (location == null) {
            Log.e("isRecentLocation", "location == null");
            return false;
        }
        long now = Calendar.getInstance().getTimeInMillis(); // now
        long loc = location.getTime(); // location time
        long age = now - (120 * 1000); // the allowed age for the location
        return loc > age;
    }

    private boolean isAccurateLocation(Location location) {
        if (location == null) {
            Log.e("isPreciseLocation", "location == null");
            return false;
        }
        return location.getAccuracy() < 500; // accuracy in meters
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_change_username:
                new DialogChangeUsername().show(getSupportFragmentManager(), "DialogChangeUsername");
                break;
            case R.id.action_leave_meet:
                leaveMeet();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(LOG, "Location changed in Activity! " + location);
        mCurrentLocation = location;
        updateUserById(location);
        getUsersInLobbyAndUpdateMap();
    }

    @Override
    public void onDialogPositiveClick(DialogFragment dialog, String username) {
        if (dialog.getTag().matches("DialogChangeUsername")) {
            // ensure username != empty
            if (!username.isEmpty()) {
                // save the current username to the server and save to shared prefs
                sharedPreferencesEditor.putString(SP_KEY_USERNAME, username);
                sharedPreferencesEditor.apply();
                // if we do not have a user id yet ask the server for one
                if (sharedPreferences.getString(SP_KEY_USER_ID, "").isEmpty()) {
                    addUserAndGetIdFromServer();
                }
                // set first run to false
                if (sharedPreferences.getBoolean(SP_KEY_FIRST_RUN, true)) {
                    sharedPreferencesEditor.putBoolean(SP_KEY_FIRST_RUN, false);
                    sharedPreferencesEditor.apply();
                }
            } else {
                // username is empty, please fill in a username
                new DialogChangeUsername().show(getSupportFragmentManager(), "DialogChangeUsername");
                Toast.makeText(this, "Please fill in a username", Toast.LENGTH_SHORT).show();
            }
        } // else if (other dialog) {}

    }

    /*
        we really, really need a valid username... user may not cancel!
     */
    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        if (dialog.getTag().matches("DialogChangeUsername")) {
            if (sharedPreferences.getString(SP_KEY_USERNAME, "").isEmpty()) {
                // username is empty, please fill in a username
                new DialogChangeUsername().show(getSupportFragmentManager(), "DialogChangeUsername");
                Toast.makeText(this, "Please fill in a username", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Username not changed", Toast.LENGTH_SHORT).show();
            }
        } // else if (other dialog) {}

    }

    /*
        we really, really need a valid username... user may not cancel!
     */
    @Override
    public void onCancelled(DialogFragment dialog) {
        if (dialog.getTag().matches("DialogChangeUsername")) {
            if (sharedPreferences.getString(SP_KEY_USERNAME, "").isEmpty()) {
                // username is empty, please fill in a username
                new DialogChangeUsername().show(getSupportFragmentManager(), "DialogChangeUsername");
                Toast.makeText(this, "Please fill in a username", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Username not changed", Toast.LENGTH_SHORT).show();
            }
        } // else if (other dialog) {}
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(LOG, "Map ready");
        // setup map settings
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        mMap.setBuildingsEnabled(true);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates();
                } else {
                    Toast.makeText(this, "Let's Meet needs access to your location...", Toast.LENGTH_LONG).show();
                }
                break;
            }
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.b_start_meet:
                startMeet();
                break;
        }
    }

    private void fadeOutViewAnimation(View view) {
        final View v = view;
        v.setVisibility(View.VISIBLE);
        v.setAlpha(1f);

        v.animate()
                .alpha(0f)
                .setDuration(mLongAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        v.setVisibility(View.INVISIBLE);
                        v.setAlpha(1f);
                    }
                });
    }

    private void fadeInViewAnimation(View view) {
        final View v = view;
        v.setAlpha(0f);
        v.setVisibility(View.VISIBLE);

        v.animate()
                .alpha(1f)
                .setDuration(mLongAnimationDuration)
                .setListener(null);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(LOG, "Google API's connected");
        // we have a connection to the API, start requesting location updates
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(LOG, "Google API's suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(LOG, "Google API's connection failed");
    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {

    }
}
