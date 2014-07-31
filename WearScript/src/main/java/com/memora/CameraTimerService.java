package com.memora;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import com.dappervision.wearscript.launcher.MainActivity;
import com.dappervision.wearscript.managers.CameraManager;
import com.dappervision.wearscript.managers.ManagerManager;
import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

import java.io.File;

public class CameraTimerService extends Service {

    private static final String LOG_TAG = "Camera Timer Service";
    public static final String ME_DIRECTORY = Environment.getExternalStorageDirectory()+ File.separator+"DCIM"+File.separator+"me"+File.separator;
    public static final String PHOTO_DIRECTORY = ME_DIRECTORY + "photos" + File.separator;

    //Intent extra constants
    public static final String JOB_EXTRA = "job";
    public static final int DEFAULT = 0;
    public static final int TAKE_PICTURE = 1;
    public static final int PICTURE_TAKEN = 2;

    //WakeLock and powermanagement stuff
    private WakeLock mWakeLock;
    private static PowerManager mPowerManager;

    private LiveCard mLiveCard;

    public static boolean wifiConnected;
    private ConnectivityManager connectivityManager;

    AlarmManager am;

    private PendingIntent alarmPendingIntent;
    private static final int SECONDS_PER_PICTURE = 10;

    CameraManager cameraManager;

    BroadcastReceiver networkStateReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("LOG_TAG", "Network state change broadcast received");
            networkStateChange();
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int returnVal = super.onStartCommand(intent, flags, startId);
        int extra = intent.getIntExtra(JOB_EXTRA, DEFAULT);
        switch(extra){
            case DEFAULT: 		break;
            case TAKE_PICTURE: 	takePicture();
                break;
            case PICTURE_TAKEN: pictureTaken();
                break;
        }
        return returnVal;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setAlarm() {
        am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 1000 * SECONDS_PER_PICTURE, alarmPendingIntent);
    }

    private void cancelAlarm() {
        am.cancel(alarmPendingIntent);
    }

    @Override
    public void onCreate(){
        Log.d(LOG_TAG, "Service Started");
        super.onCreate();
        cameraManager = (CameraManager) ManagerManager.get().get(CameraManager.class);

        publishMainActivityCard(this);
        //Setup connectivity broadcastReceiver
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkStateReceiver, filter);

        connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        wifiConnected = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
        //setup wakelock
        mPowerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");


        am = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, CameraTimerService.class).putExtra(JOB_EXTRA, TAKE_PICTURE);
        alarmPendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT); // changed PendingIntent.FLAG_UPDATE_CURRENT from 0 per http://stackoverflow.com/a/20157735/1476167
        setAlarm();
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "ME Service Destroyed");
        cancelAlarm();
        unregisterReceiver(networkStateReceiver);
        unpublishCard(this);
        super.onDestroy();
    }

    @SuppressLint("Wakelock")
    private void takePicture(){
        mWakeLock.acquire();
        Log.d(LOG_TAG, "onReceive'd");
        cameraManager.cameraStreamStart();
        setAlarm();
    }

    private void pictureTaken(){
        Log.d(LOG_TAG, "Picture taken callback'd");
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private void networkStateChange(){
        //This will send an intent to PhotoUploadService even if WIFI is on, something else changes
        //and then WIFI is still on. This is handled in the IntentService, but it could be more efficient to
        //Account for it here rather than send spurious intents.
        wifiConnected = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
        if(wifiConnected){
            //Send intent to begin upload
            Log.d(LOG_TAG, "Wifi Connected");
            //Intent mServiceIntent = new Intent(this, PhotoUploadIntentService.class);
            //this.startService(mServiceIntent);
        }
    }

    private void publishMainActivityCard(Context context) {

        if (mLiveCard == null) {
            String cardId = "my_card";

            mLiveCard = new LiveCard(this, cardId);
            Intent intent = new Intent(context, MainActivity.class);
            mLiveCard.setAction(PendingIntent.getActivity(context, 0, intent, 0));
            mLiveCard.publish(PublishMode.SILENT);
        }
    }

    private void unpublishCard(Context context) {
        if (mLiveCard != null) {
            mLiveCard.unpublish();
            mLiveCard = null;
        }
    }
}