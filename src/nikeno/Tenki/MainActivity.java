package nikeno.Tenki;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	public static final String APP_PREF = "AF.Tenki"; 
	
	private static final String TAG = "Tenki_MainActivity";
	private static final String WeekStr = "�����ΐ��؋��y"; 
	
	private DownloadTask mDownloadTask = null;
	private DayView mTodayTable;
	private DayView mTomorrowTable;
	private WeekView mWeekTable;
	private YahooWeather mWeatherData = null;
	private String mDefaultTitle;
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
        
        // �_�E�����[�_�[�̏�����
        Downloader.initialize(getApplicationContext());

        // �ݒ��ǂݍ���
        mPref = getSharedPreferences(APP_PREF, MODE_PRIVATE);
        loadSettings();
    	reload(false);

    	// �E�B���h�E�̏�����
    	requestWindowFeature(Window.FEATURE_PROGRESS);
    	
    	initViews();
        
        // �L���b�V����\��
        showCache();
        //Debug.stopMethodTracing();
    }
    
    private void initViews() {
        setContentView(R.layout.main);

        // �A�N�e�B�r�e�B�̃^�C�g����ۑ�
        mDefaultTitle = getTitle().toString();
        
        mTodayTable = (DayView)findViewById(R.id.todayTable);
        mTomorrowTable = (DayView)findViewById(R.id.tomorrowTable);
        mWeekTable = (WeekView)findViewById(R.id.weekTable);
        mTodayHeader = (TextView)findViewById(R.id.todayHeader);
        mTomorrowHeader = (TextView)findViewById(R.id.tomorrowHeader);
        
        // �u���E�U�ŊJ���{�^��
        Button btn;
        btn = (Button)findViewById(R.id.open_browser);
        btn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doOpenBrowser();
			}
		});
        
        // �n��ύX�{�^��
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
    
    // �u���E�U�ŊJ��
    private void doOpenBrowser() {
    	try {
    		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(mPrefUrl));
    		startActivity(i);
    	}
    	catch (Exception e) {
    		Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
    	}
    }

    // �L���b�V����\������
    private void showCache()
    {
    	Calendar today = Calendar.getInstance(Locale.JAPAN);
    	today.set(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DATE), 0, 0, 0);
    	
    	byte [] data = Downloader.getCache(mPrefUrl, today.getTime().getTime() / 1000);
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
    
    // �����[�h����
    private void reload(boolean force) {
    	if (mDownloadTask != null && mDownloadTask.getStatus() != FetchWeatherTask.Status.FINISHED)
    		mDownloadTask.cancel(true);
    	
    	mDownloadTask = new DownloadTask(force);
    	mDownloadTask.execute(mPrefUrl);
    }
    
    // �ݒ��ǂݍ���
    private void loadSettings() {
    	mPrefUrl = mPref.getString("url", 
    			"http://weather.yahoo.co.jp/weather/jp/13/4410/13101.html");	
    }
    
    // �����[�h����
    private void reloadComplete(DownloadTask task)
    {
    	initViews();
		if (task.mResultMessage != null) {
			// �G���[�\��
			Toast.makeText(getApplicationContext(), task.mResultMessage, Toast.LENGTH_LONG).show();
		}
		else {
			// �G���[�Ȃ�
			setProgress(75*100);
			if (task.isCache()) {
				Toast.makeText(getApplicationContext(), 
						getText(R.string.using_cache), Toast.LENGTH_LONG).show();
			}
			
			setData(task.mWeatherData);
		}
    	setProgress(100*100);
    }
    
    private void setData(YahooWeather data) {
		mWeatherData = data;
		
		setTitle(mDefaultTitle + " - " + mWeatherData.areaName);
		
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
    	return String.format("%d��%d��(%s)",
    			c.get(Calendar.MONTH) + 1, 
    			c.get(Calendar.DATE),
    			WeekStr.substring(day, day+1));
    }
    
    // �_�E�����[�h
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
    	protected String doInBackground(String... params) {
    		String result = null;
    		try {
    			publishProgress(25*100);
    			long since = -1; // �����[�h
    			if (!mForceReload) {
    				// 1���Ԉȓ��Ɏ擾���Ă����炻���\��
    				since = System.currentTimeMillis()/1000-15*60;
    			}
    			// �_�E�����[�h
    			byte [] buff = null;
    			try {
    				buff = Downloader.download(params[0], 50*1024, since, true);
    			}
    			catch (Exception e) {
        			if (since != -1) {
        				// �L���b�V���𗘗p
        				buff = Downloader.getCache(params[0], 
        						System.currentTimeMillis()/1000-7*60*60);
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
    
    // ���j���[�\��
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	getMenuInflater().inflate(R.menu.main_menu, menu);
    	return true;
    }
     
    // ���j���[�N���b�N
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_reload:	// �����[�h
			reload(true);
			break;
		case R.id.menu_pref:	// �n��ύX
			showPref();
			break;
		case R.id.menu_help:	// �w���v
			{
				Intent i = new Intent(getApplicationContext(), HelpActivity.class);
				startActivity(i);
			}
			break;
		}
    	return true;
    }
    
    // �ݒ��ʂ�\��
    void showPref() {
    	Intent i = new Intent(getApplicationContext(), AreaSelectActivity.class);
    	startActivityForResult(i, 0);
    }
    
    // �n��ύX�m�莞�̓���
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
    
    // �����{�^��
    @Override
    public boolean onSearchRequested() {
    	showPref();
    	return true;
    }
}