package com.coronotrace.coronavirustracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageListener;
import java.util.Timer;
import java.util.TimerTask;

public class LogContact extends Service {

    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    private Message message;
    private MessageListener messageListener;
    private String TAG = "LogContact";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String input = intent.getStringExtra("inputExtra");
        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getText(R.string.foreground_service_title))
                .setContentText(getText(R.string.foreground_service_description))
                .setSmallIcon(R.drawable.notify_virus)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        // Get the UUID
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("preferences", MODE_PRIVATE);
        String trackingId = sharedPreferences.getString("trackingId", null);
        Log.d(TAG, "Logging contact with Tracking ID: " + trackingId);

        // Setup the message listener
        messageListener = new MessageListener() {
            @Override
            public void onFound(Message message) {
                Log.d(TAG, "Found message: " + new String(message.getContent()));
            }

            @Override
            public void onLost(Message message) {
                Log.d(TAG, "Lost sight of message: " + new String(message.getContent()));
            }
        };

        // Setup the message (just the UUID)
        message = new Message(trackingId.getBytes());

        //do heavy work on a background thread
        startLogging();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        stopLogging();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    public void startLogging()  {
        Log.d(TAG, "Broadcasting UUID...");

        // Test - keep printing to show still active
        class SayHello extends TimerTask {
            public void run() {
                Log.d(TAG, "hi");
            }
        }
        Timer timer = new Timer();
        timer.schedule(new SayHello(), 0, 5000);

        Nearby.getMessagesClient(this).publish(message);
        Nearby.getMessagesClient(this).subscribe(messageListener);

    }

    public void stopLogging() {
        Nearby.getMessagesClient(this).unpublish(message);
        Nearby.getMessagesClient(this).unsubscribe(messageListener);
    }
}
