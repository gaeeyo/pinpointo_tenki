package nikeno.Tenki;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

public class DayView2 extends View {
	@SuppressWarnings("unused")
	private static final String TAG = "DayView2";

	private YahooWeather.Day mData;
	
	public DayView2(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		// TODO Auto-generated method stub
		//super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(320, 200);
	}
	
	public void setData(YahooWeather.Day data) {
		mData = data;
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		// TODO Auto-generated method stub
		//super.onDraw(canvas);
	}

}
