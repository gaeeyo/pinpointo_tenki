package nikeno.Tenki;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

import nikeno.Tenki.activity.TenkiWidgetConfigure;
import nikeno.Tenki.service.WidgetUpdateService;

public class TenkiWidgetProvider extends AppWidgetProvider {

	static final String TAG = TenkiWidgetProvider.class.getSimpleName();

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		super.onUpdate(context, appWidgetManager, appWidgetIds);

		WidgetUpdateService.setAlarm(context);

		WidgetUpdateService.start(context);
	}

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		super.onDeleted(context, appWidgetIds);
		TenkiWidgetConfigure.deleteWidgetConfig(context, appWidgetIds);
	}

}
