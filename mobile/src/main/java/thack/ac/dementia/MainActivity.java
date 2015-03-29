package thack.ac.dementia;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private static final String TAG = MainActivity.class.getName();

    private static final String COUNT_KEY = "thack.ac.key.count";
    private static final String KEY_IMAGE = "thack.ac.key.image";
    private static final String KEY_TITLE = "thack.ac.key.title";

    public static final String ACTION_PULSE_SERVER_ALARM =
            "thack.ac.ACTION_PULSE_SERVER_ALARM";

    private BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
    GoogleApiClient mGoogleApiClient;

    public final BroadcastReceiver launch_receiver = new LaunchReceiver();

    private int count = 0;

    private TextView signal;
    private TextView latituteField;
    private TextView longitudeField;
    private TextView deviceIDField;
    TextView rssi_msg;
    private String careGiverName = "Zhu Liang";

    private LocationManager locationManager;
    private String          provider;
    private String          deviceId;

    //Service Handler
    ServiceHandler sh = new ServiceHandler();

    //Record the list of bluetooth devices within the range
    ArrayList<String> devicesInRange = new ArrayList<>();
    private String CARE_GIVER_ID = "Moazzam";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
        final Button button = (Button) findViewById(R.id.button);
        final Button button_trigger = (Button) findViewById(R.id.button2);
        signal = (TextView) findViewById(R.id.signal);
        rssi_msg = (TextView) findViewById(R.id.result);
        latituteField = (TextView) findViewById(R.id.latitute);
        longitudeField = (TextView) findViewById(R.id.longitude);
        deviceIDField = (TextView) findViewById(R.id.device_id);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                discoveryDevices();
            }
        });

        Log.d(TAG, "Starting GoogleApiClient");

        // Build a new GoogleApiClient that includes the Wearable API
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        Log.d(TAG, "Finished starting GoogleApiClient");
        button_trigger.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                triggerDataChange(null);
            }
        });

        //Get device ID
        deviceId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);

        deviceIDField.setText(deviceIDField.getText() + deviceId);
        //Start bluetooth discovery
        discoveryDevices();

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
        } else {
            latituteField.setText("Location not available");
            longitudeField.setText("Location not available");
        }

        // Start the location monitor service
        getApplicationContext().startService(new Intent(this, MonitorService.class));

    }

    private void discoveryDevices() {
        BTAdapter.startDiscovery();
        signal.setText("Starting to discover");
    }

    /* Request updates at startup */
    @Override
    protected void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(provider, 400, 1, this);
    }

    // Connect to the data layer when the Activity starts
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    // Disconnect from the data layer when the Activity stops
    @Override
    protected void onStop() {
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        unregisterReceiver(receiver);
        super.onStop();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                String name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);

                //Match the ID of the caregiver
                if (isCareGiver(name) && !checkIfDeviceAlreadyFound(name)){
                rssi_msg.setText(rssi_msg.getText() + "\n" + name + " => " + rssi + "dBm");
                //Trigger notice if the device is not already found
                triggerDataChange(name);
                }
            }
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                Log.v(TAG,"Restarting the bluetooth discovery");
                BTAdapter.startDiscovery();
            }
        }
    };

    private boolean isCareGiver(String name) {
        if (name == null){
            return false;
        }
        return name.contains(CARE_GIVER_ID);
    }

    private boolean checkIfDeviceAlreadyFound(String name) {
        for (String s: devicesInRange){
            if (name.equals(s)){
                return true;
            }
        }
        devicesInRange.add(name);
        return false;
    }


    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected: " + connectionHint);
        signal.setText("Connected to wearable");
        // Now you can use the Data Layer API

        String message = "Hello wearable\n Via the data layer";
        //Requires a new thread to avoid blocking the UI
        new SendToDataLayerThread("/message_path", message).start();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
        signal.setText("Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "onConnectionFailed: " + result);
        signal.setText("Connection failed");
    }

    public String getCaregiverName(String name) {
        return careGiverName;
    }

    class SendToDataLayerThread extends Thread {
        String path;
        String message;

        // Constructor to send a message to the data layer
        SendToDataLayerThread(String p, String msg) {
            path = p;
            message = msg;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            for (Node node : nodes.getNodes()) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), path, message.getBytes()).await();
                if (result.getStatus().isSuccess()) {
                    Log.v("myTag", "Message: {" + message + "} sent to: " + node.getDisplayName());
                } else {
                    // Log an error
                    Log.v("myTag", "ERROR: failed to send Message");
                }
            }
        }
    }

    // Create a data map and put data in it
    private void triggerDataChange(String name) {
        Log.v("myTag", "Trigger Data Change");
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/count");
        Bitmap icon = BitmapFactory.decodeResource(
                getResources(), R.mipmap.ic_photo);
        Asset asset = createAssetFromBitmap(icon);
        count++;
        putDataMapReq.getDataMap().putString(KEY_TITLE,
                String.format("Caregiver %s is here!", getCaregiverName(name)));
        putDataMapReq.getDataMap().putInt(COUNT_KEY, count);
        putDataMapReq.getDataMap().putAsset(KEY_IMAGE, asset);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
    }

    @Override
    public void onLocationChanged(Location location) {
        double lat = (location.getLatitude());
        double lng = (location.getLongitude());

        JSONObject locationJson = new JSONObject();
        try {
            locationJson.put("latitute", lat);
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

        latituteField.setText(String.valueOf(lat));
        longitudeField.setText(String.valueOf(lng));
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
     * NameValuePair
     * @param nameValuePairs    NameValuePair
     */
    public void addToNameValuePairs(List<NameValuePair> nameValuePairs, String param_name, String param_value) {
        nameValuePairs.add(new BasicNameValuePair(param_name, param_value));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
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
            List<NameValuePair> nameValuePairs = lists[0];
            // Creating service handler class instance
            sh = new ServiceHandler();
            String json = sh.makeServiceCall(LOCATION_URL, ServiceHandler.POST, nameValuePairs);
            Log.e(TAG, "Response: " + json);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

}
