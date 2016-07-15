package nikeno.Tenki;

import android.app.Application;

public class TenkiApp extends Application {
	@Override
	public void onCreate() {
		super.onCreate();

		Const.init(this);
        // ダウンローダーの初期化
        Downloader.initialize(getApplicationContext());
	}
}
