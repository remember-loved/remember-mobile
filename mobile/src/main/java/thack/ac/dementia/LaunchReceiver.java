package thack.ac.dementia;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
* Created by paradite on 29/3/15.
*/
public class LaunchReceiver extends BroadcastReceiver {
    private final       String TAG                       = LaunchReceiver.class.getName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "OnReceive for " + intent.getAction());
        Log.d(TAG, intent.getExtras().toString());
        Intent serviceIntent = new Intent(context.getApplicationContext(),
                MonitorService.class);
        context.getApplicationContext().startService(serviceIntent);
    }
}
