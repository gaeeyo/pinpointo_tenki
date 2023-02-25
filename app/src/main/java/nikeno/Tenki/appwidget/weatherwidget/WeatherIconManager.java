package nikeno.Tenki.appwidget.weatherwidget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import nikeno.Tenki.TenkiApp;

import java.util.HashMap;

public class WeatherIconManager {

    private final Context                 mContext;
    private       HashMap<String, Bitmap> mCache = new HashMap<>();
    int mIconSize;
    int mShadowOffset;

    public WeatherIconManager(Context context) {
        mContext = context;
        float density = context.getResources().getDisplayMetrics().density;
        mShadowOffset = Math.round(1 * density);
        mIconSize = (int) (38 * density) + mShadowOffset * 2;
    }

    public Bitmap getIcon(String url) {
        try {
            Bitmap bmp = mCache.get(url);
            if (bmp != null) return bmp;

            bmp = TenkiApp.from(mContext).getDownloader().downloadImage(url, 8000, 0);
            if (bmp != null) {
                Bitmap newBitmap = convertBitmap(bmp);
                if (newBitmap != null) {
                    bmp = newBitmap;
                }
                mCache.put(url, bmp);
            }
            return bmp;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Bitmap convertBitmap(Bitmap bmp) {
        Bitmap newBmp = Bitmap.createBitmap(mIconSize, mIconSize, Bitmap.Config.ARGB_8888);
        Bitmap alpha  = bmp.extractAlpha();
        try {
            Canvas c = new Canvas(newBmp);
            Paint  p = new Paint();
            p.setFilterBitmap(true);
            p.setColor(0x88000000);
            Matrix m      = new Matrix();
            float  scaleX = (float) (mIconSize - mShadowOffset * 2) / bmp.getWidth();
            float  scaleY = (float) (mIconSize - mShadowOffset * 2) / bmp.getHeight();
            float  scale  = Math.max(scaleX, scaleY);
            m.preScale(scale, scale);
            m.postTranslate(mShadowOffset * 2, mShadowOffset * 2);
            c.drawBitmap(alpha, m, p);
            m.postTranslate(-mShadowOffset, -mShadowOffset);
            c.drawBitmap(bmp, m, null);
        } finally {
            alpha.recycle();
        }

        return newBmp;
    }

}
