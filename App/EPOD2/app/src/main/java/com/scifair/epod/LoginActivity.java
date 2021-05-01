package com.scifair.epod;

import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

	private Button subBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button infoBtn = findViewById(R.id.infoBtn);
        infoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), getCode.class);
                startActivity(i);
            }
        });

        subBtn = findViewById(R.id.subBtn);  //Submit Button
        subBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText codeIn = findViewById(R.id.codeInput);
                new DownloadCheckTask().execute(codeIn.getText().toString());
            }
        });


    }

    private static long parseUnsignedLongHex(String value) throws NumberFormatException {
    	if (value.length() == 0 || value.length() > 16)
    		throw new NumberFormatException("The value should be 1-16 characters");

    	long val = 0;
    	for (int i = 0; i < value.length(); i++) {
    		long digit = Character.digit(value.charAt(i), 16);

    		if (digit < 0) {
    			throw new NumberFormatException(value.charAt(i) + " is not a valid hex digit");
			}

			val <<= 4L;
			val |= digit;
		}

		return val;
	}


    public class DownloadCheckTask extends AsyncTask<String, Void, Boolean> {

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

			subBtn.setText(R.string.submit_code_checking);
			subBtn.setEnabled(false);
		}

		@Override
        protected Boolean doInBackground(String... key) {
            try {
				long id = parseUnsignedLongHex(key[0]);

                try {
                    URL url = new URL("http://192.168.0.118/checkvalid?id=" + URLEncoder.encode(Long.toHexString(id), "UTF-8"));

                    HttpURLConnection con = (HttpURLConnection) url.openConnection();

                    con.setRequestMethod("GET");
                    con.setConnectTimeout(5000);
                    int status = con.getResponseCode();

                    if (status != 200) {
                        con.disconnect();
                        return null;
                    } else {
                        byte value = (byte) con.getInputStream().read();

                        con.disconnect();

                        return value == (byte) 't' || value == (byte) 'T';
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return null;
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }


        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);

			subBtn.setText(R.string.submit_code_submit);
			subBtn.setEnabled(true);

			if (success == null) {
				Toast.makeText(LoginActivity.this, "Failed to check key with server", Toast.LENGTH_SHORT).show();
			} else {
				if (success) {
					Intent i = new Intent(getApplicationContext(), MainActivity.class);
					startActivity(i);
					finish();

					//Changes boolean value to true to skip login
					Shared.create(getApplicationContext());
					Shared.SHARED.secondTime();
				} else {
					TextView errorMsg = findViewById(R.id.ErrorMsg);
					errorMsg.setText("Incorrect code, please try again.");
					System.out.println("wrong place");
				}
			}
        }
    }
}
