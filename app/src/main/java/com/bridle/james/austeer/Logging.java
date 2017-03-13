package com.bridle.james.austeer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
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

    private static final String TAG = "AusteerLogging";

    private String filename = "Austeer-" + System.currentTimeMillis() + ".csv";
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

    Double lastLat;
    Double lastLng;
    Double lastAlt;
    Long lastTime;

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

            outputString = String.format("%d:%02d:%02d", minutes, seconds, millis % 1000) + "," + locString + "," + speedString + "," + steerStringY + "\n";

            try {
                fw.write(outputString);
                storageTextView.setText("Writing to " + filename);
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
        } else {
            storageTextView.setText(getString(R.string.storage_not_writable));
        }

        final DecimalFormat df = new DecimalFormat("#.####");
        df.setRoundingMode(RoundingMode.CEILING);

        final DecimalFormat df2 = new DecimalFormat("#.##");
        df2.setRoundingMode(RoundingMode.CEILING);

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

                Double newLat = location.getLatitude();
                Double newLng = location.getLongitude();
                Double newAlt = location.getAltitude();
                Long newTime = location.getTime();

                // Altitude too inaccurate so just use the same altitude fo calculating speed,
                // you're not moving that fast unless you fall off a cliff

                locString = df.format(newLat) + ", " + df.format(newLng);

                Log.v(TAG, "IN ON LOCATION CHANGE, lat=" + df.format(newLat) + ", lon=" + df.format(newLng));

                if (lastLat == null) {
                    speedString = "0";
                } else {
                    Double dist = distance(lastLat, newLat, lastLng, newLng, newAlt, newAlt);
                    Double speed = Math.abs(dist) / ((newTime - lastTime) / 1000);
                    speedString = df2.format(Math.abs(speed));
                    Log.v(TAG, "NEW SPEED = "+speed);
                }
                lastLat = newLat;
                lastLng = newLng;
                lastAlt = newAlt;
                lastTime = newTime;

            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this)
                    .setTitle("Permissions Needed")
                    .setMessage("You need to give Austeer permission to use your location.")
                    .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // do nothing
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, li);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, li);

        sMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        gyro = sMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // With Accelerometer, flat on a table is X-0, Y-0, Z-10
        // Held flat upward in portrait mode, X-0, y-10, Z-0
        // Held flat upward in landscape: X-10, Y-0, Z-0
        // Steering mode fixed to steering wheel landscape: Just use Y-value

        sev = new SensorEventListener() {
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            public void onSensorChanged(SensorEvent event) {
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];
                steerStringX = Float.toString(Math.round(axisX * 10) / 10);
                steerStringY = String.format("%.1f", axisY / 10);
                steerStringZ = Float.toString(Math.round(axisZ * 10) / 10);
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

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     *
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     * @returns Distance in Meters
     */
    public Double distance(double lat1, double lat2, double lon1,
                                  double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

}

