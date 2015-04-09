package thack.ac.dementia;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


/**
 * Class that contains useful general functions
 *
 * @author paradite
 */
public class Utils {
    static final int    RECOVERY_DIALOG_REQUEST          = 1;
    // JSON Node names
    static final String TAG_CATEGORY                     = "Category";
    static final String TAG_ID                           = "id";
    static final String TAG_NAME                         = "Name";
    static final String TAG_PHOTO                        = "Photo";
    static final String TAG_RESOURCES                    = "Materials";
    static final String TAG_STEPS                        = "Steps";
    static final String TAG_YOUTUBE                      = "VideoLink";
    static final String TAG_ABOUT                        = "About";
    static final String TAG_DATE                         = "ModifiedDate";
    static final int    PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    /**
     * Urls for server communications
     */
    static       String sendUrl                          = "http://137.132.71.19/idcount.php";
    static       String getUrl                           = "http://137.132.71.19/getExpReal.php";
    static       String image_url_prefix                 = "http://137.132.71.19/Pictures/";

    public static void CopyStream(InputStream is, OutputStream os) {
        final int buffer_size = 1024;
        try {
            byte[] bytes = new byte[buffer_size];
            for (; ; ) {
                int count = is.read(bytes, 0, buffer_size);
                if (count == -1)
                    break;
                os.write(bytes, 0, count);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Convert bitmap to BLOB for SQL storage
     *
     * @param bmp bitmap
     * @return BLOB object
     */
    public static byte[] bitmapConverter(Bitmap bmp) {
        if (bmp == null) {
            return new byte[]{};
        }
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    /**
     * Parse the date from server to String for local database
     *
     * @param date_received date received from server
     * @return Date for storing in local database
     */
    public static String parseDateFromServerToLocalDatabase(String date_received) {
        //2014-06-28 14:56:59
        SimpleDateFormat dateFormat_received = new SimpleDateFormat(
                "yyyy-MM-dd' 'HH:mm:ss", Locale.getDefault());
        Date date = null;
        try {
            date = dateFormat_received.parse(date_received);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        SimpleDateFormat dateFormat_database = new SimpleDateFormat(
                "yyyy-MM-dd' at 'HH:mm:ss.SSSZ", Locale.getDefault());
        return dateFormat_database.format(date);
    }

    /**
     * Get the current time for request to server
     *
     * @return date formatted for server request
     */
    public static String getDateTimeForServer() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyyMMddHHmmss", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    /**
     * Get an old date for request to server
     *
     * @return an old date formatted for server request
     */
    public static String getOldDateTimeForServer() {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "yyyyMMddHHmmss", Locale.getDefault());
        Date date;
        Calendar cal = Calendar.getInstance();
        cal.set(1990, Calendar.JANUARY, 1, 1, 1, 1);
        date = cal.getTime();
        return dateFormat.format(date);
    }

    /**
     * Check the time different between the last sync and the current time
     *
     * @param date_received Date from the preference of the app
     * @return true if difference is more than 30 seconds
     */
    public static Boolean checkDateDiff(String date_received) {
        Date date_pref = new Date();
        SimpleDateFormat dateFormat_send = new SimpleDateFormat(
                "yyyyMMddHHmmss", Locale.getDefault());
        try {
            date_pref = dateFormat_send.parse(date_received);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Date date_current = new Date();
        long seconds = (date_current.getTime() - date_pref.getTime()) / 1000;
//        Log.e("Check, ", "time diff in seconds: " + seconds);
        return seconds > 30;
    }
}
