package com.scifair.epod;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class reportFailed extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_failed);

        //Telling try again button to take you back to the previous screen (report activity)
        Button tryagainBtn = findViewById(R.id.tryagainBtn);
        tryagainBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(getApplicationContext(), reportActivity.class);
                startActivity(startIntent);
            }
        });

    }
}
