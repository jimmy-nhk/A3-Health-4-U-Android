package com.a3.clientapp.helper.broadcast;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.a3.clientapp.R;
import com.a3.clientapp.activity.BillingActivity;
import com.a3.clientapp.model.Cart;
import com.a3.clientapp.model.Client;

public class NotificationService extends Service {

    String TAG = "NotificationService";
    //we are going to use a handler to be able to run in our TimerTask
    final Handler handler = new Handler();


    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);

        sendNotification(intent);

        return START_STICKY;
    }

    


    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");


    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");

        super.onDestroy();


    }

    public void sendNotification(Intent intent) {

        handler.post(new Runnable() {
            @Override
            public void run() {

                String message = intent.getStringExtra("message");
                createNotificationWithIntent(message, 1, intent);
            }
        });
    }

    // create notification
    public void createNotificationWithIntent(String aMessage, int notifyId , Intent intentView) {
        NotificationManager notifManager;

        String id = this.getString(R.string.app_name); // default_channel_id
        String title = this.getString(R.string.app_name); // Default Channel
        Intent intent;
        PendingIntent pendingIntent;
        NotificationCompat.Builder builder;

        notifManager = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);

        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel mChannel = notifManager.getNotificationChannel(id);
        if (mChannel == null) {
            mChannel = new NotificationChannel(id, title, importance);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300});
            notifManager.createNotificationChannel(mChannel);
        }
        builder = new NotificationCompat.Builder(this, id);
//        MainActivity mainActivity = (MainActivity) this.this;
        intent = new Intent(this, BillingActivity.class);

        Log.d(this.getClass().getSimpleName(), "Hello noti intent: ");
        try {

            Cart cart1 = intentView.getParcelableExtra("cart");
            Log.d(this.getClass().getSimpleName(), "Cart1 in noti intent: " + cart1.toString());

            intent.putExtra("cart", cart1);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP  | Intent.FLAG_ACTIVITY_NEW_TASK);


// Create the TaskStackBuilder and add the intent, which inflates the back stack
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addNextIntentWithParentStack(intent);
            Client client = intentView.getParcelableExtra("client");
            stackBuilder.editIntentAt(0).putExtra("client", client);

// Get the PendingIntent containing the entire back stack
            pendingIntent =
                    stackBuilder.getPendingIntent(1, PendingIntent.FLAG_UPDATE_CURRENT);

            builder.setContentTitle(aMessage)                            // required
                    .setSmallIcon(android.R.drawable.ic_popup_reminder)   // required
                    .setContentText(this.getString(R.string.app_name)) // required
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setTicker(aMessage)
                    .setVibrate(new long[]{100, 200, 300});


            Notification notification = builder.build();
            int oneTimeID = (int) SystemClock.uptimeMillis(); // Init onetime ID by current time so the notification can display multiple notification
            notifManager.notify(oneTimeID, notification); // Notify by id and built notification

        } catch (Exception ignored){
            ignored.printStackTrace();
        }
    }
}