package nikeno.Tenki.activity;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;

import nikeno.Tenki.service.WidgetUpdateService;
import nikeno.Tenki.appwidget.weatherwidget.WeatherWidgetPrefs;

public class TenkiWidgetConfigure extends Activity {

	private static final String TAG = TenkiWidgetConfigure.class.getSimpleName();
	
	private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Bundle extras = getIntent().getExtras();
		if (extras != null) {
			mAppWidgetId = extras.getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID,
					mAppWidgetId);
		}

		if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
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
				WeatherWidgetPrefs.addWidgetConfig(this, mAppWidgetId, url);
				
				// Homeアプリに戻り値を返す
				Intent i = new Intent();
				i.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
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
}
