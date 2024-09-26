package nikeno.Tenki.activity

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import nikeno.Tenki.Area
import nikeno.Tenki.Prefs
import nikeno.Tenki.R
import nikeno.Tenki.TenkiApp
import nikeno.Tenki.task.Callback
import nikeno.Tenki.task.SearchAddressTask

class AreaSelectActivity : Activity(), OnItemClickListener, OnItemLongClickListener {
    private lateinit var mSearchBtn: Button
    private lateinit var mSearchText: EditText
    private lateinit var mProgress: View
    private lateinit var mList: ListView
    private lateinit var mMessage: TextView

    private lateinit var mPrefs: Prefs
    private var mListIsRecent = false
    private var mErrorMessage: String? = null
    private var mSearchTask: SearchAddressTask? = null
    private var mIsLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        TenkiApp.applyActivityTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_area_select)

        mPrefs = (application as TenkiApp).prefs

        mList = findViewById(android.R.id.list)
        mMessage = findViewById(android.R.id.message)
        mSearchBtn = findViewById(R.id.searchButton)
        mSearchText = findViewById(R.id.searchText)
        mProgress = findViewById(android.R.id.progress)

        mList.setOnItemLongClickListener(this)
        mList.setOnItemClickListener(this)

        // 検索EditText
        mSearchText.setOnEditorActionListener({ v, actionId, event ->
            onClickSearch(v)
            true
        })

        mSearchText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            }

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                updateViews()
            }

            override fun afterTextChanged(editable: Editable) {
            }
        })

        setAreaList(mPrefs.recentAreaList, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mSearchTask != null) {
            mSearchTask!!.cancel(true)
        }
    }

    override fun onResume() {
        super.onResume()
        updateViews()
    }

    fun updateViews() {
        val textIsEmpty = TextUtils.isEmpty(mSearchText.text.toString().trim { it <= ' ' })
        mSearchBtn.isEnabled = !textIsEmpty
        mProgress.visibility = if (mIsLoading) View.VISIBLE else View.GONE
        mList.isLongClickable = mListIsRecent

        if (mIsLoading) {
            mList.visibility = View.GONE
            mMessage.visibility = View.GONE
        } else {
            var message: String? = null
            if (mErrorMessage != null) {
                message = mErrorMessage
            } else if (mList.adapter == null || mList.adapter.count == 0) {
                message = getString(
                    if (mListIsRecent
                    ) R.string.area_select_search_empty
                    else R.string.area_select_result_empty
                )
            }

            mMessage.text = message
            mMessage.visibility = if (message != null) View.VISIBLE else View.GONE
            mList.visibility = if (message != null) View.GONE else View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.area_select_menu, menu)
        return true
    }

    override fun onMenuItemSelected(featureId: Int, item: MenuItem): Boolean {
        if (featureId == R.id.clearRecent) {
            val empty = ArrayList<Area>()
            mPrefs.putRecentAreaList(empty)
            setAreaList(empty, true)
            return true
        }
        return super.onMenuItemSelected(featureId, item)
    }

    fun closeSoftwareKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(mSearchText.windowToken, 0)
    }

    fun onClickSearch(v: View?) {
        val text = mSearchText.text.toString()
        if (text.length == 0) return

        if (mSearchTask != null) {
            mSearchTask!!.cancel(true)
        }

        closeSoftwareKeyboard()

        mIsLoading = true
        mErrorMessage = null
        updateViews()

        mSearchTask = SearchAddressTask(TenkiApp.from(this).downloader, text,
            object : Callback<List<Area>>() {
                override fun onSuccess(result: List<Area>) {
                    if (result.size > 0) {
                        setAreaList(result, false)
                    } else {
                        mErrorMessage = getString(R.string.area_select_result_empty, text)
                    }
                }

                override fun onError(error: Throwable) {
                    super.onError(error)
                    mErrorMessage = error.message
                }

                override fun onFinish() {
                    super.onFinish()
                    mSearchTask = null
                    mIsLoading = false
                    updateViews()
                }
            })
        mSearchTask!!.execute()
    }

    override fun onItemClick(adapterView: AdapterView<*>, view: View, position: Int, id: Long) {
        val selected = adapterView.getItemAtPosition(position) as Area

        mPrefs.addRecentArea(selected)

        val i = Intent()
        i.putExtra("url", selected.url)
        setResult(RESULT_OK, i)
        finish()
    }

    private fun setAreaList(newList: List<Area>, isRecent: Boolean) {
        mListIsRecent = isRecent
        mList.adapter = AreaAdapter(this, newList)
        updateViews()
    }

    // クリックされた履歴を消す
    override fun onItemLongClick(
        parent: AdapterView<*>?, view: View, position: Int,
        id: Long
    ): Boolean {
        if (mListIsRecent) {
            val selected = mList.getItemAtPosition(position) as Area
            AlertDialog.Builder(this)
                .setTitle(R.string.dlg_recent_remove_title)
                .setMessage(getString(R.string.dlg_recent_remove_message, selected.address2))
                .setPositiveButton(android.R.string.yes) { dialog, which ->
                    setAreaList(mPrefs.removeRecentArea(selected), true)
                    dialog.dismiss()
                }
                .setNegativeButton(android.R.string.no, null)
                .show()
            return true
        }
        return false
    }

    internal class AreaAdapter(context: Context?, items: List<Area>?) : ArrayAdapter<Area?>(
        context!!, R.layout.area_select_row, items!!
    ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val tv = super.getView(position, convertView, parent) as TextView
            val area = getItem(position)
            if (area != null) {
                tv.text = String.format("%s %s\n%s", area.zipCode, area.address1, area.address2)
            }
            return tv
        }
    }

    companion object {
        private const val TAG = "AreaSelectActivity"
    }
}
