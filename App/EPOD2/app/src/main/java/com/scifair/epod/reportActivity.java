package com.scifair.epod;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

public class reportActivity extends AppCompatActivity {
	private static final String rsaPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArZN4Qdwp0CRDZuN9eETOcgBFw1Zfrxy+TE2/TT+YLG+bXkrpzzJ3C8oj3sFbv68+wG3xBJ199g0rc7blerr3H2RQYYu4u2xCaXnJTWTUuNhOixX8IGiQ4rKy8z6jeDXudhiRpklKO4LXFoYWo3djcZeG/o2G6zEvYYTcPx+zud2yBRX+e1so0WMJP9AXlgp5j5BRgCM0T+KNQpGUgwtQUMWqaClBncd9BwONtVkLJbfGD3iYPV7gQEnOanvm7zvftkh7rEZScwo+KAaADmHuFPRR8Cjbyyi4lugk0UgaJPaNtDwacJVUGwQtGr3i27HgXmgKtWWGE8T7UKhMN5A9hwIDAQAB";

	private class ManualReport extends AsyncTask<Location, Void, Boolean> {
		private void fromDouble(byte[] arr, int offset, double value) {
			long long_ = Double.doubleToLongBits(value);

			arr[offset] = (byte) long_;
			arr[offset + 1] = (byte) (long_ >>> 8L);
			arr[offset + 2] = (byte) (long_ >>> 16L);
			arr[offset + 3] = (byte) (long_ >>> 24L);
			arr[offset + 4] = (byte) (long_ >>> 32L);
			arr[offset + 5] = (byte) (long_ >>> 40L);
			arr[offset + 6] = (byte) (long_ >>> 48L);
			arr[offset + 7] = (byte) (long_ >>> 56L);
		}

		private void fromLong(byte[] arr, int offset, long value) {
			arr[offset] = (byte) value;

			arr[offset + 1] = (byte) (value >>> 8L);
			arr[offset + 2] = (byte) (value >>> 16L);
			arr[offset + 3] = (byte) (value >>> 24L);
			arr[offset + 4] = (byte) (value >>> 32L);
			arr[offset + 5] = (byte) (value >>> 40L);
			arr[offset + 6] = (byte) (value >>> 48L);
			arr[offset + 7] = (byte) (value >>> 56L);
		}


		@Override
		protected Boolean doInBackground(Location... locations) {
			try {
				Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				KeyFactory factory = KeyFactory.getInstance("RSA");
				X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.decode(rsaPublicKey, Base64.DEFAULT));

				PublicKey key = factory.generatePublic(publicKeySpec);

				double latitude = locations[0].getLatitude();
				double longitude = locations[1].getLongitude();

				if (Double.isNaN(latitude) || Double.isNaN(longitude) || latitude > 85.0 || latitude < -85.0 || longitude > 180.0 || longitude < -180.0) {
					return false;
				}

				byte[] message = new byte[16];

				fromDouble(message, 0, latitude);
				fromDouble(message, 8, longitude);

				cipher.init(Cipher.ENCRYPT_MODE, key);

				URL url = new URL("http://192.168.0.118/manual?pos=" + new String(Base64.encode(cipher.doFinal(message), Base64.DEFAULT), "UTF-8"));
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				con.setRequestMethod("GET");
				int status = con.getResponseCode();

				con.disconnect();
				return status == 200;
			} catch (Exception e) {
				return false;
			}
		}

		@Override
		protected void onPostExecute(Boolean success) {
			if (success) {
				Intent intentS = new Intent(getApplicationContext(), reportSuccess.class);
				startActivity(intentS);
			} else {
				Intent intentF = new Intent(getApplicationContext(), reportFailed.class);
				startActivity(intentF);
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_report);

		//Telling image return button what to do when clicked.
		ImageButton returnBtn = findViewById(R.id.returnBtn);
		returnBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent startIntent = new Intent(getApplicationContext(), MainActivity.class);
				startActivity(startIntent);
			}
		});

		Button confirmBtn = findViewById(R.id.confirmBtn);
		confirmBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//Takes to success screen but does nothing if connected to wifi to reduce fake reports from users
				ConnectivityManager connManager = (ConnectivityManager) getSystemService(Application.CONNECTIVITY_SERVICE);
				NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				if (mWifi.isConnected()) {
//				if (false) {
					Intent intentS = new Intent(getApplicationContext(), reportSuccess.class);
					startActivity(intentS);
				} else {
					try {

						FusedLocationProviderClient mFusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplicationContext());

						if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
							//Skips if no location Access
							Intent intentF = new Intent(getApplicationContext(), reportFailed.class);
							startActivity(intentF);

						} else {
							mFusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
								@Override
								public void onSuccess(Location location) {
									if (location != null) {
										Shared.create(getApplicationContext());
										if (Shared.SHARED.canManualReport()) {
											new ManualReport().execute(location);
										} else {
											Intent intentS = new Intent(getApplicationContext(), reportSuccess.class);
											startActivity(intentS);
										}

									} else {
										Intent intentF = new Intent(getApplicationContext(), reportFailed.class);
										startActivity(intentF);
									}
								}
							});
						}
					} catch (Exception e) {
						e.printStackTrace();
						Intent intentF = new Intent(getApplicationContext(), reportFailed.class);
						startActivity(intentF);
					}

				}
			}
		});


	}

	//Little endian converter
	private static int writeDouble(double value, byte[] arr, int index) {
		long val = Double.doubleToLongBits(value);

		arr[index++] = (byte) (val & 0xFF);
		arr[index++] = (byte) ((val >>> 8) & 0xFF);
		arr[index++] = (byte) ((val >>> 16) & 0xFF);
		arr[index++] = (byte) ((val >>> 24) & 0xFF);
		arr[index++] = (byte) ((val >>> 32) & 0xFF);
		arr[index++] = (byte) ((val >>> 40) & 0xFF);
		arr[index++] = (byte) ((val >>> 48) & 0xFF);
		arr[index++] = (byte) ((val >>> 56));

		return index;
	}
}
