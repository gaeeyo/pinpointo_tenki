package nikeno.Tenki;

import nikeno.Tenki.R;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class WeekView extends LinearLayout {
	@SuppressWarnings("unused")
	private static final String TAG = "WeekView";
	
	private YahooWeather.WeeklyDay [] mData;
	private TableRow [] mRows = new TableRow[6] ;
	private int maxTempColor;
	private int minTempColor;
	
	public WeekView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOrientation(VERTICAL);
		
		TableLayout table = (TableLayout)LayoutInflater.from(context).inflate(R.layout.week, null);
		addView(table);
		
		for (int j=0; j<mRows.length; j++) {
			mRows[j] = (TableRow)table.getChildAt(j);
		}
		Resources res = context.getResources();
		maxTempColor = res.getColor(R.color.maxTempColor);
		minTempColor = res.getColor(R.color.minTempColor);
	}

	public void setData(YahooWeather.WeeklyDay[] data) {
		mData = data;
		ForegroundColorSpan maxTempSpan = new ForegroundColorSpan(maxTempColor);
		ForegroundColorSpan minTempSpan = new ForegroundColorSpan(minTempColor);
		for (int x = 0; x < mData.length; x++) {
			YahooWeather.WeeklyDay i = mData[x];
			
			((TextView)mRows[0].getChildAt(x)).setText(i.date);

			LinearLayout ll = (LinearLayout) mRows[1].getChildAt(x);
			try {
				Bitmap bmp = Downloader.downloadImage(i.imageUrl, 8000, 0);
				if (bmp != null) {
					((ImageView) ll.getChildAt(0)).setImageBitmap(bmp);
				}
			} catch (Exception e) {
				;
			}

			((TextView) ll.getChildAt(1)).setText(i.text);
			
			SpannableString ss = new SpannableString(i.tempMax + "/" + i.tempMin);
			ss.setSpan(maxTempSpan, 0, i.tempMax.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			ss.setSpan(minTempSpan, ss.length() - i.tempMin.length(), ss.length(), 
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			
			((TextView) mRows[2].getChildAt(x)).setText(ss);
			((TextView) mRows[3].getChildAt(x)).setText(i.rain);
		}
	}
}
