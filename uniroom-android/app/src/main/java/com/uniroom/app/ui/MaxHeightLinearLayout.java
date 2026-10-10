package com.uniroom.app.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.uniroom.app.R;

/** A LinearLayout that never grows taller than a share of the screen (used by bottom sheets). */
public class MaxHeightLinearLayout extends LinearLayout {

    private float maxHeightRatio = 0.85f;

    public MaxHeightLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MaxHeightLinearLayout);
        maxHeightRatio = a.getFloat(R.styleable.MaxHeightLinearLayout_maxHeightRatio, maxHeightRatio);
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int max = (int) (getResources().getDisplayMetrics().heightPixels * maxHeightRatio);
        int size = MeasureSpec.getSize(heightMeasureSpec);
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.UNSPECIFIED || size > max) {
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(max, MeasureSpec.AT_MOST);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
