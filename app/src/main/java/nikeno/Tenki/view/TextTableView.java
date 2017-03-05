package nikeno.Tenki.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import nikeno.Tenki.Downloader;
import nikeno.Tenki.R;

public class TextTableView extends View {

    int        mRows;
    int        mColumns;
    CellImpl[] mCells;
    int[]      mRowHeights;

    int   mBorderColor = 0;
    int   mBorderWidth = 0;
    Paint mPaint       = new Paint();
    int   mTextColor   = 0xff000000;
    int   mCellPadding = 0;
    Rect  mTmpRect     = new Rect();
    RectF mTmpRectF    = new RectF();

    public TextTableView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.TextTableView);
            mBorderColor = ta.getColor(R.styleable.TextTableView_borderColor, mBorderColor);
            mBorderWidth = ta.getDimensionPixelOffset(R.styleable.TextTableView_borderWidth, mBorderWidth);
            mTextColor = ta.getColor(R.styleable.TextTableView_android_textColor, mTextColor);
            mCellPadding = ta.getDimensionPixelOffset(R.styleable.TextTableView_cellPadding, mCellPadding);
            ta.recycle();
        }
        mPaint = new Paint();
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        if (mRows <= 0 || mColumns <= 0) return;


        int width     = getMeasuredWidth();
        int cellWidth = width / mColumns;
        int height    = 0;
        int columns   = 0;
        int rowHeight = 0;
        int row       = 0;
        for (CellImpl cell : mCells) {
            if (cell.text != null) {
                if (cell.layout == null || cell.layout.getWidth() != cellWidth) {
                    cell.layout = new StaticLayout(cell.text, cell.paint, cellWidth,
                            Layout.Alignment.ALIGN_CENTER, 1f, 0f, true);
                }

                cell.height = 0;
                if (cell.icon != null) {
                    int iconWidth = cell.icon.getWidth();
                    if (cell.iconWidth != -1) {
                        if (cell.iconWidth > iconWidth) {
                            iconWidth = cell.iconWidth;
                        }
                    }
                    cell.scale = (float) iconWidth / cell.icon.getWidth();
                    cell.height += (int) (cell.icon.getHeight() * cell.scale);
                }

                if (cell.layout != null) {
                    cell.height += cell.layout.getHeight();
                }

                if (cell.height > rowHeight) {
                    rowHeight = cell.height;
                }
            }
            if (++columns >= mColumns) {
                rowHeight += mCellPadding * 2;
                mRowHeights[row++] = rowHeight;
                columns = 0;
                height += rowHeight;
                rowHeight = 0;
            }
        }
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width  = getWidth();
        int height = getHeight();

        if (mRows <= 0 || mColumns <= 0 || width <= 0 || height <= 0) return;


        int   border = mBorderWidth;
        Paint paint  = mPaint;

        paint.setColor(mBorderColor);

        int   col       = 0;
        int   row       = 0;
        int   y         = 0;
        int   rowHeight = 0;
        int   cellWidth = width / mColumns;
        Rect  rect      = mTmpRect;
        RectF rectF     = mTmpRectF;

        for (CellImpl cell : mCells) {
            if (col == 0) {
                rowHeight = mRowHeights[row];
            }
            int x = col * width / mColumns;
            if (cell.backgroundColor != 0) {
                paint.setColor(cell.backgroundColor);
                canvas.drawRect(x, y, x + (col + 1) * width, y + rowHeight, paint);
            }

            int    offsetY = y + (rowHeight / 2) - (cell.height / 2);
            Bitmap icon    = cell.icon;
            if (icon != null && icon.getWidth() > 0) {
                int iconLeft = (int) (x + cellWidth / 2 - (icon.getWidth() * cell.scale) / 2);
                rect.set(0, 0, cell.icon.getWidth(), cell.icon.getHeight());
                rectF.set(iconLeft, offsetY,
                        iconLeft + icon.getWidth() * cell.scale,
                        offsetY + icon.getHeight() * cell.scale);
                offsetY += rectF.height();
//                canvas.translate(x, y);
                canvas.drawBitmap(cell.icon, rect, rectF, null);
//                canvas.translate(-x, -y);
//                textY = y + rowHeight - cell.layout.getHeight();
            }

            if (cell.layout != null) {
                canvas.translate(x, offsetY);
                cell.layout.draw(canvas);
                canvas.translate(-x, -offsetY);
            }

            if (++col >= mColumns) {
                col = 0;
                row++;
                y += rowHeight;
                if (border > 0) {
                    // 各行の下の線
                    paint.setColor(mBorderColor);
                    canvas.drawRect(0, y - border, width, y, paint);
                }
            }
        }

        if (border > 0) {
            paint.setColor(mBorderColor);
            // 縦線
            for (int j = 0; j < mColumns; j++) {
                int x = (j * width / mColumns);
                canvas.drawRect(x, 0, x + border, height, paint);
            }
            // 右端
            canvas.drawRect(width - border, 0, width, height, paint);
            // 上端
            canvas.drawRect(0, 0, width, border, paint);
        }
    }

    public void setSize(int columns, int rows) {
        if (mRows != rows || mColumns != columns) {
            mRows = rows;
            mColumns = columns;
            mCells = new CellImpl[mRows * mColumns];
            for (int j = 0; j < mCells.length; j++) {
                mCells[j] = new CellImpl(mTextColor);

            }
            mRowHeights = new int[mRows];
        }
    }

    public Cell getCell(int column, int row) {
        return mCells[column + row * mColumns];
    }

    public Cell getColumn(int column) {
        return new CellGroup(mCells, column, column + mColumns * mRows - 1, mColumns);
    }

    public Cell getRow(int row) {
        return new CellGroup(mCells, row * mColumns, (row + 1) * mColumns - 1, 1);
    }

    public Cell getAll() {
        return new CellGroup(mCells, 0, mCells.length - 1, 1);
    }

    public static class CellGroup implements Cell {
        final Cell[] mCells;
        final int    mStart;
        final int    mEnd;
        final int    mStep;

        public CellGroup(Cell[] cells, int start, int end, int step) {
            mCells = cells;
            mStart = start;
            mEnd = end;
            mStep = step;
        }

        @Override
        public Cell setText(CharSequence text) {
            for (int j = mStart; j <= mEnd; j += mStep) {
                mCells[j].setText(text);
            }
            return this;
        }

        @Override
        public Cell setTextColor(int color) {
            for (int j = mStart; j <= mEnd; j += mStep) {
                mCells[j].setTextColor(color);
            }
            return this;
        }

        @Override
        public Cell setBackgroundColor(int color) {
            for (int j = mStart; j <= mEnd; j += mStep) {
                mCells[j].setBackgroundColor(color);
            }
            return this;
        }

        @Override
        public Cell setTextSize(float size) {
            for (int j = mStart; j <= mEnd; j += mStep) {
                mCells[j].setTextSize(size);
            }
            return this;
        }

        @Override
        public Cell setIcon(Bitmap bmp) {
            return null;
        }

        @Override
        public Cell setIconWidth(int width) {
            for (int j = mStart; j <= mEnd; j += mStep) {
                mCells[j].setIconWidth(width);
            }
            return this;
        }

        @Override
        public Cell setIconHeight(int height) {
            for (int j = mStart; j <= mEnd; j += mStep) {
                mCells[j].setIconHeight(height);
            }
            return this;
        }
    }


    public interface Cell {
        Cell setText(CharSequence text);

        Cell setTextColor(int color);

        Cell setBackgroundColor(int color);

        Cell setTextSize(float size);

        Cell setIcon(Bitmap bmp);

        Cell setIconWidth(int width);

        Cell setIconHeight(int height);
    }

    public static class CellImpl implements Cell {
        CharSequence text;
        Layout       layout;
        TextPaint    paint;
        Bitmap       icon;
        int          iconWidth;
        int          iconHeight;
        int          backgroundColor;
        float        scale;
        int          height;

        public CellImpl(int color) {
            paint = new TextPaint();
            paint.setAntiAlias(true);
            paint.setColor(color);
            iconWidth = -1;
            iconHeight = -1;
        }

        @Override
        public Cell setText(CharSequence text) {
            if (!TextUtils.equals(this.text, text)) {
                this.text = text;
                layout = null;
            }
            return this;
        }

        @Override
        public Cell setTextColor(int color) {
            paint.setColor(color);
            return this;
        }

        @Override
        public Cell setBackgroundColor(int color) {
            backgroundColor = color;
            return this;
        }

        @Override
        public Cell setTextSize(float size) {
            paint.setTextSize(size);
            return this;
        }

        @Override
        public Cell setIcon(Bitmap bmp) {
            this.icon = bmp;
            return this;
        }

        @Override
        public Cell setIconWidth(int width) {
            this.iconWidth = width;
            return this;
        }

        @Override
        public Cell setIconHeight(int height) {
            this.iconHeight = height;
            return this;
        }
    }

    public static class CellBitmapHandler implements Downloader.ImageHandler {

        final TextTableView mView;
        final Cell          mCell;

        public CellBitmapHandler(TextTableView v, Cell cell) {
            mView = v;
            mCell = cell;
        }

        @Override
        public void setBitmap(Bitmap bmp) {
            mCell.setIcon(bmp);
            mView.requestLayout();
            mView.invalidate();
        }
    }
}
