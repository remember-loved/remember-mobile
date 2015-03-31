package thack.ac.dementia;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by paradite on 29/3/15.
 */
public class MonitorService extends Service implements LocationListener {
    private static final String TAG              = MonitorService.class.getName();
    private static final int    INTERVAL_SECONDS = 15*60;
    private sendLocationAsync      mTask;
    private String              mPulseUrl;
    private AlarmManager        alarms;
    private PendingIntent       alarmIntent;
    private ConnectivityManager cnnxManager;

    private LocationManager locationManager;
    private String          provider;
    private String          deviceId;

    //Service Handler
    ServiceHandler sh = new ServiceHandler();

    @Override
    public void onCreate() {
        super.onCreate();

        //Get device ID
        deviceId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        cnnxManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        alarms = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intentOnAlarm = new Intent(
                MainActivity.ACTION_PULSE_SERVER_ALARM);
        alarmIntent = PendingIntent.getBroadcast(this, 0, intentOnAlarm, 0);

        // Get the location manager
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Define the criteria how to select the locatioin provider -> use
        // default
        Criteria criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, false);
        Location location = locationManager.getLastKnownLocation(provider);

        // Initialize the location fields
        if (location != null) {
            Log.d(TAG, "Provider " + provider + " has been selected.");
            onLocationChanged(location);
        }


    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        double lat = (location.getLatitude());
        double lng = (location.getLongitude());

        JSONObject locationJson = new JSONObject();
        try {
            locationJson.put("latitude", lat);
            locationJson.put("longitude", lng);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String locationJSONString = locationJson.toString();

        //Send to server
        //Add nameValuePair for http request
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        addToNameValuePairs(nameValuePairs, "deviceId", deviceId);
        addToNameValuePairs(nameValuePairs, "location", locationJSONString);
        new sendLocationAsync().execute(nameValuePairs);

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "Enabled new provider " + provider,
                Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Disabled provider " + provider,
                Toast.LENGTH_SHORT).show();
    }

    /**
     * AsyncTask for updating location
     */
    private class sendLocationAsync extends AsyncTask<List<NameValuePair>, Void, Void> {
        private static final String LOCATION_URL = "http://remember-loved.rhcloud.com/api/records/";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(List<NameValuePair>... lists) {

            try {
                // if we have no data connection, no point in proceeding.
                NetworkInfo ni = cnnxManager.getActiveNetworkInfo();
                if (ni == null || !ni.isAvailable() || !ni.isConnected()) {
                    Log.d(TAG, "No usable network. Skipping pulse action.");
                    return null;
                }
                // / grab and log data

                List<NameValuePair> nameValuePairs = lists[0];
                // Creating service handler class instance
                sh = new ServiceHandler();
                String json = sh.makeServiceCall(LOCATION_URL, ServiceHandler.POST, nameValuePairs);
                Log.e(TAG, "Response: " + json);

            } catch (Exception e) {
                Log.d(TAG,
                        "Unknown error in background pulse task. Error: "
                                + e.getMessage());
            }finally {
                // always set the next wakeup alarm.
                int interval = INTERVAL_SECONDS;
                long timeToAlarm = SystemClock.elapsedRealtime() + interval
                        * 1000;
                alarms.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, timeToAlarm, alarmIntent);
            }
            stopSelf();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    /**
     * NameValuePair
     * @param nameValuePairs    NameValuePair
     */
    public void addToNameValuePairs(List<NameValuePair> nameValuePairs, String param_name, String param_value) {
        nameValuePairs.add(new BasicNameValuePair(param_name, param_value));
    }
}