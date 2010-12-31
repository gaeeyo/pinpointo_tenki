package nikeno.Tenki;

import nikeno.Tenki.activity.TenkiWidgetConfigure;
import nikeno.Tenki.service.WidgetUpdateService;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class TenkiWidgetProvider extends AppWidgetProvider {
	private static final String TAG = TenkiWidgetProvider.class.getSimpleName();
	
	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);
		
		Log.d(TAG, "onUpdate");
		context.startService(new Intent(context, WidgetUpdateService.class));
	}
	
	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		TenkiWidgetConfigure.deleteWidgetConfig(context, appWidgetIds);
	}
}
