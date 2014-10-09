package org.kaufer.matthew.tendons;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Vibrator;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;
import com.estimote.sdk.Utils;
import com.estimote.sdk.connection.BeaconConnection;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestHandle;

import org.apache.http.Header;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.app.PendingIntent.*;


public class Main extends Activity {



    BeaconManager beaconManager;
    NotificationManager notificationManager;
    private static final String
            ESTIMOTE_PROXIMITY_UUID =
            "B9407F30-F5F8-466E-AFF9-25556B57FE6D";

    private static final int major = 3289;



    private static final Region ALL_ESTIMOTE_BEACONS =
            new Region("regionId", ESTIMOTE_PROXIMITY_UUID,
                    major, null);

    protected static final String TAG =
            "Estimote Beacon";

    private final int NID = 1;

    private String serverURL = "http://mjkaufer-server.jit.su/";
    private Vibrator vibrator;

    TelephonyManager telephonyManager;

    public String username;
    private AsyncHttpClient client = new AsyncHttpClient();
    SharedPreferences settings;
    SharedPreferences.Editor editor;
    private String likely = " Check your connection.";
    private Button button;
    private Beacon beacon;//set to null when no beacon
    private long vibTime = 250;
    private long[] exitPattern  = {0, vibTime, vibTime, vibTime, vibTime};

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i("Main", "Started");


        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("Start");
        settings = this.getSharedPreferences("TendonAccountCreated", 0);
        editor = settings.edit();

        if(!settings.getBoolean("hasAccount", false)){//either no account or no var defined
            makeAccount();
        }

        final TextView textView = (TextView) findViewById(R.id.textView2);
        telephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        username = getUsername();

        button = (Button)findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView distance = (TextView)findViewById(R.id.textView3);
                if(beacon==null)
                    distance.setText("Not near a beacon");
                else
                    distance.setText("Approximately " + Utils.computeAccuracy(beacon) + " meters from the beacon.");

            }
        });

        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);


        beaconManager = new BeaconManager(this);
        notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        beaconManager.setBackgroundScanPeriod(
                TimeUnit.SECONDS.toMillis(1), 0);

        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List<Beacon> beacons) {
                beacon = beacons.get(0);
//                ((TextView)(findViewById(R.id.uuid))).setText(region.getProximityUUID());
                if (isAppInForeground(
                        getApplicationContext())) {
                    toastAlert("Entered region");

                    textView.setText("In the region!");
                } else {
                    postNotification("In the region!");
                }
                enter();
                System.out.println("ENTER");
                vibrator.vibrate(vibTime);
                //now we need to post that we're in the room


            }

            @Override
            public void onExitedRegion(Region region) {
                beacon = null;
                if (isAppInForeground(
                        getApplicationContext())) {
                    toastAlert("Exited region");
                    textView.setText("Out of the region!");

                } else {
                    postNotification("Out of the region!");
                }
                exit();
                System.out.println("LEAVE");
                vibrator.vibrate(exitPattern, -1);//makes a double vibrate on exit
                //now we need to post that we're out of the room

            }
        });


    }
    private void postNotification(String msg) {
        Intent notifyIntent = new
                Intent(Main.this,
                Main.class);


        notifyIntent.setFlags(
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent =
                getActivities(
                        Main.this, 0, new Intent[]{
                                notifyIntent},
                        FLAG_UPDATE_CURRENT);

        Notification notification = new
                Notification.Builder(Main.this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Monitoring Region")
                .setContentText(msg)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .getNotification();
        notification.defaults |=
                Notification.DEFAULT_SOUND;
        notification.defaults |=
                Notification.DEFAULT_LIGHTS;
        notificationManager.notify(NID,/*1 is the notification id*/
                notification);
    }

    public static boolean isAppInForeground(
            Context context) {
        List<ActivityManager.RunningTaskInfo> task = ((ActivityManager)
                context.getSystemService(
                        Context.ACTIVITY_SERVICE))
                .getRunningTasks(1);
        if (task.isEmpty()) {
            return false;
        }
        return task
                .get(0)
                .topActivity
                .getPackageName()
                .equalsIgnoreCase(
                        context.getPackageName());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart(){
        super.onStart();

        notificationManager.cancel(NID);
        beaconManager.connect(new
          BeaconManager.ServiceReadyCallback() {
              @Override
              public void onServiceReady() {
                  try {
                      beaconManager.startMonitoring(
                              ALL_ESTIMOTE_BEACONS);
                  } catch (RemoteException e) {
                      Log.d(TAG,
                              "Error while starting monitoring");
                  }
              }
        });
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        notificationManager.cancel(NID);
        beaconManager.disconnect();
    }

    public String getUsername(){
        AccountManager manager = AccountManager.get(this);
        Account[] accounts = manager.getAccountsByType("com.google");
        List<String> possibleEmails = new LinkedList<String>();

        for (Account account : accounts) {
            possibleEmails.add(account.name);
        }

        if(!possibleEmails.isEmpty() && possibleEmails.get(0) != null){
            String email = possibleEmails.get(0);
            String[] parts = email.split("@");
            if(parts.length > 0 && parts[0] != null)
                return parts[0];
            else
                return telephonyManager.getDeviceId();// an individual ID if the username thing doesn't work out
        }else
            return telephonyManager.getDeviceId();
    }

    public RequestHandle enter(){//we should probably add some offline support where enter/exit is tracked locally and pushed when connection is restored
        if(!settings.getBoolean("hasAccount",false)){//don't have an account
            return null;//don't do anything - don't want to be modifying imaginary sql rows
        }
        return client.post(serverURL + "data/enter/" + username, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                //we don't really need to do anything
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                toastAlert("Entry post failed..." + likely);
            }
        });
    }

    public RequestHandle exit(){
        if(!settings.getBoolean("hasAccount",false)){
            return null;
        }
        return client.post(serverURL + "data/leave/" + username, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                toastAlert("Exit post failed..." + likely);
                System.out.println(new String(bytes));
            }
        });
    }

    public RequestHandle makeAccount(){

        return client.post(serverURL + "data/create/" + username, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int i, Header[] headers, byte[] bytes) {
                toastAlert("Account created!");
                editor.putBoolean("hasAccount",true);
                editor.commit();
            }

            @Override
            public void onFailure(int i, Header[] headers, byte[] bytes, Throwable throwable) {
                toastAlert("Account creation failed." + likely);
                editor.putBoolean("hasAccount",false);
                editor.commit();
            }
        });
    }

    public void toastAlert(String s, int duration){
        Toast.makeText(getApplicationContext(),s,duration).show();
    }

    public void toastAlert(String s){
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_SHORT).show();
    }

}
