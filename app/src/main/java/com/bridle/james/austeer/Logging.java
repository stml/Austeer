package com.bridle.james.austeer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class Logging extends AppCompatActivity {

    TextView timerTextView;
    long startTime = 0;

    private String filename = "Austeer-"+System.currentTimeMillis()+".csv";
    private String filepath = "Austeer";
    File envpath;
    File outputFile;
    String outputString = "";
    FileWriter fw;

    TextView speedTextView;
    TextView locationTextView;
    TextView steeringTextView;
    TextView storageTextView;

    String locString = "";
    String speedString = "";
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
            locationTextView.setText(locString);
            speedTextView.setText(speedString);

            steeringTextView.setText(steerStringY);

            outputString = String.format("%d:%02d:%02d", minutes, seconds, millis%1000)+","+locString+","+speedString+","+steerStringY+"\n";

            try {
                fw.write(outputString);
                storageTextView.setText("Writing to "+filename);
            } catch (IOException e) {
                storageTextView.setText(getString(R.string.storage_problem));
            }

            timerHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logging);

        storageTextView = (TextView) findViewById(R.id.storageTextView);

        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            storageTextView.setText(getString(R.string.storage_writable));
            envpath = Environment.getExternalStoragePublicDirectory(filepath);
            outputFile = new File(envpath, filename);
            envpath.mkdirs();
            try {
                fw = new FileWriter(outputFile);
            } catch (IOException e) {
                storageTextView.setText(getString(R.string.storage_problem));
            }
        }
        else {
            storageTextView.setText(getString(R.string.storage_not_writable));
        }

        final DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.CEILING);

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
                locString = df.format(location.getLatitude())+','+df.format(location.getLongitude());
                speedString = df.format(location.getSpeed());
            }
            public void onStatusChanged(String provider, int status, Bundle extras) {}
            public void onProviderEnabled(String provider) {}
            public void onProviderDisabled(String provider) {}
        };

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, li);

        sMgr = (SensorManager)getSystemService(SENSOR_SERVICE);
        gyro = sMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // With Accelerometer, flat on a table is X-0, Y-0, Z-10
        // Held flat upward in portrait mode, X-0, y-10, Z-0
        // Held flat upward in landscape: X-10, Y-0, Z-0
        // Steering mode fixed to steering wheel landscape: Just use Y-value

        sev = new SensorEventListener() {
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
            public void onSensorChanged(SensorEvent event) {
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];
                steerStringX = Float.toString(Math.round(axisX*10)/10);
                steerStringY = String.format("%.1f", axisY/10);
                steerStringZ = Float.toString(Math.round(axisZ*10)/10);
            }
        };

        sMgr.registerListener(sev, gyro, SensorManager.SENSOR_DELAY_FASTEST);

    }

    @Override
    public void onPause() {
        super.onPause();
        sMgr.unregisterListener(sev);
        timerHandler.removeCallbacks(timerRunnable);
        try {
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sMgr.registerListener(sev, gyro, SensorManager.SENSOR_DELAY_FASTEST);
        timerHandler.postDelayed(timerRunnable, 0);
    }

    public File getAlbumStorageDir(Context context, String albumName) {
        File file = new File(context.getExternalFilesDir(
                Environment.DIRECTORY_PICTURES), albumName);
        if (!file.mkdirs()) {
            storageTextView.setText(getString(R.string.storage_problem));
        }
        return file;
    }

}
