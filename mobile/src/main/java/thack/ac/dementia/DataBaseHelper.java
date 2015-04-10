package thack.ac.dementia;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by Zhu on 6/12/2014.
 * For creating and managing database
 */
public class DataBaseHelper extends SQLiteOpenHelper {
    static String TAG = "DataBaseHelper";

    static List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

    /*
    * The Database version number will need to be updated each time its content gets changed!
    * */
    Resources res;
    Context   context;

    public DataBaseHelper(Context context) {
        super(context, "Caregivers.db", null, 3);
        res = context.getResources();
        this.context = context;
    }

    public byte[] drawableConverter(String imageName) {
        int resID = res.getIdentifier(imageName, "mipmap", context.getPackageName());
        Bitmap bmp = BitmapFactory.decodeResource(context.getResources(), resID);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    public static String getDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd' at 'HH:mm:ss.SSSZ", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    //The method takes in a second so that the smaller second results in earlier time for entry
    private String getOldDateTime(int second) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyy-MM-dd' at 'HH:mm:ss.SSSZ", Locale.getDefault());
        Date date;
        Calendar cal = Calendar.getInstance();
        cal.set(1997, Calendar.JANUARY, 1, 1, 1, second);
        date = cal.getTime();
        return dateFormat.format(date);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE mytable(" +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "Name TEXT UNIQUE," +
                "Image BLOB," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "BluetoothID TEXT);");

        /*
        * Actual Data
        * Anti-Chronological order, first inserted entry will be shown at the end
        * */
        ContentValues cv = new ContentValues();
        cv.put("_id", 1);
        cv.put("Name", "Zhu Liang");
        cv.put("Image", drawableConverter("ic_photo"));
        cv.put("created_at", getOldDateTime(1));
        cv.put("BluetoothID", "Zhu");
        db.insert("mytable", "Name", cv);


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS mytable");
        onCreate(db);
    }

    public static int deleteId(SQLiteDatabase db, String id) {
        Log.e(TAG, "ID: " + id + " deleted by deletedId.");
        return db.delete("mytable", "_id" + "=" + id, null);
    }

    public static boolean insertDataIntoDatabase(SQLiteDatabase db, String name, String bluetoothID, Bitmap bitmap) {
        //Insert data into database
        ContentValues cv = new ContentValues();
        cv.put("Name", name);
        cv.put("Image", Utils.bitmapConverter(bitmap));
        cv.put("BluetoothID", bluetoothID);
        try {
            db.insertWithOnConflict("mytable", "Name", cv, SQLiteDatabase.CONFLICT_REPLACE);
            return true;
        } catch (SQLiteException exception) {
            Log.e(TAG, "Insertion error.");
            exception.printStackTrace();
            return false;
        }
    }

}
