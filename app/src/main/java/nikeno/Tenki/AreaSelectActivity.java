package nikeno.Tenki;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AreaSelectActivity extends Activity implements AdapterView.OnItemClickListener, OnItemLongClickListener {
    private static final String TAG             = "AreaSelectActivity";
    private static final String SERVER_ENCODING = "UTF-8";

    private Button   mSearchBtn;
    private EditText mSearchText;
    private View     mProgress;
    private ListView mList;
    private TextView mMessage;

    private Prefs     mPrefs;
    private boolean   mListIsRecent;
    private String    mErrorMessage;
    private AsyncTask mSearchTask;
    private boolean   mIsLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_area_select);

        mPrefs = new Prefs(getSharedPreferences(MainActivity.APP_PREF, MODE_PRIVATE));

        mList = (ListView) findViewById(android.R.id.list);
        mMessage = (TextView) findViewById(android.R.id.message);
        mSearchBtn = (Button) findViewById(R.id.searchButton);
        mSearchText = (EditText) findViewById(R.id.searchText);
        mProgress = findViewById(android.R.id.progress);

        mList.setOnItemLongClickListener(this);
        mList.setOnItemClickListener(this);

        // 検索EditText
        mSearchText.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                onClickSearch(v);
                return true;
            }
        });

        mSearchText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                updateViews();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        setAreaList(mPrefs.getRecentAreaList(), true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateViews();
    }

    void updateViews() {
        boolean textIsEmpty = TextUtils.isEmpty(mSearchText.getText().toString().trim());
        mSearchBtn.setEnabled(!textIsEmpty);
        mProgress.setVisibility(mIsLoading ? View.VISIBLE : View.GONE);
        mList.setLongClickable(mListIsRecent);

        if (mIsLoading) {
            mList.setVisibility(View.GONE);
            mMessage.setVisibility(View.GONE);
        } else {
            String message = null;
            if (mErrorMessage != null) {
                message = mErrorMessage;
            } else if (mList.getAdapter() == null || mList.getAdapter().getCount() == 0) {
                message = getString(mListIsRecent
                        ? R.string.area_select_search_empty
                        : R.string.area_select_result_empty);
            }

            mMessage.setText(message);
            mMessage.setVisibility(message != null ? View.VISIBLE : View.GONE);
            mList.setVisibility(message != null ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.area_select_menu, menu);
        return true;
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (featureId) {
            case R.id.clearRecent:
                ArrayList<Area> empty = new ArrayList<>();
                mPrefs.putRecentAreaList(empty);
                setAreaList(empty, true);
                return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    void closeSoftwareKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
        }
    }

    @SuppressWarnings("unchecked")
    public void onClickSearch(View v) {
        String text = mSearchText.getText().toString();
        if (text.length() == 0) return;

        if (mSearchTask != null) {
            mSearchTask.cancel(true);
        }

        closeSoftwareKeyboard();

        mIsLoading = true;
        mErrorMessage = null;
        updateViews();

        mSearchTask = new AsyncTask<Object, Void, Object>() {

            protected Object doInBackground(Object... params) {
                Log.d(TAG, "doInBackground");
                String text = (String) params[0];
                try {
                    String url = "https://weather.yahoo.co.jp/weather/search/"
                            + "?p=" + URLEncoder.encode(text, SERVER_ENCODING)
                            + "&t=z";

                    byte[] buff = Downloader.getInstance(AreaSelectActivity.this)
                            .download(url, 50 * 1024, -1, false);
                    if (buff == null) {
                        throw new Exception("Download error.");
                    }

                    String html = new String(buff, SERVER_ENCODING);

                    List<Area> areaList = parseAreaListHtml(html);
                    if (areaList.size() == 0) {
                        return getString(R.string.area_select_result_empty, text);
                    }
                    return areaList;

                } catch (Exception e) {
                    return e.getMessage();
                }
            }

            @Override
            protected void onPostExecute(Object result) {
                Log.d(TAG, "onPostExecute");
                super.onPostExecute(result);
                if (result instanceof List) {
                    setAreaList((List<Area>) result, false);
                } else if (result instanceof String) {
                    mErrorMessage = (String) result;
                }
                onFinish();
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                onFinish();
            }

            void onFinish() {
                if (mSearchTask == this) {
                    mSearchTask = null;
                    mIsLoading = false;
                    updateViews();
                }
            }
        };
        mSearchTask.execute(text);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Area selected = (Area) adapterView.getItemAtPosition(position);

        mPrefs.addRecentArea(selected);

        Intent i = new Intent();
        i.putExtra("url", selected.url);
        setResult(RESULT_OK, i);
        finish();
    }


    private void setAreaList(List<Area> newList, boolean isRecent) {
        mListIsRecent = isRecent;
        mList.setAdapter(new AreaAdapter(this, newList));
        updateViews();
    }

    // クリックされた履歴を消す
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                                   long id) {
        if (mListIsRecent) {
            final Area selected = (Area) mList.getItemAtPosition(position);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dlg_recent_remove_title)
                    .setMessage(getString(R.string.dlg_recent_remove_message, selected.address2))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            setAreaList(mPrefs.removeRecentArea(selected), true);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
            return true;
        }
        return false;
    }

    public static ArrayList<Area> parseAreaListHtml(String html) throws Exception {
        ArrayList<Area> result = new ArrayList<>();

        html = html.replace("\n", "");

        // ざっくりとした祝
        Pattern p = Pattern.compile("<thead.*?郵便番号.*?</thead(.*?)</tbody",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(html);
        if (m.find()) {

            html = m.group(1);
            // <tr><td>zip</td><td>県名</td><td><a href="">住所</a></td>
            m = Pattern.compile(
                    "<tr.*?<td.*?>(.*?)</td>.*?<td.*?>(.*?)</td>.*?<td.*?>.*?(https://[a-zA-Z0-9./_]*?)\".*?>(.*?)</a>.*?</tr"
            ).matcher(html);
            while (m.find()) {
                Area d = new Area();
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

    static class AreaAdapter extends ArrayAdapter<Area> {

        public AreaAdapter(Context context, List<Area> items) {
            super(context, R.layout.area_select_row, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView tv   = (TextView) super.getView(position, convertView, parent);
            Area     area = getItem(position);
            if (area != null) {
                tv.setText(String.format("%s %s\n%s", area.zipCode, area.address1, area.address2));
            }
            return tv;
        }
    }
}
