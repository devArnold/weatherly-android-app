package com.arnold.weatherly;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

public class WeatherController extends AppCompatActivity {

    // Base URL for the OpenWeatherMap API.
    final String WEATHER_URL = "http://api.openweathermap.org/data/2.5/weather";

    // App ID to use OpenWeather data
    final String APP_ID = "e72ca729af228beabd5d20e3b7749713";

    final int REQUEST_CODE = 123;

    // Location provider
    String LOCATION_PROVIDER = LocationManager.GPS_PROVIDER;

    // Getting location
    LocationManager locationManager;
    LocationListener locationListener;

    // Get weather update after 1000m
    final long MIN_DISTANCE = 1000;

    //Get weather update after 5s
    final long MIN_TIME = 5000;

    TextView mCityLabel;
    ImageView mWeatherImage;
    TextView mTemperatureLabel;
//    TextView mDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_controller_layout);

        mCityLabel = findViewById(R.id.locationTV);
        mWeatherImage = findViewById(R.id.weatherSymbolIV);
        mTemperatureLabel = findViewById(R.id.tempTV);
//        mDescription =  findViewById(R.id.description);
        ImageButton changeCityButton = findViewById(R.id.changeCityButton);

        changeCityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WeatherController.this, ChangeCityController.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = getIntent();
        String city = intent.getStringExtra("City");

        if(city != null){
            getWeatherForNewCity(city);

        }else {
            Log.d("weatherly", "onResume() called");
            Log.d("weatherly", "Getting current location");

            getCurrentLocation();

//            String lat = String.valueOf(0.3149);
//            String lon = String.valueOf(32.5698);
//
//            RequestParams params = new RequestParams();
//            params.add("lat", lat);
//            params.add("lon", lon);
//            params.add("appid", APP_ID);
//
//            letsDoDomeNetworking(params);
        }



    }

    private void getWeatherForNewCity(String city) {
        RequestParams params = new RequestParams();
        params.put("q",city);
        params.put("appid",APP_ID);

        letsDoDomeNetworking(params);
    }

    private void getCurrentLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d("weatherly", "onLocationChanged() called");

                String longitude = String.valueOf(location.getLongitude());
                String latitude = String.valueOf(location.getLatitude());

                Log.d("weatherly", "Latitude " + latitude);
                Log.d("weatherly", "Longitude " + longitude);

                RequestParams params = new RequestParams();
                params.add("lat", latitude);
                params.add("lon", longitude);
                params.add("appid", APP_ID);

                letsDoDomeNetworking(params);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d("weatherly", "onProviderEnabled"+ provider);

            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d("weatherly", "onProviderDisabled() called ");
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_CODE);

            return;
        }

        locationManager.requestLocationUpdates(LOCATION_PROVIDER, MIN_TIME, MIN_DISTANCE, locationListener);
//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,MIN_TIME,MIN_DISTANCE,locationListener);

    }

    private void letsDoDomeNetworking(RequestParams params) {
        AsyncHttpClient client = new AsyncHttpClient();

        client.get(WEATHER_URL, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {

                Log.d("weatherly", "Success " + response.toString());

                WeatherDataModel weatherDataModel = WeatherDataModel.fromJson(response);

                if (weatherDataModel != null) {
                    updateUI(weatherDataModel);
                }

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable e, JSONObject response) {
                Log.d("weatherly", "Failed : " + e.toString());
                Log.d("weatherly", "Status code: " + statusCode);
                Toast toast = Toast.makeText(getApplicationContext(),"Failed to get weather",Toast.LENGTH_SHORT);
                toast.show();

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("weatherly", "Permission granted");
                getCurrentLocation();
            } else {
                Log.d("weatherly", "Permission denied");
            }
        }
    }

    private void updateUI(WeatherDataModel weatherDataModel) {
        mTemperatureLabel.setText(weatherDataModel.getmTemperature());
        mCityLabel.setText(weatherDataModel.getmCity());
//        mDescription.setText(weatherDataModel.getmDescription());

        int resourceId = getResources().getIdentifier(weatherDataModel.getmIconName(), "drawable", getPackageName());

        mWeatherImage.setImageResource(resourceId);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(locationManager != null){
            locationManager.removeUpdates(locationListener);
        }
    }
}
