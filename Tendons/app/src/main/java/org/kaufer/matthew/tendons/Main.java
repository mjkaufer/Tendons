package org.kaufer.matthew.tendons;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.estimote.sdk.Beacon;
import com.estimote.sdk.BeaconManager;
import com.estimote.sdk.Region;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static android.app.PendingIntent.*;


public class Main extends Activity {



    BeaconManager beaconManager;
    NotificationManager notificationManager;
    private static final String
            ESTIMOTE_PROXIMITY_UUID =
            "B9407F30-F5F8-466E-AFF9-25556B57FE6D";

    private static final Region ALL_ESTIMOTE_BEACONS =
            new Region("regionId", ESTIMOTE_PROXIMITY_UUID,
                    null, null);

    protected static final String TAG =
            "Estimote Beacon";

    private final int NID = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final TextView textView = (TextView) findViewById(R.id.textView2);


        beaconManager = new BeaconManager(this);
        notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        beaconManager.setBackgroundScanPeriod(
                TimeUnit.SECONDS.toMillis(1), 0);

        beaconManager.setMonitoringListener(new BeaconManager.MonitoringListener() {
            @Override
            public void onEnteredRegion(Region region, List<Beacon> beacons) {
                if (isAppInForeground(
                        getApplicationContext())) {
                    Toast.makeText(
                            getApplicationContext(),
                            "Entered region",
                            Toast.LENGTH_LONG).show();

                    textView.setText("YOU ARE NEAR DAT BEACON");
                } else {
                    postNotification("Entered region");
                }

                System.out.println("ENTER");

            }

            @Override
            public void onExitedRegion(Region region) {
                if (isAppInForeground(
                        getApplicationContext())) {
                    Toast.makeText(
                            getApplicationContext(),
                            "Exited region",
                            Toast.LENGTH_LONG).show();
                    textView.setText("YOU ARE NOT NEAR DAT BEACON");

                } else {
                    postNotification("Exited region");
                }

                System.out.println("LEAVE");
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
                .build();
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
}
