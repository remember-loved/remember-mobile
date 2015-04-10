package thack.ac.dementia;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Adapter for the caregivers' list
 * @author paradite
 */
public class LazyAdapter extends BaseAdapter {

    private ArrayList<HashMap<String, String>> data;
    private HashMap<String, byte[]>            imageMap;
    byte[] byteArray;
    private static LayoutInflater inflater = null;
    View vi;
    View mlayout;
    //public ImageLoader imageLoader;
    //TypedArray pictures;

    public LazyAdapter(Activity a, ArrayList<HashMap<String, String>> d, HashMap<String, byte[]> imageMap) {
        Activity activity = a;
        data = d;
        this.imageMap = imageMap;
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //imageLoader=new ImageLoader(activity.getApplicationContext());
        //pictures = pic;
    }

    public int getCount() {
        return data.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        vi = convertView;
        if (convertView == null)
            vi = inflater.inflate(R.layout.list_row, null);

        TextView name = (TextView) vi.findViewById(R.id.title); // name
        TextView extra = (TextView) vi.findViewById(R.id.extra); // extra information
        TextView id = (TextView) vi.findViewById(R.id.id); // extra information
        ImageView thumb_image = (ImageView) vi.findViewById(R.id.list_image); // thumb image
        mlayout = vi.findViewById(R.id.background_wrapper);
        HashMap<String, String> item;
        item = data.get(position);
        byteArray = imageMap.get(item.get(MainActivity.KEY_ID));
        Bitmap bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);

        // Setting all values in listview
        name.setText(item.get(MainActivity.KEY_NAME));
        extra.setText(item.get(MainActivity.KEY_BLUETOOTH));
        id.setText(item.get(MainActivity.KEY_ID));
        thumb_image.setImageBitmap(bmp);
        /*imageLoader.DisplayImage(item.get(CustomizedListView.KEY_THUMB_URL), thumb_image);*/

        return vi;
    }

}
