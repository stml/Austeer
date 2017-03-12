# Austeer

A simple Android app for tracking movement, speed, and steering angle for self-driving car development using neural network training, to supplement dashcam or other video footage.

### Installation

These are the raw development files from Android Studio. No guarantees of functionality or safety. Download and open in Android Studio to test in emulator or on a device.

### Operation

The app contains just one activity: a logger for device data. The device should be attached in landscape orientation to the centre of the car steering wheel.

When "start logging" is pressed, the app starts saving timestamps (M:S:MS), GPS location (lat/long), speed (m/s), and steering angle (-1 being full left, 0 being straight ahead, and +1 being full right) to a CSV file in external storage.

The file is stored in the Austeer directory, with the format Austeer-&lt;timestamp&gt;.csv.

### File format

The data is stored in a CSV file (without column headers). Example data as follows:

```
timestamp,location,speed,steering_angle
0:32:844,37.9884,23.7335,0,0.2
0:32:881,37.9884,23.7335,0,0.2
0:33:360,37.9884,23.7335,0,0.6
0:33:383,37.9884,23.7335,0,0.5
0:33:873,37.9884,23.7335,0,0.7
0:33:909,37.9884,23.7335,0,0.7
0:34:390,37.9884,23.7335,0,0.7
0:34:420,37.9884,23.7335,0,0.7
0:34:903,37.9884,23.7335,0,0.5
0:34:924,37.9884,23.7335,0,0.5
0:35:406,37.9884,23.7335,0,0.3
0:35:425,37.9884,23.7335,0,0.3
0:35:908,37.9884,23.7335,0,0.2
0:35:927,37.9884,23.7335,0,0.2
0:36:410,37.9884,23.7335,0,0.3
0:36:428,37.9884,23.7335,0,0.3
0:36:914,37.9884,23.7335,0,0.3
0:36:929,37.9884,23.7335,0,0.3
0:37:423,37.9884,23.7335,0,0.3
0:37:429,37.9884,23.7335,0,0.3
```

(Note that the first few rows may have empty values for location and speed while a GPS fix is obtained.)

### Screenshots

<img src="./austeer.png">
