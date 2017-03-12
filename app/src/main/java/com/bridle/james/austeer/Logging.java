package com.bridle.james.austeer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.TextView;

public class Logging extends AppCompatActivity {

    TextView timerTextView;
    long startTime = 0;

    TextView speedTextView;
    TextView locationTextView;
    TextView steeringTextView;

    String locString = "Getting location...";
    String speedString = "Getting speed...";
    String steerString = "Getting Steering Angle...";
    String steerStringX;
    String steerStringY;
    String steerStringZ;

    LocationManager locationManager;
    LocationListener li;

    SensorManager sMgr;
    Sensor gyro;
    SensorEventListener sev;

    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            long millis = System.currentTimeMillis() - startTime;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds = seconds % 60;

            timerTextView.setText(String.format("%d:%02d", minutes, seconds));
            locationTextView.setText("Location: "+locString);
            speedTextView.setText("Speed: "+speedString);

            steeringTextView.setText("Gyro: "+steerString);

            timerHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logging);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        timerTextView = (TextView) findViewById(R.id.timerTextView);
        locationTextView = (TextView) findViewById(R.id.locationTextView);
        speedTextView = (TextView) findViewById(R.id.speedTextView);
        steeringTextView = (TextView) findViewById(R.id.steeringTextView);

        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(timerRunnable, 0);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        li = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                locString = Double.toString(location.getLatitude())+','+Double.toString(location.getLongitude());
                speedString = Float.toString(location.getSpeed());
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, li);

        sMgr = (SensorManager)getSystemService(SENSOR_SERVICE);
        gyro = sMgr.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        sev = new SensorEventListener() {
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
            public void onSensorChanged(SensorEvent event) {
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];
                steerStringX = Float.toString(axisX);
                steerStringY = Float.toString(axisY);
                steerStringZ = Float.toString(axisZ);
                steerString = "X: "+steerStringX+" / Y: "+steerStringY+" / Z: "+steerStringZ;
            }
        };

        sMgr.registerListener(sev, gyro, SensorManager.SENSOR_DELAY_FASTEST);

    }

    @Override
    public void onPause() {
        super.onPause();
        timerHandler.removeCallbacks(timerRunnable);
        sMgr.unregisterListener(sev);
    }
}
