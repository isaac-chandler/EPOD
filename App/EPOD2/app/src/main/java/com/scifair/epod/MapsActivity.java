package com.scifair.epod;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnCameraIdleListener {

	private Handler handler;
	private Runnable updateMap = new Runnable() {
		@Override
		public void run() {
			updateHeatmap();
			handler.postDelayed(this, 60_000);
		}
	};

	public class DownloadDataTask extends AsyncTask<Double, Void, List<LatLng>> {

		@Override
		protected List<LatLng> doInBackground(Double... coords) {
			double neX = coords[0];
			double neY = coords[1];
			double swX = coords[2];
			double swY = coords[3];

			try {
				URL url = new URL("http://192.168.0.118/data"
						+ "?nex=" + URLEncoder.encode(Long.toHexString(Double.doubleToLongBits(neX)), "UTF-8")
						+ "&ney=" + URLEncoder.encode(Long.toHexString(Double.doubleToLongBits(neY)), "UTF-8")
						+ "&swx=" + URLEncoder.encode(Long.toHexString(Double.doubleToLongBits(swX)), "UTF-8")
						+ "&swy=" + URLEncoder.encode(Long.toHexString(Double.doubleToLongBits(swY)), "UTF-8")
				);

				HttpURLConnection con = (HttpURLConnection) url.openConnection();

				con.setRequestMethod("GET");
				con.setConnectTimeout(5000);

				if (isCancelled()) {
					con.disconnect();
					return null;
				}

				int status = con.getResponseCode();

				if (status != 200) {
					con.disconnect();
					return null;
				} else {
					DataInputStream in = new DataInputStream(con.getInputStream());


					if (isCancelled()) {
						con.disconnect();
						return null;
					}

					int count = in.readInt();
					List<LatLng> ret = new ArrayList<>(count);

					Log.i("DataDownload", "Downloaded " + String.valueOf(count));

					for (int i = 0; i < count; i++) {
						ret.add(new LatLng(in.readDouble(), in.readDouble()));

						if (isCancelled()) {
							con.disconnect();
							return null;
						}
					}

					con.disconnect();

					return ret;
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		@Override
		protected void onPostExecute(List<LatLng> latLngs) {
			super.onPostExecute(latLngs);

			if (latLngs != null) {
				if (latLngs.size() == 0) {
					mProvider.setWeightedData(Collections.singletonList(new WeightedLatLng(new LatLng(0, 0), 0)));
					mOverlay.clearTileCache();
				} else {
					mProvider.setData(latLngs);
					mOverlay.clearTileCache();
				}
			}
		}
	}

	private DownloadDataTask downloadDataTask;

	private GoogleMap mMap;
	private FusedLocationProviderClient mFusedLocationProviderClient;
	private static final String tag = "MapsActivity";
	private Location currentLocation;
	private HeatmapTileProvider mProvider;
	private TileOverlay mOverlay;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
		// Obtain the SupportMapFragment and get notified when the map is ready to be used.
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);
	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;
		getDeviceLocation();

		mMap.getUiSettings().setRotateGesturesEnabled(false);
		mMap.getUiSettings().setTiltGesturesEnabled(false);
		//TODO: Once addHeatMap method is complete add call here

		addHeatMap();
		mMap.setOnCameraIdleListener(this);

		handler = new Handler();
		handler.post(updateMap);

	}


	private void getDeviceLocation() {
		mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
		try {

			Task location = mFusedLocationProviderClient.getLastLocation();
			location.addOnCompleteListener(new OnCompleteListener() {
				@Override
				public void onComplete(@NonNull Task task) {
					if (task.isSuccessful()) {
						Log.d(tag, "location found");
						currentLocation = (Location) task.getResult();
						if (currentLocation != null) {

							//Setting marker to the current location of the device
							LatLng loc = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
							mMap.addMarker(new MarkerOptions().position(loc).title("Current Location"));
							mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 12));
							Log.d(tag, "placing marker");
						} else {
							Log.d(tag, "Current location is null");
						}

					} else {
						Log.d(tag, "location not found");
						Toast.makeText(MapsActivity.this, "unable to get currect location", Toast.LENGTH_SHORT).show();
					}


				}
			});

		} catch (SecurityException e) {
			Log.d(tag, e.getMessage());
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		handler.removeCallbacks(updateMap);
	}

	private void addHeatMap() {
		//Creates heatmap tile provider and then passes LatLng's
		mProvider = new HeatmapTileProvider.Builder()
				.weightedData(Collections.singletonList(new WeightedLatLng(new LatLng(0, 0), 0)))
				.build();

		// Add a tile overlay to the map, using the heat map tile provider.
		mOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
	}

	private static final double MAX_DATA_REQUEST_AREA = 1E-6;

	private void updateHeatmap() {
		LatLngBounds mapArea = mMap.getProjection().getVisibleRegion().latLngBounds;

		double neLatRad = mapArea.northeast.latitude * (Math.PI / 180.0);
		double swLatRad = mapArea.southwest.latitude * (Math.PI / 180.0);

		double neX = (mapArea.northeast.longitude + 180.0) / 360.0;
		double swX = (mapArea.southwest.longitude + 180.0) / 360.0;

		double neY = (1.0 - Math.log(Math.tan(neLatRad) + 1.0 / Math.cos(neLatRad)) / Math.PI) * 0.5;
		double swY = (1.0 - Math.log(Math.tan(swLatRad) + 1.0 / Math.cos(swLatRad)) / Math.PI) * 0.5;

		if ((neX < swX && (swY - neY) * (1.0 - swX + neX) <= MAX_DATA_REQUEST_AREA) ||
				(neX > swX && (swY - neY) * (neX - swX) <= MAX_DATA_REQUEST_AREA)) {
			if (downloadDataTask != null && downloadDataTask.getStatus() != AsyncTask.Status.FINISHED) {
				downloadDataTask.cancel(false);
			}

			downloadDataTask = new DownloadDataTask();
			downloadDataTask.execute(neX, neY, swX, swY);
		}
	}

	@Override
	public void onCameraIdle() {
		updateHeatmap();
	}

	//TODO: Add data set creation from https://developers.google.com/maps/documentation/android-sdk/utility/heatmap

}
