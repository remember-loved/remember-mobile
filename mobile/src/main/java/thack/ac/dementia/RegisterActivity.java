package thack.ac.dementia;

import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


public class RegisterActivity extends ActionBarActivity {

    RegisterActivity self = this;

    SQLiteDatabase db;

    ArrayList<String> deviceNames = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        Button registerButton = (Button) findViewById(R.id.btnRegister);
        Button backButton = (Button) findViewById(R.id.back);

        final TextView emptyText = (TextView) findViewById(R.id.empty_notice);
        final EditText nameView = (EditText) findViewById(R.id.reg_name);
        final EditText idView = (EditText) findViewById(R.id.reg_bluetooth);
        idView.setHint("Type or choose a nearby device");

        final Bitmap defaultBitmap = BitmapFactory.decodeResource(
                getResources(), R.mipmap.ic_photo);

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
                    db = (new DataBaseHelper(getApplicationContext())).getWritableDatabase();
                    boolean success = DataBaseHelper.insertDataIntoDatabase(db, name, id, defaultBitmap);
                    db.close();
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
