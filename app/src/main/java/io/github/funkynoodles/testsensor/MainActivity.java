package io.github.funkynoodles.testsensor;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class MainActivity extends Activity implements SensorEventListener {

    // Context
    private static Context context;

    // Thread flags
    private volatile boolean activityStopped = false;
    private volatile boolean activityPaused = false;

    // Gravity sensors
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private volatile double ax = 0;
    private volatile double ay = 0;
    private volatile double az = 0;

    private volatile double vx = 0;
    private volatile double vy = 0;
    private volatile double vz = 0;


    private volatile double horThrustMultiplier = 0;
    private volatile double verThrust = 0;

    private static volatile int light = 0;
    private static volatile int holdPosition = 0;

    private static volatile int propellerA_mode = 1;
    private static volatile int propellerA_thrust = 10;
    private static volatile int propellerB_mode = 1;
    private static volatile int propellerB_thrust = 10;
    private static volatile int propellerC_mode = 1;
    private static volatile int propellerC_thrust = 10;

    private double turnMultiplier = 0; //-1 = turn left, 1 = turn right

    private SeekBar horThrustBar;
    private TextView horThrustProgress;
    private SeekBar verThrustBar;
    private TextView verThrustProgress;
    private TextView turnText;
    private Button settingsButton;

    private MjpegView mv;
    private MjpegView mvBack;

    // Drawer
    private android.support.design.widget.NavigationView navigationView;
    public static int navItemIndex = 0;
    private DrawerLayout drawerLayout;


    //Networking

    int serverPort = 8008;

    public class DoReadFront extends AsyncTask<String, Void, MjpegInputStream> {
        protected MjpegInputStream doInBackground(String... url) {
            //TODO: if camera has authentication deal with it and don't just not work
            HttpResponse res = null;
            DefaultHttpClient httpclient = new DefaultHttpClient();
            //Log.d(TAG, "1. Sending http request");
            try {
                res = httpclient.execute(new HttpGet(URI.create(url[0])));
                //Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
                if (res.getStatusLine().getStatusCode() == 401) {
                    //You must turn off camera User Access Control before this will work
                    return null;
                }
                return new MjpegInputStream(res.getEntity().getContent());
            }catch (IOException e) {
                e.printStackTrace();
                //Log.d(TAG, "Request failed-IOException", e);
                //Error connecting to camera
            }
            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
            mv.setSource(result);
            mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
            mv.showFps(true);
        }
    }

    public class DoReadBack extends AsyncTask<String, Void, MjpegInputStream> {
        protected MjpegInputStream doInBackground(String... url) {
            //TODO: if camera has authentication deal with it and don't just not work
            HttpResponse res = null;
            DefaultHttpClient httpclient = new DefaultHttpClient();
            try {
                res = httpclient.execute(new HttpGet(URI.create(url[0])));
                //Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
                if (res.getStatusLine().getStatusCode() == 401) {
                    //You must turn off camera User Access Control before this will work
                    return null;
                }
                return new MjpegInputStream(res.getEntity().getContent());
            }catch (IOException e) {
                e.printStackTrace();
                //Log.d(TAG, "Request failed-IOException", e);
                //Error connecting to camera
            }
            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
            mvBack.setSource(result);
            mvBack.setDisplayMode(MjpegView.SIZE_BEST_FIT);
            mvBack.showFps(true);
        }
    }

    public class DoSendData extends AsyncTask<String, Void, Void>{
        protected Void doInBackground(String... params) {
            /*
            # Command Field 0  = reserved (send 0)
            # Command Field 1  = Propeller A - mode  [0 1 2 3]
            # Command Field 2  = Propeller A - speed [0...100]
            # Command Field 3  = Propeller B - mode  [0 1 2 3]
            # Command Field 4  = Propeller B - speed [0...100]
            # Command Field 5  = Propeller C - mode  [0 1 2 3]
            # Command Field 6  = Propeller C - speed [0...100]
            # Command Field 7  = Light 1(ON) - 0 (OFF)
            # Command Field 8  = AUX1  1(ON) - 0 (OFF)
            # Command Field 9  = AUX2  1(ON) - 0 (OFF)
            # Command Field 10 = reserved (send 0)
            # Command Field 11 = reserved (send 0)
            # Command Field 12 = reserved (send 0)
            # Command Field 13 = reserved (send 0)
            # Command Field 14 = reserved (send 0)
            # Command Field 15 = reserved (send 0)
            # Command Field 16 = reserved (send 0)
            # Command Field 17 = Desired depth position [m]
            # Command Field 18 = HOLD POSITION  1(ON) - 0 (OFF)
            # Command Field 19 = reserved (send 0)
             */
            String data;
            Socket socket;
            PrintWriter out;
            try {
                socket = new Socket(params[0], serverPort);
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (UnknownHostException e) {
                System.err.println("Host unknown");
                return null;
            } catch (IOException e) {
                System.err.println("No I/O for connection");
                return null;
            }
            while (true) {
                /*if (activityPaused) {
                    continue;
                }else if (activityStopped){
                    return null;
                }*/
                synchronized (this) {
                    data = "$CMD,";
                    data += "0,";                                           // 0
                    data += String.valueOf(propellerA_mode) + ",";          // 1
                    data += String.valueOf(propellerA_thrust) + ",";        // 2
                    data += String.valueOf(propellerB_mode) + ",";          // 3
                    data += String.valueOf(propellerB_thrust) + ",";        // 4
                    data += String.valueOf(propellerC_mode) + ",";          // 5
                    data += String.valueOf(propellerC_thrust) + ",";        // 6
                    data += String.valueOf(light) + ",";                    // 7
                    data += "0,";                                           // 8
                    data += "0,";                                           // 9
                    data += "0,";                                           // 10
                    data += "0,";                                           // 11
                    data += "0,";                                           // 12
                    data += "0,";                                           // 13
                    data += "0,";                                           // 14
                    data += "0,";                                           // 15
                    data += "0,";                                           // 16
                    data += "0,";                                           // 17
                    data += String.valueOf(holdPosition) + ",";             // 18
                    data += "0\r";                                          // 19
                    System.out.println(System.currentTimeMillis());
                    out.println(data);
                    out.flush();
                }
                try {
                    Thread.sleep(0);
                } catch (Exception e) {
                }
                if (false) {
                    break;
                }
            }
            try {
                //socket.close();
            } catch (Exception e) {

            }
            return null;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        context = this;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);


        initializeViews();

        String videoURL = "http://141.89.114.98/cgi-bin/video640x480.mjpg";//http://funkynoodles:8080/cam_1.cgi";
        videoURL = "http://96.10.1.168/mjpg/1/video.mjpg?timestamp=1469826529251"; //http://trackfield.webcam.oregonstate.edu/axis-cgi/mjpg/video.cgi?resolution=800x600&amp%3bdummy=1333689998337";
        String video2URL = "http://webcam.st-malo.com/axis-cgi/mjpg/video.cgi";
        //This is the robot webcam:
        //videoURL = "http://192.168.0.2:8090/?action=stream";
        String robotURL = "192.168.0.100";
        new DoReadFront().execute(videoURL);
        new DoReadBack().execute(video2URL);
        //new DoSendData().execute(robotURL);

        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(Gravity.LEFT);
            }
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null) {
            // success! we have an accelerometer

            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            // N0 accelerometer found
        }

        horThrustBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                horThrustMultiplier = (progress / 50.0 - 1.0);
                horThrustMultiplier = Math.round(horThrustMultiplier * 100.0) / 100.0;
                horThrustProgress.setText(String.valueOf(horThrustMultiplier));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(50);
                horThrustProgress.setText(String.valueOf(horThrustMultiplier));
            }
        });

        verThrustBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                verThrust = (progress / 50.0 - 1.0);
                verThrust = Math.round(verThrust * 100.0) / 100.0;
                verThrustProgress.setText(String.valueOf(verThrust));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBar.setProgress(50);
                verThrustProgress.setText(String.valueOf(verThrust));
            }
        });
    }

    public void initializeViews() {

        mv = (MjpegView)findViewById(R.id.mjpegView);
        mvBack = (MjpegView)findViewById(R.id.mjpegViewBack);

        turnText = (TextView) findViewById(R.id.textTurn);

        horThrustBar = (SeekBar) findViewById(R.id.thrustBar);
        horThrustBar.setProgress(50);
        horThrustProgress = (TextView) findViewById(R.id.horThrustProgress);

        verThrustBar = (SeekBar) findViewById(R.id.altitudeBar);
        verThrustBar.setProgress(50);
        verThrustProgress = (TextView) findViewById(R.id.verThrustProgress);

        settingsButton = (Button) findViewById(R.id.settings_button);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.nav_view);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                String toastMsg = "";
                switch (item.getItemId()) {
                    case R.id.nav_toggle_light:
                        navItemIndex = 0;
                        // Toggle light
                        if (light == 0) {
                            light = 1;
                            toastMsg = getString(R.string.light) + " " + getString(R.string.on);
                        } else {
                            light = 0;
                            toastMsg = getString(R.string.light) + " " + getString(R.string.off);
                        }
                        break;
                    case R.id.nav_toggle_hold:
                        navItemIndex = 1;
                        // Toggle hold position
                        if (holdPosition == 0) {
                            holdPosition = 1;
                            toastMsg = getString(R.string.hold_position) + " " + getString(R.string.on);
                        } else {
                            holdPosition = 0;
                            toastMsg = getString(R.string.hold_position) + " " + getString(R.string.off);
                        }
                        break;
                    default:
                        navItemIndex = 0;
                }
                Toast.makeText(getContext(), toastMsg, Toast.LENGTH_SHORT).show();
                if (item.isChecked()) {
                    item.setChecked(false);
                } else {
                    item.setChecked(true);
                }
                // Close drawer
                drawerLayout.closeDrawer(GravityCompat.START);
                return false;
            }
        });
    }

    //onResume() register the accelerometer for listening the events
    protected void onResume() {
        super.onResume();
        activityPaused = false;
        mv.startPlayback();
        mvBack.startPlayback();
        //sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    //onPause() unregister the accelerometer for stop listening the events
    protected void onPause() {
        super.onPause();
        activityPaused = true;
        mv.stopPlayback();
        mvBack.stopPlayback();
        //sensorManager.unregisterListener(this);
        vx = 0;
        vy = 0;
        vz = 0;
        ax = 0;
        ay = 0;
        az = 0;
    }

    protected void onStop() {
        super.onStop();
        activityStopped = true;
        mv.stopPlayback();
        mvBack.stopPlayback();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        ax = event.values[0];
        ay = event.values[1];
        az = event.values[2];
        //square root acceleration to obtain linear changes
        if(ax >= 0){
            vx = Math.sqrt(Math.abs(ax));
        }else{
            vx = -Math.sqrt(Math.abs(ax));
        }

        if(ay >= 0){
            vy = Math.sqrt(Math.abs(ay));
        }else{
            vy = -Math.sqrt(Math.abs(ay));
        }

        if(az >= 0){
            vz = Math.sqrt(Math.abs(az));
        }else{
            vz = -Math.sqrt(Math.abs(az));
        }
        /*
        Setting turn multiplier using y-axis
        bounds: 0.6 to 2.2 and -0.6 to -2.2
        negative is turn left, positive is turn right
         */
        if(vy >= 0.6 && vy <= 2.2){
            turnMultiplier = (vy-0.6)/1.6; // right
            turnText.setText("Turning Right at " + String.valueOf(Math.round(turnMultiplier*100.0)/100.0));
        }else if(vy <= -0.6 && vy >= -2.2){
            turnMultiplier = (vy+0.6)/1.6; // left
            turnText.setText("Turning Left at " + String.valueOf(-Math.round(turnMultiplier*100.0)/100.0));
        }else if(vy > 2.2){
            turnMultiplier = 1.0; // right
            turnText.setText("Turning Right at " + String.valueOf(turnMultiplier));
        }else if(vy < -2.2){
            turnMultiplier = -1.0; // left
            turnText.setText("Turning Left at " + String.valueOf(-turnMultiplier));
        }else{
            turnMultiplier = 0; // going straight
            turnText.setText("Going Straight");
        }
    }

    public void propellerConfig(){
        if(horThrustMultiplier > 0){
            if(turnMultiplier > 0){
                propellerA_mode = 1; //prop A forward
                propellerB_mode = 2; // prop B back
            }
        }
    }

    public Context getContext(){
        return context;
    }
}