package itproject.neon_client.activities;

import android.Manifest;
import android.content.Intent;
import android.location.Criteria;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;

import java.util.Random;

import itproject.neon_client.helpers.LoggedInUser;
import itproject.neon_client.R;
import itproject.neon_client.helpers.MapHelper;

import static android.content.ContentValues.TAG;
import static itproject.neon_client.helpers.MapHelper.get_latitude;
import static itproject.neon_client.helpers.MapHelper.get_longitude;
import static itproject.neon_client.helpers.MapHelper.post_location;

public class MapToFriendActivity extends AppCompatActivity implements OnMapReadyCallback {

    protected Location userLocation;
    protected FusedLocationProviderApi mFusedLocactionProviderApi;
    protected LocationManager mLocationManager;
    protected Context context;
    GoogleApiClient mGoogleApiClient;
    LocationRequest locationRequest;
    private GoogleMap mMap;
    private boolean mLocationPermissionGranted = false;
    CameraUpdate cu;
    LatLngBounds.Builder builder;

    private String friendUsername;
    private final String TAG = "testing";

    // Construct a FusedLocationProviderClient.
    FusedLocationProviderClient mFusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_to_friend);

        // Get the Intent that started this activity and extract the string
        Intent intent = getIntent();
        friendUsername = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        userLocation = new Location("user_location");


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng friendLocation = null;

        getLocationPermission();
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        updateLocationUI();
        builder = new LatLngBounds.Builder();

        try {
            double friendLat = MapHelper.get_latitude(friendUsername,LoggedInUser.getUsername());
            double friendLong = MapHelper.get_longitude(friendUsername,LoggedInUser.getUsername());
            if (friendLat != 0 && friendLong != 0) {
                friendLocation = new LatLng(friendLat,friendLong);
            }
            else {
                Random rand = new Random();
                double randLat = rand.nextInt(-37) - rand.nextDouble();
                double randLng = rand.nextInt(145) - rand.nextDouble();
                friendLocation = new LatLng(randLat,randLng); // melb uni
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (friendLocation != null) {
            mMap.addMarker(new MarkerOptions().position(friendLocation).title(friendUsername + "\'s location"));
            LatLng friendLatLng = new LatLng(friendLocation.latitude,friendLocation.longitude);
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(friendLatLng,14.0f));
        } else {
            Log.i(TAG,"friendLocation is null");
        }

        if (userLocation != null)
        {
            MapHelper.post_location(LoggedInUser.getUsername(), userLocation.getLatitude(), userLocation.getLongitude());
        } else {
            Log.i(TAG,"userLocation is null");
        }


    }

    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            mLocationPermissionGranted = false;
            Log.i(TAG,"location permission not granted");
        }
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
                Criteria criteria = new Criteria();
                userLocation = mLocationManager.getLastKnownLocation(mLocationManager.getBestProvider(criteria, false));
                if (userLocation == null) {
                    Log.i(TAG,"userLocation is null");
                    userLocation = new Location("testy_loc");
                    userLocation.setLatitude(-37.7964);
                    userLocation.setLongitude(144.9612);
                }
                else {
                    Log.i(TAG,"userLocation = " + userLocation.getLatitude() + "," + userLocation.getLongitude());
                }

            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                userLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    public void onConnected(Bundle arg0) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mFusedLocactionProviderApi.requestLocationUpdates(mGoogleApiClient, locationRequest, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                userLocation = location;
                LatLng myLocation = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());
                Log.d(TAG, "Posting location from the maps");
                post_location(LoggedInUser.getUsername(), userLocation.getLatitude(), userLocation.getLongitude());

                builder.include(myLocation);
                LatLngBounds bounds = builder.build();

                cu = CameraUpdateFactory.newLatLngBounds(bounds, 1);
                mMap.moveCamera(cu);

            }
        });
    }

    void friend_marker(GoogleMap map, String to_user) throws JSONException {
        double latitude = get_latitude(to_user, LoggedInUser.getUsername());
        double longitude = get_longitude(to_user, LoggedInUser.getUsername());

        map.addMarker(new MarkerOptions()
                .position(new LatLng(latitude, longitude))
                .title(to_user));
    }
}
