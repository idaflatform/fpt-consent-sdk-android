package vn.fpt.fis.cmp.consent.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/**
 * Xep con theo hang, het cho thi xuong dong (chip truong du lieu / "Chia se voi"). Dat
 * {@link #setColumns} &gt; 0 de chia luoi so cot bang nhau (checkbox o man rong).
 *
 * <p>Tu viet thay cho ChipGroup cua Material: Chip luon tao RippleDrawable, ma ripple lam
 * RenderThread abort tren Android 7.x khi form nam trong PlatformView cua Flutter.</p>
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class CmpFlowLayout extends ViewGroup {

    private int horizontalGap;
    private int verticalGap;
    private int columns;

    public CmpFlowLayout(Context context) {
        this(context, null);
    }

    public CmpFlowLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float density = context.getResources().getDisplayMetrics().density;
        horizontalGap = Math.round(6 * density);
        verticalGap = Math.round(8 * density);
    }

    public void setGaps(int horizontal, int vertical) {
        if (horizontal != horizontalGap || vertical != verticalGap) {
            horizontalGap = horizontal;
            verticalGap = vertical;
            requestLayout();
        }
    }

    /** 0 = xep tu do theo do rong tung con; &gt; 0 = luoi so cot bang nhau. */
    public void setColumns(int columns) {
        if (columns != this.columns) {
            this.columns = columns;
            requestLayout();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int available = widthMode == MeasureSpec.UNSPECIFIED
                ? Integer.MAX_VALUE
                : MeasureSpec.getSize(widthMeasureSpec) - getPaddingLeft() - getPaddingRight();
        int cellWidth = columns > 0 && available != Integer.MAX_VALUE
                ? Math.max(0, (available - horizontalGap * (columns - 1)) / columns)
                : 0;

        int x = 0;
        int rowHeight = 0;
        int height = 0;
        int widest = 0;
        boolean rowHasChild = false;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }
            LayoutParams lp = child.getLayoutParams();
            int childWidthSpec = cellWidth > 0
                    ? MeasureSpec.makeMeasureSpec(cellWidth, MeasureSpec.EXACTLY)
                    : getChildMeasureSpec(widthMeasureSpec,
                            getPaddingLeft() + getPaddingRight(), lp.width);
            int childHeightSpec = getChildMeasureSpec(heightMeasureSpec,
                    getPaddingTop() + getPaddingBottom(), lp.height);
            child.measure(childWidthSpec, childHeightSpec);

            int w = child.getMeasuredWidth();
            if (rowHasChild && x + horizontalGap + w > available) {
                height += rowHeight + verticalGap;
                widest = Math.max(widest, x);
                x = 0;
                rowHeight = 0;
                rowHasChild = false;
            }
            x += (rowHasChild ? horizontalGap : 0) + w;
            rowHeight = Math.max(rowHeight, child.getMeasuredHeight());
            rowHasChild = true;
        }
        height += rowHeight;
        widest = Math.max(widest, x);

        int width = widthMode == MeasureSpec.EXACTLY
                ? MeasureSpec.getSize(widthMeasureSpec)
                : resolveSize(widest + getPaddingLeft() + getPaddingRight(), widthMeasureSpec);
        setMeasuredDimension(width,
                resolveSize(height + getPaddingTop() + getPaddingBottom(), heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int available = r - l - getPaddingLeft() - getPaddingRight();
        boolean rtl = getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        int y = getPaddingTop();
        int i = 0;
        int count = getChildCount();
        while (i < count) {
            // Luot 1: lay cac con cua 1 hang + chieu cao hang.
            int rowEnd = i;
            int x = 0;
            int rowHeight = 0;
            boolean rowHasChild = false;
            while (rowEnd < count) {
                View child = getChildAt(rowEnd);
                if (child.getVisibility() != GONE) {
                    int w = child.getMeasuredWidth();
                    if (rowHasChild && x + horizontalGap + w > available) {
                        break;
                    }
                    x += (rowHasChild ? horizontalGap : 0) + w;
                    rowHeight = Math.max(rowHeight, child.getMeasuredHeight());
                    rowHasChild = true;
                }
                rowEnd++;
            }
            // Luot 2: dat tung con, can giua theo chieu doc trong hang ("Chia se voi:" + chip).
            x = 0;
            boolean first = true;
            for (int j = i; j < rowEnd; j++) {
                View child = getChildAt(j);
                if (child.getVisibility() == GONE) {
                    continue;
                }
                int w = child.getMeasuredWidth();
                int h = child.getMeasuredHeight();
                if (!first) {
                    x += horizontalGap;
                }
                int left = rtl
                        ? r - l - getPaddingRight() - x - w
                        : getPaddingLeft() + x;
                int top = y + (rowHeight - h) / 2;
                child.layout(left, top, left + w, top + h);
                x += w;
                first = false;
            }
            y += rowHeight + verticalGap;
            i = rowEnd;
        }
    }
}
