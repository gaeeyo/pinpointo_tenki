package nikeno.Tenki;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Locale;

public class DayView extends LinearLayout {
	@SuppressWarnings("unused")
	private static final String TAG = "DayView";

	private YahooWeather.Day mData;
	private TableRow [] mRows = new TableRow[6] ;
	private static int mDisabledColor;
	private static int mDisabledBackgroundColor;

	public DayView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOrientation(VERTICAL);
		TableLayout table = (TableLayout)LayoutInflater.from(context).inflate(R.layout.day, null);
		//TableLayout table = inflateByCode(context, attrs);
		addView(table);

		for (int j=0; j<mRows.length; j++) {
			mRows[j] = (TableRow)table.getChildAt(j);
		}

		mDisabledColor = context.getResources().getColor(R.color.pastHourColor);
		mDisabledBackgroundColor = context.getResources().getColor(R.color.pastHourBackgroundColor);
	}

	private TableLayout inflateByCode(Context context, AttributeSet attrs) {

		Resources res = context.getResources();
		TableLayout table = new TableLayout(context);

		int tableCellColor = res.getColor(R.color.tableBackground);
		int tableTextColor = res.getColor(R.color.tableTextColor);

		table.setLayoutParams(new LayoutParams(
				TableLayout.LayoutParams.FILL_PARENT,
				TableLayout.LayoutParams.WRAP_CONTENT
				));
		table.setPadding(1, 1, 0, 0);
		table.setBackgroundColor(res.getColor(R.color.tableBorderColor));

		// TableRow を追加
		int j;
		TableRow row;
		for (j=0; j<6; j++) {
			row = new TableRow(context);
			mRows[j] = row;
			table.addView(row);
		}

		float tableTextSize = res.getDimension(R.dimen.textSizeMedium);
		float windTextSize = res.getDimension(R.dimen.textSizeXSmall);

		TextView tv;

		TableRow.LayoutParams lp_cell = new TableRow.LayoutParams(
				TableRow.LayoutParams.FILL_PARENT,
				TableRow.LayoutParams.FILL_PARENT,
				1);
		lp_cell.bottomMargin = 1;
		lp_cell.rightMargin = 1;

		// 単純な TextView を追加
		float textSize;
		int textColor;
		int background;
		for (int y=0; y<6; y++) {
			if (y == 1) continue;

			row = mRows[y];
			textSize = tableTextSize;
			textColor = tableTextColor;
			background = tableCellColor;
			switch (y) {
			case 0:
				background = res.getColor(R.color.tableDateBackground);
				break;
			case 3:
				textColor = res.getColor(R.color.tableTempColor);
				break;
			case 4:
				textColor = res.getColor(R.color.tableHumidColor);
				break;
			case 5:
				textSize = windTextSize;
				break;
			}

			for (j=0; j<8; j++) {
				tv = new TextView(context);
				tv.setTextSize(0, textSize);
				tv.setGravity(Gravity.CENTER);
				tv.setBackgroundColor(background);
				tv.setTextColor(textColor);
				row.addView(tv, lp_cell);
			}
		}

		// アイコンとテキストの行

//		LinearLayout.LayoutParams lp_image = new LinearLayout.LayoutParams(
//				LinearLayout.LayoutParams.WRAP_CONTENT,
//				LinearLayout.LayoutParams.WRAP_CONTENT
//				);


		row = mRows[1];
		LinearLayout ll;
		ImageView iv;
		for (j=0; j<8; j++) {
			ll = new LinearLayout(context);
			ll.setOrientation(LinearLayout.VERTICAL);
			ll.setBackgroundColor(tableCellColor);

			iv = new ImageView(context);
			//iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
			ll.addView(iv);

			tv = new TextView(context);
			tv.setTextSize(0, tableTextSize);
			tv.setGravity(Gravity.CENTER);
			tv.setBackgroundColor(tableCellColor);
			ll.addView(tv);

			row.addView(ll, lp_cell);
		}

		return table;
	}

	public void setData(YahooWeather.Day data) {
		this.mData = data;
		final int HOUR = 60 * 60 * 1000;
		Calendar nowJapan = Calendar.getInstance(Locale.JAPAN);

		long now = nowJapan.getTime().getTime() - 3 * HOUR;
		long baseTime = data.date.getTime();

		for (int x=0; x<8; x++) {
			YahooWeather.Hour h = mData.hours[x];
			int column = x;

			boolean enabled = (baseTime + h.hour * HOUR) > now;
			if (!enabled) {
				mRows[0].getChildAt(column).setBackgroundColor(mDisabledBackgroundColor);
			}

			LinearLayout ll = (LinearLayout)mRows[1].getChildAt(column);
			setCellText((TextView)ll.getChildAt(1), h.text, enabled);
			try {
				Bitmap bmp = null;
				String imageUrl = h.getImageUrl(enabled);
				ImageDownloader.getInstance().setImage(imageUrl, ((ImageView)ll.getChildAt(0)));
			}
			catch (Exception e) {
				e.printStackTrace();
				;
			}

			setCellText(0, column, Integer.toString(h.hour), enabled);
			setCellText(2, column, h.temp, enabled);
			setCellText(3, column, h.humidity, enabled);
			setCellText(4, column, h.rain, enabled);
			setCellText(5, column,
				h.wind.length() < 3 ?
					h.wind :
					h.wind.substring(0,2) + "\n" + h.wind.substring(2),
				enabled
			);
		}
	}

	private void setCellText(int row, int column, String text, boolean enabled) {
		TextView tv = (TextView)mRows[row].getChildAt(column);
		setCellText(tv, text, enabled);
	}

	private static void setCellText(TextView tv, String text, boolean enabled) {
		tv.setText(text);
		if (!enabled) {
			tv.setTextColor(mDisabledColor);
		}
	}



}
