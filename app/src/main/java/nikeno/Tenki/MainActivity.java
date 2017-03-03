package nikeno.Tenki;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class MainActivity extends Activity {
	public static final String APP_PREF = "AF.Tenki";

	private static final String TAG = "Tenki_MainActivity";
	private static final String WeekStr = "日月火水木金土";

	private DownloadTask mDownloadTask = null;
	private DayView mTodayTable;
	private DayView mTomorrowTable;
	private WeekView mWeekTable;
	private YahooWeather mWeatherData = null;
	private TextView mTodayHeader;
	private TextView mTomorrowHeader;
	private SharedPreferences mPref;
	private String mPrefUrl;
//	private ConnectivityManager mConnectivityManager;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //System.gc();
        //Debug.startMethodTracing("MainActivity1");

        // 設定を読み込み
        mPref = getSharedPreferences(APP_PREF, MODE_PRIVATE);

        mPrefUrl = getIntent().getDataString();
        if (mPrefUrl == null) {
        	loadSettings();
        }
    	reload(false);

    	// ウィンドウの初期化
    	requestWindowFeature(Window.FEATURE_PROGRESS);
    	requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        initViews();

        // キャッシュを表示
        showCache();
        //Debug.stopMethodTracing();
    }

    private void initViews() {
        setContentView(R.layout.main);

        mTodayTable = (DayView)findViewById(R.id.todayTable);
        mTomorrowTable = (DayView)findViewById(R.id.tomorrowTable);
        mWeekTable = (WeekView)findViewById(R.id.weekTable);
        mTodayHeader = (TextView)findViewById(R.id.todayHeader);
        mTomorrowHeader = (TextView)findViewById(R.id.tomorrowHeader);

        // ブラウザで開くボタン
        Button btn;
        btn = (Button)findViewById(R.id.open_browser);
        btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doOpenBrowser();
			}
		});

        // 地域変更ボタン
        btn = (Button)findViewById(R.id.area_select);
        btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				showPref();
			}
		});
    }

//    private boolean isOnline() {
//        NetworkInfo ni = mConnectivityManager.getActiveNetworkInfo();
//    	if (ni != null) {
//    		return ni.isAvailable();
//    	}
//    	return false;
//    }
//

    // ブラウザで開く
    private void doOpenBrowser() {
    	try {
    		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(mPrefUrl));
    		startActivity(i);
    	}
    	catch (Exception e) {
    		Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
    	}
    }

    // キャッシュを表示する
    private void showCache() {
    	byte [] data = Downloader.getInstance(this).getCache(mPrefUrl, System.currentTimeMillis() - 24 * DateUtils.HOUR_IN_MILLIS);
    	if (data != null) {
    		try {
    			YahooWeather weatherData = YahooWeather.parse(data);
    			setData(weatherData);
    		}
    		catch (Exception e) {
    			Log.d(TAG, e.getMessage());
    		}
    	}
    }

    // リロードする
    private void reload(boolean force) {
    	if (mDownloadTask != null && mDownloadTask.getStatus() != FetchWeatherTask.Status.FINISHED)
    		mDownloadTask.cancel(true);

    	mDownloadTask = new DownloadTask(force);
    	mDownloadTask.execute(mPrefUrl);
    }

    // 設定を読み込み
    private void loadSettings() {
    	mPrefUrl = mPref.getString("url",
    			"http://weather.yahoo.co.jp/weather/jp/13/4410/13101.html");
    }

    // リロード完了
    private void reloadComplete(DownloadTask task)
    {
//    	initViews();
		if (task.mResultMessage != null) {
			// エラー表示
			Toast.makeText(getApplicationContext(), task.mResultMessage, Toast.LENGTH_LONG).show();
		}
		else {
			// エラーなし
			setProgress(75*100);
			if (task.isCache()) {
				Toast.makeText(getApplicationContext(),
						getText(R.string.using_cache), Toast.LENGTH_LONG).show();
			}

			setData(task.mWeatherData);
		}
    	setProgress(100*100);
    	setProgressBarIndeterminateVisibility(false);
    }

    private void setData(YahooWeather data) {
		mWeatherData = data;

		setTitle(getString(R.string.mainTitleFormat, mWeatherData.areaName));

		if (mTodayHeader != null) {
			mTodayHeader.setText(getDateText(mWeatherData.today.date));
		}
		if (mTomorrowHeader != null) {
			mTomorrowHeader.setText(getDateText(mWeatherData.tomorrow.date));
		}
		if (mTodayTable != null) {
			mTodayTable.setData(mWeatherData.today);
		}
		if (mTomorrowTable != null) {
			mTomorrowTable.setData(mWeatherData.tomorrow);
		}
		if (mWeekTable != null) {
			mWeekTable.setData(mWeatherData.days);
		}
    }

    private String getDateText(Date d)
    {
    	Calendar c = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
    	c.setTime(d);
    	int day = c.get(Calendar.DAY_OF_WEEK) - 1;
    	return String.format("%d月%d日(%s)",
    			c.get(Calendar.MONTH) + 1,
    			c.get(Calendar.DATE),
    			WeekStr.substring(day, day+1));
    }

    // ダウンロード
    private class DownloadTask extends AsyncTask<String, Integer, String> {

    	private boolean mForceReload;
    	private boolean mIsCache = false;
    	public String mResultMessage = null;
    	public YahooWeather mWeatherData = null;

    	public DownloadTask(Boolean force) {
    		mForceReload = force;
    	}

    	public boolean isCache() {
    		return mIsCache;
    	}

    	@Override
    	protected void onPreExecute() {
        	setProgressBarIndeterminateVisibility(true);
    		super.onPreExecute();
    	}

    	@Override
    	protected String doInBackground(String... params) {
    		String result = null;
    		try {
    			publishProgress(25*100);
    			long since = -1; // リロード
    			if (!mForceReload) {
    				// 1時間以内に取得していたらそれを表示
    				since = System.currentTimeMillis() - Const.PRIORITY_CACHE_TIME;
    			}
    			// ダウンロード
    			byte [] buff = null;
    			try {
    				buff = Downloader.getInstance(MainActivity.this)
							.download(params[0], 50*1024, since, true);
    			}
    			catch (Exception e) {
        			if (since != -1) {
        				// エラーが発生してもキャッシュがあれば使う
        				buff = Downloader.getInstance(MainActivity.this).getCache(params[0],
        						System.currentTimeMillis() - 8 * DateUtils.HOUR_IN_MILLIS);
        				if (buff != null) {
        					mIsCache = true;
        				}
        			}
        			else {
        				throw e;
        			}
    			}
    			if (buff == null) {
    				throw new Exception("Download error.");
    			}
    			mWeatherData = YahooWeather.parse(buff);
    			publishProgress(50*100);
    		}
    		catch (Exception e) {
    			mResultMessage = getText(R.string.reload_error) +
    				"\n" + e.getMessage();
    			e.printStackTrace();
    		}
    		return result;
    	}

    	@Override
    	protected void onPostExecute(String result) {
    		super.onPostExecute(result);
    		reloadComplete(this);
    	}

    	@Override
    	protected void onProgressUpdate(Integer... values) {
    		super.onProgressUpdate(values);
    		setProgress(values[0]);
    	}
    }

    // メニュー表示
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.main_menu, menu);
    	return true;
    }

    // メニュークリック
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_reload:	// リロード
			reload(true);
			break;
		case R.id.menu_pref:	// 地域変更
			showPref();
			break;
		case R.id.menu_help:	// ヘルプ
			{
				Intent i = new Intent(getApplicationContext(), HelpActivity.class);
				startActivity(i);
			}
			break;
		}
    	return true;
    }

    // 設定画面を表示
    void showPref() {
    	Intent i = new Intent(getApplicationContext(), AreaSelectActivity.class);
    	startActivityForResult(i, 0);
    }

    // 地域変更確定時の動作
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	Log.d(TAG,"onActivityResult");
    	if (resultCode == RESULT_OK) {
	    	mPrefUrl = data.getStringExtra("url");

	    	Editor editor = mPref.edit();
	    	editor.putString("url", mPrefUrl);
	    	editor.commit();

	    	reload(false);
    	}
    }

    // 検索ボタン
    @Override
    public boolean onSearchRequested() {
    	showPref();
    	return true;
    }
}