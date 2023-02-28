package com.example.javamaps;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.Manifest;
import android.widget.Toast;

import com.example.javamaps.models.Place;
import com.example.javamaps.roomdb.PlaceDao;
import com.example.javamaps.roomdb.PlaceDatabase;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.javamaps.databinding.ActivityMapsBinding;
import com.google.android.material.snackbar.Snackbar;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    ActivityResultLauncher<String> permissionLauncher;
    LocationManager locationManager;
    LocationListener locationListener;
    PlaceDao placeDao;
    PlaceDatabase db;
    Double selectedLatitude;
    Double selectedLongitude;
    private CompositeDisposable compositeDisposable=new CompositeDisposable();
    Place selectedPlace;


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
        db= Room.databaseBuilder(MapsActivity.this,PlaceDatabase.class,"Places")
               // .allowMainThreadQueries() veritabanını ön planda çalıştırır çokta tavsiye edilmez yerine rxjava kullandım.
                .build();
        placeDao=db.placeDao();

        selectedLatitude=0.0;
        selectedLongitude=0.0;

        binding.savebutton.setEnabled(false);
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);

        Intent intent=getIntent();
        String intentInfo= intent.getStringExtra("info");
        if (intentInfo.equals("new")){
            binding.savebutton.setVisibility(View.VISIBLE);
            binding.deletebutton.setVisibility(View.GONE);

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

        }else {

            mMap.clear();
          selectedPlace=(Place) intent.getSerializableExtra("place");
          LatLng latLng=new LatLng(selectedPlace.latitude,selectedPlace.longitude);
          mMap.addMarker(new MarkerOptions().position(latLng).title(selectedPlace.name));
          mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));

          binding.editTextTextPlaceName.setText(selectedPlace.name);
          binding.savebutton.setVisibility(View.GONE);
          binding.deletebutton.setVisibility(View.VISIBLE);
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

        selectedLatitude= latLng.latitude;
        selectedLongitude=latLng.longitude;
        binding.savebutton.setEnabled(true);// kullanıcı haritadan bir yer seçmeden kayıt yapmasını önler.
    }

    public void save (View view){
        Place place=new Place(binding.editTextTextPlaceName.getText().toString(),selectedLatitude,selectedLongitude);
       // placeDao.insert(place).subscribeOn(Schedulers.io()).subscribe();
        //disposable kullan at veri birikimini engeller
        compositeDisposable.add(placeDao.insert(place).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(MapsActivity.this::handleResponse));
    }
    private void handleResponse(){ //gelen cevabı ele al
        Intent intent=new Intent(MapsActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }
    public void delete(View view){

       compositeDisposable.add(placeDao.delete(selectedPlace).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(MapsActivity.this::handleResponse));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}