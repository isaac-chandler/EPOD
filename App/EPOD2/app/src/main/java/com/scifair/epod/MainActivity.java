package com.scifair.epod;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int ERROR_DIALOG_REQUEST = 9001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Checks to see if application has access to FINE_LOCATION and if not requests it(has to be manually pushed to user for them to accept as target SDK is > 22)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }

        Button mnlBtn = findViewById(R.id.reportBtn);
        mnlBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(getApplicationContext(), reportActivity.class);
                startActivity(startIntent);
            }
        });

        //Telling info button on home screen what to do when clicked.
        Button infoBtn = findViewById(R.id.infoBtn);
        infoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(getApplicationContext(), infoActivity.class);
                startActivity(startIntent);
            }
        });

        //Telling outage map button what to do
        Button mapBtn = findViewById(R.id.mapBtn);
        mapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isServicesOK()){
                Intent i = new Intent(getApplicationContext(), MapsActivity.class);
                startActivity(i);
                } else {
                    //TODO: If they haven't allowed location what to do here
                }
            }
        });

    }
    @Override
    protected void onStart(){
        super.onStart();
        Shared.create(getApplicationContext());

        //To check if b is true or false
        Shared.SHARED.firstTime();
    }

    public boolean isServicesOK(){
        Log.d(TAG, "isServicesOK: checking google services version");
        int avaliable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);

        if(avaliable == ConnectionResult.SUCCESS){

            //User is able to make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;

        } else if(GoogleApiAvailability.getInstance().isUserResolvableError(avaliable)) {
            //an error occured but is resolvable
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, avaliable, ERROR_DIALOG_REQUEST);
            dialog.show();

        } else {
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;

    }

}
