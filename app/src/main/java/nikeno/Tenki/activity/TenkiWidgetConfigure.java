package nikeno.Tenki.activity;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import nikeno.Tenki.AreaSelectActivity;
import nikeno.Tenki.Utils;
import nikeno.Tenki.service.WidgetUpdateService;

public class TenkiWidgetConfigure extends Activity {

	private static final String TAG = TenkiWidgetConfigure.class.getSimpleName();
	
	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			appWidgetId = extras.getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID, 
					appWidgetId);
		}

		if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
			finish();
			return;
		}
		
		Intent i = new Intent(this, AreaSelectActivity.class);
		startActivityForResult(i, 1);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		switch (resultCode) {
		case RESULT_OK:
			if (data != null){
				// ウィジェットの設定を更新 
				String url = data.getStringExtra("url");
				addWidgetConfig(this, appWidgetId, url);
				
				// Homeアプリに戻り値を返す
				Intent i = new Intent();
				i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
				setResult(RESULT_OK, i);
				
				// ウィジェットを更新する
				startService(new Intent(this,WidgetUpdateService.class));
			}
			finish();
			break;
		default:
			finish();
			break;
		}
	}
	
	// ----------------------------------
	// 設定の読み書き
	// ----------------------------------
	
	public static void addWidgetConfig(Context context, int id, String url) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		
		Editor edit = pref.edit();
		edit.putString(prefKey(id,"url"), url);
		edit.commit();
	}
	
	public static WidgetConfig getWidgetConfig(Context context, int id) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);

		WidgetConfig config = new WidgetConfig();
		config.url = Utils.httpsUrl(pref.getString(prefKey(id, "url"), ""));

		return config;
	}
	
	public static void deleteWidgetConfig(Context context, int [] ids) {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		Editor edit = pref.edit();
		
		for (int id: ids) {
			Log.d(TAG, "deleteWidgetConfig id:"+id);
			edit.remove(prefKey(id, "url"));
		}
		edit.commit();
	}
	
	public static String prefKey(int id, String name) {
		return "widget_" + String.valueOf(id) + "name";
	}
	
	// ----------------------------------
	// 設定
	// ----------------------------------
	
	public static class WidgetConfig {
		public String url;
	}
}
