package com.scifair.epod;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class Shared {

	//Creates shared preferences file
	SharedPreferences sharedPreferences;

	//to edit the shared preferences file
	SharedPreferences.Editor editor;

	//context pass the reference to another class
	Context context;

	//mode should be private for shared preferences file
	int mode = 0;

	//shared preferences file name
	String filename = "sdfile";

	//Store the boolean value with respect to key id
	String Data = "b";

	String lastManualReportTime = "lastManualReportTime";

	public static Shared SHARED;

	public static void create(Context context) {
		if (SHARED == null) {
			SHARED = new Shared(context);
		}
	}

	//create constuctor to pass memory at runtime to the shared file
	private Shared(Context context) {
		this.context = context;
		sharedPreferences = context.getSharedPreferences(filename, mode);
		editor = sharedPreferences.edit();
		SHARED = this;
	}

	public void manualReport() {
		editor.putLong(lastManualReportTime, System.currentTimeMillis() / 1000);
		editor.commit();
	}

	public boolean canManualReport() {
        return System.currentTimeMillis() / 1000 - sharedPreferences.getLong(lastManualReportTime, 0) > 1800;
//		return true;
	}

	//For second time user
	public void secondTime() {
		editor.putBoolean(Data, true);
		editor.commit();
	}

	//For first time user
	public void firstTime() {
		if (!this.login()) {
			Intent i = new Intent(context, LoginActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			context.startActivity(i);
		}

	}

	//to get the default value as false
	private boolean login() {
		return sharedPreferences.getBoolean(Data, false);
	}
}
