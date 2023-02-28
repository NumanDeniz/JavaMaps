package com.example.javamaps.view;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.example.javamaps.R;
import com.example.javamaps.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    ActivityResultLauncher<String> permissionLauncher;
    LocationManager locationManager;
    LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        registerLauncher();
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);

        locationManager= (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener=new LocationListener() { // değişen konumu bize gösteriyor.
            @Override
            public void onLocationChanged(@NonNull Location location) {
                SharedPreferences sharedPreferences=MapsActivity.this.getSharedPreferences("com.example.javamaps",MODE_PRIVATE);
                boolean info= sharedPreferences.getBoolean("info",false);
                if ( !info){
                    LatLng userLocation= new LatLng(location.getLatitude(),location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15));// zoom değeri 0 ile 25 arasında veebiliyoruz.
                    sharedPreferences.edit().putBoolean("info",true).apply();
                }


                LatLng userLocation= new LatLng(location.getLatitude(),location.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15));// zoom değeri 0 ile 25 arasında veebiliyoruz.

            }

        };
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                Snackbar.make(binding.getRoot(),"Permission needed for maps",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                    }
                }).show();
            }else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);

            }
        }else {
             locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,10000,10000,locationListener);// Kullanıcı izin verdikten sonra konum güncellemesi yapılır.

             Location lastlocation= locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);// son konumu alıp kamerayı buraya dönderiyoruz.
             if (lastlocation!=null){
                 LatLng lastUserLocation=new LatLng(lastlocation.getLatitude(),lastlocation.getLongitude());
                 mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15));
             }
            mMap.setMyLocationEnabled(true);
        }

    }

    private void registerLauncher(){
        permissionLauncher=registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if (result){// izin verildiyse
                    //permission granted
                    if (ContextCompat.checkSelfPermission( MapsActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10000, locationListener);// Kullanıcı izin verdikten sonra konum güncellemesi yapılır.

                        Location lastlocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);// son konumu alıp kamerayı buraya dönderiyoruz.
                        if (lastlocation != null) {
                            LatLng lastUserLocation = new LatLng(lastlocation.getLatitude(), lastlocation.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation, 15));
                        }
                    }

                }else {
                    //permission denied
                    Toast.makeText(MapsActivity.this,"Permission needed",Toast.LENGTH_LONG).show();

                }
            }
        });
    }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) {
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(latLng));
    }
}