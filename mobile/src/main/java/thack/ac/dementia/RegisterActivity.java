package thack.ac.dementia;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;


public class RegisterActivity extends ActionBarActivity {

    RegisterActivity self = this;

    SQLiteDatabase db;

    ArrayList<String> deviceNames = new ArrayList<>();

    //Photo related
    private static final int SELECT_PICTURE = 1;
    private String selectedImagePath;
    private ImageButton imgButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        Button registerButton = (Button) findViewById(R.id.btnRegister);
        Button backButton = (Button) findViewById(R.id.back);
        imgButton = (ImageButton) findViewById(R.id.imageButton);
        final TextView emptyText = (TextView) findViewById(R.id.empty_notice);
        final EditText nameView = (EditText) findViewById(R.id.reg_name);
        final EditText idView = (EditText) findViewById(R.id.reg_bluetooth);
        idView.setHint("Type or choose a nearby device");

        imgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,
                        "Select Picture"), SELECT_PICTURE);
            }
        });

        deviceNames = getIntent().getStringArrayListExtra(MainActivity.EXTRA_IDS);
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radiogrp);
        if (deviceNames.isEmpty()){
            //emptyText.setVisibility(View.VISIBLE);
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText("No devices detected, try go back to main screen to detect again");
            radioButton.setClickable(false);
            radioGroup.addView(radioButton);
        }
        for (String device: deviceNames){
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(device);
            radioButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    RadioButton radioButton = (RadioButton) v;
                    idView.setText(radioButton.getText());
                }
            });
            radioGroup.addView(radioButton);
        }



        registerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                String name = nameView.getText().toString();
                String id = idView.getText().toString();
                if (!name.isEmpty() && !id.isEmpty()){
                    ProgressDialog progressDialog = ProgressDialog.show(self, "Please wait", "Registering new caregiver...");
                    progressDialog.setMessage("Registering new caregiver...");
                    progressDialog.show();
                    db = (new DataBaseHelper(getApplicationContext())).getWritableDatabase();
                    Bitmap defaultBitmap = ((BitmapDrawable)imgButton.getDrawable()).getBitmap();

                    boolean success = DataBaseHelper.insertDataIntoDatabase(db, name, id, defaultBitmap);
                    db.close();
                    progressDialog.dismiss();
                    if (success){
                        Toast.makeText(self, "New caregiver " + name +" registered!", Toast.LENGTH_SHORT).show();
                        finish();
                    }else{
                        Toast.makeText(self, "Registration failed, please try again!", Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Toast.makeText(self, "Please fill in name and ID!", Toast.LENGTH_SHORT).show();
                }


            }
        });


        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                // Closing registration screen
                finish();
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                if (Build.VERSION.SDK_INT < 19) {
                    selectedImagePath = getPath(selectedImageUri);
                    Bitmap b = BitmapFactory.decodeFile(selectedImagePath);
                    Matrix m = new Matrix();
                    m.setRectToRect(new RectF(0, 0, b.getWidth(), b.getHeight()), new RectF(0, 0, 500, 500), Matrix.ScaleToFit.CENTER);
                    b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                    imgButton.setImageBitmap(b);

                }
                else {
                    ParcelFileDescriptor parcelFileDescriptor;
                    try {
                        parcelFileDescriptor = getContentResolver().openFileDescriptor(selectedImageUri, "r");
                        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                        Bitmap b = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                        Matrix m = new Matrix();
                        m.setRectToRect(new RectF(0, 0, b.getWidth(), b.getHeight()), new RectF(0, 0, 500, 500), Matrix.ScaleToFit.CENTER);
                        b = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                        parcelFileDescriptor.close();
                        imgButton.setImageBitmap(b);

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * helper to retrieve the path of an image URI
     */
    public String getPath(Uri uri) {
        if( uri == null ) {
            return null;
        }
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if( cursor != null ){
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        return uri.getPath();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_register, menu);
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
}
