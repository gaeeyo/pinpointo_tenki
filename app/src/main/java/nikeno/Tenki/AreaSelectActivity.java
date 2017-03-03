package nikeno.Tenki;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.net.URLEncoder;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AreaSelectActivity extends ListActivity implements OnItemLongClickListener {
	private static final String TAG = "AreaSelectActivity";
	private static final String SERVER_ENCODING = "UTF-8";
	private static final int RECENT_MAX = 5;
	private Button mSearchBtn;
	private EditText mSearchText;
	private SearchTask mSearchTask;
	private AreaDataList mListData;
	private ArrayAdapter<String> mAdapter;
	private SharedPreferences mPref;
	private AreaDataList mRecent;
	private boolean isRecentShowing;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.area_select);

		mPref = getSharedPreferences(MainActivity.APP_PREF, MODE_PRIVATE);

		mSearchBtn = (Button)findViewById(R.id.searchButton);
		mSearchText = (EditText)findViewById(R.id.searchText);

		// リストの設定
		mAdapter = new ArrayAdapter<String>(this, R.layout.area_select_row);
		setListAdapter(mAdapter);
		getListView().setOnItemLongClickListener(this);

		// 検索Button
		mSearchBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				doSearch();
			}
		});

		// 検索EditText
		mSearchText.setSingleLine(true);
		mSearchText.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (event == null || event.getAction() == KeyEvent.ACTION_UP) {
					doSearch();
				}
				return true;
			}
		});
		mSearchText.requestFocus();

		loadRecent();
	}

	// メニュー作成
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.area_select_menu, menu);
    	return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		mRecent.clear();
		saveRecent();
		mAdapter.clear();
		return super.onMenuItemSelected(featureId, item);
	}

	// 最近使った地域を読み込む
	private void loadRecent() {
		mRecent = new AreaDataList();

		String data;
		for (int j=0; j<RECENT_MAX; j++) {
			data = mPref.getString("Recent" + j, null);
			if (data != null) {
				AreaData areaData = AreaData.parse(data);
				if (areaData != null) {
					mRecent.add(areaData);
				}
			}
		}
		setNewList((AreaDataList)mRecent.clone(), true);
	}

	// 最近使った地域を保存
	private void saveRecent() {
		Editor editor = mPref.edit();

		String key;
		for (int j=0; j<RECENT_MAX; j++) {
			key = "Recent" + j;
			if (j < mRecent.size()) {
				editor.putString("Recent" + j, mRecent.get(j).serialize());
			}
			else {
				editor.remove(key);
			}
		}
		editor.commit();
	}

	// 検索を実行
	private void doSearch() {
		Log.d(TAG, "doSearch");
		String text = mSearchText.getText().toString();
		if (text.length() == 0) return;

		if (mSearchTask != null && mSearchTask.getStatus() != SearchTask.Status.FINISHED) {
			mSearchTask.cancel(true);
			mSearchTask = null;
			return;
		}
		InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
		if (imm != null) {
			imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
		}

		mSearchTask = new SearchTask(this, text);
		mSearchTask.execute(new String[] {});
	}

	// リストがクリックされたら結果を返す
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);

		AreaData selected = mListData.get(position);

		mRecent.insertElementAt(selected, 0);
		for (int j=mRecent.size()-1; j>0; j--) {
			if (mRecent.get(j).url.compareTo(selected.url)==0) {
				mRecent.remove(j);
			}
		}
		saveRecent();

		Intent i = new Intent();
		i.putExtra("url", mListData.get(position).url);
		setResult(RESULT_OK, i);
		finish();
	}

	// クリックされた履歴を消す
	public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
			long id) {
		if (isRecentShowing) {
			final AreaData selected = mRecent.get(position);
			new AlertDialog.Builder(this)
				.setTitle(R.string.dlg_recent_remove_title)
				.setMessage(String.format(getString(R.string.dlg_recent_remove_message), selected.address2))
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						mRecent.remove(selected);
						saveRecent();
						setNewList(mRecent, true);
						dialog.cancel();
					}
				})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				})
				.show();

		}
		return false;
	}

	// 地域データ
	public static class AreaData {
		public String zipCode;
		public String address1;
		public String address2;
		public String url;

		public static AreaData parse(String text) {
			AreaData data = null;
			String [] values = text.split("\n");
			if (values.length >= 4) {
				data = new AreaData();
				data.zipCode = values[0];
				data.address1 = values[1];
				data.address2 = values[2];
				data.url = values[3];
			}
			return data;
		}
		public String serialize() {
			return zipCode + "\n" +
				address1 + "\n" +
				address2 + "\n" +
				url;
		}
	}

	// 地域データ配列
	private static class AreaDataList extends Vector<AreaData> {
		private static final long serialVersionUID = 6002692427075044056L;

		public static AreaDataList fromHTML(String html) throws Exception {
			AreaDataList result = new AreaDataList();

			html = html.replace("\n", "");

			// ざっくりとした祝
			Pattern p = Pattern.compile("<thead.*?郵便番号.*?</thead(.*?)</tbody",
					Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(html);
			if (m.find()) {

				html = m.group(1);
				// <tr><td>zip</td><td>県名</td><td><a href="">住所</a></td>
				m = Pattern.compile(
						"<tr.*?<td.*?>(.*?)</td>.*?<td.*?>(.*?)</td>.*?<td.*?>.*?(http://[a-zA-Z0-9./_]*?)\".*?>(.*?)</a>.*?</tr"
						).matcher(html);
				while (m.find()) {
					AreaData d = new AreaData();
					d.address1 = m.group(2);
					d.address2 = m.group(4);
					d.zipCode = m.group(1);
					d.url = m.group(3);
					result.add(d);
					//Log.d(TAG, m.group(1) +"," +m.group(2) + "," + m.group(4) + "," + m.group(3) );
				}
			}
			return result;
		}
	}

	// AreaDataList をリストにセットする
	private void setNewList(AreaDataList newList, boolean isRecent) {
		isRecentShowing = isRecent;
		mListData = newList;
		mAdapter.clear();
		for (int j=0; j<mListData.size(); j++) {
			AreaData d = mListData.get(j);
			mAdapter.add(String.format("%s %s\n%s", d.zipCode, d.address1, d.address2));
		}
	}

	// 地域データの検索タスク
	private class SearchTask extends AsyncTask<String, Integer, AreaDataList> {
		private final String TAG = "SearchTask";
		private final String mSearchText;
		private final Context mContext;
		private String mErrorMessage;

		public SearchTask(Context context, String text) {
			mContext = context;
			mSearchText = text;
		}

		@Override
		protected AreaDataList doInBackground(String... params) {
			Log.d(TAG, "doInBackground");
			publishProgress(0);
			AreaDataList result = null;
    		try {
    			String url = "http://weather.yahoo.co.jp/weather/search/"
    				+ "?p=" + URLEncoder.encode(mSearchText, SERVER_ENCODING)
    				+ "&t=z";

    			byte [] buff = Downloader.getInstance(mContext)
                        .download(url, 50*1024, -1, false);
    			if (buff == null) {
    				throw new Exception("Download error.");
    			}

    			String html = new String(buff, SERVER_ENCODING);
    			publishProgress(50*100);

    			result = AreaDataList.fromHTML(html);
    			publishProgress(75*100);
    		}
    		catch (Exception e) {
    			mErrorMessage = e.getMessage();
    		}
    		publishProgress(100*100);
			return result;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			setProgress(values[0]);
		}

		@Override
		protected void onPostExecute(AreaDataList result) {
			super.onPostExecute(result);
			if (result == null || result.isEmpty()) {
				//mAdapter.add(/object)
				mAdapter.clear();
				if (mErrorMessage != null) {
	    			Toast.makeText(mContext,
	    					"ERROR:" + mErrorMessage, Toast.LENGTH_LONG).show();
				}
				else {
					Toast.makeText(mContext,
							mContext.getText(R.string.area_select_result_empty),
							Toast.LENGTH_LONG).show();
				}
			}
			else {
				setNewList(result, false);
			}
			Log.d(TAG, "onPostExecute");
		}
	}


}
