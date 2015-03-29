package thack.ac.dementia;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by paradite on 22/3/15.
 */
public class DataLayerListenerService extends WearableListenerService {
    private static final String TAG                     = "DataLayer";
    private static final String START_ACTIVITY_PATH     = "/start-activity";
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";

    private static final String COUNT_KEY       = "thack.ac.key.count";
    private static final String KEY_IMAGE       = "thack.ac.key.image";
    private static final String KEY_TITLE       = "thack.ac.key.title";
    private static final int    NOTIFICATION_ID = 1;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        if (messageEvent.getPath().equals("/message_path")) {
            final String message = new String(messageEvent.getData());
            Log.d(TAG, "Message path received on watch is: " + messageEvent.getPath());
            Log.d(TAG, "Message received on watch is: " + message);

            // Broadcast message to wearable activity for display
            //Intent messageIntent = new Intent();
            //messageIntent.setAction(Intent.ACTION_SEND);
            //messageIntent.putExtra("message", message);
            //LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        } else {
            super.onMessageReceived(messageEvent);
        }
    }


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.d(TAG, "onDataChanged: " + dataEvents);
        final List events = FreezableUtils
                .freezeIterable(dataEvents);

        //GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
        //        .addApi(Wearable.API)
        //        .build();
        //
        //ConnectionResult connectionResult =
        //        googleApiClient.blockingConnect(30, TimeUnit.SECONDS);
        //
        //if (!connectionResult.isSuccess()) {
        //    Log.e(TAG, "Failed to connect to GoogleApiClient.");
        //    return;
        //}

        // Loop through the events and send a message
        // to the node that created the data item.
        for (DataEvent event : (List<DataEvent>) events) {
            DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
            String title = dataMapItem.getDataMap().getString(KEY_TITLE);
            int count = dataMapItem.getDataMap().getInt(COUNT_KEY);
            Asset asset = dataMapItem.getDataMap().getAsset(KEY_IMAGE);

            // Build the intent to display our custom notification
            //Intent notificationIntent =
            //        new Intent(this, MainActivity.class);
            //notificationIntent.putExtra(
            //        MainActivity.EXTRA_TITLE, title);
            //notificationIntent.putExtra(
            //        MainActivity.EXTRA_IMAGE, asset);
            //PendingIntent notificationPendingIntent = PendingIntent.getActivity(
            //        this,
            //        0,
            //        notificationIntent,
            //        PendingIntent.FLAG_UPDATE_CURRENT);

            Bitmap icon = loadBitmapFromAsset(asset);

            Log.d(TAG, "Loaded icon");

            Notification.Builder notificationBuilder =
                    new Notification.Builder(this)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setLargeIcon(icon)
                            .setContentTitle(title)
                            //.setContentText(title)
                            .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000, 1000})
                            .setOngoing(false)
                            .extend(new Notification.WearableExtender()
                                    .setCustomSizePreset(Notification.WearableExtender.SIZE_XSMALL));

            // Build the notification and show it
            NotificationManager notificationManager =
                    (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(
                    NOTIFICATION_ID, notificationBuilder.build());
            Log.d(TAG, "Displayed notification");
            // Broadcast message to wearable activity for display
            //Intent messageIntent = new Intent();
            //messageIntent.setAction(Intent.ACTION_SEND);
            //messageIntent.putExtra("message", "count: " + count);
            //LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
            // Send the RPC
            //Wearable.MessageApi.sendMessage(googleApiClient, nodeId,
            //        DATA_ITEM_RECEIVED_PATH, payload);
        }
    }

    public Bitmap loadBitmapFromAsset(Asset asset) {
        if (asset == null) {
            throw new IllegalArgumentException("Asset must be non-null");
        }

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        ConnectionResult result =
                googleApiClient.blockingConnect(
                        1000, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            return null;
        }


        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                googleApiClient, asset).await().getInputStream();
        googleApiClient.disconnect();

        if (assetInputStream == null) {
            Log.w(TAG, "Requested an unknown Asset.");
            return null;
        }
        // decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }
}
