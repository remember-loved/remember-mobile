package thack.ac.dementia;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
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

import java.io.ByteArrayOutputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends ActionBarActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {
    private static final String TAG               = MainActivity.class.getName();
    private static final long   RE_ENTER_DURATION = 30;
    MainActivity self = this;

    public static final String COUNT_KEY     = "thack.ac.key.count";
    public static final String KEY_IMAGE     = "thack.ac.key.image";
    public static final String KEY_TITLE     = "thack.ac.key.title";
    public static final String KEY_NAME      = "thack.ac.key.name";
    public static final String KEY_BLUETOOTH = "thack.ac.key.bluetooth";
    public static final String KEY_ID        = "thack.ac.key.id";
    public static final String KEY_DATE      = "thack.ac.key.date";

    public static final String ACTION_PULSE_SERVER_ALARM =
            "thack.ac.ACTION_PULSE_SERVER_ALARM";
    public static final String EXTRA_IDS                 = "extra_ids";

    private BluetoothAdapter BTAdapter = BluetoothAdapter.getDefaultAdapter();
    GoogleApiClient mGoogleApiClient;

    public final BroadcastReceiver launch_receiver = new LaunchReceiver();

    private int count = 0;

    private TextView signal;
    //private TextView latituteField;
    //private TextView longitudeField;
    private TextView deviceIDField;
    private TextView rssi_msg;

    ListView    list;
    LazyAdapter adapter;

    private LocationManager locationManager;
    private String          provider;
    private String          deviceId;

    //Service Handler
    ServiceHandler sh = new ServiceHandler();

    //Database
    SQLiteDatabase db;

    //Record the list of bluetooth devices and their duration within the range
    ArrayList<Map.Entry<String, Long>> devicesInRange = new ArrayList<>();

    //Record of caregivers in memory
    ArrayList<HashMap<String, String>> caregiverList      = new ArrayList<HashMap<String, String>>();
    ArrayList<byte[]>                  caregiverImageList = new ArrayList<byte[]>();

    private ArrayList<String> CARE_GIVER_IDS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeCareGivers();

        final Button button = (Button) findViewById(R.id.button);
        final Button button_trigger = (Button) findViewById(R.id.button2);
        signal = (TextView) findViewById(R.id.signal);
        rssi_msg = (TextView) findViewById(R.id.result);
        //latituteField = (TextView) findViewById(R.id.latitute);
        //longitudeField = (TextView) findViewById(R.id.longitude);
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
            //latituteField.setText("Location not available");
            //longitudeField.setText("Location not available");
        }

        // Start the location monitor service
        getApplicationContext().startService(new Intent(this, MonitorService.class));

    }

    private void initializeCareGivers() {

        CARE_GIVER_IDS = new ArrayList<>();
        //CARE_GIVER_IDS.add(CARE_GIVER_ID_Z);
        //CARE_GIVER_IDS.add(CARE_GIVER_ID_M);
        caregiverList = new ArrayList<>();
        caregiverImageList = new ArrayList<>();

        db = (new DataBaseHelper(getApplicationContext())).getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT _id,Name,Image,created_at,BluetoothID FROM mytable ORDER BY _id", null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            // creating new HashMap
            HashMap<String, String> map = new HashMap<String, String>();
            // adding each child node to HashMap key => value

            String id = cursor.getString(cursor.getColumnIndex("_id"));
            String name = cursor.getString(cursor.getColumnIndex("Name"));
            String bluetoothID = cursor.getString(cursor.getColumnIndex("BluetoothID"));
            String date;
            date = cursor.getString(cursor.getColumnIndex("created_at"));

            map.put(KEY_ID, id);
            map.put(KEY_NAME, name);
            map.put(KEY_BLUETOOTH, bluetoothID);
            map.put(KEY_DATE, date);
            CARE_GIVER_IDS.add(bluetoothID);

            // adding HashList to ArrayList
            caregiverList.add(map);
            caregiverImageList.add(cursor.getBlob(cursor.getColumnIndex("Image")));
            cursor.moveToNext();
        }
        db.close();

        list = (ListView) findViewById(R.id.list);

        // Getting adapter by passing data ArrayList
        adapter = new LazyAdapter(this, caregiverList, caregiverImageList);
        list.setAdapter(adapter);

        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                final int final_position = position;
                final String id_text = ((TextView) view.findViewById(R.id.id)).getText().toString();
                final int item_id = Integer.parseInt(id_text);
                AlertDialog.Builder alert = new AlertDialog.Builder(
                        self);
                alert.setTitle("Confirm Delete");
                alert.setMessage("Are you sure to delete the caregiver?");
                alert.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db = (new DataBaseHelper(getApplicationContext())).getWritableDatabase();
                        boolean success = db.delete("mytable", "_id=" + item_id, null) > 0;
                        caregiverList.remove(final_position);
                        caregiverImageList.remove(final_position);
                        adapter.notifyDataSetChanged();
                        dialog.dismiss();
                    }
                });
                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dialog.dismiss();
                    }
                });

                alert.show();

                return true;
            }
        });
    }

    public void launchRegister(View view) {
        Intent intent = new Intent(this, RegisterActivity.class);
        //EditText editText = (EditText) findViewById(R.id.edit_message);
        //String message = editText.getText().toString();

        ArrayList<String> deviceNames = new ArrayList<>();

        for (Map.Entry<String, Long> entry : devicesInRange) {
            deviceNames.add(entry.getKey());
        }
        intent.putExtra(EXTRA_IDS, deviceNames);
        startActivity(intent);
    }

    private void discoveryDevices() {
        if (BTAdapter.isDiscovering()) {
            Log.v(TAG, "Discovery already ongoing");
        } else {
            boolean success = BTAdapter.startDiscovery();
            Log.v(TAG, "Starting new discover with result: " + success);
            signal.setText("Starting new discovery");
        }

    }

    /* Request updates at startup */
    @Override
    protected void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(provider, 400, 1, this);
        initializeCareGivers();
        discoveryDevices();
    }

    // Connect to the data layer when the Activity starts
    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
    }

    // Disconnect from the data layer when the Activity stops
    @Override
    protected void onStop() {
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            //    ignore
        }

        cancelDiscoveryIfOngoing();
        super.onStop();
    }

    private void cancelDiscoveryIfOngoing() {
        if (BTAdapter.isDiscovering()) {
            Log.v(TAG, "Cancelled discovery");
            BTAdapter.cancelDiscovery();
        }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);
                String blueToothId = intent.getStringExtra(BluetoothDevice.EXTRA_NAME);
                Log.v(TAG, "Found Bluetooth device: " + blueToothId);
                if (blueToothId != null) {
                    if (!checkIfDeviceAlreadyInRange(blueToothId)) {
                        //New device, or re-entered device, update the display list and test caregiver
                        rssi_msg.setText(getActiveDevices());
                        //rssi_msg.setText(rssi_msg.getText() + "\n" + blueToothId + " => " + rssi + "dBm");
                        //Match the ID of the caregiver
                        if (isCareGiver(blueToothId)) {
                            //Trigger notice if the device is not already found
                            triggerDataChange(blueToothId);
                        }
                    }else{
                        //Old device, update the display list
                        rssi_msg.setText(getActiveDevices());
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.v(TAG, "Restarting the bluetooth discovery");
                rssi_msg.setText(getActiveDevices());
                discoveryDevices();
            }
        }
    };

    /**
     * Get a list of devices that are considered to be active in String for display
     * @return  list of devices as String
     */
    private String getActiveDevices() {
        long current_time = System.currentTimeMillis();
        StringBuilder builder = new StringBuilder();
        builder.append("Bluetooth devices detected:");
        for (Map.Entry<String, Long> entry : devicesInRange) {
            long difference = (current_time - entry.getValue()) / 1000;
            if(difference < RE_ENTER_DURATION){
                builder.append(System.lineSeparator() + entry.getKey());
            }
        }
        return builder.toString();
    }

    private boolean isCareGiver(String name) {
        Log.v(TAG, "Matching caregiver IDs");
        for (String careGiverId : CARE_GIVER_IDS) {
            Log.v(TAG, "Matching " + name + " with ID " + careGiverId);
            if (name.contains(careGiverId)) {
                return true;
            }
        }
        return false;
    }

    private boolean checkIfDeviceAlreadyInRange(String name) {
        long current_time = System.currentTimeMillis();
        for (Map.Entry<String, Long> entry : devicesInRange) {
            String s = entry.getKey();
            if (name.equals(s)) {
                long previous_time = entry.getValue();
                long difference = (current_time - previous_time) / 1000;
                if (difference > RE_ENTER_DURATION) {
                    //The device is considered to have re-entered if duration is longer than 30 seconds
                    entry.setValue(current_time);
                    return false;
                } else {
                    //If the device is found again within 30 seconds, consider to be already there
                    entry.setValue(current_time);
                    return true;
                }
            }
        }
        //Device is not in the list
        devicesInRange.add(new AbstractMap.SimpleEntry<>(name, current_time));
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
    private void triggerDataChange(String bluetooth_id) {
        Log.v("myTag", "Trigger Data Change");

        //String message = "triggerDataChange";
        //Requires a new thread to avoid blocking the UI
        //new SendToDataLayerThread("/message_path", message).start();

        String name = "";
        Bitmap image = null;
        //Get Caregiver details
        for (HashMap<String, String> caregiver : caregiverList) {
            String shortID = caregiver.get(KEY_BLUETOOTH);
            if (bluetooth_id.contains(shortID)) {
                int id = Integer.parseInt(caregiver.get(KEY_ID));
                name = caregiver.get(KEY_NAME);
                byte[] image_byteArray = caregiverImageList.get(id);
                image = BitmapFactory.decodeByteArray(image_byteArray, 0, image_byteArray.length);
            }
        }

        //Trigger music playing
        triggerMusicPlaying();

        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/count");
        if (image == null) {
            image = BitmapFactory.decodeResource(
                    getResources(), R.mipmap.ic_photo);
        }

        Asset asset = createAssetFromBitmap(image);
        count++;
        putDataMapReq.getDataMap().putString(KEY_TITLE,
                String.format("Caregiver %s is here!", name));
        putDataMapReq.getDataMap().putInt(COUNT_KEY, count);
        putDataMapReq.getDataMap().putLong("time", new Date().getTime());
        putDataMapReq.getDataMap().putAsset(KEY_IMAGE, asset);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
    }

    private void triggerMusicPlaying() {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        final MediaPlayer mp = MediaPlayer.create(getApplicationContext(), notification);
        mp.start();

        CountDownTimer timer = new CountDownTimer(7000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                // Nothing to do
            }

            @Override
            public void onFinish() {
                if (mp.isPlaying()) {
                    mp.stop();
                    mp.release();
                }
            }
        };
        timer.start();
    }

    @Override
    public void onLocationChanged(Location location) {
        double lat = (location.getLatitude());
        double lng = (location.getLongitude());

        //JSONObject locationJson = new JSONObject();
        //try {
        //    locationJson.put("latitute", lat);
        //    locationJson.put("longitude", lng);
        //} catch (JSONException e) {
        //    e.printStackTrace();
        //}
        //String locationJSONString = locationJson.toString();

        //Send to server
        //Add nameValuePair for http request
        //List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        //addToNameValuePairs(nameValuePairs, "deviceId", deviceId);
        //addToNameValuePairs(nameValuePairs, "location", locationJSONString);
        //new sendLocationAsync().execute(nameValuePairs);

        //latituteField.setText(String.valueOf(lat));
        //longitudeField.setText(String.valueOf(lng));
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
     *
     * @param nameValuePairs NameValuePair
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
