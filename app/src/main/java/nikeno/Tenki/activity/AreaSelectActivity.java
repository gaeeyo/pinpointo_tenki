package nikeno.Tenki.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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

import java.util.ArrayList;
import java.util.List;

import nikeno.Tenki.Area;
import nikeno.Tenki.Prefs;
import nikeno.Tenki.R;
import nikeno.Tenki.TenkiApp;
import nikeno.Tenki.task.Callback;
import nikeno.Tenki.task.SearchAddressTask;

public class AreaSelectActivity extends Activity implements AdapterView.OnItemClickListener, OnItemLongClickListener {
    private static final String TAG = "AreaSelectActivity";

    private Button   mSearchBtn;
    private EditText mSearchText;
    private View     mProgress;
    private ListView mList;
    private TextView mMessage;

    private Prefs             mPrefs;
    private boolean           mListIsRecent;
    private String            mErrorMessage;
    private SearchAddressTask mSearchTask;
    private boolean           mIsLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TenkiApp.applyActivityTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_area_select);

        mPrefs = ((TenkiApp) getApplication()).getPrefs();

        mList = findViewById(android.R.id.list);
        mMessage = findViewById(android.R.id.message);
        mSearchBtn = findViewById(R.id.searchButton);
        mSearchText = findViewById(R.id.searchText);
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
    protected void onDestroy() {
        super.onDestroy();
        if (mSearchTask != null) {
            mSearchTask.cancel(true);
        }
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
        final String text = mSearchText.getText().toString();
        if (text.length() == 0) return;

        if (mSearchTask != null) {
            mSearchTask.cancel(true);
        }

        closeSoftwareKeyboard();

        mIsLoading = true;
        mErrorMessage = null;
        updateViews();

        mSearchTask = new SearchAddressTask(TenkiApp.from(this).getDownloader(), text,
                new Callback<List<Area>>() {
                    @Override
                    public void onSuccess(List<Area> result) {
                        if (result.size() > 0) {
                            setAreaList(result, false);
                        } else {
                            mErrorMessage = getString(R.string.area_select_result_empty, text);
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        super.onError(error);
                        mErrorMessage = error.getMessage();
                    }

                    @Override
                    public void onFinish() {
                        super.onFinish();
                        mSearchTask = null;
                        mIsLoading = false;
                        updateViews();
                    }
                });
        mSearchTask.execute();
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
